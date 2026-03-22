package com.jinyu.aps.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinyu.aps.entity.Stock;
import com.jinyu.aps.mapper.StockMapper;
import com.jinyu.aps.service.StockService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 库存Service实现类
 *
 * @author APS Team
 */
@Service
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock> implements StockService {

    @Override
    public Stock getByMaterialCode(String materialCode) {
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Stock::getMaterialCode, materialCode);
        return getOne(wrapper);
    }

    @Override
    public List<Stock> listLowStock() {
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Stock::getAlertStatus, "LOW");
        return list(wrapper);
    }

    @Override
    public List<Stock> listHighStock() {
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Stock::getAlertStatus, "HIGH");
        return list(wrapper);
    }

    @Override
    public BigDecimal calculateStockHours(String materialCode) {
        Stock stock = getByMaterialCode(materialCode);
        if (stock == null || stock.getCurrentStock() == null || stock.getCurrentStock() <= 0) {
            return BigDecimal.ZERO;
        }
        // 简化计算：库存 / (硫化机台数 * 模数 * 每小时产能)
        // 实际应根据硫化时间和模数计算
        if (stock.getVulcanizeMoldCount() != null && stock.getVulcanizeMoldCount() > 0) {
            // 假设每模每小时生产1条
            return new BigDecimal(stock.getCurrentStock())
                    .divide(new BigDecimal(stock.getVulcanizeMoldCount()), 2, RoundingMode.HALF_UP);
        }
        return new BigDecimal(stock.getCurrentStock());
    }
}
