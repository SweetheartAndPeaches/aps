package com.zlt.aps.cx.service;

import com.zlt.aps.cx.entity.schedule.CxScheduleAlgorithmLog;

import java.time.LocalDate;
import java.util.List;

/**
 * 成型算法日志服务接口
 *
 * @author APS Team
 */
public interface CxScheduleAlgorithmLogService {

    /**
     * 记录算法日志
     *
     * @param log 日志对象
     * @return 是否成功
     */
    boolean saveLog(CxScheduleAlgorithmLog log);

    /**
     * 批量记录算法日志
     *
     * @param logs 日志列表
     * @return 成功数量
     */
    int saveBatchLogs(List<CxScheduleAlgorithmLog> logs);

    /**
     * 根据排程日期删除历史日志（保留最新一次排程的日志）
     *
     * @param scheduleDate 排程日期
     * @param factoryCode 工厂编码
     * @return 删除数量
     */
    int deleteHistoryLogs(LocalDate scheduleDate, String factoryCode);

    /**
     * 查询指定排程日期的日志
     *
     * @param scheduleDate 排程日期
     * @return 日志列表
     */
    List<CxScheduleAlgorithmLog> getLogsByScheduleDate(LocalDate scheduleDate);

    /**
     * 查询指定排程日期和结构的日志
     *
     * @param scheduleDate 排程日期
     * @param structureName 结构名称
     * @return 日志列表
     */
    List<CxScheduleAlgorithmLog> getLogsByScheduleDateAndStructure(LocalDate scheduleDate, String structureName);

    /**
     * 查询最新的日志
     *
     * @param limit 返回数量
     * @return 日志列表
     */
    List<CxScheduleAlgorithmLog> getLatestLogs(int limit);
}
