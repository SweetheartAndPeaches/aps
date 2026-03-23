package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.cx.dto.ScheduleQueryDTO;
import com.zlt.aps.cx.dto.ScheduleResultDTO;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;

import java.time.LocalDate;
import java.util.List;

/**
 * 成型排程结果服务接口
 *
 * @author APS Team
 */
public interface CxScheduleResultService extends IService<CxScheduleResult> {

    /**
     * 根据排程日期查询排程结果
     *
     * @param scheduleDate 排程日期
     * @return 排程结果列表
     */
    List<CxScheduleResult> listByScheduleDate(LocalDate scheduleDate);

    /**
     * 根据成型机台编号和日期查询排程结果
     *
     * @param cxMachineCode 成型机台编号
     * @param scheduleDate  排程日期
     * @return 排程结果列表
     */
    List<CxScheduleResult> listByMachineAndDate(String cxMachineCode, LocalDate scheduleDate);

    /**
     * 根据胎胚代码查询排程结果
     *
     * @param embryoCode 胎胚代码
     * @return 排程结果列表
     */
    List<CxScheduleResult> listByEmbryoCode(String embryoCode);

    /**
     * 分页查询排程结果
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    Page<ScheduleResultDTO> pageList(ScheduleQueryDTO queryDTO);

    /**
     * 获取排程详情（含明细）
     *
     * @param id 排程ID
     * @return 排程详情
     */
    ScheduleResultDTO getDetailById(Long id);

    /**
     * 更新生产状态
     *
     * @param id               排程ID
     * @param productionStatus 生产状态
     * @return 是否成功
     */
    boolean updateProductionStatus(Long id, String productionStatus);

    /**
     * 发布排程
     *
     * @param id 排程ID
     * @return 是否成功
     */
    boolean releaseSchedule(Long id);

    /**
     * 批量发布排程
     *
     * @param ids 排程ID列表
     * @return 是否成功
     */
    boolean batchReleaseSchedule(List<Long> ids);

    /**
     * 更新班次计划量
     *
     * @param id           排程ID
     * @param shiftCode    班次编码
     * @param planQty      计划量
     * @return 是否成功
     */
    boolean updateShiftPlanQty(Long id, String shiftCode, java.math.BigDecimal planQty);

    /**
     * 更新班次完成量
     *
     * @param id         排程ID
     * @param shiftCode  班次编码
     * @param finishQty  完成量
     * @return 是否成功
     */
    boolean updateShiftFinishQty(Long id, String shiftCode, java.math.BigDecimal finishQty);

    /**
     * 删除指定日期的排程
     *
     * @param scheduleDate 排程日期
     * @return 是否成功
     */
    boolean deleteByScheduleDate(LocalDate scheduleDate);

    /**
     * 生成批次号
     *
     * @param scheduleDate 排程日期
     * @return 批次号
     */
    String generateBatchNo(LocalDate scheduleDate);
}
