package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;

import java.time.LocalDate;
import java.util.List;

/**
 * 硫化排程结果服务接口
 *
 * @author APS Team
 */
public interface LhScheduleResultService extends IService<LhScheduleResult> {

    /**
     * 根据排程日期查询硫化排程
     *
     * @param scheduleDate 排程日期
     * @return 硫化排程列表
     */
    List<LhScheduleResult> listByScheduleDate(LocalDate scheduleDate);

    /**
     * 根据硫化机台编号和日期查询排程
     *
     * @param lhMachineCode 硫化机台编号
     * @param scheduleDate  排程日期
     * @return 硫化排程列表
     */
    List<LhScheduleResult> listByMachineAndDate(String lhMachineCode, LocalDate scheduleDate);

    /**
     * 根据胎胚代码查询硫化排程
     *
     * @param embryoCode 胎胚代码
     * @return 硫化排程列表
     */
    List<LhScheduleResult> listByEmbryoCode(String embryoCode);

    /**
     * 分页查询硫化排程
     *
     * @param scheduleDate 排程日期
     * @param lhMachineCode 硫化机台编号（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    Page<LhScheduleResult> pageList(LocalDate scheduleDate, String lhMachineCode, Integer pageNum, Integer pageSize);

    /**
     * 更新生产状态
     *
     * @param id               排程ID
     * @param productionStatus 生产状态
     * @return 是否成功
     */
    boolean updateProductionStatus(Long id, String productionStatus);

    /**
     * 发布硫化排程
     *
     * @param id 排程ID
     * @return 是否成功
     */
    boolean releaseSchedule(Long id);

    /**
     * 批量发布硫化排程
     *
     * @param ids 排程ID列表
     * @return 是否成功
     */
    boolean batchReleaseSchedule(List<Long> ids);

    /**
     * 更新班次计划量
     *
     * @param id        排程ID
     * @param shiftNo   班次号（1-4）
     * @param planQty   计划量
     * @return 是否成功
     */
    boolean updateShiftPlanQty(Long id, Integer shiftNo, Integer planQty);

    /**
     * 更新班次完成量
     *
     * @param id        排程ID
     * @param shiftNo   班次号（1-4）
     * @param finishQty 完成量
     * @return 是否成功
     */
    boolean updateShiftFinishQty(Long id, Integer shiftNo, Integer finishQty);

    /**
     * 删除指定日期的硫化排程
     *
     * @param scheduleDate 排程日期
     * @return 是否成功
     */
    boolean deleteByScheduleDate(LocalDate scheduleDate);

    /**
     * 根据成型排程ID关联查询硫化排程
     *
     * @param cxScheduleId 成型排程ID
     * @return 硫化排程列表
     */
    List<LhScheduleResult> listByCxScheduleId(Long cxScheduleId);

    /**
     * 计算硫化产能
     *
     * @param lhMachineCode 硫化机台编号
     * @param mouldQty      使用模数
     * @param lhTime        硫化时长（秒）
     * @return 单班产能
     */
    Integer calculateShiftCapacity(String lhMachineCode, Integer mouldQty, Integer lhTime);
}
