package com.jinyu.aps.mapper.config;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinyu.aps.entity.config.CxStock;
import org.apache.ibatis.annotations.Mapper;

/**
 * 成型库存信息 Mapper接口
 *
 * @author APS Team
 * @since 2.0.0
 */
@Mapper
public interface CxStockMapper extends BaseMapper<CxStock> {

}
