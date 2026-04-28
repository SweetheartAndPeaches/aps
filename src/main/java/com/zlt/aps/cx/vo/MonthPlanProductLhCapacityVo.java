package com.zlt.aps.cx.vo;

import com.zlt.aps.cx.enums.DayVulcanizationModeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 工厂月度排产-SKU日硫化产能对象
 * 用于计算成型机台的满算力
 *
 * @author APS Team
 */
@Data
@Slf4j
@ApiModel(value = "SKU日硫化产能对象", description = "工厂月度排产-SKU日硫化产能对象")
public class MonthPlanProductLhCapacityVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编码
     */
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    private String factoryCode;

    /**
     * 物料编码
     */
    @ApiModelProperty(value = "物料编号", name = "materialCode")
    private String materialCode;

    /**
     * 物料描述
     */
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    private String materialDesc;

    /**
     * MES的日硫化量
     */
    @ApiModelProperty(value = "MES的日硫化量", name = "mesCapacity")
    private Integer mesCapacity;

    /**
     * 标准日硫化量
     */
    @ApiModelProperty(value = "标准日硫化量", name = "standardCapacity")
    private Integer standardCapacity;

    /**
     * APS的日硫化量
     */
    @ApiModelProperty(value = "APS的日硫化量", name = "apsCapacity")
    private Integer apsCapacity;

    /**
     * 总硫化时间(单位s)
     */
    @ApiModelProperty(value = "总硫化时间(单位s)", name = "vulcanizationTime")
    private BigDecimal vulcanizationTime;

    /**
     * 类型 01 模具关系 02 新模具到货计划
     */
    @ApiModelProperty(value = "类型", name = "type")
    private String type;

    /**
     * 日硫化量（根据计算模式动态计算）
     */
    @ApiModelProperty(value = "日硫化量(单位条)", name = "dayVulcanizationQty")
    private Integer dayVulcanizationQty;

    /**
     * 日硫化量计算（根据模式选择MES/标准/APS产能）
     *
     * @param mode 计算模式
     */
    public void calculateDayVulcanizationQty(DayVulcanizationModeEnum mode) {
        if (mode == null) {
            mode = DayVulcanizationModeEnum.STANDARD_CAPACITY;
        }
        switch (mode) {
            case MES_CAPACITY:
                this.dayVulcanizationQty = this.mesCapacity;
                break;
            case STANDARD_CAPACITY:
                this.dayVulcanizationQty = this.standardCapacity;
                break;
            case APS_CAPACITY:
                this.dayVulcanizationQty = this.apsCapacity;
                break;
            default:
                this.dayVulcanizationQty = this.standardCapacity;
        }
        if (this.dayVulcanizationQty == null || this.dayVulcanizationQty <= 0) {
            log.warn("日硫化量计算: 物料={}, 模式={}, 选中字段为null/0, mesCapacity={}, standardCapacity={}, apsCapacity={}",
                    this.materialCode, mode.getDesc(), this.mesCapacity, this.standardCapacity, this.apsCapacity);
        }
    }

    /**
     * 根据编码获取计算模式并计算日硫化量
     *
     * @param modeCode 计算模式编码
     */
    public void calculateDayVulcanizationQty(String modeCode) {
        DayVulcanizationModeEnum mode = DayVulcanizationModeEnum.getByCode(modeCode);
        calculateDayVulcanizationQty(mode);
    }

    /**
     * 根据编码获取计算模式并计算日硫化量
     *
     * @param modeCode 计算模式编码（数字）
     */
    public void calculateDayVulcanizationQty(Integer modeCode) {
        DayVulcanizationModeEnum mode = DayVulcanizationModeEnum.getByCode(modeCode);
        calculateDayVulcanizationQty(mode);
    }

    /**
     * 获取默认日硫化量（优先使用标准产能，其次MES产能）
     *
     * @return 默认日硫化量
     */
    public Integer getDefaultDayVulcanizationQty() {
        if (this.standardCapacity != null && this.standardCapacity > 0) {
            return this.standardCapacity;
        }
        if (this.mesCapacity != null && this.mesCapacity > 0) {
            return this.mesCapacity;
        }
        if (this.apsCapacity != null && this.apsCapacity > 0) {
            return this.apsCapacity;
        }
        return 0;
    }
}
