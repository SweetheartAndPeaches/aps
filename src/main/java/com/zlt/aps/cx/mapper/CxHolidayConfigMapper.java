package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.config.CxHolidayConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 节假日配置Mapper
 *
 * @author APS Team
 * @deprecated 已弃用，请使用 {@link MdmWorkCalendarMapper} 工作日历Mapper
 */
@Deprecated
@Mapper
public interface CxHolidayConfigMapper extends BaseMapper<CxHolidayConfig> {
}
