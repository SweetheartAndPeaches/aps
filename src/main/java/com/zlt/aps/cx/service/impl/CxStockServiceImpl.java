package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cx.entity.CxStock;
import com.zlt.aps.cx.mapper.CxStockMapper;
import com.zlt.aps.cx.service.CxStockService;
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
public class CxStockServiceImpl extends ServiceImpl<CxStockMapper, CxStock> implements CxStockService {

    @Override
    public CxStock getByMaterialCode(String materialCode) {
        LambdaQueryWrapper<CxStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxStock::getMaterialCode, materialCode);
        return getOne(wrapper);
    }

    @Override
    public List<CxStock> listLowStock() {
        LambdaQueryWrapper<CxStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxStock::getAlertStatus, "LOW");
        return list(wrapper);
    }

    @Override
    public List<CxStock> listHighStock() {
        LambdaQueryWrapper<CxStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxStock::getAlertStatus, "HIGH");
        return list(wrapper);
    }

    @Override
    public BigDecimal calculateStockHours(String materialCode) {
        CxStock stock = getByMaterialCode(materialCode);
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
