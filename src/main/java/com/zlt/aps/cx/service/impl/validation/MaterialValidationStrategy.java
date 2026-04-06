package com.zlt.aps.cx.service.impl.validation;

import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
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

        // 获取硫化任务涉及的物料编码
        Set<String> requiredMaterials = lhResults.stream()
                .map(LhScheduleResult::getEmbryoCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (requiredMaterials.isEmpty()) {
            addWarn(result,
                    "硫化任务中缺少胎胚编码(EMBRYO_CODE)",
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

        // 检查缺失的物料
        Set<String> missingMaterials = new HashSet<>(requiredMaterials);
        missingMaterials.removeAll(existingMaterials);

        if (!missingMaterials.isEmpty()) {
            String missingList = String.join(", ", 
                    missingMaterials.size() > 5 
                        ? Arrays.asList(missingMaterials.iterator().next() + "...") 
                        : missingMaterials);
            addError(result,
                    "硫化任务中的" + missingMaterials.size() + "个物料缺少物料信息",
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

        addInfo(result,
                "物料信息：" + materials.size() + "条，覆盖硫化任务物料：" + existingMaterials.size() + "种",
                null);
    }
}
