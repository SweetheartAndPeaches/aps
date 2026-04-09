package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.service.impl.CoreScheduleAlgorithmServiceImpl;
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
@Component
public class ScheduleDayTypeHelper {

    @Autowired
    private CoreScheduleAlgorithmServiceImpl coreScheduleAlgorithmService;

    /**
     * 获取指定日期对应的 DayFlagInfo
     *
     * @param date 查询日期
     * @return 最近的标识信息，无则返回 null
     */
    public CoreScheduleAlgorithmServiceImpl.DayFlagInfo getDayFlagInfo(LocalDate date) {
        return coreScheduleAlgorithmService.findNearestDayFlag(date);
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
        CoreScheduleAlgorithmServiceImpl.DayFlagInfo flagInfo =
                coreScheduleAlgorithmService.findNearestDayFlag(date);
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
        CoreScheduleAlgorithmServiceImpl.DayFlagInfo flagInfo =
                coreScheduleAlgorithmService.findNearestDayFlag(date);
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
        CoreScheduleAlgorithmServiceImpl.DayFlagInfo flagInfo =
                coreScheduleAlgorithmService.findNearestDayFlag(date);
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
