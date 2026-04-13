package com.zlt.aps.cx.service;

import com.zlt.aps.cx.vo.ScheduleContextVo;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 成型算法日志记录器接口
 *
 * <p>记录排产过程中的详细操作日志，每个结构一条记录。
 * 日志格式清晰易读，便于测试人员验证算法准确性。
 *
 * @author APS Team
 */
public interface CxAlgorithmLogRecorder {

    /**
     * 设置当前正在处理的结构名称
     */
    void setCurrentStructure(String structureName);

    /**
     * 获取当前结构名称
     */
    String getCurrentStructure();

    /**
     * 开始新的排程批次
     */
    void startNewBatch(String batchNo);

    /**
     * 获取当前批次号
     */
    String getBatchNo();

    /**
     * 开始一个操作阶段
     */
    void logStart(String operation);

    /**
     * 记录操作详情
     */
    void logDetail(String operation, String detail);

    /**
     * 结束一个操作阶段
     */
    void logEnd(String operation, String result);

    /**
     * 记录成功信息
     */
    void logSuccess(String operation, String message);

    /**
     * 记录警告信息
     */
    void logWarn(String operation, String message);

    /**
     * 记录错误信息
     */
    void logError(String operation, String message);

    /**
     * 获取所有结构的完整日志
     */
    String getAllLogs();

    /**
     * 获取日志Map（结构名 -> 日志内容）
     */
    ConcurrentHashMap<String, String> getLogMap();

    /**
     * 清空当前线程的日志缓冲区
     */
    void clear();

    /**
     * 获取批次统计信息
     */
    String getBatchSummary();

    /**
     * 获取执行耗时
     */
    long getExecutionTimeMs();

    /**
     * 异步保存日志到数据库
     */
    void saveLogsToDatabase(ScheduleContextVo context, int totalPlanQty, int resultCount);
}
