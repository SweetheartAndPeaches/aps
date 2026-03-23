package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成型机台实体类
 *
 * @author APS Team
 */
@Data
@TableName(value = "t_cx_machine", keepGlobalPrefix = false)
@Schema(description = "成型机台")
public class Machine implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "机台编号")
    @TableField("machine_code")
    private String machineCode;

    @Schema(description = "机台名称")
    @TableField("machine_name")
    private String machineName;

    @Schema(description = "机台类型")
    @TableField("machine_type")
    private String machineType;

    @Schema(description = "反包方式")
    @TableField("wrapping_type")
    private String wrappingType;

    @Schema(description = "是否有零度供料架")
    @TableField("has_zero_degree_feeder")
    private Integer hasZeroDegreeFeeder;

    @Schema(description = "在产结构")
    @TableField("structure")
    private String structure;

    @Schema(description = "每小时最大产能")
    @TableField("max_capacity_per_hour")
    private BigDecimal maxCapacityPerHour;

    @Schema(description = "设备最大日产能")
    @TableField("max_daily_capacity")
    private Integer maxDailyCapacity;

    @Schema(description = "对应硫化机上限")
    @TableField("max_curing_machines")
    private Integer maxCuringMachines;

    @Schema(description = "固定规格1")
    @TableField("fixed_structure1")
    private String fixedStructure1;

    @Schema(description = "固定规格2")
    @TableField("fixed_structure2")
    private String fixedStructure2;

    @Schema(description = "固定规格3")
    @TableField("fixed_structure3")
    private String fixedStructure3;

    @Schema(description = "不可作业结构")
    @TableField("restricted_structures")
    private String restrictedStructures;

    @Schema(description = "排产限制说明")
    @TableField("production_restriction")
    private String productionRestriction;

    @Schema(description = "产线编号")
    @TableField("line_number")
    private Integer lineNumber;

    @Schema(description = "状态")
    @TableField("status")
    private String status;

    @Schema(description = "是否启用")
    @TableField("is_active")
    private Integer isActive;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
