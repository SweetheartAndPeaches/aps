package com.zlt.aps.cx.model.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 物料分组实体
 * 用于将相同产品结构的胎胚物料归组，按组分配和计算
 * 
 * @author APS Team
 */
public class MaterialGroup implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 分组ID */
    private String groupId;

    /** 产品结构 */
    private String productStructure;

    /** 分组包含的物料列表 */
    private List<String> materialCodes;

    /** 分组总计划量 */
    private int totalPlanQuantity;

    /** 分组总库存 */
    private int totalStock;

    /** 分组总硫化机台数 */
    private int totalVulcanizeMachines;

    /** 是否主销产品分组 */
    private boolean isMainProduct;

    /** 分组优先级 */
    private int priority;

    /** 分组排序 */
    private int sortOrder;

    public MaterialGroup() {
        this.materialCodes = new ArrayList<>();
    }

    public MaterialGroup(String productStructure) {
        this.groupId = "MG_" + productStructure.replace("/", "_");
        this.productStructure = productStructure;
        this.materialCodes = new ArrayList<>();
    }

    /**
     * 添加物料到分组
     */
    public void addMaterial(String materialCode, int planQuantity, int stock, int vulcanizeMachines, boolean isMainProduct) {
        if (!this.materialCodes.contains(materialCode)) {
            this.materialCodes.add(materialCode);
            this.totalPlanQuantity += planQuantity;
            this.totalStock += stock;
            this.totalVulcanizeMachines += vulcanizeMachines;
            this.isMainProduct = this.isMainProduct || isMainProduct;
        }
    }

    /**
     * 计算分组库存可供硫化时长（小时）
     * 公式：库存可供硫化时长 = 总库存 / (总硫化机台数 × 4车/小时)
     */
    public double calculateStockHours() {
        if (totalVulcanizeMachines == 0) {
            return 0.0;
        }
        // 假设每台硫化机每小时可硫化4车（48条）
        double vulcanizeCapacityPerHour = totalVulcanizeMachines * 48.0;
        return totalStock / vulcanizeCapacityPerHour;
    }

    // Getters and Setters
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getProductStructure() {
        return productStructure;
    }

    public void setProductStructure(String productStructure) {
        this.productStructure = productStructure;
    }

    public List<String> getMaterialCodes() {
        return materialCodes;
    }

    public void setMaterialCodes(List<String> materialCodes) {
        this.materialCodes = materialCodes;
    }

    public int getTotalPlanQuantity() {
        return totalPlanQuantity;
    }

    public void setTotalPlanQuantity(int totalPlanQuantity) {
        this.totalPlanQuantity = totalPlanQuantity;
    }

    public int getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(int totalStock) {
        this.totalStock = totalStock;
    }

    public int getTotalVulcanizeMachines() {
        return totalVulcanizeMachines;
    }

    public void setTotalVulcanizeMachines(int totalVulcanizeMachines) {
        this.totalVulcanizeMachines = totalVulcanizeMachines;
    }

    public boolean isMainProduct() {
        return isMainProduct;
    }

    public void setMainProduct(boolean mainProduct) {
        isMainProduct = mainProduct;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
