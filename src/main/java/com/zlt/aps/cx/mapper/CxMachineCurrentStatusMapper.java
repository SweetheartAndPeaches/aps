package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.CxMachineCurrentStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 机台当前状态Mapper
 *
 * @author APS Team
 */
@Mapper
public interface CxMachineCurrentStatusMapper extends BaseMapper<CxMachineCurrentStatus> {

    /**
     * 根据机台编码查询当前状态
     */
    CxMachineCurrentStatus selectByMachineCode(@Param("machineCode") String machineCode);

    /**
     * 查询正在生产的机台列表
     */
    List<CxMachineCurrentStatus> selectProducingMachines();

    /**
     * 查询空闲机台列表
     */
    List<CxMachineCurrentStatus> selectIdleMachines();

    /**
     * 查询续作机台列表
     */
    List<CxMachineCurrentStatus> selectContinueMachines();

    /**
     * 更新机台状态
     */
    int updateMachineStatus(
            @Param("machineCode") String machineCode,
            @Param("status") String status,
            @Param("reason") String reason);
}
