package com.zlt.aps.cx.service.impl;

import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.cx.entity.CxStock;
import com.zlt.aps.cx.entity.config.CxKeyProduct;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.config.CxStructureShiftCapacity;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.mp.api.domain.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 排程数据完整性校验器
 *
 * 在构建排程上下文时，对查询出的基础数据进行完整性校验
 * 提前发现问题并给出明确的错误提示和解决建议
 *
 * @author APS Team
 */
@Slf4j
@Component
public class ScheduleDataValidator {

    /** 关键参数编码列表（必须存在） */
    private static final Set<String> REQUIRED_PARAM_CODES = new HashSet<>(Arrays.asList(
            "LOSS_RATE",           // 损耗率
            "MAX_TYPES_PER_MACHINE", // 机台种类上限
            "DEFAULT_TRIP_CAPACITY"   // 默认整车容量
    ));

    /** 关键参数默认值映射 */
    private static final Map<String, String> PARAM_DEFAULTS = new HashMap<>();

    static {
        PARAM_DEFAULTS.put("LOSS_RATE", "0.02");
        PARAM_DEFAULTS.put("MAX_TYPES_PER_MACHINE", "4");
        PARAM_DEFAULTS.put("DEFAULT_TRIP_CAPACITY", "200");
    }

    /**
     * 校验排程上下文数据完整性
     *
     * @param context 排程上下文
     * @param scheduleDate 排程日期
     * @param factoryCode 工厂编码
     * @return 校验结果
     */
    public ScheduleDataValidationResult validate(ScheduleContextDTO context,
                                                 LocalDate scheduleDate,
                                                 String factoryCode) {
        ScheduleDataValidationResult result = new ScheduleDataValidationResult();
        log.info("开始校验排程数据完整性，日期：{}，工厂：{}", scheduleDate, factoryCode);

        // 1. 校验班次配置
        validateShiftConfig(context.getShiftConfigList(), context.getScheduleDays(), result);

        // 2. 校验成型机台
        validateMoldingMachines(context.getAvailableMachines(), result);

        // 3. 校验硫化排程结果
        validateLhScheduleResults(context.getLhScheduleResults(), result);

        // 4. 校验物料信息完整性
        validateMaterials(context.getLhScheduleResults(), context.getMaterials(), result);

        // 5. 校验库存信息
        validateStocks(context.getStocks(), result);

        // 6. 校验参数配置
        validateParamConfigs(context.getParamConfigMap(), result);

        // 7. 校验结构班产配置
        validateStructureShiftCapacities(context.getStructureShiftCapacities(), result);

        // 8. 校验关键产品配置
        validateKeyProducts(context.getKeyProducts(), result);

        // 9. 校验结构硫化配比
        validateStructureLhRatios(context.getStructureLhRatioMap(), result);

        // 10. 校验月度计划余量
        validateMonthSurplus(context.getMonthSurplusList(), result);

        // 11. 校验物料日硫化产能
        validateMaterialLhCapacity(context.getMaterialLhCapacityMap(),
                context.getLhMachineCapacityMap(), result);

        // 12. 校验收尾数据
        validateMaterialEndings(context.getMaterialEndings(), result);

        // 生成摘要
        String summary = result.generateSummary();
        log.info("数据完整性校验完成：{}", summary);

        // 输出详细校验结果
        if (!result.isPassed() || result.getWarnCount() > 0) {
            logValidationDetails(result);
        }

        return result;
    }

    /**
     * 校验班次配置
     */
    private void validateShiftConfig(List<CxShiftConfig> shiftConfigs, Integer scheduleDays,
                                     ScheduleDataValidationResult result) {
        String dataItem = "班次配置(T_CX_SHIFT_CONFIG)";

        if (shiftConfigs == null || shiftConfigs.isEmpty()) {
            result.addError(dataItem,
                    "班次配置为空",
                    "请在班次配置表中配置工厂【" + scheduleDays + "】天的班次信息");
            return;
        }

        // 检查排程天数对应的班次配置是否完整
        if (scheduleDays == null || scheduleDays < 1) {
            result.addError(dataItem,
                    "排程天数配置异常：" + scheduleDays,
                    "请检查班次配置表中的SCHEDULE_DAY字段");
            return;
        }

        // 检查每一天的班次配置是否完整
        Map<Integer, Long> dayShiftCount = shiftConfigs.stream()
                .filter(c -> c.getScheduleDay() != null)
                .collect(Collectors.groupingBy(CxShiftConfig::getScheduleDay, Collectors.counting()));

        for (int day = 1; day <= scheduleDays; day++) {
            Long count = dayShiftCount.get(day);
            if (count == null || count == 0) {
                result.addWarn(dataItem,
                        "第" + day + "天缺少班次配置",
                        "请为第" + day + "天配置班次信息");
            } else if (count < 2) {
                result.addInfo(dataItem,
                        "第" + day + "天班次配置数量较少：" + count,
                        "建议至少配置2个班次（早班、中班）或3个班次（早班、中班、夜班）");
            }
        }

        result.addInfo(dataItem,
                "班次配置完整，共" + shiftConfigs.size() + "条记录，覆盖" + scheduleDays + "天",
                null);
    }

    /**
     * 校验成型机台
     */
    private void validateMoldingMachines(List<MdmMoldingMachine> machines,
                                        ScheduleDataValidationResult result) {
        String dataItem = "成型机台(T_MDM_MOLDING_MACHINE)";

        if (machines == null || machines.isEmpty()) {
            result.addError(dataItem,
                    "成型机台数据为空",
                    "请检查成型机台基础数据是否配置");
            return;
        }

        // 检查机台状态
        long activeCount = machines.stream()
                .filter(m -> "1".equals(m.getIsActive()) || m.getIsActive() == null)
                .count();

        if (activeCount == 0) {
            result.addError(dataItem,
                    "没有启用状态的成型机台",
                    "请检查机台IS_ACTIVE字段配置");
        } else if (activeCount < machines.size()) {
            result.addWarn(dataItem,
                    "部分机台未启用，启用：" + activeCount + "，总计：" + machines.size(),
                    "如需启用请修改机台IS_ACTIVE字段");
        }

        result.addInfo(dataItem,
                "成型机台数量：" + machines.size() + "，启用：" + activeCount,
                null);
    }

    /**
     * 校验硫化排程结果
     */
    private void validateLhScheduleResults(List<LhScheduleResult> lhResults,
                                          ScheduleDataValidationResult result) {
        String dataItem = "硫化排程结果(T_LH_SCHEDULE_RESULT)";

        if (lhResults == null || lhResults.isEmpty()) {
            result.addWarn(dataItem,
                    "硫化排程结果为空，今日无硫化任务",
                    "如果今日应有硫化任务，请检查硫化排程是否已执行");
            return;
        }

        // 检查数据完整性
        long validCount = lhResults.stream()
                .filter(r -> r.getEmbryoCode() != null && r.getDayVulcanizationQty() != null)
                .count();

        if (validCount < lhResults.size()) {
            result.addWarn(dataItem,
                    "部分硫化任务数据不完整，有效：" + validCount + "，总计：" + lhResults.size(),
                    "请检查硫化排程结果中的EMBRYO_CODE和DAY_VULCANIZATION_QTY字段");
        }

        // 按物料汇总产能
        Map<String, Integer> capacityByEmbryo = lhResults.stream()
                .filter(r -> r.getEmbryoCode() != null && r.getDayVulcanizationQty() != null)
                .collect(Collectors.groupingBy(LhScheduleResult::getEmbryoCode,
                        Collectors.summingInt(LhScheduleResult::getDayVulcanizationQty)));

        result.addInfo(dataItem,
                "硫化排程任务：" + lhResults.size() + "条，涉及" + capacityByEmbryo.size() + "种物料",
                null);
    }

    /**
     * 校验物料信息完整性
     */
    private void validateMaterials(List<LhScheduleResult> lhResults,
                                   List<MdmMaterialInfo> materials,
                                   ScheduleDataValidationResult result) {
        String dataItem = "物料信息(T_MDM_MATERIAL_INFO)";

        if (lhResults == null || lhResults.isEmpty()) {
            result.addInfo(dataItem, "无硫化任务，无需校验物料信息", null);
            return;
        }

        Set<String> requiredMaterials = lhResults.stream()
                .map(LhScheduleResult::getEmbryoCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (materials == null || materials.isEmpty()) {
            result.addError(dataItem,
                    "硫化任务涉及的物料信息为空",
                    "请检查物料基础数据是否正确配置");
            return;
        }

        Set<String> existingMaterials = materials.stream()
                .map(MdmMaterialInfo::getMaterialCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 检查缺失的物料
        Set<String> missingMaterials = new HashSet<>(requiredMaterials);
        missingMaterials.removeAll(existingMaterials);

        if (!missingMaterials.isEmpty()) {
            result.addError(dataItem,
                    "硫化任务中的" + missingMaterials.size() + "个物料缺少物料信息",
                    "缺少的物料编码：" + String.join(", ", missingMaterials));
        }

        result.addInfo(dataItem,
                "物料信息：" + materials.size() + "条，覆盖硫化任务物料：" + existingMaterials.size() + "种",
                null);
    }

    /**
     * 校验库存信息
     */
    private void validateStocks(List<CxStock> stocks, ScheduleDataValidationResult result) {
        String dataItem = "胎胚库存(T_CX_STOCK)";

        if (stocks == null || stocks.isEmpty()) {
            result.addInfo(dataItem, "当前无有效库存", null);
            return;
        }

        // 统计库存信息
        int totalStock = stocks.stream()
                .filter(s -> s.getStockNum() != null)
                .mapToInt(CxStock::getStockNum)
                .sum();

        long validStockCount = stocks.stream()
                .filter(s -> s.getEffectiveStock() != null && s.getEffectiveStock() > 0)
                .count();

        if (validStockCount == 0) {
            result.addWarn(dataItem,
                    "所有物料库存均为0或负数",
                    "请检查库存数据是否正确");
        }

        result.addInfo(dataItem,
                "有库存物料：" + validStockCount + "种，总库存量：" + totalStock,
                null);
    }

    /**
     * 校验参数配置
     */
    private void validateParamConfigs(Map<String, CxParamConfig> paramConfigMap,
                                     ScheduleDataValidationResult result) {
        String dataItem = "参数配置(T_CX_PARAM_CONFIG)";

        if (paramConfigMap == null || paramConfigMap.isEmpty()) {
            result.addError(dataItem,
                    "参数配置为空",
                    "请配置排程所需的参数（损耗率、机台种类上限等）");
            return;
        }

        // 检查关键参数
        for (String paramCode : REQUIRED_PARAM_CODES) {
            CxParamConfig config = paramConfigMap.get(paramCode);
            if (config == null) {
                String defaultValue = PARAM_DEFAULTS.get(paramCode);
                result.addWarn(dataItem,
                        "缺少关键参数【" + paramCode + "】",
                        "将使用默认值【" + defaultValue + "】，建议配置该参数");
            } else if (config.getParamValue() == null || config.getParamValue().isEmpty()) {
                result.addWarn(dataItem,
                        "参数【" + paramCode + "】的值为空",
                        "请检查参数配置");
            } else {
                // 校验数值格式
                try {
                    new BigDecimal(config.getParamValue());
                } catch (NumberFormatException e) {
                    result.addWarn(dataItem,
                            "参数【" + paramCode + "】的值格式错误：" + config.getParamValue(),
                            "请检查参数配置，确保为数值为空");
                }
            }
        }

        result.addInfo(dataItem,
                "参数配置：" + paramConfigMap.size() + "项",
                null);
    }

    /**
     * 校验结构班产配置
     */
    private void validateStructureShiftCapacities(List<CxStructureShiftCapacity> capacities,
                                                  ScheduleDataValidationResult result) {
        String dataItem = "结构班产配置(T_CX_STRUCTURE_SHIFT_CAPACITY)";

        if (capacities == null || capacities.isEmpty()) {
            result.addWarn(dataItem,
                    "结构班产配置为空，将使用默认产能计算",
                    "建议配置各结构的班次产能以提高排程精度");
            return;
        }

        // 统计配置的结构数量
        Set<String> structures = capacities.stream()
                .filter(c -> c.getStructureName() != null)
                .map(CxStructureShiftCapacity::getStructureName)
                .collect(Collectors.toSet());

        result.addInfo(dataItem,
                "结构班产配置：" + capacities.size() + "条，覆盖" + structures.size() + "种结构",
                null);
    }

    /**
     * 校验关键产品配置
     */
    private void validateKeyProducts(List<CxKeyProduct> keyProducts,
                                    ScheduleDataValidationResult result) {
        String dataItem = "关键产品配置(T_CX_KEY_PRODUCT)";

        if (keyProducts == null || keyProducts.isEmpty()) {
            result.addInfo(dataItem, "未配置关键产品，开产首班将正常排产", null);
        } else {
            Set<String> keyEmbryoCodes = keyProducts.stream()
                    .map(CxKeyProduct::getEmbryoCode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            result.addInfo(dataItem,
                    "关键产品数量：" + keyProducts.size() + "个",
                    null);
        }
    }

    /**
     * 校验结构硫化配比
     */
    private void validateStructureLhRatios(Map<String, MdmStructureLhRatio> ratioMap,
                                          ScheduleDataValidationResult result) {
        String dataItem = "结构硫化配比(T_MDM_STRUCTURE_LH_RATIO)";

        if (ratioMap == null || ratioMap.isEmpty()) {
            result.addWarn(dataItem,
                    "结构硫化配比为空，满算力计算可能不准确",
                    "建议配置各结构的硫化机台配比");
            return;
        }

        // 检查配比数据的完整性
        long validRatioCount = ratioMap.values().stream()
                .filter(r -> r.getLhMachineMaxQty() != null && r.getLhMachineMaxQty() > 0)
                .count();

        if (validRatioCount < ratioMap.size()) {
            result.addWarn(dataItem,
                    "部分结构硫化配比数据不完整，有效：" + validRatioCount + "，总计：" + ratioMap.size(),
                    "请检查LH_MACHINE_MAX_QTY字段配置");
        }

        result.addInfo(dataItem,
                "结构硫化配比：" + ratioMap.size() + "种结构",
                null);
    }

    /**
     * 校验月度计划余量
     */
    private void validateMonthSurplus(List<MdmMonthSurplus> monthSurplusList,
                                      ScheduleDataValidationResult result) {
        String dataItem = "月度计划余量(T_MDM_MONTH_SURPLUS)";

        if (monthSurplusList == null || monthSurplusList.isEmpty()) {
            result.addInfo(dataItem, "月度计划余量为空，收尾计算将无数据支持", null);
        } else {
            result.addInfo(dataItem,
                    "月度计划余量：" + monthSurplusList.size() + "条",
                    null);
        }
    }

    /**
     * 校验物料日硫化产能
     */
    private void validateMaterialLhCapacity(
            Map<String, com.zlt.aps.mp.engine.domain.vo.MonthPlanProductLhCapacityVo> capacityMap,
            Map<String, List<LhMachineCapacityInfo>> machineCapacityMap,
            ScheduleDataValidationResult result) {

        String dataItem = "物料日硫化产能";

        if (capacityMap == null || capacityMap.isEmpty()) {
            result.addWarn(dataItem,
                    "物料日硫化产能为空",
                    "请检查硫化排程结果是否正确");
            return;
        }

        // 检查硫化机台配比覆盖情况
        if (machineCapacityMap != null) {
            Set<String> materialsWithMachines = machineCapacityMap.keySet();
            long totalMachines = machineCapacityMap.values().stream()
                    .mapToLong(List::size)
                    .sum();

            result.addInfo(dataItem,
                    "涉及物料：" + materialsWithMachines.size() + "种，硫化机台：" + totalMachines + "台",
                    null);
        }
    }

    /**
     * 校验收尾数据
     */
    private void validateMaterialEndings(List<com.zlt.aps.cx.entity.CxMaterialEnding> endings,
                                        ScheduleDataValidationResult result) {
        String dataItem = "物料收尾管理(T_CX_MATERIAL_ENDING)";

        if (endings == null || endings.isEmpty()) {
            result.addInfo(dataItem, "无物料收尾数据", null);
            return;
        }

        // 统计紧急收尾和需要调整月计划的数量
        long urgentCount = endings.stream()
                .filter(e -> e.getIsUrgentEnding() != null && e.getIsUrgentEnding() == 1)
                .count();

        long needAdjustCount = endings.stream()
                .filter(e -> e.getNeedMonthPlanAdjust() != null && e.getNeedMonthPlanAdjust() == 1)
                .count();

        if (urgentCount > 0) {
            result.addWarn(dataItem,
                    urgentCount + "个物料需要紧急收尾",
                    "请关注这些物料的生产进度");
        }

        if (needAdjustCount > 0) {
            result.addWarn(dataItem,
                    needAdjustCount + "个物料需要调整月计划",
                    "请联系计划部门调整月计划");
        }

        result.addInfo(dataItem,
                "物料收尾数据：" + endings.size() + "条，紧急：" + urgentCount + "，需调整：" + needAdjustCount,
                null);
    }

    /**
     * 输出校验详情日志
     */
    private void logValidationDetails(ScheduleDataValidationResult result) {
        log.warn("===== 排程数据校验详情 =====");

        for (ScheduleDataValidationResult.ValidationDetail detail : result.getDetails()) {
            String levelStr;
            switch (detail.getLevel()) {
                case ERROR:
                    levelStr = "❌ ERROR";
                    break;
                case WARN:
                    levelStr = "⚠️  WARN";
                    break;
                default:
                    levelStr = "ℹ️  INFO";
            }

            log.warn("{} [{}] {}", levelStr, detail.getDataItem(), detail.getMessage());
            if (detail.getSuggestion() != null) {
                log.warn("    建议：{}", detail.getSuggestion());
            }
        }

        log.warn("==============================");
    }
}
