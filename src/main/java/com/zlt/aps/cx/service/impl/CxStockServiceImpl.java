package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cx.entity.CxStock;
import com.zlt.aps.cx.mapper.CxStockMapper;
import com.zlt.aps.cx.service.CxStockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
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
        wrapper.eq(CxStock::getEmbryoCode, materialCode);
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
    public List<CxStock> listEndingStock() {
        LambdaQueryWrapper<CxStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxStock::getIsEndingSku, 1);
        return list(wrapper);
    }

    @Override
    public BigDecimal calculateStockHours(String materialCode) {
        CxStock stock = getByMaterialCode(materialCode);
        if (stock == null || stock.getStockNum() == null || stock.getStockNum() <= 0) {
            return BigDecimal.ZERO;
        }
        // 使用有效库存计算可供时长
        Integer effectiveStock = stock.getEffectiveStock();
        if (effectiveStock <= 0) {
            return BigDecimal.ZERO;
        }
        // 简化计算：库存 / (硫化机台数 * 模数 * 每小时产能)
        // 实际应根据硫化时间和模数计算
        return new BigDecimal(effectiveStock).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshAllAlertStatus() {
        List<CxStock> stocks = list();
        Date now = new Date();

        for (CxStock stock : stocks) {
            String alertStatus = calculateAlertStatus(stock);

            if (!alertStatus.equals(stock.getAlertStatus())) {
                stock.setAlertStatus(alertStatus);
                stock.setAlertTime(now);
                stock.setUpdateTime(now);
                updateById(stock);
            }
        }
    }

    @Override
    public Page<CxStock> pageList(Page<CxStock> page, String alertStatus) {
        LambdaQueryWrapper<CxStock> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(alertStatus)) {
            wrapper.eq(CxStock::getAlertStatus, alertStatus);
        }

        wrapper.orderByDesc(CxStock::getAlertTime)
                .orderByAsc(CxStock::getEmbryoCode);

        return page(page, wrapper);
    }

    /**
     * 计算预警状态
     */
    private String calculateAlertStatus(CxStock stock) {
        if (stock == null || stock.getStockNum() == null) {
            return "NORMAL";
        }

        // 根据库存可供硫化时长判断
        BigDecimal stockHours = stock.getStockHours();
        if (stockHours == null) {
            stockHours = calculateStockHours(stock.getEmbryoCode());
        }

        // 预警阈值（可配置）
        BigDecimal lowThreshold = new BigDecimal("4");   // 低于4小时预警
        BigDecimal highThreshold = new BigDecimal("18"); // 高于18小时预警

        if (stockHours.compareTo(lowThreshold) < 0) {
            return "LOW";
        } else if (stockHours.compareTo(highThreshold) > 0) {
            return "HIGH";
        }

        return "NORMAL";
    }
}
