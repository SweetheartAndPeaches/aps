package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cx.entity.AlertConfig;
import com.zlt.aps.cx.mapper.AlertConfigMapper;
import com.zlt.aps.cx.service.AlertConfigService;
import org.springframework.stereotype.Service;

/**
 * 预警配置Service实现类
 *
 * @author APS Team
 */
@Service
public class AlertConfigServiceImpl extends ServiceImpl<AlertConfigMapper, AlertConfig> implements AlertConfigService {

    @Override
    public String getConfigValue(String configCode) {
        LambdaQueryWrapper<AlertConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertConfig::getConfigCode, configCode)
                .eq(AlertConfig::getIsActive, 1);
        AlertConfig config = getOne(wrapper);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public Integer getConfigValueAsInt(String configCode) {
        String value = getConfigValue(configCode);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public Double getConfigValueAsDouble(String configCode) {
        String value = getConfigValue(configCode);
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
