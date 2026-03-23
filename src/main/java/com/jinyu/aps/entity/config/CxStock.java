package com.jinyu.aps.entity.config;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.jinyu.aps.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 成型库存信息表
 * 对应表：T_CX_STOCK
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_CX_STOCK")
@Schema(description = "成型库存信息对象")
public class CxStock extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 库存日期
     */
    @Schema(description = "库存日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @TableField("STOCK_DATE")
    private Date stockDate;

    /**
     * 施工版本信息
     */
    @Schema(description = "施工版本信息")
    @TableField("BOM_DATA_VERSION")
    private String bomDataVersion;

    /**
     * 胎胚代码
     */
    @Schema(description = "胎胚代码")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    /**
     * 排程使用库存
     */
    @Schema(description = "排程使用库存")
    @TableField("SCHEDULE_USE_STOCK")
    private Long scheduleUseStock;

    /**
     * 库存量(可用)
     */
    @Schema(description = "库存量(可用)")
    @TableField("STOCK_NUM")
    private Long stockNum;

    /**
     * 库存量(不可用)
     */
    @Schema(description = "库存量(不可用)")
    @TableField("UNAVAILABLE_STOCK")
    private Long unavailableStock;

    /**
     * 超期库存
     */
    @Schema(description = "超期库存")
    @TableField("OVER_TIME_STOCK")
    private Long overTimeStock;

    /**
     * 修正数量
     */
    @Schema(description = "修正数量")
    @TableField("MODIFY_NUM")
    private Long modifyNum;

    /**
     * 不良数量
     */
    @Schema(description = "不良数量")
    @TableField("BAD_NUM")
    private Long badNum;

    // ========== 非数据库字段，用于查询参数 ==========

    /**
     * 查询库存的开始日期
     */
    @Schema(description = "查询库存的开始日期")
    @TableField(exist = false)
    private String startTime;

    /**
     * 查询库存的结束日期
     */
    @Schema(description = "查询库存的结束日期")
    @TableField(exist = false)
    private String endTime;

    /**
     * 库存日期字符串条件(引擎端使用)
     */
    @Schema(description = "库存日期字符串条件")
    @TableField(exist = false)
    private String stockDateStr;

    /**
     * 实际库存数=库存+修正-不良(引擎端使用)
     */
    @Schema(description = "实际库存数")
    @TableField(exist = false)
    private Integer stockRealNum;
}
