package com.zlt.aps.cx.service.impl.validation;

import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.cx.entity.config.CxStructureTreadConfig;
import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 硫化排程结果数据完整性校验策略
 *
 * <p>校验 T_LH_SCHEDULE_RESULT 表中的关键字段是否完整：
 * <ul>
 *   <li>MATERIAL_CODE - 物料编号</li>
 *   <li>EMBRYO_CODE - 胎胚代码</li>
 *   <li>STRUCTURE_NAME - 产品结构</li>
 *   <li>MATERIAL_DESC - 物料描述</li>
 *   <li>MAIN_MATERIAL_DESC - 主物料(胎胚描述)</li>
 *   <li>LH_TIME - 硫化时长</li>
 *   <li>MOULD_QTY - 使用模数</li>
 *   <li>SINGLE_MOULD_SHIFT_QTY - 单班硫化量</li>
 *   <li>CONSTRUCTION_STAGE - 施工阶段</li>
 *   <li>EMBRYO_NO - 制造示方书号</li>
 * </ul>
 *
 * <p>校验关联配置完整性：
 * <ul>
 *   <li>硫化任务中的物料必须在 T_MDM_MATERIAL_INFO 中有配置</li>
 *   <li>硫化任务中的结构必须在 T_MP_STRUCTURE_ALLOCATION 中有配置</li>
 *   <li>硫化任务中的结构必须在 T_MDM_STRUCTURE_TREAD_CONFIG 中有配置</li>
 * </ul>
 *
 * @author APS Team
 */
@Slf4j
@Component
public class LhScheduleResultValidationStrategy extends BaseValidationStrategy {

    /** 必填字段列表 */
    private static final Set<String> REQUIRED_FIELDS = new HashSet<>(Arrays.asList(
            "MATERIAL_CODE",      // 物料编号
            "EMBRYO_CODE",        // 胎胚代码
            "STRUCTURE_NAME",     // 产品结构
            "MATERIAL_DESC",      // 物料描述
            "MAIN_MATERIAL_DESC", // 主物料(胎胚描述)
            "LH_TIME",            // 硫化时长
            "MOULD_QTY",          // 使用模数
            "SINGLE_MOULD_SHIFT_QTY", // 单班硫化量
            "CONSTRUCTION_STAGE", // 施工阶段
            "EMBRYO_NO"           // 制造示方书号
    ));

    /** 字段中文名称映射 */
    private static final Map<String, String> FIELD_NAMES = new HashMap<>();
    static {
        FIELD_NAMES.put("MATERIAL_CODE", "物料编号");
        FIELD_NAMES.put("EMBRYO_CODE", "胎胚代码");
        FIELD_NAMES.put("STRUCTURE_NAME", "产品结构");
        FIELD_NAMES.put("MATERIAL_DESC", "物料描述");
        FIELD_NAMES.put("MAIN_MATERIAL_DESC", "主物料(胎胚描述)");
        FIELD_NAMES.put("LH_TIME", "硫化时长");
        FIELD_NAMES.put("MOULD_QTY", "使用模数");
        FIELD_NAMES.put("SINGLE_MOULD_SHIFT_QTY", "单班硫化量");
        FIELD_NAMES.put("CONSTRUCTION_STAGE", "施工阶段");
        FIELD_NAMES.put("EMBRYO_NO", "制造示方书号");
    }

    @Override
    public ValidationItem getValidationItem() {
        return ValidationItem.LH_SCHEDULE_RESULT;
    }

    @Override
    public int getOrder() {
        return 10; // 优先级较高，在其他校验之前执行
    }

    @Override
    public void validate(ScheduleContextVo context, LocalDate scheduleDate, String factoryCode,
                        ScheduleDataValidationResult result) {

        List<LhScheduleResult> lhResults = context.getLhScheduleResults();

        if (lhResults == null || lhResults.isEmpty()) {
            addWarn(result,
                    "硫化排程结果为空，无可排产任务",
                    "请先执行硫化排程或检查排程日期是否正确");
            return;
        }

        int totalCount = lhResults.size();
        log.info("开始校验硫化排程结果数据完整性，共 {} 条记录，需校验 {} 个必填字段", totalCount, REQUIRED_FIELDS.size());

        // ==================== 1. 校验必填字段 ====================
        validateRequiredFields(lhResults, totalCount, result);

        // ==================== 2. 校验物料信息配置完整性 ====================
        validateMaterialInfoConfig(context, lhResults, result);

        // ==================== 3. 校验结构排产配置完整性 ====================
        validateStructureAllocationConfig(context, lhResults, result);

        // ==================== 4. 校验结构整车配置完整性 ====================
        validateStructureTreadConfig(context, lhResults, result);
    }

    /**
     * 校验必填字段
     */
    private void validateRequiredFields(List<LhScheduleResult> lhResults, int totalCount,
                                       ScheduleDataValidationResult result) {
        // 统计各字段缺失情况
        Map<String, Integer> missingCountMap = new HashMap<>();
        Map<String, List<String>> missingSampleMap = new HashMap<>();
        for (String field : REQUIRED_FIELDS) {
            missingCountMap.put(field, 0);
            missingSampleMap.put(field, new ArrayList<>());
        }

        // 遍历每条记录，检查所有必填字段
        for (LhScheduleResult r : lhResults) {
            String materialCode = r.getMaterialCode();
            if (materialCode == null) {
                materialCode = "未知物料";
            }

            // 检查 MATERIAL_CODE
            checkField(r.getMaterialCode(), "MATERIAL_CODE", materialCode, missingCountMap, missingSampleMap);

            // 检查 EMBRYO_CODE
            checkField(r.getEmbryoCode(), "EMBRYO_CODE", materialCode, missingCountMap, missingSampleMap);

            // 检查 STRUCTURE_NAME
            checkField(r.getStructureName(), "STRUCTURE_NAME", materialCode, missingCountMap, missingSampleMap);

            // 检查 MATERIAL_DESC
            checkField(r.getMaterialDesc(), "MATERIAL_DESC", materialCode, missingCountMap, missingSampleMap);

            // 检查 MAIN_MATERIAL_DESC
            checkField(r.getMainMaterialDesc(), "MAIN_MATERIAL_DESC", materialCode, missingCountMap, missingSampleMap);

            // 检查 LH_TIME (数值类型，null或<=0视为缺失)
            checkNumericField(r.getLhTime(), "LH_TIME", materialCode, missingCountMap, missingSampleMap);

            // 检查 MOULD_QTY (数值类型，null或<=0视为缺失)
            checkNumericField(r.getMouldQty(), "MOULD_QTY", materialCode, missingCountMap, missingSampleMap);

            // 检查 SINGLE_MOULD_SHIFT_QTY (数值类型，null或<=0视为缺失)
            checkNumericField(r.getSingleMouldShiftQty(), "SINGLE_MOULD_SHIFT_QTY", materialCode, missingCountMap, missingSampleMap);

            // 检查 CONSTRUCTION_STAGE
            checkField(r.getConstructionStage(), "CONSTRUCTION_STAGE", materialCode, missingCountMap, missingSampleMap);

            // 检查 EMBRYO_NO
            checkField(r.getEmbryoNo(), "EMBRYO_NO", materialCode, missingCountMap, missingSampleMap);
        }

        // 汇总校验结果
        boolean hasError = false;
        List<String> errorMessages = new ArrayList<>();

        for (String field : REQUIRED_FIELDS) {
            int missingCount = missingCountMap.get(field);
            String fieldName = FIELD_NAMES.get(field);

            if (missingCount > 0) {
                hasError = true;
                List<String> samples = missingSampleMap.get(field);
                String sampleList = samples.isEmpty() ? "无" : String.join(", ", samples);
                String message = String.format(
                        "硫化排程结果中有 %d 条记录缺少【%s】(%s)，示例物料：%s",
                        missingCount, fieldName, field, sampleList);
                errorMessages.add(message);
                addError(result, message, getFixSuggestion(field));
            }
        }

        // 数据完整，校验通过
        if (!hasError) {
            addInfo(result,
                    String.format("硫化排程结果数据完整，共 %d 条记录，10个必填字段均已配置", totalCount),
                    null);
        } else {
            // 添加汇总信息
            long missingFields = errorMessages.size();
            long totalMissing = missingCountMap.values().stream().mapToInt(Integer::intValue).sum();
            addInfo(result,
                    String.format("校验完成：%d个字段缺失，共%d条记录受影响", missingFields, totalMissing),
                    "请根据上述错误提示补充完整数据");
        }

        log.info("硫化排程结果必填字段校验完成：总数={}, 缺失字段数={}, 受影响记录={}",
                totalCount, errorMessages.size(),
                missingCountMap.values().stream().mapToInt(Integer::intValue).sum());
    }

    /**
     * 校验物料信息配置完整性
     * 硫化任务中的物料必须在 T_MDM_MATERIAL_INFO 中有配置
     */
    private void validateMaterialInfoConfig(ScheduleContextVo context, List<LhScheduleResult> lhResults,
                                           ScheduleDataValidationResult result) {
        // 获取已配置的物料信息
        List<MdmMaterialInfo> materials = context.getMaterials();

        if (materials == null || materials.isEmpty()) {
            addError(result,
                    "物料信息为空（T_MDM_MATERIAL_INFO）",
                    "请检查 T_MDM_MATERIAL_INFO 表中是否存在物料数据");
            return;
        }

        // 构建物料编码集合
        Set<String> configuredMaterialCodes = materials.stream()
                .map(MdmMaterialInfo::getMaterialCode)
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toSet());

        // 获取硫化任务中出现的所有物料编码
        Set<String> lhMaterialCodes = lhResults.stream()
                .map(LhScheduleResult::getMaterialCode)
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toSet());

        // 找出缺失配置的物料
        List<String> missingMaterials = new ArrayList<>();
        for (String materialCode : lhMaterialCodes) {
            if (!configuredMaterialCodes.contains(materialCode)) {
                missingMaterials.add(materialCode);
            }
        }

        if (!missingMaterials.isEmpty()) {
            String message = String.format(
                    "硫化排程结果中有 %d 个物料在【T_MDM_MATERIAL_INFO】表中没有配置：%s",
                    missingMaterials.size(), String.join(", ", missingMaterials));
            addError(result, message,
                    "请在 T_MDM_MATERIAL_INFO 表中为这些物料添加配置（MATERIAL_CODE、EMBRYO_CODE、STRUCTURE_NAME等）");
        } else {
            addInfo(result,
                    String.format("物料信息配置完整，共 %d 条物料配置，硫化任务中的 %d 个物料均有配置",
                            configuredMaterialCodes.size(), lhMaterialCodes.size()),
                    null);
        }

        log.info("物料信息配置校验完成：配置物料数={}, 硫化任务物料数={}, 缺失数={}",
                configuredMaterialCodes.size(), lhMaterialCodes.size(), missingMaterials.size());
    }

    /**
     * 校验结构排产配置完整性
     * 硫化任务中的结构必须在 T_MP_STRUCTURE_ALLOCATION 中有配置
     */
    private void validateStructureAllocationConfig(ScheduleContextVo context, List<LhScheduleResult> lhResults,
                                                  ScheduleDataValidationResult result) {
        // 获取已配置的结构集合
        Map<String, List<MpCxCapacityConfiguration>> structureAllocationMap = context.getStructureAllocationMap();

        if (structureAllocationMap == null || structureAllocationMap.isEmpty()) {
            addError(result,
                    "结构排产配置为空（T_MP_STRUCTURE_ALLOCATION）",
                    "请检查 T_MP_STRUCTURE_ALLOCATION 表中是否存在当月的结构排产配置");
            return;
        }

        // 获取硫化任务中出现的所有结构
        Set<String> lhStructures = lhResults.stream()
                .map(LhScheduleResult::getStructureName)
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toSet());

        // 找出缺失配置的结构
        List<String> missingStructures = new ArrayList<>();
        for (String structure : lhStructures) {
            if (!structureAllocationMap.containsKey(structure)) {
                missingStructures.add(structure);
            }
        }

        if (!missingStructures.isEmpty()) {
            String message = String.format(
                    "硫化排程结果中有 %d 个结构在【T_MP_STRUCTURE_ALLOCATION】表中没有配置：%s",
                    missingStructures.size(), String.join(", ", missingStructures));
            addError(result, message,
                    "请在 T_MP_STRUCTURE_ALLOCATION 表中为这些结构添加配置（STRUCTURE_NAME、FACTORY_CODE、YEAR、MONTH）");
        } else {
            addInfo(result,
                    String.format("结构排产配置完整，共 %d 个结构，硫化任务中的 %d 个结构均有配置",
                            structureAllocationMap.size(), lhStructures.size()),
                    null);
        }

        log.info("结构排产配置校验完成：配置结构数={}, 硫化任务结构数={}, 缺失数={}",
                structureAllocationMap.size(), lhStructures.size(), missingStructures.size());
    }

    /**
     * 校验结构整车配置完整性
     * 硫化任务中的结构必须在 T_MDM_STRUCTURE_TREAD_CONFIG 中有配置
     */
    private void validateStructureTreadConfig(ScheduleContextVo context, List<LhScheduleResult> lhResults,
                                             ScheduleDataValidationResult result) {
        // 获取已配置的结构集合
        List<CxStructureTreadConfig> structureTreadConfigs = context.getStructureTreadConfigs();

        if (structureTreadConfigs == null || structureTreadConfigs.isEmpty()) {
            addError(result,
                    "结构整车配置为空（T_MDM_STRUCTURE_TREAD_CONFIG）",
                    "请检查 T_MDM_STRUCTURE_TREAD_CONFIG 表中是否存在结构配置");
            return;
        }

        // 构建结构名称集合（支持STRUCTURE_CODE和STRUCTURE_NAME两种方式匹配）
        Set<String> configuredStructures = structureTreadConfigs.stream()
                .map(CxStructureTreadConfig::getStructureCode)
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toSet());

        // 获取硫化任务中出现的所有结构
        Set<String> lhStructures = lhResults.stream()
                .map(LhScheduleResult::getStructureName)
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toSet());

        // 找出缺失配置的结构
        List<String> missingStructures = new ArrayList<>();
        for (String structure : lhStructures) {
            // 结构名称与结构编码匹配
            if (!configuredStructures.contains(structure)) {
                missingStructures.add(structure);
            }
        }

        if (!missingStructures.isEmpty()) {
            String message = String.format(
                    "硫化排程结果中有 %d 个结构在【T_MDM_STRUCTURE_TREAD_CONFIG】表中没有配置：%s",
                    missingStructures.size(), String.join(", ", missingStructures));
            addError(result, message,
                    "请在 T_MDM_STRUCTURE_TREAD_CONFIG 表中为这些结构添加配置（STRUCTURE_CODE、TREAD_COUNT）");
        } else {
            addInfo(result,
                    String.format("结构整车配置完整，共 %d 个配置，硫化任务中的 %d 个结构均有配置",
                            configuredStructures.size(), lhStructures.size()),
                    null);
        }

        log.info("结构整车配置校验完成：配置结构数={}, 硫化任务结构数={}, 缺失数={}",
                configuredStructures.size(), lhStructures.size(), missingStructures.size());
    }

    /**
     * 检查字符串字段是否为空
     */
    private void checkField(String value, String field, String materialCode,
                           Map<String, Integer> missingCountMap,
                           Map<String, List<String>> missingSampleMap) {
        if (value == null || value.trim().isEmpty()) {
            missingCountMap.merge(field, 1, Integer::sum);
            List<String> samples = missingSampleMap.get(field);
            if (samples.size() < 5) {
                samples.add(materialCode);
            }
        }
    }

    /**
     * 检查数值字段是否为null或<=0
     */
    private void checkNumericField(Integer value, String field, String materialCode,
                                  Map<String, Integer> missingCountMap,
                                  Map<String, List<String>> missingSampleMap) {
        if (value == null || value <= 0) {
            missingCountMap.merge(field, 1, Integer::sum);
            List<String> samples = missingSampleMap.get(field);
            if (samples.size() < 5) {
                samples.add(materialCode + "(当前值:" + value + ")");
            }
        }
    }

    /**
     * 获取字段修复建议
     */
    private String getFixSuggestion(String field) {
        switch (field) {
            case "MATERIAL_CODE":
                return "请检查硫化排程数据中是否存在有效的物料编号";
            case "EMBRYO_CODE":
                return "请从T_MDM_MATERIAL_INFO表根据MATERIAL_CODE同步EMBRYO_CODE";
            case "STRUCTURE_NAME":
                return "请从T_MDM_MATERIAL_INFO表根据MATERIAL_CODE同步STRUCTURE_NAME";
            case "MATERIAL_DESC":
                return "请从T_MDM_MATERIAL_INFO表根据MATERIAL_CODE同步MATERIAL_DESC";
            case "MAIN_MATERIAL_DESC":
                return "请从T_MDM_MATERIAL_INFO表根据MATERIAL_CODE同步MAIN_MATERIAL_DESC";
            case "LH_TIME":
                return "请检查硫化排程数据中硫化时长(LH_TIME)是否正确配置";
            case "MOULD_QTY":
                return "请检查硫化排程数据中使用模数(MOULD_QTY)是否正确配置";
            case "SINGLE_MOULD_SHIFT_QTY":
                return "请检查硫化排程数据中单班硫化量(SINGLE_MOULD_SHIFT_QTY)是否正确配置";
            case "CONSTRUCTION_STAGE":
                return "请检查硫化排程数据中施工阶段(CONSTRUCTION_STAGE)是否正确配置(00/01/02/03)";
            case "EMBRYO_NO":
                return "请从T_MDM_MATERIAL_INFO表根据MATERIAL_CODE同步EMBRYO_NO";
            default:
                return "请检查该字段数据是否正确配置";
        }
    }
}
