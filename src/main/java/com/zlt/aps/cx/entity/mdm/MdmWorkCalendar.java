package com.zlt.aps.cx.entity.mdm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.cx.entity.base.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel(value = "工作日历对象", description = "工作日历对象")
public class MdmWorkCalendar extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工序编码")
    @TableField("PROC_CODE")
    private String procCode;

    @ApiModelProperty(value = "年份")
    @TableField("YEAR")
    private Integer year;

    @ApiModelProperty(value = "月份")
    @TableField("MONTH")
    private Integer month;

    @ApiModelProperty(value = "日期")
    @TableField("DAY")
    private Integer day;

    @ApiModelProperty(value = "生产日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("PRODUCTION_DATE")
    private Date productionDate;

    @ApiModelProperty(value = "一班开停产标志，0-停,1-开")
    @TableField("ONE_SHIFT_FLAG")
    private String oneShiftFlag;

    @ApiModelProperty(value = "二班开停产标志，0-停,1-开")
    @TableField("TWO_SHIFT_FLAG")
    private String twoShiftFlag;

    @ApiModelProperty(value = "三班开停产标志，0-停,1-开")
    @TableField("THREE_SHIFT_FLAG")
    private String threeShiftFlag;

    @ApiModelProperty(value = "日期开停产标志，0-停,1-开")
    @TableField("DAY_FLAG")
    private String dayFlag;

    @ApiModelProperty(value = "比例")
    @TableField("RATE")
    private Integer rate;

    @ApiModelProperty(value = "日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField(exist = false)
    private Date calendarTime;

    @ApiModelProperty(value = "节假日名称")
    @TableField(exist = false)
    private String holidayNames;

    @ApiModelProperty(value = "源工序")
    @TableField(exist = false)
    private String sourceProcCode;

    @ApiModelProperty(value = "目标工序")
    @TableField(exist = false)
    private String targetProcCode;

    @ApiModelProperty(value = "源年份")
    @TableField(exist = false)
    private Integer sourceYear;

    @ApiModelProperty(value = "源月份")
    @TableField(exist = false)
    private Integer sourceMonth;

    @ApiModelProperty(value = "目标年份")
    @TableField(exist = false)
    private Integer targetYear;

    @ApiModelProperty(value = "目标月份")
    @TableField(exist = false)
    private Integer targetMonth;

    @ApiModelProperty(value = "复制时源工厂编号")
    @TableField(exist = false)
    private String sourceFactoryCode;

    @ApiModelProperty(value = "复制时目标工厂编号")
    @TableField(exist = false)
    private String targetFactoryCode;
}
