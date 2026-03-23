package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.AlertConfig;
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
public interface AlertConfigMapper extends BaseMapper<AlertConfig> {

    /**
     * 查询所有预警配置
     */
    @Select("SELECT * FROM t_cx_alert_config WHERE is_active = 1 ORDER BY config_code")
    List<AlertConfig> selectAll();

    /**
     * 根据配置编码查询
     */
    @Select("SELECT * FROM t_cx_alert_config WHERE config_code = #{configCode}")
    AlertConfig selectByCode(@Param("configCode") String configCode);
}
