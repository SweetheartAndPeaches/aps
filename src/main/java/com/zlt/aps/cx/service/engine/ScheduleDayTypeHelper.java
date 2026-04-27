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
 * <p>按班次级别判断开产/停产逻辑：
 * - 停产班：本班次 = 0(停产)，不做处理
 * - 开产班（首个）：本班次 = 1(开产) 且 上个班次 = 0(停产)，走开产逻辑
 * - 停产前一个班（末个）：本班次 = 1(开产) 且 下个班次 = 0(停产)，走停产前一个班逻辑
 *
 * @author APS Team
 */
@Slf4j
@Component
public class ScheduleDayTypeHelper {

    @Autowired
    private MdmWorkCalendarMapper workCalendarMapper;

    /** 班次停产标志：0-停 */
    private static final String SHIFT_FLAG_STOP = "0";
    
    /** 班次开产标志：1-开 */
    private static final String SHIFT_FLAG_START = "1";

    /**
     * 缓存：factoryCode|yyyy-MM-dd -> 工作日历数据
     * key 格式：工厂编号|日期（字符串），如 "116|2026-05-19"
     */
    private Map<String, MdmWorkCalendar> calendarCache = new HashMap<>();

    /**
     * 缓存加载标记，避免重复加载
     */
    private volatile boolean cacheLoaded = false;

    /**
     * 当前缓存对应的工厂编号
     */
    private String cachedFactoryCode;

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
     * 班次类型枚举（按班次级别）
     */
    public enum ShiftType {
        /** 停产班：本班次=0(停产) */
        CLOSED("停产班"),
        /** 开产班（首个）：本班次=1(开产) 且 上个班次=0(停产) */
        OPEN_START("开产首个班次"),
        /** 停产前一个班（末个）：本班次=1(开产) 且 下个班次=0(停产) */
        BEFORE_CLOSE("停产前一个班次"),
        /** 正常班：本班次=1(开产) 且 上下班次都是开产 */
        NORMAL("正常班");
        
        private final String desc;
        
        ShiftType(String desc) {
            this.desc = desc;
        }
        
        public String getDesc() {
            return desc;
        }
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
     * 班次标识信息
     */
    public static class ShiftFlagInfo {
        /** 日期 */
        public final LocalDate date;
        /** 班次序号 */
        public final int shiftOrder;
        /** 班次名称 */
        public final String shiftName;
        /** 开停产标志：0=停，1=开 */
        public final String flag;

        public ShiftFlagInfo(LocalDate date, int shiftOrder, String shiftName, String flag) {
            this.date = date;
            this.shiftOrder = shiftOrder;
            this.shiftName = shiftName;
            this.flag = flag;
        }
    }

    // ==================== 缓存管理 ====================

    /**
     * 生成缓存 key：工厂编号|日期
     */
    private String buildCacheKey(String factoryCode, LocalDate date) {
        return (factoryCode != null ? factoryCode : "UNKNOWN") + "|" + date.toString();
    }

    /**
     * 生成缓存 key：工厂编号|日期字符串
     */
    private String buildCacheKey(String factoryCode, String dateStr) {
        return (factoryCode != null ? factoryCode : "UNKNOWN") + "|" + dateStr;
    }

    /**
     * 预加载缓存数据（带工厂编号，推荐使用）
     *
     * <p>在排程开始前调用，一次性加载指定日期范围内的所有工作日历数据。
     * 支持按工厂编号和工序编码过滤。
     *
     * @param startDate   开始日期（包含）
     * @param endDate     结束日期（包含）
     * @param factoryCode 工厂编号
     */
    public void preloadCache(LocalDate startDate, LocalDate endDate, String factoryCode) {
        // 扩展日期范围
        LocalDate actualStart = startDate.minusDays(CACHE_EXTRA_DAYS);
        LocalDate actualEnd = endDate.plusDays(CACHE_EXTRA_DAYS);

        log.info("预加载工作日历缓存: 工厂={}, 日期范围={} ~ {}", factoryCode, actualStart, actualEnd);

        try {
            // 先尝试用 factoryCode + procCode="CX" 查询
            LambdaQueryWrapper<MdmWorkCalendar> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MdmWorkCalendar::getProcCode, "CX");
            if (factoryCode != null && !factoryCode.isEmpty()) {
                wrapper.eq(MdmWorkCalendar::getFactoryCode, factoryCode);
            }
            wrapper.ge(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(actualStart))
                   .le(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(actualEnd));

            List<MdmWorkCalendar> list = workCalendarMapper.selectList(wrapper);

            // 如果 CX 查不到数据，尝试用 "03" 作为工序编码（兼容不同数据配置）
            if (list.isEmpty() && factoryCode != null) {
                log.info("PROC_CODE='CX' 未查到工作日历数据(工厂={}), 尝试 PROC_CODE='03'", factoryCode);
                LambdaQueryWrapper<MdmWorkCalendar> wrapper03 = new LambdaQueryWrapper<>();
                wrapper03.eq(MdmWorkCalendar::getProcCode, "03")
                         .eq(MdmWorkCalendar::getFactoryCode, factoryCode)
                         .ge(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(actualStart))
                         .le(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(actualEnd));
                list = workCalendarMapper.selectList(wrapper03);
            }

            // 如果还是查不到，不按工厂过滤重试（兼容老数据）
            if (list.isEmpty() && factoryCode != null) {
                log.info("按工厂+工序仍查不到数据, 尝试仅按 PROC_CODE='CX' 不限工厂");
                LambdaQueryWrapper<MdmWorkCalendar> wrapperNoFactory = new LambdaQueryWrapper<>();
                wrapperNoFactory.eq(MdmWorkCalendar::getProcCode, "CX")
                         .ge(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(actualStart))
                         .le(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(actualEnd));
                list = workCalendarMapper.selectList(wrapperNoFactory);
                
                if (list.isEmpty()) {
                    log.info("仅按 PROC_CODE='CX' 也查不到, 尝试仅按 PROC_CODE='03' 不限工厂");
                    LambdaQueryWrapper<MdmWorkCalendar> wrapper03NoFactory = new LambdaQueryWrapper<>();
                    wrapper03NoFactory.eq(MdmWorkCalendar::getProcCode, "03")
                             .ge(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(actualStart))
                             .le(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(actualEnd));
                    list = workCalendarMapper.selectList(wrapper03NoFactory);
                }
            }

            // 写入缓存
            calendarCache.clear();
            String effectiveFactoryCode = factoryCode;
            for (MdmWorkCalendar calendar : list) {
                if (calendar.getProductionDate() != null) {
                    String calFactoryCode = calendar.getFactoryCode() != null ? calendar.getFactoryCode() : factoryCode;
                    if (effectiveFactoryCode == null && calFactoryCode != null) {
                        effectiveFactoryCode = calFactoryCode;
                    }
                    String dateStr = formatDateKey(calendar.getProductionDate());
                    String key = buildCacheKey(calFactoryCode, dateStr);
                    calendarCache.put(key, calendar);
                }
            }

            cachedFactoryCode = effectiveFactoryCode;
            cacheStartDate = actualStart;
            cacheEndDate = actualEnd;
            cacheLoaded = true;

            log.info("工作日历缓存加载完成: 工厂={}, {} 条记录", effectiveFactoryCode, list.size());
            
            // 输出关键日期的缓存数据用于诊断
            for (MdmWorkCalendar cal : list) {
                if (cal.getProductionDate() != null) {
                    String dateStr = formatDateKey(cal.getProductionDate());
                    log.info("工作日历缓存: 日期={}, 一班={}, 二班={}, 三班={}, DAY_FLAG={}, 工厂={}",
                            dateStr, cal.getOneShiftFlag(), cal.getTwoShiftFlag(), 
                            cal.getThreeShiftFlag(), cal.getDayFlag(), cal.getFactoryCode());
                }
            }
        } catch (Exception e) {
            log.error("预加载工作日历缓存失败", e);
            cacheLoaded = false;
        }
    }

    /**
     * 预加载缓存数据（兼容旧调用）
     *
     * @param startDate 开始日期（包含）
     * @param endDate   结束日期（包含）
     */
    public void preloadCache(LocalDate startDate, LocalDate endDate) {
        preloadCache(startDate, endDate, null);
    }

    /**
     * 确保缓存已加载（懒加载模式）
     *
     * @param date        当前查询日期
     * @param factoryCode 工厂编号
     */
    private void ensureCacheLoaded(LocalDate date, String factoryCode) {
        if (!cacheLoaded || (factoryCode != null && !factoryCode.equals(cachedFactoryCode))) {
            // 默认加载前后30天的数据
            LocalDate start = date.minusDays(30);
            LocalDate end = date.plusDays(30);
            preloadCache(start, end, factoryCode);
        }
    }

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
     * 从缓存或数据库获取工作日历
     *
     * @param queryDate   查询日期
     * @param factoryCode 工厂编号
     * @return 工作日历对象，无则返回 null
     */
    private MdmWorkCalendar getCalendar(LocalDate queryDate, String factoryCode) {
        ensureCacheLoaded(queryDate, factoryCode);

        String key = buildCacheKey(factoryCode, queryDate);
        MdmWorkCalendar calendar = calendarCache.get(key);

        // 如果缓存中没有，尝试用缓存的工厂编号再查一次
        if (calendar == null && cachedFactoryCode != null && !cachedFactoryCode.equals(factoryCode)) {
            key = buildCacheKey(cachedFactoryCode, queryDate);
            calendar = calendarCache.get(key);
        }

        // 如果缓存中没有且在缓存范围内，说明确实没有数据
        if (calendar == null && cacheStartDate != null && cacheEndDate != null) {
            if (queryDate.isBefore(cacheStartDate) || queryDate.isAfter(cacheEndDate)) {
                // 查询日期超出缓存范围，需要扩展缓存
                extendCache(queryDate, factoryCode);
                
                key = buildCacheKey(factoryCode, queryDate);
                calendar = calendarCache.get(key);
                
                if (calendar == null && cachedFactoryCode != null) {
                    key = buildCacheKey(cachedFactoryCode, queryDate);
                    calendar = calendarCache.get(key);
                }
            }
        }

        return calendar;
    }

    /**
     * 扩展缓存范围（懒加载模式下调用）
     */
    private synchronized void extendCache(LocalDate date, String factoryCode) {
        // 再次检查，可能其他线程已经扩展了
        if (cacheStartDate != null && cacheEndDate != null) {
            if (date.isBefore(cacheStartDate) || date.isAfter(cacheEndDate)) {
                log.info("扩展工作日历缓存: 工厂={}, 当前范围 {} ~ {}, 新增日期 {}",
                        factoryCode, cacheStartDate, cacheEndDate, date);

                LocalDate newStart = cacheStartDate.isBefore(date) ? cacheStartDate : date.minusDays(30);
                LocalDate newEnd = cacheEndDate.isAfter(date) ? cacheEndDate : date.plusDays(30);

                // 查询新增范围的数据
                LambdaQueryWrapper<MdmWorkCalendar> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(MdmWorkCalendar::getProcCode, "CX");
                if (factoryCode != null && !factoryCode.isEmpty()) {
                    wrapper.eq(MdmWorkCalendar::getFactoryCode, factoryCode);
                }
                wrapper.ge(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(newStart))
                       .le(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(newEnd));

                List<MdmWorkCalendar> list = workCalendarMapper.selectList(wrapper);

                // 如果 CX 查不到，尝试 03
                if (list.isEmpty() && factoryCode != null) {
                    LambdaQueryWrapper<MdmWorkCalendar> wrapper03 = new LambdaQueryWrapper<>();
                    wrapper03.eq(MdmWorkCalendar::getProcCode, "03")
                             .eq(MdmWorkCalendar::getFactoryCode, factoryCode)
                             .ge(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(newStart))
                             .le(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(newEnd));
                    list = workCalendarMapper.selectList(wrapper03);
                }

                for (MdmWorkCalendar calendar : list) {
                    if (calendar.getProductionDate() != null) {
                        String calFactoryCode = calendar.getFactoryCode() != null ? calendar.getFactoryCode() : factoryCode;
                        String dateKey = formatDateKey(calendar.getProductionDate());
                        String key = buildCacheKey(calFactoryCode, dateKey);
                        calendarCache.put(key, calendar);
                    }
                }

                cacheStartDate = newStart;
                cacheEndDate = newEnd;
            }
        }
    }

    /**
     * 获取班次名称
     */
    private String getShiftName(int shiftOrder) {
        switch (shiftOrder) {
            case 1: return "一班";
            case 2: return "二班";
            case 3: return "三班";
            default: return "未知班次";
        }
    }

    /**
     * 获取每天班次数（默认3班）
     */
    private int getShiftsPerDay(LocalDate date) {
        return 3;
    }

    // ==================== 按班次级别判断方法 ====================

    /**
     * 获取指定班次的开停产标志
     *
     * @param date       日期
     * @param shiftOrder 班次序号（1,2,3）
     * @return 开产标志（0-停, 1-开），默认返回"1"（开产）
     */
    public String getShiftFlag(LocalDate date, int shiftOrder) {
        return getShiftFlag(date, shiftOrder, null);
    }

    /**
     * 获取指定班次的开停产标志（带工厂编号）
     *
     * @param date        日期
     * @param shiftOrder  班次序号（1,2,3）
     * @param factoryCode 工厂编号
     * @return 开产标志（0-停, 1-开），默认返回"1"（开产）
     */
    public String getShiftFlag(LocalDate date, int shiftOrder, String factoryCode) {
        MdmWorkCalendar calendar = getCalendar(date, factoryCode);

        if (calendar == null) {
            log.warn("未找到工作日历配置，工厂={}, 日期={}, 班次={}, 默认视为开产", factoryCode, date, getShiftName(shiftOrder));
            return SHIFT_FLAG_START;
        }

        switch (shiftOrder) {
            case 1:
                return calendar.getOneShiftFlag();
            case 2:
                return calendar.getTwoShiftFlag();
            case 3:
                return calendar.getThreeShiftFlag();
            default:
                log.warn("未知的班次序号: {}，默认视为开产", shiftOrder);
                return SHIFT_FLAG_START;
        }
    }

    /**
     * 获取上一个班次的开停产标志
     * 
     * 班次顺序：一班 → 二班 → 三班 → 下一天一班
     * 一班的"上一个班次" = 前一天的三班
     *
     * @param date       日期
     * @param shiftOrder 班次序号（1,2,3）
     * @return 上一个班次的开产标志（0-停, 1-开），默认返回"1"（开产）
     */
    public String getPreviousShiftFlag(LocalDate date, int shiftOrder) {
        return getPreviousShiftFlag(date, shiftOrder, null);
    }

    /**
     * 获取上一个班次的开停产标志（带工厂编号）
     */
    public String getPreviousShiftFlag(LocalDate date, int shiftOrder, String factoryCode) {
        LocalDate prevDate = date;
        int prevShiftOrder;
        
        // 获取当前排产每天班次数（从班次配置中获取，默认3班）
        int shiftsPerDay = getShiftsPerDay(date);
        
        switch (shiftOrder) {
            case 1:
                // 一班的"上一个班次" = 前一天的最后一个班次
                prevDate = date.minusDays(1);
                prevShiftOrder = shiftsPerDay;
                break;
            case 2:
                // 二班的"上一个班次" = 当天的一班
                prevShiftOrder = 1;
                break;
            case 3:
                // 三班的"上一个班次" = 当天的二班
                prevShiftOrder = 2;
                break;
            default:
                log.warn("未知的班次序号: {}", shiftOrder);
                return SHIFT_FLAG_START;
        }
        
        return getShiftFlag(prevDate, prevShiftOrder, factoryCode);
    }

    /**
     * 获取下一个班次的开停产标志
     * 
     * 班次顺序：一班 → 二班 → 三班 → 下一天一班
     * 三班的"下一个班次" = 下一天的一班
     *
     * @param date       日期
     * @param shiftOrder 班次序号（1,2,3）
     * @return 下一个班次的开产标志（0-停, 1-开），默认返回"1"（开产）
     */
    public String getNextShiftFlag(LocalDate date, int shiftOrder) {
        return getNextShiftFlag(date, shiftOrder, null);
    }

    /**
     * 获取下一个班次的开停产标志（带工厂编号）
     */
    public String getNextShiftFlag(LocalDate date, int shiftOrder, String factoryCode) {
        LocalDate nextDate = date;
        int nextShiftOrder;
        
        switch (shiftOrder) {
            case 1:
                // 一班的"下一个班次" = 当天的二班
                nextShiftOrder = 2;
                break;
            case 2:
                // 二班的"下一个班次" = 当天的三班
                nextShiftOrder = 3;
                break;
            case 3:
                // 三班的"下一个班次" = 下一天的一班
                nextDate = date.plusDays(1);
                nextShiftOrder = 1;
                break;
            default:
                log.warn("未知的班次序号: {}", shiftOrder);
                return SHIFT_FLAG_START;
        }
        
        return getShiftFlag(nextDate, nextShiftOrder, factoryCode);
    }

    // ==================== 班次类型判定 ====================

    /**
     * 按班次级别判断班次类型
     * 
     * 判断逻辑：
     * - 停产班：本班次 = 0(停产)，不做处理
     * - 开产班（首个）：本班次 = 1(开产) 且 上个班次 = 0(停产)，走开产逻辑
     * - 停产前一个班（末个）：本班次 = 1(开产) 且 下个班次 = 0(停产)，走停产前一个班逻辑
     *
     * @param date       日期
     * @param shiftOrder 班次序号（1,2,3）
     * @return 班次类型
     */
    public ShiftType determineShiftType(LocalDate date, int shiftOrder) {
        return determineShiftType(date, shiftOrder, null);
    }

    /**
     * 按班次级别判断班次类型（带工厂编号）
     */
    public ShiftType determineShiftType(LocalDate date, int shiftOrder, String factoryCode) {
        String currentFlag = getShiftFlag(date, shiftOrder, factoryCode);
        
        // 1. 判断是否为停产班
        if (SHIFT_FLAG_STOP.equals(currentFlag)) {
            log.debug("班次类型判定：工厂={}, 日期={}, 当天第{}班, 结果=停产班", 
                    factoryCode, date, shiftOrder);
            return ShiftType.CLOSED;
        }
        
        // 2. 本班次是开产，判断是开产首个班还是停产前一个班
        String prevFlag = getPreviousShiftFlag(date, shiftOrder, factoryCode);
        String nextFlag = getNextShiftFlag(date, shiftOrder, factoryCode);
        
        // 上个班次是停产 -> 开产首个班次
        if (SHIFT_FLAG_STOP.equals(prevFlag)) {
            log.debug("班次类型判定：工厂={}, 日期={}, 当天第{}班, 上个班次停产, 结果=开产首个班次", 
                    factoryCode, date, shiftOrder);
            return ShiftType.OPEN_START;
        }
        
        // 下个班次是停产 -> 停产前一个班次
        if (SHIFT_FLAG_STOP.equals(nextFlag)) {
            log.debug("班次类型判定：工厂={}, 日期={}, 当天第{}班, 下个班次停产, 结果=停产前一个班次", 
                    factoryCode, date, shiftOrder);
            return ShiftType.BEFORE_CLOSE;
        }
        
        // 正常班
        log.debug("班次类型判定：工厂={}, 日期={}, 当天第{}班, 结果=正常班(上下班次均正常)", 
                factoryCode, date, shiftOrder);
        return ShiftType.NORMAL;
    }

    // ==================== 判断方法（带工厂编号） ====================

    /**
     * 判断是否为停产班（本班次=0），带工厂编号
     */
    public boolean isClosedShift(LocalDate date, int shiftOrder, String factoryCode) {
        return determineShiftType(date, shiftOrder, factoryCode) == ShiftType.CLOSED;
    }

    /**
     * 判断是否为开产首个班次（本班次=1 且 上班次=0），带工厂编号
     */
    public boolean isOpenStartShift(LocalDate date, int shiftOrder, String factoryCode) {
        return determineShiftType(date, shiftOrder, factoryCode) == ShiftType.OPEN_START;
    }

    /**
     * 判断是否为停产班（本班次=0），带工厂编号
     */
    public boolean isClosingShift(LocalDate date, int shiftOrder, String factoryCode) {
        return determineShiftType(date, shiftOrder, factoryCode) == ShiftType.CLOSED;
    }

    /**
     * 判断是否为停产前一个班次（本班次=1 且 下班次=0），带工厂编号
     */
    public boolean isBeforeCloseShift(LocalDate date, int shiftOrder, String factoryCode) {
        return determineShiftType(date, shiftOrder, factoryCode) == ShiftType.BEFORE_CLOSE;
    }

    // ==================== 班次停产判断（用于排程主循环跳过停产班次） ====================

    /**
     * 判断某天某班次是否停产（带工厂编号）
     *
     * @param date          查询日期
     * @param dayShiftOrder 班次顺序（1=一班, 2=二班, 3=三班）
     * @param factoryCode   工厂编号
     * @return true 表示该班次停产
     */
    public boolean isShiftStopped(LocalDate date, int dayShiftOrder, String factoryCode) {
        String flag = getShiftFlag(date, dayShiftOrder, factoryCode);
        log.debug("班次停产判断：工厂={}, 日期={}, 当天第{}班, 标志={}, 结果={}", 
                factoryCode, date, dayShiftOrder, flag, SHIFT_FLAG_STOP.equals(flag));
        return SHIFT_FLAG_STOP.equals(flag);
    }

    /**
     * 判断某天是否整天停产（dayFlag=0 或 三个班次全部停产）
     *
     * @param date 查询日期
     * @return true 表示整天停产
     */
    public boolean isFullDayStopped(LocalDate date) {
        return isFullDayStopped(date, null);
    }

    /**
     * 判断某天是否整天停产（带工厂编号）
     */
    public boolean isFullDayStopped(LocalDate date, String factoryCode) {
        MdmWorkCalendar calendar = getCalendar(date, factoryCode);
        if (calendar == null) {
            return false;
        }
        if ("0".equals(calendar.getDayFlag())) {
            return true;
        }
        boolean shift1Stopped = SHIFT_FLAG_STOP.equals(calendar.getOneShiftFlag());
        boolean shift2Stopped = SHIFT_FLAG_STOP.equals(calendar.getTwoShiftFlag());
        boolean shift3Stopped = SHIFT_FLAG_STOP.equals(calendar.getThreeShiftFlag());
        return shift1Stopped && shift2Stopped && shift3Stopped;
    }

    /**
     * 判断某天是否包含任一班次停产（用于跨天封顶检查）
     * 只要有一个班次的shift_flag="0"就返回true
     */
    public boolean hasAnyClosingShift(LocalDate date, String factoryCode) {
        MdmWorkCalendar calendar = getCalendar(date, factoryCode);
        if (calendar == null) {
            return false;
        }
        return SHIFT_FLAG_STOP.equals(calendar.getOneShiftFlag())
                || SHIFT_FLAG_STOP.equals(calendar.getTwoShiftFlag())
                || SHIFT_FLAG_STOP.equals(calendar.getThreeShiftFlag());
    }

    // ==================== 原有按天级别判断方法（兼容保留） ====================

    /**
     * 从当前排产日期往前找最近一个有 dayFlag 标识的日期
     *
     * @param date 当前排产日期
     * @return 最近标识信息，包含标识日期和标识值（"0"=停，"1"=开）
     */
    public DayFlagInfo findNearestDayFlag(LocalDate date) {
        return findNearestDayFlag(date, null);
    }

    /**
     * 从当前排产日期往前找最近一个有 dayFlag 标识的日期（带工厂编号）
     */
    public DayFlagInfo findNearestDayFlag(LocalDate date, String factoryCode) {
        // 最多往前查 30 天
        for (int i = 0; i < 30; i++) {
            LocalDate queryDate = date.minusDays(i);
            MdmWorkCalendar calendar = getCalendar(queryDate, factoryCode);
            if (calendar != null && calendar.getDayFlag() != null) {
                return new DayFlagInfo(queryDate, calendar.getDayFlag());
            }
        }
        return null;
    }

    /**
     * 判断是否为停产日（已停产，往后停产）
     */
    public boolean isStopDay(LocalDate date) {
        return isStopDay(date, null);
    }

    /**
     * 判断是否为停产日（已停产，带工厂编号）
     */
    public boolean isStopDay(LocalDate date, String factoryCode) {
        DayFlagInfo flagInfo = findNearestDayFlag(date, factoryCode);
        if (flagInfo == null || flagInfo.dayFlag == null) {
            return false;
        }
        return "0".equals(flagInfo.dayFlag) && date.isAfter(flagInfo.nearestDate);
    }

    /**
     * 判断是否为停产标识日（停产标记当天，最后一天有量）
     */
    public boolean isStopFlagDay(LocalDate date) {
        return isStopFlagDay(date, null);
    }

    /**
     * 判断是否为停产标识日（带工厂编号）
     */
    public boolean isStopFlagDay(LocalDate date, String factoryCode) {
        DayFlagInfo flagInfo = findNearestDayFlag(date, factoryCode);
        if (flagInfo == null || flagInfo.dayFlag == null) {
            return false;
        }
        return "0".equals(flagInfo.dayFlag) && !date.isAfter(flagInfo.nearestDate);
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
                String start = startTime.length() >= 5 ? startTime.substring(0, 5) : startTime;
                String end = endTime.length() >= 5 ? endTime.substring(0, 5) : endTime;
                boolean inShift;
                if (start.compareTo(end) <= 0) {
                    inShift = timeStr.compareTo(start) >= 0 && timeStr.compareTo(end) < 0;
                } else {
                    inShift = timeStr.compareTo(start) >= 0 || timeStr.compareTo(end) < 0;
                }
                if (inShift) {
                    return shiftConfig.getDayShiftOrder();
                }
            }
        }
        // 可能是最后一个班次的结束时间（跨天），取最后一个班次
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
        // 时间早于第一个班次的开始时间（如停锅05:30，第一班06:00），归为第一班
        com.zlt.aps.cx.entity.config.CxShiftConfig firstShift = shiftConfigs.get(0);
        if (firstShift != null && firstShift.getDayShiftOrder() != null) {
            return firstShift.getDayShiftOrder();
        }
        return null;
    }
}
