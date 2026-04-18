package com.zlt.aps.cx.service.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cx.mapper.MdmWorkCalendarMapper;
import com.zlt.aps.mp.api.domain.entity.MdmWorkCalendar;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 开停产日判断工具类
 *
 * <p>统一管理所有开停产日判断逻辑，所有涉及开产日/停产日判断的代码应优先使用此类。
 * <p>优化：数据一次性加载到缓存，后续查询从缓存获取，避免频繁数据库访问。
 *
 * @author APS Team
 */
@Slf4j
@Component
public class ScheduleDayTypeHelper {

    @Autowired
    private MdmWorkCalendarMapper workCalendarMapper;

    /**
     * 缓存：日期 -> 工作日历数据
     * key 格式：yyyy-MM-dd（字符串）
     */
    private Map<String, MdmWorkCalendar> calendarCache = new HashMap<>();

    /**
     * 缓存加载标记，避免重复加载
     */
    private volatile boolean cacheLoaded = false;

    /**
     * 缓存加载的数据范围
     */
    private LocalDate cacheStartDate;
    private LocalDate cacheEndDate;

    /**
     * 缓存数据的天数范围（前后各扩展几天，防止边界问题）
     */
    private static final int CACHE_EXTRA_DAYS = 5;

    /**
     * 日期格式化工具
     */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * 将 Date 转换为 yyyy-MM-dd 格式的字符串作为缓存 key
     */
    private String formatDateKey(java.util.Date date) {
        if (date == null) {
            return null;
        }
        return DATE_FORMAT.format(date);
    }

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
     * 预加载缓存数据
     *
     * <p>在排程开始前调用，一次性加载指定日期范围内的所有工作日历数据。
     *
     * @param startDate 开始日期（包含）
     * @param endDate   结束日期（包含）
     */
    public void preloadCache(LocalDate startDate, LocalDate endDate) {
        // 扩展日期范围
        LocalDate actualStart = startDate.minusDays(CACHE_EXTRA_DAYS);
        LocalDate actualEnd = endDate.plusDays(CACHE_EXTRA_DAYS);

        log.info("预加载工作日历缓存: {} ~ {}", actualStart, actualEnd);

        try {
            // 一次性查询所有数据
            LambdaQueryWrapper<MdmWorkCalendar> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MdmWorkCalendar::getProcCode, "01")
                   .ge(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(actualStart))
                   .le(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(actualEnd));

            List<MdmWorkCalendar> list = workCalendarMapper.selectList(wrapper);

            // 写入缓存
            calendarCache.clear();
            for (MdmWorkCalendar calendar : list) {
                if (calendar.getProductionDate() != null) {
                    String key = formatDateKey(calendar.getProductionDate());
                    calendarCache.put(key, calendar);
                }
            }

            cacheStartDate = actualStart;
            cacheEndDate = actualEnd;
            cacheLoaded = true;

            log.info("工作日历缓存加载完成: {} 条记录", list.size());
        } catch (Exception e) {
            log.error("预加载工作日历缓存失败", e);
            cacheLoaded = false;
        }
    }

    /**
     * 确保缓存已加载（懒加载模式）
     *
     * @param date 当前查询日期
     */
    private void ensureCacheLoaded(LocalDate date) {
        if (!cacheLoaded) {
            // 默认加载前后30天的数据
            LocalDate start = date.minusDays(30);
            LocalDate end = date.plusDays(30);
            preloadCache(start, end);
        }
    }

    /**
     * 从缓存或数据库获取工作日历
     *
     * @param queryDate 查询日期
     * @return 工作日历对象，无则返回 null
     */
    private MdmWorkCalendar getCalendar(LocalDate queryDate) {
        ensureCacheLoaded(queryDate);

        String key = queryDate.toString();
        MdmWorkCalendar calendar = calendarCache.get(key);

        // 如果缓存中没有且在缓存范围内，说明确实没有数据
        if (calendar == null && cacheStartDate != null && cacheEndDate != null) {
            if (queryDate.isBefore(cacheStartDate) || queryDate.isAfter(cacheEndDate)) {
                // 查询日期超出缓存范围，需要扩展缓存
                extendCache(queryDate);
                calendar = calendarCache.get(key);
            }
        }

        return calendar;
    }

    /**
     * 扩展缓存范围（懒加载模式下调用）
     */
    private synchronized void extendCache(LocalDate date) {
        // 再次检查，可能其他线程已经扩展了
        if (cacheStartDate != null && cacheEndDate != null) {
            if (date.isBefore(cacheStartDate) || date.isAfter(cacheEndDate)) {
                log.info("扩展工作日历缓存: 当前范围 {} ~ {}, 新增日期 {}",
                        cacheStartDate, cacheEndDate, date);

                LocalDate newStart = cacheStartDate.isBefore(date) ? cacheStartDate : date.minusDays(30);
                LocalDate newEnd = cacheEndDate.isAfter(date) ? cacheEndDate : date.plusDays(30);

                // 查询新增范围的数据
                LambdaQueryWrapper<MdmWorkCalendar> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(MdmWorkCalendar::getProcCode, "01")
                       .ge(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(newStart))
                       .le(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(newEnd));

                List<MdmWorkCalendar> list = workCalendarMapper.selectList(wrapper);

                for (MdmWorkCalendar calendar : list) {
                    if (calendar.getProductionDate() != null) {
                        String key = formatDateKey(calendar.getProductionDate());
                        calendarCache.put(key, calendar);
                    }
                }

                cacheStartDate = newStart;
                cacheEndDate = newEnd;
            }
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
            MdmWorkCalendar calendar = getCalendar(queryDate);
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

    /**
     * 清理缓存（测试用或需要重新加载时调用）
     */
    public void clearCache() {
        calendarCache.clear();
        cacheLoaded = false;
        cacheStartDate = null;
        cacheEndDate = null;
    }

    /**
     * 判断某天某班次是否停产
     *
     * @param date 查询日期
     * @param dayShiftOrder 班次顺序（1=一班, 2=二班, 3=三班）
     * @return true 表示该班次停产
     */
    public boolean isShiftStopped(LocalDate date, int dayShiftOrder) {
        ensureCacheLoaded(date);
        MdmWorkCalendar calendar = calendarCache.get(date);
        if (calendar == null) {
            return false;
        }
        switch (dayShiftOrder) {
            case 1: return "0".equals(calendar.getOneShiftFlag());
            case 2: return "0".equals(calendar.getTwoShiftFlag());
            case 3: return "0".equals(calendar.getThreeShiftFlag());
            default: return false;
        }
    }

    /**
     * 判断某天是否整天停产（dayFlag=0 或 三个班次全部停产）
     *
     * @param date 查询日期
     * @return true 表示整天停产
     */
    public boolean isFullDayStopped(LocalDate date) {
        ensureCacheLoaded(date);
        MdmWorkCalendar calendar = calendarCache.get(date);
        if (calendar == null) {
            return false;
        }
        if ("0".equals(calendar.getDayFlag())) {
            return true;
        }
        boolean shift1Stopped = "0".equals(calendar.getOneShiftFlag());
        boolean shift2Stopped = "0".equals(calendar.getTwoShiftFlag());
        boolean shift3Stopped = "0".equals(calendar.getThreeShiftFlag());
        return shift1Stopped && shift2Stopped && shift3Stopped;
    }

    /**
     * 根据时间字符串（HH:mm格式）确定对应的班次序号
     *
     * <p>逻辑：遍历班次配置（按dayShiftOrder排序），找到 startTime <= timeStr < endTime 的班次。
     * 如果时间恰好是某个班次的开始时间，则属于该班次。
     *
     * @param timeStr       时间字符串（HH:mm格式，如 "08:00"）
     * @param shiftConfigs  班次配置列表（已按dayShiftOrder排序）
     * @return 对应的dayShiftOrder（1=一班, 2=二班, 3=三班），找不到返回null
     */
    public Integer getShiftOrderByTime(String timeStr, List<com.zlt.aps.cx.entity.config.CxShiftConfig> shiftConfigs) {
        if (timeStr == null || shiftConfigs == null || shiftConfigs.isEmpty()) {
            return null;
        }
        for (com.zlt.aps.cx.entity.config.CxShiftConfig shiftConfig : shiftConfigs) {
            String startTime = shiftConfig.getStartTime();
            String endTime = shiftConfig.getEndTime();
            if (startTime != null && endTime != null) {
                // 时间格式为 HH:mm:ss，截取前5位比较
                String start = startTime.length() >= 5 ? startTime.substring(0, 5) : startTime;
                String end = endTime.length() >= 5 ? endTime.substring(0, 5) : endTime;
                if (timeStr.compareTo(start) >= 0 && timeStr.compareTo(end) < 0) {
                    return shiftConfig.getDayShiftOrder();
                }
            }
        }
        // 可能是最后一个班次的结束时间（跨天），取最后一个班次
        // 或者时间是最后一个班次的时间范围（如 16:00-24:00，但24:00不在<范围内）
        for (int i = shiftConfigs.size() - 1; i >= 0; i--) {
            com.zlt.aps.cx.entity.config.CxShiftConfig shiftConfig = shiftConfigs.get(i);
            String startTime = shiftConfig.getStartTime();
            if (startTime != null) {
                String start = startTime.length() >= 5 ? startTime.substring(0, 5) : startTime;
                if (timeStr.compareTo(start) >= 0) {
                    return shiftConfig.getDayShiftOrder();
                }
            }
        }
        return null;
    }
}
