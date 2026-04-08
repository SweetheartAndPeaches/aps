package com.zlt.aps.cx.service.impl.validation;

import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.vo.ScheduleContextVo;
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
 *   <li>EMBRYO_CODE - 胎胚编码</li>
 *   <li>STRUCTURE_NAME - 产品结构名称</li>
 * </ul>
 *
 * @author APS Team
 */
@Slf4j
@Component
public class LhScheduleResultValidationStrategy extends BaseValidationStrategy {

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
        log.info("开始校验硫化排程结果数据完整性，共 {} 条记录", totalCount);

        // 统计各字段缺失情况
        int missingEmbryoCodeCount = 0;
        int missingStructureNameCount = 0;
        List<String> missingEmbryoCodeSamples = new ArrayList<>();
        List<String> missingStructureNameSamples = new ArrayList<>();

        for (LhScheduleResult r : lhResults) {
            String materialCode = r.getMaterialCode();

            // 检查 EMBRYO_CODE
            if (r.getEmbryoCode() == null || r.getEmbryoCode().trim().isEmpty()) {
                missingEmbryoCodeCount++;
                if (missingEmbryoCodeSamples.size() < 5) {
                    missingEmbryoCodeSamples.add(materialCode);
                }
            }

            // 检查 STRUCTURE_NAME
            if (r.getStructureName() == null || r.getStructureName().trim().isEmpty()) {
                missingStructureNameCount++;
                if (missingStructureNameSamples.size() < 5) {
                    missingStructureNameSamples.add(materialCode);
                }
            }
        }

        // 汇总校验结果
        boolean hasError = false;

        // EMBRYO_CODE 缺失
        if (missingEmbryoCodeCount > 0) {
            hasError = true;
            String sampleList = String.join(", ", missingEmbryoCodeSamples);
            addError(result,
                    "硫化排程结果中有 " + missingEmbryoCodeCount + " 条记录缺少胎胚编码(EMBRYO_CODE)，示例物料：" + sampleList,
                    "请执行以下SQL补充数据：\n" +
                    "-- 根据物料编码和物料描述从 T_MDM_MATERIAL_INFO 刷 EMBRYO_CODE\n" +
                    "UPDATE T_LH_SCHEDULE_RESULT lh\n" +
                    "SET lh.EMBRYO_CODE = (\n" +
                    "    SELECT mi.EMBRYO_CODE\n" +
                    "    FROM T_MDM_MATERIAL_INFO mi\n" +
                    "    WHERE mi.MATERIAL_CODE = lh.MATERIAL_CODE\n" +
                    "    AND mi.SPEC_DESC = lh.SPEC_DESC\n" +
                    "    AND ROWNUM = 1\n" +
                    ")\n" +
                    "WHERE lh.EMBRYO_CODE IS NULL;");
        }

        // STRUCTURE_NAME 缺失
        if (missingStructureNameCount > 0) {
            hasError = true;
            String sampleList = String.join(", ", missingStructureNameSamples);
            addError(result,
                    "硫化排程结果中有 " + missingStructureNameCount + " 条记录缺少产品结构(STRUCTURE_NAME)，示例物料：" + sampleList,
                    "请执行以下SQL补充数据：\n" +
                    "-- 根据物料编码和物料描述从 T_MDM_MATERIAL_INFO 刷 STRUCTURE_NAME\n" +
                    "UPDATE T_LH_SCHEDULE_RESULT lh\n" +
                    "SET lh.STRUCTURE_NAME = (\n" +
                    "    SELECT mi.STRUCTURE_NAME\n" +
                    "    FROM T_MDM_MATERIAL_INFO mi\n" +
                    "    WHERE mi.MATERIAL_CODE = lh.MATERIAL_CODE\n" +
                    "    AND mi.SPEC_DESC = lh.SPEC_DESC\n" +
                    "    AND ROWNUM = 1\n" +
                    ")\n" +
                    "WHERE lh.STRUCTURE_NAME IS NULL;");
        }

        // 同时缺失 EMBRYO_CODE 和 STRUCTURE_NAME（一次性修复）
        if (missingEmbryoCodeCount > 0 && missingStructureNameCount > 0) {
            addInfo(result,
                    "可执行以下SQL一次性补充 EMBRYO_CODE 和 STRUCTURE_NAME：\n" +
                    "UPDATE T_LH_SCHEDULE_RESULT lh\n" +
                    "SET (\n" +
                    "    lh.EMBRYO_CODE,\n" +
                    "    lh.STRUCTURE_NAME\n" +
                    ") = (\n" +
                    "    SELECT mi.EMBRYO_CODE, mi.STRUCTURE_NAME\n" +
                    "    FROM T_MDM_MATERIAL_INFO mi\n" +
                    "    WHERE mi.MATERIAL_CODE = lh.MATERIAL_CODE\n" +
                    "    AND mi.SPEC_DESC = lh.SPEC_DESC\n" +
                    "    AND ROWNUM = 1\n" +
                    ")\n" +
                    "WHERE lh.EMBRYO_CODE IS NULL OR lh.STRUCTURE_NAME IS NULL;",
                    null);
        }

        // 数据完整，校验通过
        if (!hasError) {
            addInfo(result,
                    "硫化排程结果数据完整，共 " + totalCount + " 条记录，EMBRYO_CODE和STRUCTURE_NAME均已配置",
                    null);
        }

        log.info("硫化排程结果数据完整性校验完成：总数={}, 缺失EMBRYO_CODE={}, 缺失STRUCTURE_NAME={}",
                totalCount, missingEmbryoCodeCount, missingStructureNameCount);
    }
}
