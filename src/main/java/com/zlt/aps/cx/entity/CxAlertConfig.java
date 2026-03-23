package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预警配置实体
 *
 * @author APS Team
 */
@Data
@TableName("cx_alert_config")
public class CxAlertConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预警类型 */
    private String alertType;

    /** 预警名称 */
    private String alertName;

    /** 预警阈值 */
    private BigDecimal thresholdValue;

    /** 预警级别 */
    private String alertLevel;

    /** 是否启用 */
    private Integer isEnabled;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
