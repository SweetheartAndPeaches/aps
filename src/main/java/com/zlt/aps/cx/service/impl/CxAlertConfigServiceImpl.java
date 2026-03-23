package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cx.entity.CxAlertConfig;
import com.zlt.aps.cx.mapper.CxAlertConfigMapper;
import com.zlt.aps.cx.service.CxAlertConfigService;
import org.springframework.stereotype.Service;

/**
 * 预警配置Service实现类
 *
 * @author APS Team
 */
@Service
public class CxAlertConfigServiceImpl extends ServiceImpl<CxAlertConfigMapper, CxAlertConfig> implements CxAlertConfigService {

    @Override
    public String getConfigValue(String configCode) {
        LambdaQueryWrapper<CxAlertConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxAlertConfig::getConfigCode, configCode)
                .eq(CxAlertConfig::getIsActive, 1);
        CxAlertConfig config = getOne(wrapper);
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
