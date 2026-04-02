package com.zlt.aps.cx.service.impl.validation;

import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 参数配置校验策略
 *
 * @author APS Team
 */
@Slf4j
@Component
public class ParamConfigValidationStrategy extends BaseValidationStrategy {

    /** 关键参数编码列表（必须存在） */
    private static final Set<String> REQUIRED_PARAM_CODES = new HashSet<>(Arrays.asList(
            "LOSS_RATE",              // 损耗率
            "MAX_TYPES_PER_MACHINE",  // 机台种类上限
            "DEFAULT_TRIP_CAPACITY"   // 默认整车容量
    ));

    /** 关键参数默认值映射 */
    private static final Map<String, String> PARAM_DEFAULTS = new HashMap<>();

    /** 关键参数说明 */
    private static final Map<String, String> PARAM_DESCRIPTIONS = new HashMap<>();

    static {
        PARAM_DEFAULTS.put("LOSS_RATE", "0.02");
        PARAM_DEFAULTS.put("MAX_TYPES_PER_MACHINE", "4");
        PARAM_DEFAULTS.put("DEFAULT_TRIP_CAPACITY", "200");
        
        PARAM_DESCRIPTIONS.put("LOSS_RATE", "损耗率，用于计算实际产能");
        PARAM_DESCRIPTIONS.put("MAX_TYPES_PER_MACHINE", "单个机台最多生产的物料种类数");
        PARAM_DESCRIPTIONS.put("DEFAULT_TRIP_CAPACITY", "默认整车容量（条/班次）");
    }

    @Override
    public ValidationItem getValidationItem() {
        return ValidationItem.PARAM_CONFIG;
    }

    @Override
    public void validate(ScheduleContextVo context, LocalDate scheduleDate, String factoryCode,
                        ScheduleDataValidationResult result) {
        
        Map<String, CxParamConfig> paramConfigMap = context.getParamConfigMap();

        if (paramConfigMap == null || paramConfigMap.isEmpty()) {
            addError(result,
                    "参数配置为空，无法获取排程所需参数",
                    "请在参数配置表(T_CX_PARAM_CONFIG)中配置排程参数");
            return;
        }

        // 检查关键参数
        for (String paramCode : REQUIRED_PARAM_CODES) {
            CxParamConfig config = paramConfigMap.get(paramCode);
            String description = PARAM_DESCRIPTIONS.get(paramCode);
            String defaultValue = PARAM_DEFAULTS.get(paramCode);

            if (config == null) {
                addWarn(result,
                        "缺少关键参数【" + paramCode + "】",
                        "将使用默认值【" + defaultValue + "】，" + description);
            } else if (config.getParamValue() == null || config.getParamValue().trim().isEmpty()) {
                addWarn(result,
                        "参数【" + paramCode + "】的值为空",
                        "将使用默认值【" + defaultValue + "】，请检查参数配置");
            } else {
                // 校验数值格式
                try {
                    new BigDecimal(config.getParamValue());
                    addInfo(result,
                            "参数【" + paramCode + "】已配置，值：" + config.getParamValue(),
                            null);
                } catch (NumberFormatException e) {
                    addWarn(result,
                            "参数【" + paramCode + "】的值格式错误：" + config.getParamValue(),
                            "请确保参数为数数值，当前将使用默认值【" + defaultValue + "】");
                }
            }
        }

        addInfo(result,
                "参数配置总数：" + paramConfigMap.size() + "项",
                null);
    }
}
