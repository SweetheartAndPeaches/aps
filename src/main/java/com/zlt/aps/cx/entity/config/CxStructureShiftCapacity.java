package com.zlt.aps.cx.entity.config;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结构班产配置实体
 * 
 * 定义每个结构在各班次的标准产能（整车条数）
 * 整车固定12条，此表记录每个班次可生产的整车数
 *
 * @author APS Team
 */
@Data
@TableName("T_CX_STRUCTURE_SHIFT_CAPACITY")
@Schema(description = "结构班产配置")
public class CxStructureShiftCapacity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @Schema(description = "结构编码")
    @TableField("STRUCTURE_CODE")
    private String structureCode;

    @Schema(description = "结构名称")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    @Schema(description = "机型编码")
    @TableField("MACHINE_TYPE_CODE")
    private String machineTypeCode;

    @Schema(description = "班次编码：NIGHT(夜班)/DAY(早班)/AFTERNOON(中班)")
    @TableField("SHIFT_CODE")
    private String shiftCode;

    @Schema(description = "班次名称")
    @TableField("SHIFT_NAME")
    private String shiftName;

    @Schema(description = "整车条数（以整车为单位，1车=12条）")
    @TableField("TRIP_QTY")
    private Integer tripQty;

    @Schema(description = "标准产能（条）= 整车条数 × 12")
    @TableField("STANDARD_CAPACITY")
    private Integer standardCapacity;

    @Schema(description = "每小时产能（条/小时）")
    @TableField("CAPACITY_PER_HOUR")
    private BigDecimal capacityPerHour;

    @Schema(description = "最小产能（条）")
    @TableField("MIN_CAPACITY")
    private Integer minCapacity;

    @Schema(description = "最大产能（条）")
    @TableField("MAX_CAPACITY")
    private Integer maxCapacity;

    @Schema(description = "是否启用：0-禁用 1-启用")
    @TableField("IS_ACTIVE")
    private Integer isActive;

    @Schema(description = "备注")
    @TableField("REMARK")
    private String remark;

    @Schema(description = "创建时间")
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 整车容量常量
     */
    public static final int TRIP_CAPACITY = 12;

    /**
     * 计算标准产能
     */
    public void calculateStandardCapacity() {
        if (this.tripQty != null) {
            this.standardCapacity = this.tripQty * TRIP_CAPACITY;
        }
    }
}
