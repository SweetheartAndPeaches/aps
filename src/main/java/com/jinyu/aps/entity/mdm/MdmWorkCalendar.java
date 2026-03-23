package com.jinyu.aps.entity.mdm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.jinyu.aps.entity.base.ApsBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 工作日历对象
 * 对应表：T_MDM_WORK_CALENDAR
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_MDM_WORK_CALENDAR")
@Schema(description = "工作日历对象")
public class MdmWorkCalendar extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工序编码
     * 01-月计划,02-硫化,03-成型,04-胎面,05-胎侧,06-内衬,07-垫胶,08-胎圈,09-钢丝圈,10-斜裁,11-直裁,12-压延,15-零度,16-密炼
     */
    @Schema(description = "工序编码")
    @TableField("PROC_CODE")
    private String procCode;

    /**
     * 年份
     */
    @Schema(description = "年份")
    @TableField("YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Schema(description = "月份")
    @TableField("MONTH")
    private Integer month;

    /**
     * 日期
     */
    @Schema(description = "日期")
    @TableField("DAY")
    private Integer day;

    /**
     * 生产日期
     */
    @Schema(description = "生产日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("PRODUCTION_DATE")
    private Date productionDate;

    /**
     * 一班开停产标志，0-停,1-开
     */
    @Schema(description = "一班开停产标志，0-停,1-开")
    @TableField("ONE_SHIFT_FLAG")
    private String oneShiftFlag;

    /**
     * 二班开停产标志，0-停,1-开
     */
    @Schema(description = "二班开停产标志，0-停,1-开")
    @TableField("TWO_SHIFT_FLAG")
    private String twoShiftFlag;

    /**
     * 三班开停产标志，0-停,1-开
     */
    @Schema(description = "三班开停产标志，0-停,1-开")
    @TableField("THREE_SHIFT_FLAG")
    private String threeShiftFlag;

    /**
     * 日期开停产标志，0-停,1-开
     */
    @Schema(description = "日期开停产标志，0-停,1-开")
    @TableField("DAY_FLAG")
    private String dayFlag;

    /**
     * 比例
     */
    @Schema(description = "比例")
    @TableField("RATE")
    private Integer rate;

    // ========== 非数据库字段，用于查询参数 ==========

    /**
     * 日期（非数据库字段）
     */
    @Schema(description = "日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField(exist = false)
    private Date calendarTime;

    /**
     * 节假日名称（非数据库字段）
     */
    @Schema(description = "节假日名称")
    @TableField(exist = false)
    private String holidayNames;

    /**
     * 源工序（非数据库字段）
     */
    @Schema(description = "源工序")
    @TableField(exist = false)
    private String sourceProcCode;

    /**
     * 目标工序（非数据库字段）
     */
    @Schema(description = "目标工序")
    @TableField(exist = false)
    private String targetProcCode;

    /**
     * 源年份（非数据库字段）
     */
    @Schema(description = "源年份")
    @TableField(exist = false)
    private Integer sourceYear;

    /**
     * 源月份（非数据库字段）
     */
    @Schema(description = "源月份")
    @TableField(exist = false)
    private Integer sourceMonth;

    /**
     * 目标年份（非数据库字段）
     */
    @Schema(description = "目标年份")
    @TableField(exist = false)
    private Integer targetYear;

    /**
     * 目标月份（非数据库字段）
     */
    @Schema(description = "目标月份")
    @TableField(exist = false)
    private Integer targetMonth;

    /**
     * 复制时源工厂编号（非数据库字段）
     */
    @Schema(description = "复制时源工厂编号")
    @TableField(exist = false)
    private String sourceFactoryCode;

    /**
     * 复制时目标工厂编号（非数据库字段）
     */
    @Schema(description = "复制时目标工厂编号")
    @TableField(exist = false)
    private String targetFactoryCode;
}
