package com.zlt.aps.cx.service.impl;

import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.cx.service.impl.validation.BaseValidationStrategy;
import com.zlt.aps.cx.service.impl.validation.ValidationItem;
import com.zlt.aps.cx.service.impl.validation.ValidationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 排程数据完整性校验器（策略模式版本）
 *
 * 特性：
 * 1. 自动注册：所有实现 ValidationStrategy 的类都会被自动注入执行
 * 2. 可扩展：新增校验只需添加新的策略类
 * 3. 可配置：通过 @ConditionalOnProperty 或 isEnabled() 方法控制是否执行
 * 4. 可排序：按 getOrder() 返回值顺序执行
 *
 * 新增校验项：
 * 1. 在 ValidationItem 枚举中添加新的校验项
 * 2. 创建新的策略类继承 BaseValidationStrategy 或实现 ValidationStrategy
 * 3. 添加 @Component 注解（自动注册）
 *
 * @author APS Team
 */
@Slf4j
@Component
public class ScheduleDataValidator {

    /** 自动注入的校验策略列表 */
    private List<ValidationStrategy> validationStrategies;

    /** 策略映射（用于快速查找） */
    private Map<ValidationItem, ValidationStrategy> strategyMap;

    @Autowired
    private List<ValidationStrategy> strategies;

    @PostConstruct
    public void init() {
        // 按 order 排序
        this.validationStrategies = strategies.stream()
                .filter(ValidationStrategy::isEnabled)
                .sorted(Comparator.comparingInt(ValidationStrategy::getOrder))
                .collect(Collectors.toList());

        // 构建映射
        this.strategyMap = new HashMap<>();
        for (ValidationStrategy strategy : validationStrategies) {
            // 获取策略对应的校验项（如果有）
            if (strategy instanceof BaseValidationStrategy) {
                ValidationItem item = ((BaseValidationStrategy) strategy).getValidationItem();
                if (item != null) {
                    strategyMap.put(item, strategy);
                }
            }
        }

        log.info("数据校验器初始化完成，已注册 {} 个校验策略", validationStrategies.size());
        for (ValidationStrategy strategy : validationStrategies) {
            log.debug("  - {} (order={})", strategy.getDataItemName(), strategy.getOrder());
        }
    }

    /**
     * 执行所有校验
     *
     * @param context 排程上下文
     * @param scheduleDate 排程日期
     * @param factoryCode 工厂编码
     * @return 校验结果
     */
    public ScheduleDataValidationResult validate(ScheduleContextDTO context,
                                               LocalDate scheduleDate,
                                               String factoryCode) {
        ScheduleDataValidationResult result = new ScheduleDataValidationResult();
        log.info("===== 开始数据完整性校验 =====");
        log.info("排程日期：{}，工厂编码：{}", scheduleDate, factoryCode);
        log.info("已注册 {} 个校验策略", validationStrategies.size());

        long startTime = System.currentTimeMillis();

        // 按顺序执行所有策略
        for (ValidationStrategy strategy : validationStrategies) {
            try {
                log.debug("执行校验：{}", strategy.getDataItemName());
                strategy.validate(context, scheduleDate, factoryCode, result);
            } catch (Exception e) {
                log.error("校验策略执行异常：{}", strategy.getDataItemName(), e);
                result.addError(strategy.getDataItemName(),
                        "校验执行异常：" + e.getMessage(),
                        "请联系系统管理员");
            }
        }

        long costTime = System.currentTimeMillis() - startTime;

        // 生成摘要
        result.generateSummary();
        
        log.info("===== 数据完整性校验完成 =====");
        log.info("校验结果：{}，耗时：{}ms", result.getSummary(), costTime);
        log.info("错误：{}，警告：{}，提示：{}", 
                result.getErrorCount(), result.getWarnCount(), result.getInfoCount());

        // 输出详细结果
        if (!result.isPassed() || result.getWarnCount() > 0) {
            logValidationDetails(result);
        }

        return result;
    }

    /**
     * 执行指定校验项
     *
     * @param context 排程上下文
     * @param scheduleDate 排程日期
     * @param factoryCode 工厂编码
     * @param validationItems 需要校验的项
     * @return 校验结果
     */
    public ScheduleDataValidationResult validate(ScheduleContextDTO context,
                                               LocalDate scheduleDate,
                                               String factoryCode,
                                               ValidationItem... validationItems) {
        ScheduleDataValidationResult result = new ScheduleDataValidationResult();
        log.info("执行指定校验项：{}", Arrays.toString(validationItems));

        for (ValidationItem item : validationItems) {
            ValidationStrategy strategy = strategyMap.get(item);
            if (strategy != null) {
                try {
                    strategy.validate(context, scheduleDate, factoryCode, result);
                } catch (Exception e) {
                    log.error("校验策略执行异常：{}", item.getName(), e);
                    result.addError(item.getName(),
                            "校验执行异常：" + e.getMessage(),
                            "请联系系统管理员");
                }
            } else {
                log.warn("未找到校验策略：{}", item.getName());
                result.addInfo(item.getName(), "校验策略未实现", null);
            }
        }

        result.generateSummary();
        return result;
    }

    /**
     * 获取已注册的校验策略列表
     */
    public List<ValidationStrategy> getValidationStrategies() {
        return Collections.unmodifiableList(validationStrategies);
    }

    /**
     * 获取策略映射
     */
    public Map<ValidationItem, ValidationStrategy> getStrategyMap() {
        return Collections.unmodifiableMap(strategyMap);
    }

    /**
     * 输出校验详情
     */
    private void logValidationDetails(ScheduleDataValidationResult result) {
        log.warn("========== 校验详情 ==========");

        // 按级别分组输出
        for (ScheduleDataValidationResult.ValidationDetail detail : result.getDetails()) {
            String levelStr;
            switch (detail.getLevel()) {
                case ERROR:
                    levelStr = "❌ ERROR";
                    break;
                case WARN:
                    levelStr = "⚠️  WARN";
                    break;
                default:
                    levelStr = "ℹ️  INFO";
            }

            log.warn("{} [{}] {}", levelStr, detail.getDataItem(), detail.getMessage());
            if (detail.getSuggestion() != null && !detail.getSuggestion().isEmpty()) {
                log.warn("    → 建议：{}", detail.getSuggestion());
            }
        }

        log.warn("==============================");
    }

    // ==================== 便捷方法 ====================

    /**
     * 快速校验（用于外部调用）
     */
    public boolean quickValidate(ScheduleContextDTO context, LocalDate scheduleDate, String factoryCode) {
        ScheduleDataValidationResult result = validate(context, scheduleDate, factoryCode);
        return result.isPassed();
    }

    /**
     * 获取错误信息摘要
     */
    public String getErrorSummary(ScheduleContextDTO context, LocalDate scheduleDate, String factoryCode) {
        ScheduleDataValidationResult result = validate(context, scheduleDate, factoryCode);
        return result.getDetails().stream()
                .filter(d -> d.getLevel() == ScheduleDataValidationResult.ValidationLevel.ERROR)
                .map(d -> "[" + d.getDataItem() + "] " + d.getMessage())
                .collect(Collectors.joining("; "));
    }
}
