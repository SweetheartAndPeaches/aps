package com.zlt.aps.cx.entity.config;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 节假日配置实体
 *
 * @author APS Team
 * @deprecated 已弃用，请使用 {@link com.zlt.aps.cx.entity.mdm.MdmWorkCalendar} 工作日历来判断开产停产
 */
@Deprecated
@Data
@TableName("cx_holiday_config")
public class CxHolidayConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 节假日名称 */
    private String holidayName;

    /** 节假日日期 */
    private LocalDate holidayDate;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 是否停工 */
    private Integer isStopWork;

    /** 是否启用 */
    private Integer isEnabled;

    /** 备注 */
    private String remark;
}
