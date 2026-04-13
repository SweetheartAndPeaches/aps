package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cx.entity.schedule.CxScheduleAlgorithmLog;
import com.zlt.aps.cx.mapper.CxScheduleAlgorithmLogMapper;
import com.zlt.aps.cx.service.CxAlgorithmLogRecorder;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 算法日志记录器实现
 * 
 * <p>记录排产过程中的详细操作日志，每个结构一条记录。
 * 日志格式清晰易读，便于测试人员验证算法准确性。
 *
 * @author APS Team
 */
@Slf4j
@Service
public class CxAlgorithmLogRecorderImpl implements CxAlgorithmLogRecorder {

    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private CxScheduleAlgorithmLogMapper algorithmLogMapper;

    /** 线程本地存储当前排程批次的日志列表 */
    private final ThreadLocal<ConcurrentHashMap<String, StringBuilder>> logBuffer = ThreadLocal.withInitial(ConcurrentHashMap::new);

    /** 当前批次号 */
    private final ThreadLocal<String> currentBatchNo = ThreadLocal.withInitial(() -> null);

    /** 当前结构名称 */
    private final ThreadLocal<String> currentStructure = ThreadLocal.withInitial(() -> "UNKNOWN");

    /** 当前开始时间 */
    private final ThreadLocal<LocalDateTime> currentStartTime = ThreadLocal.withInitial(() -> null);

    /** 当前操作类型 */
    private final ThreadLocal<String> currentOperation = ThreadLocal.withInitial(() -> "自动调整");

    /** 执行开始时间 */
    private final ThreadLocal<Long> executionStartTime = ThreadLocal.withInitial(() -> System.currentTimeMillis());

    @Override
    public void setCurrentStructure(String structureName) {
        currentStructure.set(structureName != null ? structureName : "UNKNOWN");
    }

    @Override
    public String getCurrentStructure() {
        return currentStructure.get();
    }

    @Override
    public void startNewBatch(String batchNo) {
        // 清空旧数据
        clear();
        currentBatchNo.set(batchNo);
        executionStartTime.set(System.currentTimeMillis());
        appendLog("========== 开始新的排程批次: " + batchNo + " ==========");
        log.info("========== 开始新的排程批次: {} ==========", batchNo);
    }

    @Override
    public String getBatchNo() {
        return currentBatchNo.get();
    }

    @Override
    public void logStart(String operation) {
        currentOperation.set(operation);
        currentStartTime.set(LocalDateTime.now());
        String struct = currentStructure.get();
        String timestamp = currentStartTime.get().format(DT_FORMATTER);
        String line = String.format("结构:%s,【%s】,开始时间:%s", struct, operation, timestamp);
        appendLog(line);
        log.info(line);
    }

    @Override
    public void logDetail(String operation, String detail) {
        String struct = currentStructure.get();
        String line = String.format("结构:%s,【%s】,%s", struct, operation, detail);
        appendLog(line);
        log.info(line);
    }

    @Override
    public void logEnd(String operation, String result) {
        LocalDateTime startTime = currentStartTime.get();
        LocalDateTime endTime = LocalDateTime.now();
        String struct = currentStructure.get();
        long durationMs = 0;
        if (startTime != null) {
            durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("结构:%s,【%s】,结束时间:%s", struct, operation, endTime.format(DT_FORMATTER)));
        if (durationMs > 0) {
            sb.append(String.format(",总耗时:%d毫秒", durationMs));
        }
        if (result != null && !result.isEmpty()) {
            sb.append(",").append(result);
        }
        
        String line = sb.toString();
        appendLog(line);
        log.info(line);
    }

    @Override
    public void logSuccess(String operation, String message) {
        String struct = currentStructure.get();
        String line = String.format("结构:%s,【%s】,✓ %s", struct, operation, message);
        appendLog(line);
        log.info(line);
    }

    @Override
    public void logWarn(String operation, String message) {
        String struct = currentStructure.get();
        String line = String.format("结构:%s,【%s】,⚠ %s", struct, operation, message);
        appendLog(line);
        log.warn(line);
    }

    @Override
    public void logError(String operation, String message) {
        String struct = currentStructure.get();
        String line = String.format("结构:%s,【%s】,✗ %s", struct, operation, message);
        appendLog(line);
        log.error(line);
    }

    @Override
    public String getAllLogs() {
        ConcurrentHashMap<String, StringBuilder> buffer = logBuffer.get();
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, StringBuilder> entry : buffer.entrySet()) {
            result.append("【结构:").append(entry.getKey()).append("】\n");
            result.append(entry.getValue());
            result.append("\n");
        }
        return result.toString();
    }

    @Override
    public ConcurrentHashMap<String, String> getLogMap() {
        ConcurrentHashMap<String, StringBuilder> buffer = logBuffer.get();
        ConcurrentHashMap<String, String> result = new ConcurrentHashMap<>();
        for (Map.Entry<String, StringBuilder> entry : buffer.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toString());
        }
        return result;
    }

    @Override
    public void clear() {
        logBuffer.get().clear();
        currentBatchNo.remove();
        currentStructure.remove();
        currentStartTime.remove();
        currentOperation.remove();
    }

    @Override
    public String getBatchSummary() {
        ConcurrentHashMap<String, StringBuilder> buffer = logBuffer.get();
        return String.format("排程批次统计: 共 %d 个结构", buffer.size());
    }

    @Override
    public long getExecutionTimeMs() {
        Long start = executionStartTime.get();
        return start != null ? System.currentTimeMillis() - start : 0;
    }

    @Override
    @Async
    public void saveLogsToDatabase(ScheduleContextVo context, int totalPlanQty, int resultCount) {
        try {
            String batchNo = currentBatchNo.get();
            if (batchNo == null || batchNo.isEmpty()) {
                batchNo = "BATCH_" + System.currentTimeMillis();
            }

            ConcurrentHashMap<String, StringBuilder> buffer = logBuffer.get();
            
            for (Map.Entry<String, StringBuilder> entry : buffer.entrySet()) {
                String structureName = entry.getKey();
                String logContent = entry.getValue().toString();
                
                CxScheduleAlgorithmLog algorithmLog = new CxScheduleAlgorithmLog();
                algorithmLog.setScheduleBatchNo(batchNo);
                algorithmLog.setScheduleDate(context.getScheduleDate());
                algorithmLog.setFactoryCode(context.getFactoryCode());
                algorithmLog.setScheduleType(context.getScheduleType());
                algorithmLog.setStructureName(structureName);
                algorithmLog.setLogContent(logContent);
                algorithmLog.setTotalPlanQty(BigDecimal.valueOf(totalPlanQty));
                algorithmLog.setScheduleResultCount(resultCount);
                algorithmLog.setExecutionTimeMs(getExecutionTimeMs());
                
                algorithmLogMapper.insert(algorithmLog);
            }

            log.info("算法日志已保存到数据库，共 {} 条记录", buffer.size());
        } catch (Exception e) {
            log.error("保存算法日志到数据库失败: {}", e.getMessage(), e);
        }
    }
}
