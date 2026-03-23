package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 材料异常记录实体
 *
 * @author APS Team
 */
@Data
@TableName("cx_material_exception")
public class CxMaterialException {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 排程明细ID */
    private Long scheduleDetailId;

    /** 物料编码 */
    private String materialCode;

    /** 异常类型（TREAD_LENGTH_SHORTAGE-胎面米数不够，CURTAIN_ROLL_DEPLETION-帘布用完） */
    private String exceptionType;

    /** 计划值 */
    private BigDecimal plannedValue;

    /** 实际值 */
    private BigDecimal actualValue;

    /** 处理状态 */
    private String handleStatus;

    /** 处理方法 */
    private String handleMethod;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
