package com.jinyu.aps.entity.mdm;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jinyu.aps.entity.base.ApsBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 成型固定机台对象
 * 对应表：T_MDM_CX_MACHINE_FIXED
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_MDM_CX_MACHINE_FIXED")
@Schema(description = "成型固定机台对象")
public class MdmCxMachineFixed extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 成型机编码
     */
    @Schema(description = "成型机编码")
    @TableField("CX_MACHINE_CODE")
    private String cxMachineCode;

    /**
     * 固定结构1 多个以,分隔拼接
     */
    @Schema(description = "固定结构1")
    @TableField(value = "FIXED_STRUCTURE1", updateStrategy = FieldStrategy.IGNORED)
    private String fixedStructure1;

    /**
     * 固定结构2 多个以,分隔拼接
     */
    @Schema(description = "固定结构2")
    @TableField(value = "FIXED_STRUCTURE2", updateStrategy = FieldStrategy.IGNORED)
    private String fixedStructure2;

    /**
     * 固定结构3 多个以,分隔拼接
     */
    @Schema(description = "固定结构3")
    @TableField(value = "FIXED_STRUCTURE3", updateStrategy = FieldStrategy.IGNORED)
    private String fixedStructure3;

    /**
     * 固定SKU 多个以,分隔拼接
     */
    @Schema(description = "固定SKU")
    @TableField(value = "FIXED_MATERIAL_CODE", updateStrategy = FieldStrategy.IGNORED)
    private String fixedMaterialCode;

    /**
     * 固定物料描述 多个以,分隔拼接
     */
    @Schema(description = "固定物料描述")
    @TableField(value = "FIXED_MATERIAL_DESC", updateStrategy = FieldStrategy.IGNORED)
    private String fixedMaterialDesc;

    /**
     * 不可作业结构 多个以,分隔拼接
     */
    @Schema(description = "不可作业结构")
    @TableField(value = "DISABLE_STRUCTURE", updateStrategy = FieldStrategy.IGNORED)
    private String disableStructure;

    /**
     * 不可作业SKU 多个以,分隔拼接
     */
    @Schema(description = "不可作业SKU")
    @TableField(value = "DISABLE_MATERIAL_CODE", updateStrategy = FieldStrategy.IGNORED)
    private String disableMaterialCode;

    /**
     * 不可作业物料描述 多个以,分隔拼接
     */
    @Schema(description = "不可作业物料描述")
    @TableField(value = "DISABLE_MATERIAL_DESC", updateStrategy = FieldStrategy.IGNORED)
    private String disableMaterialDesc;

    // ========== 便捷方法：将逗号分隔字符串转为列表 ==========

    /**
     * 获取固定结构1列表
     */
    public List<String> getSplitFixedStructure1() {
        String value = StringUtils.defaultIfBlank(this.fixedStructure1, "");
        return Arrays.stream(value.split(","))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * 获取固定结构2列表
     */
    public List<String> getSplitFixedStructure2() {
        String value = StringUtils.defaultIfBlank(this.fixedStructure2, "");
        return Arrays.stream(value.split(","))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * 获取固定结构3列表
     */
    public List<String> getSplitFixedStructure3() {
        String value = StringUtils.defaultIfBlank(this.fixedStructure3, "");
        return Arrays.stream(value.split(","))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * 获取固定SKU列表
     */
    public List<String> getSplitFixedMaterialCode() {
        String value = StringUtils.defaultIfBlank(this.fixedMaterialCode, "");
        return Arrays.stream(value.split(","))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * 获取不可作业SKU列表
     */
    public List<String> getSplitDisableMaterialCode() {
        String value = StringUtils.defaultIfBlank(this.disableMaterialCode, "");
        return Arrays.stream(value.split(","))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * 获取固定物料描述列表
     */
    public List<String> getSplitFixedMaterialDesc() {
        String value = StringUtils.defaultIfBlank(this.fixedMaterialDesc, "");
        return Arrays.stream(value.split(","))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * 获取不可作业结构列表
     */
    public List<String> getSplitDisableStructure() {
        String value = StringUtils.defaultIfBlank(this.disableStructure, "");
        return Arrays.stream(value.split(","))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * 获取不可作业物料描述列表
     */
    public List<String> getSplitDisableMaterialDesc() {
        String value = StringUtils.defaultIfBlank(this.disableMaterialDesc, "");
        return Arrays.stream(value.split(","))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toList());
    }
}
