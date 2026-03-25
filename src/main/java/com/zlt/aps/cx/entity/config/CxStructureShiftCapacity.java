package com.zlt.aps.cx.entity.config;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 结构班产配置实体
 * 
 * 定义每个结构在每台成型机上的整车条数
 * 整车条数按结构不同而不同，可能是12条、18条等
 *
 * @author APS Team
 */
@Data
@TableName("T_CX_STRUCTURE_SHIFT_CAPACITY")
@ApiModel(value = "结构班产配置")
public class CxStructureShiftCapacity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "结构编码")
    @TableField("STRUCTURE_CODE")
    private String structureCode;

    @ApiModelProperty(value = "整车条数（该结构每车的条数，可能是12、18等）")
    @TableField("TRIP_QTY")
    private Integer tripQty;

    @ApiModelProperty(value = "成型机台编码")
    @TableField("CX_MACHINE_CODE")
    private String cxMachineCode;

    @ApiModelProperty(value = "班次编码")
    @TableField(exist = false)
    private String shiftCode;

    @ApiModelProperty(value = "是否启用：0-禁用 1-启用")
    @TableField("IS_ACTIVE")
    private Integer isActive;

    @ApiModelProperty(value = "创建时间")
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 获取标准产能（条）
     * 标准产能 = 整车条数 × 1车
     */
    public Integer getStandardCapacity() {
        if (this.tripQty != null) {
            return this.tripQty;
        }
        return 12; // 默认12条
    }
}
