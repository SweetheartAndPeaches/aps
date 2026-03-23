package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 胎面整车配置对象 T_CX_TREAD_CONFIG
 */
@ApiModel(value = "胎面整车配置对象", description = "胎面整车配置对象")
@Data
@TableName(value = "T_CX_TREAD_CONFIG")
public class CxTreadConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "主键ID", name = "id")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /** 物料编码(胎胚) */
    @ApiModelProperty(value = "物料编码(胎胚)", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 结构 */
    @ApiModelProperty(value = "结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /** 每车胎面生产的胎胚数量(条) */
    @ApiModelProperty(value = "每车胎面生产的胎胚数量(条)", name = "vehicleQuantity")
    @TableField(value = "VEHICLE_QUANTITY")
    private Integer vehicleQuantity;
}
