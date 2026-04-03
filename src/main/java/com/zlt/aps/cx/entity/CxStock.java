package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：CxStock.java
 * 描    述：成型库存信息对象 t_cx_stock
 *@author zlt
 *@date 2025-02-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "成型库存信息对象", description = "成型库存信息对象 ")
@Data
@TableName(value = "T_CX_STOCK")
public class CxStock extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 库存日期，格式：yyyy-MM-dd */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.cxStock.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "库存日期，格式：yyyy-MM-dd", name = "stockDate")
    @TableField(value = "STOCK_DATE")
    private Date stockDate;

    /** 胎胚代码 */
    @Excel(name = "ui.data.column.cxStock.embryoCode")
    @ApiModelProperty(value = "胎胚代码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /** 库存量 */
    @Excel(name = "ui.data.column.cxStock.stockNum")
    @ApiModelProperty(value = "库存量", name = "stockNum")
    @TableField(value = "STOCK_NUM")
    private Integer stockNum;

    /** 超期库存 */
    @Excel(name = "ui.data.column.cxStock.overTimeStock")
    @ApiModelProperty(value = "超期库存", name = "overTimeStock")
    @TableField(value = "OVER_TIME_STOCK")
    private Integer overTimeStock;

    /** 修正数量 */
    @Excel(name = "ui.data.column.cxStock.modifyNum")
    @ApiModelProperty(value = "修正数量", name = "modifyNum")
    @TableField(value = "MODIFY_NUM")
    private Integer modifyNum;

    /** 不良数量 */
    @Excel(name = "ui.data.column.cxStock.badNum")
    @ApiModelProperty(value = "不良数量", name = "badNum")
    @TableField(value = "BAD_NUM")
    private Integer badNum;

    // ============== 非数据库字段（用于业务计算） ==============

    /** 排程使用库存 */
    @Excel(name = "ui.data.column.stock.scheduleUseStock")
    @TableField(exist = false)
    private Long scheduleUseStock;

    /** 胎胚名称（关联查询） */
    @TableField(exist = false)
    private String materialName;

    /** 库存可供硫化时长(小时) */
    @TableField(exist = false)
    private BigDecimal stockHours;

    /** 预警状态：NORMAL-正常，LOW-低库存，HIGH-高库存 */
    @TableField(exist = false)
    private String alertStatus;

    /** 预警时间 */
    @TableField(exist = false)
    private Date alertTime;

    /** 是否收尾SKU：0-否，1-是 */
    @TableField(value = "IS_ENDING_SKU")
    private Integer isEndingSku;

    /** 可用硫化机台数（业务计算字段） */
    @TableField(exist = false)
    private Integer vulcanizeMachineCount;

    /** 可用硫化模数（业务计算字段） */
    @TableField(exist = false)
    private Integer vulcanizeMoldCount;

    // ============== 业务计算方法 ==============

    /**
     * 计算有效库存
     * 有效库存 = 库存量 - 超期库存 - 不良数量 + 修正数量
     * 
     * @return 有效库存量
     */
    public Integer getEffectiveStock() {
        int effective = stockNum != null ? stockNum : 0;
        if (overTimeStock != null) {
            effective -= overTimeStock;
        }
        if (badNum != null) {
            effective -= badNum;
        }
        if (modifyNum != null) {
            effective += modifyNum;
        }
        return Math.max(0, effective);
    }

    /**
     * 计算可用库存
     * 可用库存 = 有效库存 - 排程使用库存
     * 
     * @return 可用库存量
     */
    public Long getAvailableStock() {
        long available = getEffectiveStock();
        if (scheduleUseStock != null) {
            available -= scheduleUseStock;
        }
        return Math.max(0L, available);
    }

    /**
     * 获取可用小时数（用于排程计算）
     * 
     * @return 可用小时数
     */
    public BigDecimal getAvailableHours() {
        return stockHours != null ? stockHours : BigDecimal.ZERO;
    }
}
