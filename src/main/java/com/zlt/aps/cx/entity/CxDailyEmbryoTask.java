package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@TableName(value = "t_cx_daily_embryo_task", keepGlobalPrefix = false)
@ApiModel(value = "日胎胚任务")
public class CxDailyEmbryoTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "排程主表ID")
    @TableField("SCHEDULE_MAIN_ID")
    private Long scheduleMainId;

    @ApiModelProperty(value = "任务分组ID")
    @TableField("TASK_GROUP_ID")
    private String taskGroupId;

    @ApiModelProperty(value = "胎胚物料编码")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    @ApiModelProperty(value = "任务量")
    @TableField("TASK_QUANTITY")
    private Integer taskQuantity;

    @ApiModelProperty(value = "产品结构")
    @TableField("PRODUCT_STRUCTURE")
    private String productStructure;

    @ApiModelProperty(value = "是否主销产品")
    @TableField("IS_MAIN_PRODUCT")
    private Integer isMainProduct;

    @ApiModelProperty(value = "优先级")
    @TableField("PRIORITY")
    private Integer priority;

    @ApiModelProperty(value = "排序")
    @TableField("SORT_ORDER")
    private Integer sortOrder;

    @ApiModelProperty(value = "已分配量")
    @TableField("ASSIGNED_QUANTITY")
    private Integer assignedQuantity;

    @ApiModelProperty(value = "剩余量")
    @TableField("REMAINDER_QUANTITY")
    private Integer remainderQuantity;

    @ApiModelProperty(value = "是否全部分配")
    @TableField("IS_FULLY_ASSIGNED")
    private Integer isFullyAssigned;

    @ApiModelProperty(value = "创建时间")
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
    @ApiModelProperty(value = "是否紧急收尾（3天内）")
    private Integer isUrgentEnding;
    
    @TableField(exist = false)
    @ApiModelProperty(value = "是否关键产品")
    private Integer isKeyProduct;
    
    @TableField(exist = false)
    @ApiModelProperty(value = "收尾天数")
    private BigDecimal endingDays;
    
    @TableField(exist = false)
    @ApiModelProperty(value = "是否开产首日")
    private Integer isStartingDay;
    
    // ==================== P1功能扩展字段 ====================
    
    @TableField(exist = false)
    @ApiModelProperty(value = "是否试制任务")
    private Integer isTrialTask;
    
    @TableField(exist = false)
    @ApiModelProperty(value = "试制单号")
    private String trialNo;
    
    @TableField(exist = false)
    @ApiModelProperty(value = "计划日期")
    private LocalDate planDate;
    
    @TableField(exist = false)
    @ApiModelProperty(value = "计划数量")
    private Integer planQuantity;
    
    @TableField(exist = false)
    @ApiModelProperty(value = "结构编码")
    private String structureCode;
    
    @TableField(exist = false)
    @ApiModelProperty(value = "机台编码")
    private String machineCode;
    
    @TableField(exist = false)
    @ApiModelProperty(value = "状态")
    private String status;
    
    // ==================== P2功能扩展字段 ====================
    
    @TableField(exist = false)
    @ApiModelProperty(value = "生产时间（用于停放时间计算）")
    private LocalDateTime produceTime;
    
    @TableField(exist = false)
    @ApiModelProperty(value = "停放时间（小时）")
    private Double parkingHours;
    
    @TableField(exist = false)
    @ApiModelProperty(value = "原材料编码（替换前）")
    private String originalMaterialCode;
}
