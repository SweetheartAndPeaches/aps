package com.zlt.aps.cx.service.impl.validation;

import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 班次配置校验策略
 *
 * @author APS Team
 */
@Slf4j
@Component
public class ShiftConfigValidationStrategy extends BaseValidationStrategy {

    @Override
    public ValidationItem getValidationItem() {
        return ValidationItem.SHIFT_CONFIG;
    }

    @Override
    public void validate(ScheduleContextVo context, LocalDate scheduleDate, String factoryCode,
                        ScheduleDataValidationResult result) {
        
        List<CxShiftConfig> shiftConfigs = context.getShiftConfigList();
        Integer scheduleDays = context.getScheduleDays();

        if (isEmpty(shiftConfigs)) {
            addError(result,
                    "班次配置为空，无法确定排程班次",
                    "请在班次配置表(T_CX_SHIFT_CONFIG)中配置工厂【" + factoryCode + "】的班次信息");
            return;
        }

        // 检查排程天数
        if (scheduleDays == null || scheduleDays < 1) {
            addError(result,
                    "排程天数配置异常：" + scheduleDays,
                    "请检查班次配置表中的SCHEDULE_DAY字段");
            return;
        }

        // 检查每一天的班次配置
        Map<Integer, Long> dayShiftCount = shiftConfigs.stream()
                .filter(c -> c.getScheduleDay() != null)
                .collect(Collectors.groupingBy(CxShiftConfig::getScheduleDay, Collectors.counting()));

        for (int day = 1; day <= scheduleDays; day++) {
            Long count = dayShiftCount.get(day);
            if (count == null || count == 0) {
                addWarn(result,
                        "第" + day + "天缺少班次配置",
                        "请为第" + day + "天配置班次信息（早班、中班、夜班）");
            } else if (count < 2) {
                addInfo(result,
                        "第" + day + "天班次配置数量较少：" + count + "个",
                        "建议至少配置2个班次");
            }
        }

        addInfo(result,
                "班次配置完整，共" + shiftConfigs.size() + "条记录，覆盖" + scheduleDays + "天",
                null);
    }
}
