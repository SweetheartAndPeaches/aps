package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.MdmDevicePlanShut;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 设备计划停机 Mapper 接口
 *
 * @author APS Team
 */
@Mapper
public interface MdmDevicePlanShutMapper extends BaseMapper<MdmDevicePlanShut> {

    /**
     * 查询指定机台在指定日期范围内的停机计划
     *
     * @param machineCode 机台编号
     * @param beginDate   开始日期
     * @param endDate     结束日期
     * @return 停机计划列表
     */
    @Select("SELECT * FROM T_MDM_DEVICE_PLAN_SHUT " +
            "WHERE MACHINE_CODE = #{machineCode} " +
            "AND BEGIN_DATE < #{endDate} " +
            "AND END_DATE > #{beginDate} " +
            "ORDER BY BEGIN_DATE")
    List<MdmDevicePlanShut> selectByMachineAndDateRange(
            @Param("machineCode") String machineCode,
            @Param("beginDate") LocalDate beginDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 查询指定机台类型在指定日期范围内的停机计划
     *
     * @param machineType 机台类型（如"成型"）
     * @param beginDate   开始日期
     * @param endDate     结束日期
     * @return 停机计划列表
     */
    @Select("SELECT * FROM T_MDM_DEVICE_PLAN_SHUT " +
            "WHERE MACHINE_TYPE = #{machineType} " +
            "AND BEGIN_DATE < #{endDate} " +
            "AND END_DATE > #{beginDate} " +
            "ORDER BY MACHINE_CODE, BEGIN_DATE")
    List<MdmDevicePlanShut> selectByMachineTypeAndDateRange(
            @Param("machineType") String machineType,
            @Param("beginDate") LocalDate beginDate,
            @Param("endDate") LocalDate endDate);
}
