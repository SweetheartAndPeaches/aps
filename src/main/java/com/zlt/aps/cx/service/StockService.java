package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.cx.entity.Stock;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存Service接口
 *
 * @author APS Team
 */
public interface StockService extends IService<Stock> {

    /**
     * 根据物料编码获取库存
     */
    Stock getByMaterialCode(String materialCode);

    /**
     * 获取低库存预警列表
     */
    List<Stock> listLowStock();

    /**
     * 获取高库存预警列表
     */
    List<Stock> listHighStock();

    /**
     * 计算库存可供硫化时长
     */
    BigDecimal calculateStockHours(String materialCode);
}
