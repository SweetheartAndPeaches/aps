package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.vo.CxScheduleDetailVo;

import java.time.LocalDate;
import java.util.List;

/**
 * 排程明细服务接口
 *
 * @author APS Team
 */
public interface CxScheduleDetailService extends IService<CxScheduleDetail> {

    /**
     * 根据主表ID查询明细列表（带主表信息）
     *
     * @param mainId 主表ID
     * @return 明细列表（包含主表关联信息）
     */
    List<CxScheduleDetailVo> listVoByMainId(Long mainId);

    /**
     * 根据机台和日期查询明细（带主表信息）
     *
     * @param cxMachineCode 成型机台编号
     * @param scheduleDate  排程日期
     * @return 明细列表（包含主表关联信息）
     */
    List<CxScheduleDetailVo> listVoByMachineAndDate(String cxMachineCode, LocalDate scheduleDate);

    /**
     * 根据机台和日期范围查询明细（带主表信息，按机台降序+胎胚排序）
     *
     * @param machineCodeStart 机台编号（起始）
     * @param machineCodeEnd   机台编号（结束）
     * @param scheduleDateStart 排程日期（起始）
     * @param scheduleDateEnd   排程日期（结束）
     * @return 明细列表（按机台降序+胎胚排序）
     */
    List<CxScheduleDetailVo> listVoByMachineAndDateRange(String machineCodeStart, String machineCodeEnd,
                                                          LocalDate scheduleDateStart, LocalDate scheduleDateEnd);

    /**
     * 根据主表ID查询明细列表
     *
     * @param mainId 主表ID
     * @return 明细列表
     */
    List<CxScheduleDetail> listByMainId(Long mainId);

    /**
     * 根据机台和日期查询明细
     *
     * @param cxMachineCode 成型机台编号
     * @param scheduleDate  排程日期
     * @return 明细列表
     */
    List<CxScheduleDetail> listByMachineAndDate(String cxMachineCode, LocalDate scheduleDate);

    /**
     * 根据班次查询明细
     *
     * @param mainId    主表ID
     * @param shiftCode 班次编码
     * @return 明细列表
     */
    List<CxScheduleDetail> listByShift(Long mainId, String shiftCode);

    /**
     * 更新完成量
     *
     * @param detailId         明细ID
     * @param completedQuantity 完成量
     * @return 是否成功
     */
    boolean updateCompletedQuantity(Long detailId, Integer completedQuantity);

    /**
     * 更新车次状态
     *
     * @param detailId   明细ID
     * @param tripStatus 车次状态
     * @return 是否成功
     */
    boolean updateTripStatus(Long detailId, String tripStatus);

    /**
     * 批量保存明细
     *
     * @param details 明细列表
     * @return 是否成功
     */
    boolean batchSave(List<CxScheduleDetail> details);

    /**
     * 根据主表ID删除明细
     *
     * @param mainId 主表ID
     * @return 是否成功
     */
    boolean deleteByMainId(Long mainId);

    /**
     * 批量根据主表ID删除明细
     *
     * @param mainIds 主表ID列表
     * @return 是否成功
     */
    boolean deleteByMainIds(List<Long> mainIds);

    /**
     * 获取下一个车次号
     *
     * @param mainId    主表ID
     * @param shiftCode 班次编码
     * @return 车次号
     */
    Integer getNextTripNo(Long mainId, String shiftCode);
}
