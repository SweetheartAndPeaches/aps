package com.zlt.aps.cx.service.impl.validation;

import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 物料信息校验策略
 *
 * @author APS Team
 */
@Slf4j
@Component
public class MaterialValidationStrategy extends BaseValidationStrategy {

    @Override
    public ValidationItem getValidationItem() {
        return ValidationItem.MATERIAL_INFO;
    }

    @Override
    public void validate(ScheduleContextVo context, LocalDate scheduleDate, String factoryCode,
                        ScheduleDataValidationResult result) {
        
        List<LhScheduleResult> lhResults = context.getLhScheduleResults();
        List<MdmMaterialInfo> materials = context.getMaterials();

        if (isEmpty(lhResults)) {
            addInfo(result, "无硫化排程任务，无需校验物料信息", null);
            return;
        }

        // 获取硫化任务涉及的物料编码（硫化任务的 MATERIAL_CODE 应该对应物料信息的 MATERIAL_CODE）
        Set<String> requiredMaterials = lhResults.stream()
                .map(LhScheduleResult::getMaterialCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 同时也检查 EMBRYO_CODE
        Set<String> requiredEmbryoCodes = lhResults.stream()
                .map(LhScheduleResult::getEmbryoCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (requiredMaterials.isEmpty() && requiredEmbryoCodes.isEmpty()) {
            addWarn(result,
                    "硫化任务中缺少物料编码(MATERIAL_CODE)和胎胚编码(EMBRYO_CODE)",
                    "请检查硫化排程结果数据");
            return;
        }

        if (isEmpty(materials)) {
            addError(result,
                    "硫化任务涉及的物料信息为空，无法获取物料基础数据",
                    "请检查物料基础数据(T_MDM_MATERIAL_INFO)是否正确配置");
            return;
        }

        Set<String> existingMaterials = materials.stream()
                .map(MdmMaterialInfo::getMaterialCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 物料信息表中也可能有 EMBRYO_CODE
        Set<String> existingEmbryoCodes = materials.stream()
                .map(MdmMaterialInfo::getEmbryoCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 检查缺失的物料（同时检查 MATERIAL_CODE 和 EMBRYO_CODE）
        Set<String> missingByMaterialCode = new HashSet<>(requiredMaterials);
        missingByMaterialCode.removeAll(existingMaterials);

        Set<String> missingByEmbryoCode = new HashSet<>(requiredEmbryoCodes);
        missingByEmbryoCode.removeAll(existingEmbryoCodes);

        // 统计缺失的物料数量
        Set<String> allMissing = new HashSet<>();
        allMissing.addAll(missingByMaterialCode);
        allMissing.addAll(missingByEmbryoCode);

        if (!allMissing.isEmpty()) {
            String missingList = String.join(", ",
                    allMissing.size() > 5
                        ? Arrays.asList(allMissing.iterator().next() + "...")
                        : allMissing);
            addError(result,
                    "硫化任务中的" + allMissing.size() + "个物料缺少物料信息（按MATERIAL_CODE缺失" + missingByMaterialCode.size() + "个，按EMBRYO_CODE缺失" + missingByEmbryoCode.size() + "个）",
                    "请为以下物料配置基础数据：" + missingList);
        }

        // 检查物料数据完整性
        Set<String> materialsWithoutCode = materials.stream()
                .filter(m -> m.getMaterialCode() == null || m.getMaterialCode().isEmpty())
                .map(m -> m.getMaterialCode())
                .collect(Collectors.toSet());

        if (!materialsWithoutCode.isEmpty()) {
            addWarn(result,
                    materialsWithoutCode.size() + "个物料缺少物料编码",
                    "请检查物料基础数据");
        }

        int coveredCount = requiredMaterials.size() - missingByMaterialCode.size() + requiredEmbryoCodes.size() - missingByEmbryoCode.size();
        addInfo(result,
                "物料信息：" + materials.size() + "条，覆盖硫化任务物料：" + coveredCount + "种",
                null);
    }
}
