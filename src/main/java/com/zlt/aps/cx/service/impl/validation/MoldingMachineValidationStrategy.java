package com.zlt.aps.cx.service.impl.validation;

import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mdm.api.domain.entity.MdmMoldingMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 成型机台校验策略
 *
 * @author APS Team
 */
@Slf4j
@Component
public class MoldingMachineValidationStrategy extends BaseValidationStrategy {

    @Override
    public ValidationItem getValidationItem() {
        return ValidationItem.MOLDING_MACHINE;
    }

    @Override
    public void validate(ScheduleContextVo context, LocalDate scheduleDate, String factoryCode,
                        ScheduleDataValidationResult result) {
        
        List<MdmMoldingMachine> machines = context.getAvailableMachines();

        if (isEmpty(machines)) {
            addError(result,
                    "成型机台数据为空，无可用机台进行排程",
                    "请检查成型机台基础数据(T_MDM_MOLDING_MACHINE)是否正确配置");
            return;
        }

        // 检查机台状态（IS_ACTIVE是整数，1表示启用）
        long activeCount = machines.stream()
                .filter(m -> m.getIsActive() != null && m.getIsActive() == 1)
                .count();

        if (activeCount == 0) {
            addError(result,
                    "没有启用状态的成型机台，无法进行排程",
                    "请检查机台IS_ACTIVE字段，至少需要1台启用状态的机台");
        } else if (activeCount < machines.size()) {
            addWarn(result,
                    "部分机台未启用，启用：" + activeCount + "，总计：" + machines.size(),
                    "如需启用请修改机台IS_ACTIVE字段为'1'");
        }

        // 检查关键字段
        long validCount = machines.stream()
                .filter(m -> m.getCxMachineCode() != null)
                .count();

        if (validCount < machines.size()) {
            addWarn(result,
                    "部分机台缺少机台编码，有效：" + validCount + "，总计：" + machines.size(),
                    "请检查机台CX_MACHINE_CODE字段");
        }

        addInfo(result,
                "成型机台数量：" + machines.size() + "，启用：" + activeCount,
                null);
    }
}
