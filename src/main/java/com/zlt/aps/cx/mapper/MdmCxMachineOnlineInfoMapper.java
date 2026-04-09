package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mdm.api.domain.entity.MdmCxMachineOnlineInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 成型在机信息Mapper
 *
 * @author APS Team
 */
@Mapper
public interface MdmCxMachineOnlineInfoMapper extends BaseMapper<MdmCxMachineOnlineInfo> {

    /**
     * 按日期查询在机信息
     *
     * @param date 日期
     * @return 在机信息列表
     */
    @Select("SELECT * FROM T_MDM_CX_MACHINE_ONLINE_INFO WHERE ONLINE_DATE = #{date} AND IS_DELETE = 0")
    List<MdmCxMachineOnlineInfo> selectByDate(@Param("date") LocalDate date);

    /**
     * 按日期范围查询在机信息（今天和昨天）
     * 用于判断续作，可能跨班次生产
     *
     * @param today    今天
     * @param yesterday 昨天
     * @return 在机信息列表
     */
    @Select("SELECT * FROM T_MDM_CX_MACHINE_ONLINE_INFO " +
            "WHERE (ONLINE_DATE = #{today} OR ONLINE_DATE = #{yesterday}) AND IS_DELETE = 0")
    List<MdmCxMachineOnlineInfo> selectByDateRange(@Param("today") LocalDate today, 
                                                    @Param("yesterday") LocalDate yesterday);

    /**
     * 按机台编码查询在机信息
     *
     * @param cxCode 机台编码
     * @return 在机信息列表
     */
    @Select("SELECT * FROM T_MDM_CX_MACHINE_ONLINE_INFO WHERE CX_CODE = #{cxCode} AND IS_DELETE = 0 ORDER BY ONLINE_DATE DESC")
    List<MdmCxMachineOnlineInfo> selectByCxCode(@Param("cxCode") String cxCode);

    /**
     * 查询所有有效在机信息
     *
     * @return 在机信息列表
     */
    @Select("SELECT * FROM T_MDM_CX_MACHINE_ONLINE_INFO WHERE IS_DELETE = 0 ORDER BY CX_CODE, ONLINE_DATE DESC")
    List<MdmCxMachineOnlineInfo> selectAllValid();
}
