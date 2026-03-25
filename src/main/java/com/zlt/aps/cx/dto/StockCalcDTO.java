package com.zlt.aps.cx.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 库存计算DTO
 *
 * @author APS Team
 */
@Data
@ApiModel(description = "库存计算参数")
public class StockCalcDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "物料编码")
    private String materialCode;

    @ApiModelProperty(value = "当前库存")
    private Integer currentStock;

    @ApiModelProperty(value = "可用硫化机台数")
    private Integer vulcanizeMachineCount;

    @ApiModelProperty(value = "总模数")
    private Integer vulcanizeMoldCount;

    @ApiModelProperty(value = "硫化时间(分钟)")
    private BigDecimal vulcanizeTimeMinutes;

    @ApiModelProperty(value = "单班硫化量（每模每班产能）")
    private Integer singleMouldShiftQty;

    @ApiModelProperty(value = "计算的库存可供硫化时长(小时)")
    private BigDecimal stockHours;

    @ApiModelProperty(value = "计算公式记录")
    private String stockHoursFormula;

    /**
     * 计算库存可供硫化时长
     * 公式: 库存时长(小时) = (当前库存 * 硫化时间(分钟)) / (硫化机台数 * 总模数 * 单班硫化量 * 60)
     * 
     * 简化公式: 库存时长 = 库存 / (硫化机台数 * 总模数 * 单班硫化量 / 硫化时间(小时))
     */
    public void calculateStockHours() {
        if (currentStock == null || vulcanizeMachineCount == null || 
            vulcanizeMoldCount == null || singleMouldShiftQty == null ||
            vulcanizeTimeMinutes == null) {
            this.stockHours = BigDecimal.ZERO;
            this.stockHoursFormula = "参数不完整，无法计算";
            return;
        }

        if (vulcanizeMachineCount == 0 || vulcanizeMoldCount == 0 || singleMouldShiftQty == 0) {
            this.stockHours = BigDecimal.ZERO;
            this.stockHoursFormula = "硫化能力为零，无法计算";
            return;
        }

        // 硫化时间转换为小时
        BigDecimal vulcanizeTimeHours = vulcanizeTimeMinutes.divide(BigDecimal.valueOf(60), 4, BigDecimal.ROUND_HALF_UP);
        
        // 单班产能 = 硫化机台数 * 总模数 * 单班硫化量
        BigDecimal shiftCapacity = BigDecimal.valueOf(vulcanizeMachineCount)
                .multiply(BigDecimal.valueOf(vulcanizeMoldCount))
                .multiply(BigDecimal.valueOf(singleMouldShiftQty));
        
        // 库存时长 = 库存 / (单班产能 * 硫化时间(小时))
        if (shiftCapacity.compareTo(BigDecimal.ZERO) > 0 && vulcanizeTimeHours.compareTo(BigDecimal.ZERO) > 0) {
            this.stockHours = BigDecimal.valueOf(currentStock)
                    .divide(shiftCapacity.multiply(vulcanizeTimeHours), 2, BigDecimal.ROUND_HALF_UP);
        } else {
            this.stockHours = BigDecimal.ZERO;
        }
        
        this.stockHoursFormula = String.format(
            "库存时长 = %d / (%d机台 * %d模 * %d条/班 * %.2f小时) = %.2f小时",
            currentStock, vulcanizeMachineCount, vulcanizeMoldCount, 
            singleMouldShiftQty, vulcanizeTimeHours, this.stockHours
        );
    }
}
