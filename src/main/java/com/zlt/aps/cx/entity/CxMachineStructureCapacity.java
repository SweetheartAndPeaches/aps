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
 * 机台结构产能配置实体
 * 
 * 记录每个结构在不同成型机台上的产能配置。
 * 不同结构在不同机台上的产能可能不同，需要单独配置。
 * 
 * 用于排程计算时获取：
 * - 小时产能：用于计算精度计划产能扣减
 * - 班次产能：用于计算单班最大产量
 * 
 * 注：整车条数由 CxStructureShiftCapacity 表配置
 *
 * @author APS Team
 */
@Data
@TableName("T_CX_MACHINE_STRUCTURE_CAPACITY")
@ApiModel(value = "机台结构产能配置", description = "机台-结构维度的产能配置")
public class CxMachineStructureCapacity implements Serializable {

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

    @ApiModelProperty(value = "结构编码")
    @TableField("STRUCTURE_CODE")
    private String structureCode;

    @ApiModelProperty(value = "结构名称")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    @ApiModelProperty(value = "小时产能（条/小时）")
    @TableField("HOURLY_CAPACITY")
    private Integer hourlyCapacity;

    @ApiModelProperty(value = "早班产能（条/班）")
    @TableField("MORNING_SHIFT_CAPACITY")
    private Integer morningShiftCapacity;

    @ApiModelProperty(value = "中班产能（条/班）")
    @TableField("AFTERNOON_SHIFT_CAPACITY")
    private Integer afternoonShiftCapacity;

    @ApiModelProperty(value = "夜班产能（条/班）")
    @TableField("NIGHT_SHIFT_CAPACITY")
    private Integer nightShiftCapacity;

    @ApiModelProperty(value = "换产时间（分钟）")
    @TableField("CHANGE_OVER_TIME")
    private Integer changeOverTime;

    @ApiModelProperty(value = "损耗率")
    @TableField("LOSS_RATE")
    private BigDecimal lossRate;

    @ApiModelProperty(value = "是否启用：0-禁用 1-启用")
    @TableField("IS_ACTIVE")
    private Integer isActive;

    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "创建人")
    @TableField("CREATE_BY")
    private String createBy;

    // ==================== 业务方法 ====================

    /**
     * 获取指定班次的产能
     *
     * @param shiftCode 班次编码：SHIFT_DAY-早班，SHIFT_AFTERNOON-中班，SHIFT_NIGHT-夜班
     * @return 班次产能（条/班）
     */
    public Integer getShiftCapacity(String shiftCode) {
        if (shiftCode == null) {
            return hourlyCapacity != null ? hourlyCapacity * 8 : 400; // 默认8小时
        }
        switch (shiftCode) {
            case "SHIFT_DAY":
                return morningShiftCapacity != null ? morningShiftCapacity : 
                       (hourlyCapacity != null ? hourlyCapacity * 8 : 400);
            case "SHIFT_AFTERNOON":
                return afternoonShiftCapacity != null ? afternoonShiftCapacity : 
                       (hourlyCapacity != null ? hourlyCapacity * 8 : 400);
            case "SHIFT_NIGHT":
                return nightShiftCapacity != null ? nightShiftCapacity : 
                       (hourlyCapacity != null ? hourlyCapacity * 8 : 400);
            default:
                return hourlyCapacity != null ? hourlyCapacity * 8 : 400;
        }
    }

    /**
     * 获取日产能（三班总和）
     */
    public Integer getDailyCapacity() {
        int morning = morningShiftCapacity != null ? morningShiftCapacity : 
                      (hourlyCapacity != null ? hourlyCapacity * 8 : 400);
        int afternoon = afternoonShiftCapacity != null ? afternoonShiftCapacity : 
                        (hourlyCapacity != null ? hourlyCapacity * 8 : 400);
        int night = nightShiftCapacity != null ? nightShiftCapacity : 
                    (hourlyCapacity != null ? hourlyCapacity * 8 : 400);
        return morning + afternoon + night;
    }
}
