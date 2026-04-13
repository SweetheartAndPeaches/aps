package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cx.entity.schedule.CxScheduleAlgorithmLog;
import com.zlt.aps.cx.mapper.CxScheduleAlgorithmLogMapper;
import com.zlt.aps.cx.service.CxScheduleAlgorithmLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 成型算法日志服务实现类
 *
 * @author APS Team
 */
@Slf4j
@Service
public class CxScheduleAlgorithmLogServiceImpl implements CxScheduleAlgorithmLogService {

    @Autowired
    private CxScheduleAlgorithmLogMapper algorithmLogMapper;

    @Override
    public boolean saveLog(CxScheduleAlgorithmLog algorithmLog) {
        try {
            int rows = algorithmLogMapper.insert(algorithmLog);
            return rows > 0;
        } catch (Exception e) {
            log.error("保存算法日志失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int saveBatchLogs(List<CxScheduleAlgorithmLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return 0;
        }
        try {
            int successCount = 0;
            for (CxScheduleAlgorithmLog logItem : logs) {
                if (saveLog(logItem)) {
                    successCount++;
                }
            }
            log.info("批量保存算法日志: 成功 {} 条, 总数 {} 条", successCount, logs.size());
            return successCount;
        } catch (Exception e) {
            log.error("批量保存算法日志失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    @Transactional
    public int deleteHistoryLogs(LocalDate scheduleDate, String factoryCode) {
        try {
            // 删除指定日期的历史日志（逻辑删除）
            LambdaQueryWrapper<CxScheduleAlgorithmLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CxScheduleAlgorithmLog::getScheduleDate, scheduleDate)
                   .eq(CxScheduleAlgorithmLog::getFactoryCode, factoryCode);
            
            // 使用逻辑删除
            int rows = algorithmLogMapper.delete(wrapper);
            log.info("删除历史算法日志: 日期={}, 工厂={}, 删除数量={}", scheduleDate, factoryCode, rows);
            return rows;
        } catch (Exception e) {
            log.error("删除历史算法日志失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public List<CxScheduleAlgorithmLog> getLogsByScheduleDate(LocalDate scheduleDate) {
        return algorithmLogMapper.selectByScheduleDate(scheduleDate);
    }

    @Override
    public List<CxScheduleAlgorithmLog> getLogsByScheduleDateAndStructure(LocalDate scheduleDate, String structureName) {
        return algorithmLogMapper.selectByScheduleDateAndStructure(scheduleDate, structureName);
    }

    @Override
    public List<CxScheduleAlgorithmLog> getLatestLogs(int limit) {
        return algorithmLogMapper.selectLatestLogs(limit);
    }
}
