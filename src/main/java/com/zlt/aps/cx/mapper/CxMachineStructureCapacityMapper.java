package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.CxMachineStructureCapacity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 机台结构产能Mapper
 *
 * @author APS Team
 */
@Mapper
public interface CxMachineStructureCapacityMapper extends BaseMapper<CxMachineStructureCapacity> {

    /**
     * 根据机台编码查询产能配置列表
     */
    List<CxMachineStructureCapacity> selectByMachineCode(@Param("machineCode") String machineCode);

    /**
     * 根据结构编码查询产能配置列表
     */
    List<CxMachineStructureCapacity> selectByStructureCode(@Param("structureCode") String structureCode);

    /**
     * 根据机台和结构查询产能配置
     */
    CxMachineStructureCapacity selectByMachineAndStructure(
            @Param("machineCode") String machineCode, 
            @Param("structureCode") String structureCode);
}
