package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 机台当前状态实体
 * 
 * 记录成型机台的实时状态信息，包括：
 * - 当前在产结构/胎胚
 * - 当前生产进度
 * - 状态变更时间
 * 
 * 这是一个动态状态表，数据会随生产进度实时更新，
 * 与机台主数据表(MdmMoldingMachine)分离，便于维护和扩展。
 *
 * @author APS Team
 */
@Data
@TableName("T_CX_MACHINE_CURRENT_STATUS")
@ApiModel(value = "机台当前状态", description = "成型机台实时状态信息")
public class CxMachineCurrentStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "成型机台编码")
    @TableField("CX_MACHINE_CODE")
    private String cxMachineCode;

    @ApiModelProperty(value = "成型机台名称")
    @TableField("CX_MACHINE_NAME")
    private String cxMachineName;

    // ==================== 当前生产信息 ====================

    @ApiModelProperty(value = "当前在产结构编码")
    @TableField("CURRENT_STRUCTURE_CODE")
    private String currentStructureCode;

    @ApiModelProperty(value = "当前在产结构名称")
    @TableField("CURRENT_STRUCTURE_NAME")
    private String currentStructureName;

    @ApiModelProperty(value = "当前在产胎胚编码")
    @TableField("CURRENT_EMBRYO_CODE")
    private String currentEmbryoCode;

    @ApiModelProperty(value = "当前在产胎胚名称")
    @TableField("CURRENT_EMBRYO_NAME")
    private String currentEmbryoName;

    @ApiModelProperty(value = "当前班次编码")
    @TableField("CURRENT_SHIFT_CODE")
    private String currentShiftCode;

    @ApiModelProperty(value = "当前车次号")
    @TableField("CURRENT_TRIP_NO")
    private Integer currentTripNo;

    @ApiModelProperty(value = "当前计划数量")
    @TableField("CURRENT_PLAN_QTY")
    private Integer currentPlanQty;

    @ApiModelProperty(value = "当前已生产数量")
    @TableField("CURRENT_ACTUAL_QTY")
    private Integer currentActualQty;

    @ApiModelProperty(value = "当前生产进度(%)")
    @TableField("CURRENT_PROGRESS")
    private BigDecimal currentProgress;

    // ==================== 状态信息 ====================

    @ApiModelProperty(value = "机台状态：NORMAL-正常，MAINTAINING-维护中，FAULT-故障，IDLE-空闲，PRECISION-精度校验中")
    @TableField("MACHINE_STATUS")
    private String machineStatus;

    @ApiModelProperty(value = "状态变更时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("STATUS_CHANGE_TIME")
    private LocalDateTime statusChangeTime;

    @ApiModelProperty(value = "状态变更原因")
    @TableField("STATUS_CHANGE_REASON")
    private String statusChangeReason;

    @ApiModelProperty(value = "预计恢复时间（故障/维护时）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("ESTIMATED_RECOVERY_TIME")
    private LocalDateTime estimatedRecoveryTime;

    // ==================== 班次统计 ====================

    @ApiModelProperty(value = "本班已生产数量")
    @TableField("SHIFT_PRODUCED_QTY")
    private Integer shiftProducedQty;

    @ApiModelProperty(value = "本班计划数量")
    @TableField("SHIFT_PLAN_QTY")
    private Integer shiftPlanQty;

    @ApiModelProperty(value = "本日已生产数量")
    @TableField("DAILY_PRODUCED_QTY")
    private Integer dailyProducedQty;

    @ApiModelProperty(value = "本日计划数量")
    @TableField("DAILY_PLAN_QTY")
    private Integer dailyPlanQty;

    // ==================== 续作相关 ====================

    @ApiModelProperty(value = "是否续作：0-否 1-是")
    @TableField("IS_CONTINUE")
    private Integer isContinue;

    @ApiModelProperty(value = "续作开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CONTINUE_START_TIME")
    private LocalDateTime continueStartTime;

    @ApiModelProperty(value = "续作预计完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CONTINUE_END_TIME")
    private LocalDateTime continueEndTime;

    // ==================== 时间戳 ====================

    @ApiModelProperty(value = "开始生产时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("START_TIME")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "预计完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("EXPECTED_END_TIME")
    private LocalDateTime expectedEndTime;

    @ApiModelProperty(value = "最后更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "更新人")
    @TableField("UPDATE_BY")
    private String updateBy;

    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;

    // ==================== 业务方法 ====================

    /**
     * 判断机台是否可用
     */
    public boolean isAvailable() {
        return "NORMAL".equals(machineStatus) || "IDLE".equals(machineStatus);
    }

    /**
     * 判断机台是否正在生产
     */
    public boolean isProducing() {
        return currentEmbryoCode != null && !"IDLE".equals(machineStatus);
    }

    /**
     * 计算生产进度
     */
    public BigDecimal calculateProgress() {
        if (currentPlanQty == null || currentPlanQty <= 0) {
            return BigDecimal.ZERO;
        }
        int actual = currentActualQty != null ? currentActualQty : 0;
        return new BigDecimal(actual)
                .divide(new BigDecimal(currentPlanQty), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal(100));
    }

    /**
     * 判断是否为续作任务
     */
    public boolean isContinueTask() {
        return isContinue != null && isContinue == 1;
    }
}
