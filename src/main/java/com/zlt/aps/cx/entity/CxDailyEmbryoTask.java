package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日胎胚任务实体类
 * 对应数据库表：T_CX_DAILY_EMBRYO_TASK
 *
 * @author APS Team
 */
@Data
@TableName(value = "T_CX_DAILY_EMBRYO_TASK", keepGlobalPrefix = false)
@Schema(description = "日胎胚任务")
public class CxDailyEmbryoTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @Schema(description = "排程主表ID")
    @TableField("SCHEDULE_MAIN_ID")
    private Long scheduleMainId;

    @Schema(description = "任务分组ID")
    @TableField("TASK_GROUP_ID")
    private String taskGroupId;

    @Schema(description = "胎胚物料编码")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    @Schema(description = "任务量")
    @TableField("TASK_QUANTITY")
    private Integer taskQuantity;

    @Schema(description = "产品结构")
    @TableField("PRODUCT_STRUCTURE")
    private String productStructure;

    @Schema(description = "是否主销产品")
    @TableField("IS_MAIN_PRODUCT")
    private Integer isMainProduct;

    @Schema(description = "优先级")
    @TableField("PRIORITY")
    private Integer priority;

    @Schema(description = "排序")
    @TableField("SORT_ORDER")
    private Integer sortOrder;

    @Schema(description = "已分配量")
    @TableField("ASSIGNED_QUANTITY")
    private Integer assignedQuantity;

    @Schema(description = "剩余量")
    @TableField("REMAINDER_QUANTITY")
    private Integer remainderQuantity;

    @Schema(description = "是否全部分配")
    @TableField("IS_FULLY_ASSIGNED")
    private Integer isFullyAssigned;

    @Schema(description = "创建时间")
    @TableField("CREATE_TIME")
    private LocalDateTime createTime;

    // ==================== 扩展字段（用于算法计算，不映射到数据库） ====================

    @TableField(exist = false)
    private String materialName;

    @TableField(exist = false)
    private Integer stockQuantity;

    @TableField(exist = false)
    private Integer vulcanizeMachineCount;

    @TableField(exist = false)
    private Double stockHours;
    
    @TableField(exist = false)
    @Schema(description = "是否紧急收尾（3天内）")
    private Integer isUrgentEnding;
    
    @TableField(exist = false)
    @Schema(description = "是否关键产品")
    private Integer isKeyProduct;
    
    @TableField(exist = false)
    @Schema(description = "收尾天数")
    private BigDecimal endingDays;
    
    @TableField(exist = false)
    @Schema(description = "是否开产首日")
    private Integer isStartingDay;
    
    // ==================== P1功能扩展字段 ====================
    
    @TableField(exist = false)
    @Schema(description = "是否试制任务")
    private Integer isTrialTask;
    
    @TableField(exist = false)
    @Schema(description = "试制单号")
    private String trialNo;
    
    @TableField(exist = false)
    @Schema(description = "计划日期")
    private LocalDate planDate;
    
    @TableField(exist = false)
    @Schema(description = "计划数量")
    private Integer planQuantity;
    
    @TableField(exist = false)
    @Schema(description = "结构编码")
    private String structureCode;
    
    @TableField(exist = false)
    @Schema(description = "机台编码")
    private String machineCode;
    
    @TableField(exist = false)
    @Schema(description = "状态")
    private String status;
    
    // ==================== P2功能扩展字段 ====================
    
    @TableField(exist = false)
    @Schema(description = "生产时间（用于停放时间计算）")
    private LocalDateTime produceTime;
    
    @TableField(exist = false)
    @Schema(description = "停放时间（小时）")
    private Double parkingHours;
    
    @TableField(exist = false)
    @Schema(description = "原材料编码（替换前）")
    private String originalMaterialCode;
}
