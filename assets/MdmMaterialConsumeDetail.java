package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * BOM物料消耗明细实体类
 * 记录解开树形结构后的关系，用胎胚代码+胎胚版本查就能找出下面用到的所有原材料
 */
@ApiModel(value = "BOM物料消耗明细对象", description = "BOM物料消耗明细对象")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "T_MDM_MATERIAL_CONSUME_DETAIL")
public class MdmMaterialConsumeDetail extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工厂编号 */
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 胎胚代码 */
    @ApiModelProperty(value = "胎胚代码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /** 胎胚版本 */
    @ApiModelProperty(value = "胎胚版本", name = "embryoVersion")
    @TableField(value = "EMBRYO_VERSION")
    private String embryoVersion;

    /** 原材料物编号 */
    @ApiModelProperty(value = "原材料物编号", name = "childMaterialCode")
    @TableField(value = "CHILD_MATERIAL_CODE")
    private String childMaterialCode;

    /** 原材料物版本 */
    @ApiModelProperty(value = "原材料物版本", name = "childMaterialVersion")
    @TableField(value = "CHILD_MATERIAL_VERSION")
    private String childMaterialVersion;

    /** 原材料物料描述 */
    @ApiModelProperty(value = "原材料物料描述", name = "childMaterialName")
    @TableField(value = "CHILD_MATERIAL_NAME")
    private String childMaterialName;

    /** 单位描述 */
    @ApiModelProperty(value = "单位描述", name = "unit")
    @TableField(value = "UNIT")
    private String unit;

    /** 用量 */
    @ApiModelProperty(value = "用量", name = "dosage")
    @TableField(value = "DOSAGE")
    private BigDecimal dosage;

    /**
     * 获取胎胚代码和版本的组合键
     */
    public String getEmbryoKey() {
        return this.embryoCode + "_" + this.embryoVersion;
    }

    /**
     * 获取原材料代码和版本的组合键
     */
    public String getMaterialKey() {
        return this.childMaterialCode + "_" + this.childMaterialVersion;
    }

    /**
     * 计算原材料需求量
     */
    public BigDecimal calculateRequiredQty(Long productionQty) {
        if (productionQty == null || productionQty <= 0 || this.dosage == null) {
            return BigDecimal.ZERO;
        }
        return this.dosage.multiply(new BigDecimal(productionQty));
    }

    /**
     * 判断是否有效
     */
    public boolean isValid() {
        return this.getIsDelete() != null && this.getIsDelete() == 0
                && this.embryoCode != null && !this.embryoCode.isEmpty()
                && this.childMaterialCode != null && !this.childMaterialCode.isEmpty()
                && this.dosage != null && this.dosage.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 判断是否为叶子节点（原材料）
     */
    public boolean isLeafNode() {
        return true;
    }

    /**
     * 获取完整的物料描述
     */
    public String getFullMaterialDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.childMaterialCode);
        if (this.childMaterialVersion != null && !this.childMaterialVersion.isEmpty()) {
            sb.append("_").append(this.childMaterialVersion);
        }
        if (this.childMaterialName != null && !this.childMaterialName.isEmpty()) {
            sb.append(" ").append(this.childMaterialName);
        }
        return sb.toString();
    }
}
