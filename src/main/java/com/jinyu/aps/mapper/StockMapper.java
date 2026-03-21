package com.jinyu.aps.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinyu.aps.entity.Stock;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存Mapper接口
 *
 * @author APS Team
 */
@Mapper
public interface StockMapper extends BaseMapper<Stock> {
}
