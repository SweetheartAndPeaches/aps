package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 胎面停放时间配置实体
 *
 * @author APS Team
 */
@Data
@TableName("cx_tread_parking_config")
public class CxTreadParkingConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 结构编码 */
    private String structureCode;

    /** 结构名称 */
    private String structureName;

    /** 最小停放时间（小时） */
    private Integer minParkingHours;

    /** 最大停放时间（小时） */
    private Integer maxParkingHours;

    /** 是否启用 */
    private Integer isEnabled;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
