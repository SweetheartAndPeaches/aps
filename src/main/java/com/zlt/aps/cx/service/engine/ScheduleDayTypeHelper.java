package com.zlt.aps.cx.service.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cx.mapper.MdmWorkCalendarMapper;
import com.zlt.aps.mp.api.domain.entity.MdmWorkCalendar;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 开停产日判断工具类
 *
 * <p>统一管理所有开停产日判断逻辑，所有涉及开产日/停产日判断的代码应优先使用此类。
 *
 * @author APS Team
 */
@Slf4j
@Component
public class ScheduleDayTypeHelper {

    @Autowired
    private MdmWorkCalendarMapper workCalendarMapper;

    /**
     * 最近工作日历标识信息
     */
    public static class DayFlagInfo {
        /** 标识日期 */
        public final LocalDate nearestDate;
        /** 标识值：0=停，1=开 */
        public final String dayFlag;

        public DayFlagInfo(LocalDate nearestDate, String dayFlag) {
            this.nearestDate = nearestDate;
            this.dayFlag = dayFlag;
        }
    }

    /**
     * 从当前排产日期往前找最近一个有 dayFlag 标识的日期
     *
     * @param date 当前排产日期
     * @return 最近标识信息，包含标识日期和标识值（"0"=停，"1"=开）
     */
    public DayFlagInfo findNearestDayFlag(LocalDate date) {
        // 最多往前查 30 天
        for (int i = 0; i < 30; i++) {
            LocalDate queryDate = date.minusDays(i);
            MdmWorkCalendar calendar = workCalendarMapper.selectOne(
                    new LambdaQueryWrapper<MdmWorkCalendar>()
                            .eq(MdmWorkCalendar::getProcCode, "CX")
                            .eq(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(queryDate)));
            if (calendar != null && calendar.getDayFlag() != null) {
                return new DayFlagInfo(queryDate, calendar.getDayFlag());
            }
        }
        return null;
    }

    /**
     * 获取指定日期对应的 DayFlagInfo
     *
     * @param date 查询日期
     * @return 最近的标识信息，无则返回 null
     */
    public DayFlagInfo getDayFlagInfo(LocalDate date) {
        return findNearestDayFlag(date);
    }

    /**
     * 判断是否为停产日（已停产）
     *
     * <p>判断规则：从当前日期往前找最近一个有 dayFlag 标识的日期。
     * <ul>
     *   <li>若最近标识为「停」（dayFlag="0"）</li>
     *   <li>停产标识日当天有量（最后一天生产）</li>
     *   <li>停产标识日之后才算停产 → 返回 true</li>
     * </ul>
     *
     * @param date 查询日期
     * @return true 表示停产日（已停产）
     */
    public boolean isStopDay(LocalDate date) {
        DayFlagInfo flagInfo = findNearestDayFlag(date);
        if (flagInfo == null || flagInfo.dayFlag == null) {
            return false;
        }
        return "0".equals(flagInfo.dayFlag) && date.isAfter(flagInfo.nearestDate);
    }

    /**
     * 判断是否为停产标识日（停产标记当天，最后一天有量）
     *
     * @param date 查询日期
     * @return true 表示停产标识日
     */
    public boolean isStopFlagDay(LocalDate date) {
        DayFlagInfo flagInfo = findNearestDayFlag(date);
        if (flagInfo == null || flagInfo.dayFlag == null) {
            return false;
        }
        return "0".equals(flagInfo.dayFlag) && !date.isAfter(flagInfo.nearestDate);
    }

    /**
     * 判断是否为开产日
     *
     * <p>从当前日期往前找最近一个有 dayFlag 标识的日期，
     * 若最近标识为「开」（dayFlag="1"）则视为开产日。
     *
     * @param date 查询日期
     * @return true 表示开产日
     */
    public boolean isOpeningDay(LocalDate date) {
        DayFlagInfo flagInfo = findNearestDayFlag(date);
        return flagInfo != null && "1".equals(flagInfo.dayFlag);
    }

    /**
     * 判断是否正常生产日（既不是停产日也不是停产标识日）
     *
     * @param date 查询日期
     * @return true 表示正常生产日
     */
    public boolean isNormalProductionDay(LocalDate date) {
        return !isStopDay(date) && !isStopFlagDay(date);
    }
}
