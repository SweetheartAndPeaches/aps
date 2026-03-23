package com.zlt.aps.cx.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * @ClassName CxStock
 * @Description TODO
 * @Author Joran.Zhang
 * @Date ${Date} ${Time}
 * @Version 1.0
 **/
@Data
@TableName("T_CX_STOCK")
@ApiModel(value = "CxStock对象", description = "成型库存信息")
@KeySequence(value = "SEQ_PUBLIC",dbType = DbType.ORACLE)
public class CxStock extends ApsBaseEntity {

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    /**
     * 库存日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ImportValidated(required = true, date = true)
    @Excel(name = "ui.data.column.stock.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "库存日期")
    @TableField("STOCK_DATE")
    private Date stockDate;
    /**
     * 施工版本信息
     */
    //@Excel(name = "ui.data.column.productStatus.bomDataVersion")
    // @ImportValidated(required = true,maxLength = 30)
    private  String bomDataVersion;

    @ApiModelProperty(value = "胎胚代码")
    @TableField("EMBRYO_CODE")
    @ImportValidated(required = true, maxLength = 50, isCode = true)
    @Excel(name = "ui.data.column.stock.embryoCode")
    private String embryoCode;
    
    /**
     * 排程使用库存
     */
    @Excel(name = "ui.data.column.stock.scheduleUseStock",type = Excel.Type.EXPORT)
    private Long scheduleUseStock;

    @ApiModelProperty(value = "库存量(可用)")
    @TableField("STOCK_NUM")
    @ImportValidated(required = true, digits = true, min = 0, max = 999999)
    @Excel(name = "ui.data.column.stock.stockNumAvailable")
    private Long stockNum;
    
    /**
     * 库存量(不可用)
     */
    @ApiModelProperty(value = "不可用库存")
    @TableField("UNAVAILABLE_STOCK")
  // @ImportValidated(digits = true, min = 0, max = 999999)
  // @Excel(name = "ui.data.column.stock.unavailableStock")
    private Long unavailableStock;

    @ApiModelProperty(value = "超期库存")
    @TableField("OVER_TIME_STOCK")
    private Long overTimeStock;

    @ApiModelProperty(value = "修正数量")
    @TableField("MODIFY_NUM")
    @ImportValidated(isInteger = true, min = -999999, max = 999999)
    @Excel(name = "ui.data.column.stock.modifyNum")
    private Long modifyNum;

    @ApiModelProperty(value = "不良数量")
    @TableField("BAD_NUM")
    @ImportValidated(digits = true, min = 0, max = 999999)
    @Excel(name = "ui.data.column.stock.badNum")
    private Long badNum;

    @ApiModelProperty(value = "查询库存的开始日期yyyy-MM-dd", position = 21)
    private String startTime;

    @ApiModelProperty(value = "查询库存的结束日期yyyy-MM-dd", position = 22)
    private String endTime;

    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.data.column.stock.remark")
    private String remark;

    /**
     * 库存日期字符串条件(引擎端使用)
     */
    private String stockDateStr;

    /**
     * 实际库存数=库存+修正-不良(引擎端使用)
     */
    private Integer stockRealNum;
}
