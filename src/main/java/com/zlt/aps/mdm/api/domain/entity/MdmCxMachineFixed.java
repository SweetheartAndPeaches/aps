package com.zlt.aps.mdm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 成型机台定值配置
 *
 * @author APS Team
 */
@Data
@ApiModel(description = "成型机台定值配置")
@TableName("T_MDM_CX_MACHINE_FIXED")
public class MdmCxMachineFixed implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 成型机台编码
     */
    @ApiModelProperty(value = "成型机台编码")
    @TableField("CX_MACHINE_CODE")
    private String cxMachineCode;

    /**
     * 结构名称
     */
    @ApiModelProperty(value = "结构名称")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    /**
     * 花纹代号
     */
    @ApiModelProperty(value = "花纹代号")
    @TableField("PATTERN_CODE")
    private String patternCode;

    /**
     * 物料编码
     */
    @ApiModelProperty(value = "物料编码")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    /**
     * 优先级
     */
    @ApiModelProperty(value = "优先级")
    @TableField("PRIORITY")
    private Integer priority;

    /**
     * 是否启用
     */
    @ApiModelProperty(value = "是否启用(0-禁用 1-启用)")
    @TableField("IS_ACTIVE")
    private Integer isActive;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;

    /**
     * 创建人
     */
    @ApiModelProperty(value = "创建人")
    @TableField("CREATE_BY")
    private String createBy;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间")
    @TableField("CREATE_TIME")
    private Date createTime;

    /**
     * 更新人
     */
    @ApiModelProperty(value = "更新人")
    @TableField("UPDATE_BY")
    private String updateBy;

    /**
     * 更新时间
     */
    @ApiModelProperty(value = "更新时间")
    @TableField("UPDATE_TIME")
    private Date updateTime;
}
