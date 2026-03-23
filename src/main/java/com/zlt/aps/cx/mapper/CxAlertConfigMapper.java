package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.CxAlertConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 预警配置Mapper接口
 *
 * @author APS Team
 */
@Mapper
public interface CxAlertConfigMapper extends BaseMapper<CxAlertConfig> {

    /**
     * 查询所有启用的配置
     */
    @Select("SELECT * FROM T_CX_ALERT_CONFIG WHERE IS_ACTIVE = 1 ORDER BY CONFIG_CODE")
    List<CxAlertConfig> selectAllActive();

    /**
     * 根据配置编码查询
     */
    @Select("SELECT * FROM T_CX_ALERT_CONFIG WHERE CONFIG_CODE = #{configCode}")
    CxAlertConfig selectByCode(@Param("configCode") String configCode);

    /**
     * 根据配置类型查询
     */
    @Select("SELECT * FROM T_CX_ALERT_CONFIG WHERE CONFIG_TYPE = #{configType} AND IS_ACTIVE = 1 ORDER BY CONFIG_CODE")
    List<CxAlertConfig> selectByType(@Param("configType") String configType);
}
