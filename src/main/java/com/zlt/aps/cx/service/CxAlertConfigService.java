package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.cx.entity.CxAlertConfig;

/**
 * 预警配置Service接口
 *
 * @author APS Team
 */
public interface CxAlertConfigService extends IService<CxAlertConfig> {

    /**
     * 根据配置编码获取配置值
     */
    String getConfigValue(String configCode);

    /**
     * 根据配置编码获取配置(数字类型)
     */
    Integer getConfigValueAsInt(String configCode);

    /**
     * 根据配置编码获取配置(数字类型)
     */
    Double getConfigValueAsDouble(String configCode);
}
