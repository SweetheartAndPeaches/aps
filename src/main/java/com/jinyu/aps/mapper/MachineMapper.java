package com.jinyu.aps.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinyu.aps.entity.Machine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 成型机台Mapper接口
 *
 * @author APS Team
 */
@Mapper
public interface MachineMapper extends BaseMapper<Machine> {

    /**
     * 查询所有机台
     */
    @Select("SELECT * FROM t_cx_machine WHERE is_active = 1 ORDER BY machine_code")
    List<Machine> selectAll();

    /**
     * 查询运行中的机台
     */
    @Select("SELECT * FROM t_cx_machine WHERE status = 'RUNNING' AND is_active = 1 ORDER BY machine_code")
    List<Machine> selectRunningMachines();

    /**
     * 根据机台编码查询
     */
    @Select("SELECT * FROM t_cx_machine WHERE machine_code = #{machineCode}")
    Machine selectByCode(@Param("machineCode") String machineCode);

    /**
     * 更新机台状态
     */
    @Update("UPDATE t_cx_machine SET status = #{status}, update_time = NOW() WHERE machine_code = #{machineCode}")
    int updateStatus(@Param("machineCode") String machineCode, @Param("status") String status);
}
