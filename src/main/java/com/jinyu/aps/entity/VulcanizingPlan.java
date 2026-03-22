package com.jinyu.aps.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 硫化计划实体类
 *
 * @author APS Team
 */
@Data
@TableName(value = "t_cx_vulcanizing_plan", keepGlobalPrefix = false)
@Schema(description = "硫化计划")
public class VulcanizingPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "计划单号")
    @TableField("plan_code")
    private String planCode;

    @Schema(description = "计划日期")
    @TableField("plan_date")
    private LocalDate planDate;

    @Schema(description = "胎胚物料编码")
    @TableField("material_code")
    private String materialCode;

    @Schema(description = "计划产量")
    @TableField("plan_quantity")
    private Integer planQuantity;

    @Schema(description = "优先级")
    @TableField("priority")
    private Integer priority;

    @Schema(description = "来源")
    @TableField("source")
    private String source;

    @Schema(description = "状态")
    @TableField("status")
    private String status;

    @Schema(description = "已分配数量")
    @TableField("assigned_quantity")
    private Integer assignedQuantity;

    @Schema(description = "剩余未分配数量")
    @TableField("remainder_quantity")
    private Integer remainderQuantity;

    @Schema(description = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField("update_time")
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    @TableField("create_by")
    private String createBy;
}
