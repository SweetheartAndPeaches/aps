package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 硫化计划实体
 * 对应数据库表：T_CX_LH_PLAN
 *
 * @author APS Team
 */
@Data
@TableName("T_CX_LH_PLAN")
public class CxLhPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /** 计划日期 */
    @TableField("PLAN_DATE")
    private LocalDate planDate;

    /** 硫化机台编码 */
    @TableField("LH_MACHINE_CODE")
    private String lhMachineCode;

    /** 机台编码 */
    @TableField(exist = false)
    private String machineCode;

    /** 硫化机台名称 */
    @TableField("LH_MACHINE_NAME")
    private String lhMachineName;

    /** 物料编码 */
    @TableField("MATERIAL_CODE")
    private String materialCode;

    /** 物料名称 */
    @TableField("MATERIAL_NAME")
    private String materialName;

    /** 计划数量 */
    @TableField("PLAN_QTY")
    private Integer planQty;

    /** 实际数量 */
    @TableField("ACTUAL_QTY")
    private Integer actualQty;

    /** 班次编码 */
    @TableField("SHIFT_CODE")
    private String shiftCode;

    /** 模具编号 */
    @TableField("MOLD_CODE")
    private String moldCode;

    /** 状态 */
    @TableField("STATUS")
    private String status;

    /** 备注 */
    @TableField("REMARK")
    private String remark;

    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
