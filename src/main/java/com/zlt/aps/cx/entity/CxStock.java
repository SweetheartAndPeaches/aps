package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 成型库存信息实体类
 * 对应数据库表：T_CX_STOCK
 *
 * @author APS Team
 */
@ApiModel(value = "成型库存信息对象", description = "成型库存信息对象")
@Data
@TableName(value = "T_CX_STOCK")
public class CxStock implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /** 库存日期，格式：yyyy-MM-dd */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "库存日期", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "库存日期，格式：yyyy-MM-dd")
    @TableField(value = "STOCK_DATE")
    private Date stockDate;

    /** 胎胚代码 */
    @Excel(name = "胎胚代码")
    @ApiModelProperty(value = "胎胚代码")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /** 胎胚名称（非数据库字段，关联查询） */
    @Excel(name = "胎胚名称")
    @ApiModelProperty(value = "胎胚名称")
    @TableField(exist = false)
    private String materialName;

    /** 库存量 */
    @Excel(name = "库存量")
    @ApiModelProperty(value = "库存量")
    @TableField(value = "STOCK_NUM")
    private Integer stockNum;

    /** 超期库存 */
    @Excel(name = "超期库存")
    @ApiModelProperty(value = "超期库存")
    @TableField(value = "OVER_TIME_STOCK")
    private Integer overTimeStock;

    /** 修正数量 */
    @Excel(name = "修正数量")
    @ApiModelProperty(value = "修正数量")
    @TableField(value = "MODIFY_NUM")
    private Integer modifyNum;

    /** 不良数量 */
    @Excel(name = "不良数量")
    @ApiModelProperty(value = "不良数量")
    @TableField(value = "BAD_NUM")
    private Integer badNum;

    /** 预警状态 */
    @Excel(name = "预警状态")
    @ApiModelProperty(value = "预警状态：NORMAL-正常，LOW-低库存，HIGH-高库存")
    @TableField(value = "ALERT_STATUS")
    private String alertStatus;

    /** 预警触发时间 */
    @ApiModelProperty(value = "预警触发时间")
    @TableField(value = "ALERT_TIME")
    private LocalDateTime alertTime;

    /** 库存可供硫化时长(小时) */
    @Excel(name = "可供时长")
    @ApiModelProperty(value = "库存可供硫化时长(小时)")
    @TableField(value = "STOCK_HOURS")
    private BigDecimal stockHours;

    /** 是否收尾SKU */
    @Excel(name = "是否收尾SKU")
    @ApiModelProperty(value = "是否收尾SKU：0-否，1-是")
    @TableField(value = "IS_ENDING_SKU")
    private Integer isEndingSku;

    /** 预计收尾日期 */
    @ApiModelProperty(value = "预计收尾日期")
    @TableField(value = "ENDING_DATE")
    private LocalDateTime endingDate;

    /** 计算时间 */
    @ApiModelProperty(value = "计算时间")
    @TableField(value = "CALC_TIME")
    private LocalDateTime calcTime;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @ApiModelProperty(value = "更新时间")
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 
     * 排程使用库存（非数据库字段，用于排程计算）
     * 表示已被其他排程计划占用的库存量
     */
    @Excel(name = "排程使用库存")
    @ApiModelProperty(value = "排程使用库存（非数据库字段）")
    @TableField(exist = false)
    private Long scheduleUseStock;

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
