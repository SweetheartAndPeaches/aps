package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.cx.api.domain.entity.CxStock;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存Service接口
 *
 * @author APS Team
 */
public interface CxStockService extends IService<CxStock> {

    /**
     * 根据物料编码获取库存
     */
    CxStock getByMaterialCode(String materialCode);

    /**
     * 获取低库存预警列表
     */
    List<CxStock> listLowStock();

    /**
     * 获取高库存预警列表
     */
    List<CxStock> listHighStock();

    /**
     * 获取收尾预警列表
     */
    List<CxStock> listEndingStock();

    /**
     * 计算库存可供硫化时长
     */
    BigDecimal calculateStockHours(String materialCode);

    /**
     * 刷新所有库存预警状态
     */
    void refreshAllAlertStatus();

    /**
     * 分页查询库存
     */
    Page<CxStock> pageList(Page<CxStock> page, String alertStatus);
}
