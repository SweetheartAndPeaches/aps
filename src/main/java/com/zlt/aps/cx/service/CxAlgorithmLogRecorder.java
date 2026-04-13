package com.zlt.aps.cx.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 成型算法日志记录器
 * 
 * <p>记录排产过程中的详细逻辑操作流水账，每个结构一条日志。
 * 日志格式清晰易读，便于测试人员验证算法准确性。
 * 
 * <p>日志格式：
 * <pre>
 * 结构:XXX,【操作类型】,开始时间:2026-04-10 16:44:13
 * 结构:XXX,【操作类型】,物料编码:123,参数:值
 * 结构:XXX,【操作类型】,结束时间:2026-04-10 16:44:14,总耗时:1000毫秒
 * </pre>
 *
 * @author APS Team
 */
@Slf4j
@Component
public class CxAlgorithmLogRecorder {

    /** 线程本地存储当前排程批次的日志列表 */
    private final ThreadLocal<ConcurrentHashMap<String, StringBuilder>> logBuffer = ThreadLocal.withInitial(ConcurrentHashMap::new);

    /** 日期时间格式化 */
    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 分隔线 */
    private static final String SEPARATOR = "---------------------------------------------------";

    /** 当前结构名称 */
    private ThreadLocal<String> currentStructure = ThreadLocal.withInitial(() -> "UNKNOWN");

    /**
     * 设置当前正在处理的结构名称
     */
    public void setCurrentStructure(String structureName) {
        currentStructure.set(structureName != null ? structureName : "UNKNOWN");
    }

    /**
     * 获取当前结构名称
     */
    public String getCurrentStructure() {
        return currentStructure.get();
    }

    /**
     * 开始一个操作阶段
     * 
     * @param operation 操作类型，如：自动调整、减量调整、在机SKU增量、新增SKU、增模排产
     */
    public void logStart(String operation) {
        String struct = currentStructure.get();
        String timestamp = LocalDateTime.now().format(DT_FORMATTER);
        String line = String.format("结构:%s,【%s】,开始时间:%s", struct, operation, timestamp);
        appendLog(struct, line);
        log.info(line);
    }

    /**
     * 结束一个操作阶段
     * 
     * @param operation 操作类型
     * @param startTime 开始时间
     * @param details 额外详情（可选）
     */
    public void logEnd(String operation, LocalDateTime startTime, String... details) {
        String struct = currentStructure.get();
        String timestamp = LocalDateTime.now().format(DT_FORMATTER);
        long durationMs = 0;
        if (startTime != null) {
            durationMs = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("结构:%s,【%s】,结束时间:%s", struct, operation, timestamp));
        if (durationMs > 0) {
            sb.append(String.format(",总耗时:%d毫秒", durationMs));
        }
        if (details != null && details.length > 0) {
            for (String detail : details) {
                if (detail != null && !detail.isEmpty()) {
                    sb.append(",").append(detail);
                }
            }
        }
        
        String line = sb.toString();
        appendLog(struct, line);
        log.info(line);
    }

    /**
     * 记录操作详情（中间步骤）
     * 
     * @param operation 操作类型
     * @param details 详情键值对
     */
    public void logDetail(String operation, String... details) {
        String struct = currentStructure.get();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("结构:%s,【%s】", struct, operation));
        for (String detail : details) {
            if (detail != null && !detail.isEmpty()) {
                sb.append(",").append(detail);
            }
        }
        
        String line = sb.toString();
        appendLog(struct, line);
        log.info(line);
    }

    /**
     * 记录操作详情（支持在方法内部调用，自动获取调用者操作类型）
     * 
     * @param details 详情键值对
     */
    public void logStep(String... details) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String operation = "操作";
        if (stackTrace.length > 2) {
            String methodName = stackTrace[2].getMethodName();
            operation = extractOperationFromMethodName(methodName);
        }
        logDetail(operation, details);
    }

    /**
     * 提取操作类型从方法名
     */
    private String extractOperationFromMethodName(String methodName) {
        if (methodName == null) return "操作";
        // 移除前缀如 process、schedule、calculate 等
        if (methodName.startsWith("process") || methodName.startsWith("Process")) {
            return methodName.substring(7);
        }
        if (methodName.startsWith("schedule") || methodName.startsWith("Schedule")) {
            return methodName.substring(8);
        }
        if (methodName.startsWith("calculate") || methodName.startsWith("Calculate")) {
            return methodName.substring(9);
        }
        if (methodName.startsWith("get") || methodName.startsWith("Get")) {
            return methodName.substring(3);
        }
        return methodName;
    }

    /**
     * 记录开始（简化版，自动推断操作类型）
     */
    public void start(String... details) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String operation = "自动调整";
        if (stackTrace.length > 2) {
            operation = extractOperationFromMethodName(stackTrace[2].getMethodName());
        }
        
        String struct = currentStructure.get();
        String timestamp = LocalDateTime.now().format(DT_FORMATTER);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("结构:%s,【%s】,开始时间:%s", struct, operation, timestamp));
        for (String detail : details) {
            if (detail != null && !detail.isEmpty()) {
                sb.append(",").append(detail);
            }
        }
        
        String line = sb.toString();
        appendLog(struct, line);
        log.info(line);
    }

    /**
     * 记录结束（简化版）
     */
    public void end(String... details) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String operation = "操作";
        if (stackTrace.length > 2) {
            operation = extractOperationFromMethodName(stackTrace[2].getMethodName());
        }
        
        logEnd(operation, null, details);
    }

    /**
     * 记录成功信息
     */
    public void logSuccess(String operation, String message) {
        String struct = currentStructure.get();
        String line = String.format("结构:%s,【%s】,✓ %s", struct, operation, message);
        appendLog(struct, line);
        log.info(line);
    }

    /**
     * 记录警告信息
     */
    public void logWarn(String operation, String message) {
        String struct = currentStructure.get();
        String line = String.format("结构:%s,【%s】,⚠ %s", struct, operation, message);
        appendLog(struct, line);
        log.warn(line);
    }

    /**
     * 记录错误信息
     */
    public void logError(String operation, String message) {
        String struct = currentStructure.get();
        String line = String.format("结构:%s,【%s】,✗ %s", struct, operation, message);
        appendLog(struct, line);
        log.error(line);
    }

    /**
     * 记录不满足条件的信息
     */
    public void logNotMeet(String operation, String condition, String reason) {
        String struct = currentStructure.get();
        String line = String.format("结构:%s,【%s】,%s,不满足！原因:%s", struct, operation, condition, reason);
        appendLog(struct, line);
        log.info(line);
    }

    /**
     * 记录满足条件的信息
     */
    public void logMeet(String operation, String condition) {
        String struct = currentStructure.get();
        String line = String.format("结构:%s,【%s】,%s,满足！", struct, operation, condition);
        appendLog(struct, line);
        log.info(line);
    }

    /**
     * 将日志追加到缓冲区
     */
    private void appendLog(String struct, String line) {
        ConcurrentHashMap<String, StringBuilder> buffer = logBuffer.get();
        buffer.computeIfAbsent(struct, k -> new StringBuilder()).append(line).append("\n");
    }

    /**
     * 获取指定结构的完整日志
     */
    public String getLogForStructure(String structureName) {
        ConcurrentHashMap<String, StringBuilder> buffer = logBuffer.get();
        StringBuilder sb = buffer.get(structureName);
        return sb != null ? sb.toString() : "";
    }

    /**
     * 获取所有结构的完整日志
     */
    public String getAllLogs() {
        ConcurrentHashMap<String, StringBuilder> buffer = logBuffer.get();
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, StringBuilder> entry : buffer.entrySet()) {
            result.append("【结构:").append(entry.getKey()).append("】\n");
            result.append(entry.getValue());
            result.append("\n").append(SEPARATOR).append("\n\n");
        }
        return result.toString();
    }

    /**
     * 获取日志Map（结构名 -> 日志内容）
     */
    public ConcurrentHashMap<String, String> getLogMap() {
        ConcurrentHashMap<String, StringBuilder> buffer = logBuffer.get();
        ConcurrentHashMap<String, String> result = new ConcurrentHashMap<>();
        for (Map.Entry<String, StringBuilder> entry : buffer.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toString());
        }
        return result;
    }

    /**
     * 清空当前线程的日志缓冲区
     */
    public void clear() {
        logBuffer.get().clear();
        currentStructure.remove();
    }

    /**
     * 开始新的排程批次（清空旧数据）
     */
    public void startNewBatch(String batchNo) {
        clear();
        log.info("========== 开始新的排程批次: {} ==========", batchNo);
    }

    /**
     * 获取批次统计信息
     */
    public String getBatchSummary() {
        ConcurrentHashMap<String, StringBuilder> buffer = logBuffer.get();
        return String.format("排程批次统计: 共 %d 个结构", buffer.size());
    }

    // ==================== 便捷的静态工厂方法 ====================

    /**
     * 记录物料相关操作
     */
    public void logMaterial(String operation, String materialCode, String... details) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("物料编码:%s", materialCode));
        for (String detail : details) {
            if (detail != null && !detail.isEmpty()) {
                sb.append(",").append(detail);
            }
        }
        logDetail(operation, sb.toString());
    }

    /**
     * 记录排产日相关信息
     */
    public void logDay(String operation, int day, String... details) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("排产日:%d", day));
        for (String detail : details) {
            if (detail != null && !detail.isEmpty()) {
                sb.append(",").append(detail);
            }
        }
        logDetail(operation, sb.toString());
    }

    /**
     * 记录机台相关信息
     */
    public void logMachine(String operation, String machineCode, String... details) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("机台:%s", machineCode));
        for (String detail : details) {
            if (detail != null && !detail.isEmpty()) {
                sb.append(",").append(detail);
            }
        }
        logDetail(operation, sb.toString());
    }

    /**
     * 记录库存相关信息
     */
    public void logStock(String operation, String embryoCode, BigDecimal stock, String... details) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("胎胚:%s,库存:%.2f", embryoCode, stock));
        for (String detail : details) {
            if (detail != null && !detail.isEmpty()) {
                sb.append(",").append(detail);
            }
        }
        logDetail(operation, sb.toString());
    }

    /**
     * 记录产能相关信息
     */
    public void logCapacity(String operation, int day, BigDecimal maxCapacity, BigDecimal remainCapacity, String... details) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("排产日:%d,日最大排产量:%.0f,日剩余排产量:%.0f", day, maxCapacity, remainCapacity));
        for (String detail : details) {
            if (detail != null && !detail.isEmpty()) {
                sb.append(",").append(detail);
            }
        }
        logDetail(operation, sb.toString());
    }

    /**
     * 记录比例信息
     */
    public void logRatio(String operation, String key, int actual, int total, int percentage) {
        String line = String.format("结构:%s,【%s】,%s,实际:%d,总计:%d,比例:%d", 
                currentStructure.get(), operation, key, actual, total, percentage);
        appendLog(currentStructure.get(), line);
        log.info(line);
    }

    /**
     * 记录开始（带物料编码）
     */
    public void startWithMaterial(String operation, String materialCode) {
        String struct = currentStructure.get();
        String timestamp = LocalDateTime.now().format(DT_FORMATTER);
        String line = String.format("结构:%s,【%s】,物料编码:%s,开始时间:%s", struct, operation, materialCode, timestamp);
        appendLog(struct, line);
        log.info(line);
    }

    /**
     * 记录结束（带物料编码和剩余量）
     */
    public void endWithMaterial(String operation, String materialCode, BigDecimal remainQty) {
        String struct = currentStructure.get();
        String timestamp = LocalDateTime.now().format(DT_FORMATTER);
        String line;
        if (remainQty != null && remainQty.compareTo(BigDecimal.ZERO) > 0) {
            line = String.format("结构:%s,【%s】,物料编码:%s,结束时间:%s,还有剩余排产计划量:%.0f", 
                    struct, operation, materialCode, timestamp, remainQty);
        } else {
            line = String.format("结构:%s,【%s】,物料编码:%s,结束时间:%s,还有剩余排产计划量:0", 
                    struct, operation, materialCode, timestamp);
        }
        appendLog(struct, line);
        log.info(line);
    }

    // ==================== 内部类用于构建日志详情 ====================

    /**
     * 日志详情构建器
     */
    public static class LogDetailBuilder {
        private final StringBuilder sb = new StringBuilder();

        public LogDetailBuilder add(String key, Object value) {
            if (value != null) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(key).append(":").append(value);
            }
            return this;
        }

        public LogDetailBuilder addIf(boolean condition, String key, Object value) {
            if (condition && value != null) {
                return add(key, value);
            }
            return this;
        }

        public String build() {
            return sb.toString();
        }
    }

    /**
     * 创建日志详情构建器
     */
    public static LogDetailBuilder detail() {
        return new LogDetailBuilder();
    }
}
