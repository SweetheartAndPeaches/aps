package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 成型固定机台对象 t_mdm_cx_machine_fixed
 */
@ApiModel(value = "成型固定机台对象", description = "成型固定机台对象")
@Data
@TableName(value = "T_MDM_CX_MACHINE_FIXED")
public class MdmCxMachineFixed extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工厂编号 */
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 成型机编码 */
    @ApiModelProperty(value = "成型机编码", name = "cxMachineCode")
    @TableField(value = "CX_MACHINE_CODE")
    private String cxMachineCode;

    /** 固定结构1 多个以,分隔拼接 */
    @ApiModelProperty(value = "固定结构1", name = "fixedStructure1")
    @TableField(value = "FIXED_STRUCTURE1", updateStrategy = FieldStrategy.IGNORED)
    private String fixedStructure1;

    /** 固定结构2 多个以,分隔拼接 */
    @ApiModelProperty(value = "固定结构2", name = "fixedStructure2")
    @TableField(value = "FIXED_STRUCTURE2", updateStrategy = FieldStrategy.IGNORED)
    private String fixedStructure2;

    /** 固定结构3 多个以,分隔拼接 */
    @ApiModelProperty(value = "固定结构3", name = "fixedStructure3")
    @TableField(value = "FIXED_STRUCTURE3", updateStrategy = FieldStrategy.IGNORED)
    private String fixedStructure3;

    /** 固定SKU 多个以,分隔拼接 */
    @ApiModelProperty(value = "固定SKU", name = "fixedMaterialCode")
    @TableField(value = "FIXED_MATERIAL_CODE", updateStrategy = FieldStrategy.IGNORED)
    private String fixedMaterialCode;

    /** 固定物料描述 多个以,分隔拼接 */
    @ApiModelProperty(value = "固定物料描述", name = "fixedMaterialDesc")
    @TableField(value = "FIXED_MATERIAL_DESC", updateStrategy = FieldStrategy.IGNORED)
    private String fixedMaterialDesc;

    /** 不可作业结构 多个以,分隔拼接 */
    @ApiModelProperty(value = "不可作业结构", name = "disableStructure")
    @TableField(value = "DISABLE_STRUCTURE", updateStrategy = FieldStrategy.IGNORED)
    private String disableStructure;

    /** 不可作业SKU 多个以,分隔拼接 */
    @ApiModelProperty(value = "不可作业SKU", name = "disableMaterialCode")
    @TableField(value = "DISABLE_MATERIAL_CODE", updateStrategy = FieldStrategy.IGNORED)
    private String disableMaterialCode;

    /** 不可作业物料描述 多个以,分隔拼接 */
    @ApiModelProperty(value = "不可作业物料描述", name = "disableMaterialDesc")
    @TableField(value = "DISABLE_MATERIAL_DESC", updateStrategy = FieldStrategy.IGNORED)
    private String disableMaterialDesc;

    public List<String> getSplitFixedStructure1() {
        String fixedStructure1 = StringUtils.defaultIfBlank(this.fixedStructure1, "");
        return Arrays.stream(fixedStructure1.split(",")).filter(StringUtils::isNotBlank).map(String::trim).collect(Collectors.toList());
    }

    public List<String> getSplitFixedStructure2() {
        String fixedStructure2 = StringUtils.defaultIfBlank(this.fixedStructure2, "");
        return Arrays.stream(fixedStructure2.split(",")).filter(StringUtils::isNotBlank).map(String::trim).collect(Collectors.toList());
    }

    public List<String> getSplitFixedStructure3() {
        String fixedStructure3 = StringUtils.defaultIfBlank(this.fixedStructure3, "");
        return Arrays.stream(fixedStructure3.split(",")).filter(StringUtils::isNotBlank).map(String::trim).collect(Collectors.toList());
    }

    public List<String> getSplitFixedMaterialCode() {
        String fixedMaterialCode = StringUtils.defaultIfBlank(this.fixedMaterialCode, "");
        return Arrays.stream(fixedMaterialCode.split(",")).filter(StringUtils::isNotBlank).map(String::trim).collect(Collectors.toList());
    }

    public List<String> getSplitDisableMaterialCode() {
        String disableMaterialCode = StringUtils.defaultIfBlank(this.disableMaterialCode, "");
        return Arrays.stream(disableMaterialCode.split(",")).filter(StringUtils::isNotBlank).map(String::trim).collect(Collectors.toList());
    }

    public List<String> getSplitFixedMaterialDesc() {
        String fixedMaterialDesc = StringUtils.defaultIfBlank(this.fixedMaterialDesc, "");
        return Arrays.stream(fixedMaterialDesc.split(",")).filter(StringUtils::isNotBlank).map(String::trim).collect(Collectors.toList());
    }

    public List<String> getSplitDisableStructure() {
        String disableStructure = StringUtils.defaultIfBlank(this.disableStructure, "");
        return Arrays.stream(disableStructure.split(",")).filter(StringUtils::isNotBlank).map(String::trim).collect(Collectors.toList());
    }

    public List<String> getSplitDisableMaterialDesc() {
        String disableMaterialDesc = StringUtils.defaultIfBlank(this.disableMaterialDesc, "");
        return Arrays.stream(disableMaterialDesc.split(",")).filter(StringUtils::isNotBlank).map(String::trim).collect(Collectors.toList());
    }
}
