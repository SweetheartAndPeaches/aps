package com.zlt.aps.mdm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 工作日历
 *
 * @author APS Team
 */
@Data
@ApiModel(description = "工作日历")
@TableName("T_MDM_WORK_CALENDAR")
public class MdmWorkCalendar implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 工厂编号
     */
    @ApiModelProperty(value = "工厂编号")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    /**
     * 工作日期
     */
    @ApiModelProperty(value = "工作日期")
    @TableField("WORK_DATE")
    private Date workDate;

    /**
     * 日期类型(0-休息日 1-工作日)
     */
    @ApiModelProperty(value = "日期类型(0-休息日 1-工作日)")
    @TableField("DATE_TYPE")
    private String dateType;

    /**
     * 班次编码
     */
    @ApiModelProperty(value = "班次编码")
    @TableField("SHIFT_CODE")
    private String shiftCode;

    /**
     * 班次名称
     */
    @ApiModelProperty(value = "班次名称")
    @TableField("SHIFT_NAME")
    private String shiftName;

    /**
     * 班次开始时间
     */
    @ApiModelProperty(value = "班次开始时间")
    @TableField("SHIFT_START_TIME")
    private Date shiftStartTime;

    /**
     * 班次结束时间
     */
    @ApiModelProperty(value = "班次结束时间")
    @TableField("SHIFT_END_TIME")
    private Date shiftEndTime;

    /**
     * 工作时长(小时)
     */
    @ApiModelProperty(value = "工作时长(小时)")
    @TableField("WORK_HOURS")
    private BigDecimal workHours;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;

    /**
     * 创建人
     */
    @ApiModelProperty(value = "创建人")
    @TableField("CREATE_BY")
    private String createBy;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间")
    @TableField("CREATE_TIME")
    private Date createTime;

    /**
     * 更新人
     */
    @ApiModelProperty(value = "更新人")
    @TableField("UPDATE_BY")
    private String updateBy;

    /**
     * 更新时间
     */
    @ApiModelProperty(value = "更新时间")
    @TableField("UPDATE_TIME")
    private Date updateTime;
}
