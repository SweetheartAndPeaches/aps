package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mdm.api.domain.entity.MpCxCapacityConfiguration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 成型产能分配配置(结构)Mapper接口
 *
 * @author APS Team
 */
@Mapper
public interface MpCxCapacityConfigurationMapper extends BaseMapper<MpCxCapacityConfiguration> {

    /**
     * 根据年月查询结构排产配置
     *
     * @param factoryCode 工厂编码
     * @param year       年份
     * @param month      月份
     * @return 结构排产配置列表
     */
    @Select("SELECT * FROM T_MP_STRUCTURE_ALLOCATION WHERE FACTORY_CODE = #{factoryCode} AND YEAR = #{year} AND MONTH = #{month} AND IS_DELETE = '0' ORDER BY STRUCTURE_NAME, CX_MACHINE_CODE")
    List<MpCxCapacityConfiguration> selectByYearAndMonth(
            @Param("factoryCode") String factoryCode,
            @Param("year") Integer year,
            @Param("month") Integer month);

    /**
     * 根据结构和年月查询可分配机台
     *
     * @param factoryCode   工厂编码
     * @param structureName 结构名称
     * @param year          年份
     * @param month         月份
     * @return 可分配机台列表
     */
    @Select("SELECT * FROM T_MP_STRUCTURE_ALLOCATION WHERE FACTORY_CODE = #{factoryCode} AND STRUCTURE_NAME = #{structureName} AND YEAR = #{year} AND MONTH = #{month} AND IS_DELETE = '0' ORDER BY CX_MACHINE_CODE")
    List<MpCxCapacityConfiguration> selectByStructureAndYearMonth(
            @Param("factoryCode") String factoryCode,
            @Param("structureName") String structureName,
            @Param("year") Integer year,
            @Param("month") Integer month);

    /**
     * 根据结构和日期范围查询可分配机台
     *
     * @param factoryCode   工厂编码
     * @param structureName 结构名称
     * @param day           日期（几号）
     * @param year          年份
     * @param month         月份
     * @return 可分配机台列表
     */
    @Select("SELECT * FROM T_MP_STRUCTURE_ALLOCATION WHERE FACTORY_CODE = #{factoryCode} AND STRUCTURE_NAME = #{structureName} AND YEAR = #{year} AND MONTH = #{month} AND BEGIN_DAY <= #{day} AND END_DAY >= #{day} AND IS_DELETE = '0' ORDER BY CX_MACHINE_CODE")
    List<MpCxCapacityConfiguration> selectAvailableMachinesByDay(
            @Param("factoryCode") String factoryCode,
            @Param("structureName") String structureName,
            @Param("day") Integer day,
            @Param("year") Integer year,
            @Param("month") Integer month);

    /**
     * 根据机台编码和年月查询
     *
     * @param factoryCode   工厂编码
     * @param cxMachineCode 成型机编码
     * @param year          年份
     * @param month         月份
     * @return 结构排产配置列表
     */
    @Select("SELECT * FROM T_MP_STRUCTURE_ALLOCATION WHERE FACTORY_CODE = #{factoryCode} AND CX_MACHINE_CODE = #{cxMachineCode} AND YEAR = #{year} AND MONTH = #{month} AND IS_DELETE = '0' ORDER BY STRUCTURE_NAME")
    List<MpCxCapacityConfiguration> selectByMachineAndYearMonth(
            @Param("factoryCode") String factoryCode,
            @Param("cxMachineCode") String cxMachineCode,
            @Param("year") Integer year,
            @Param("month") Integer month);
}
