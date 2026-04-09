package com.zlt.aps.mdm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 物料信息
 *
 * @author APS Team
 */
@Data
@ApiModel(description = "物料信息")
@TableName("T_MDM_MATERIAL_INFO")
public class MdmMaterialInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 物料编码
     */
    @ApiModelProperty(value = "物料编码")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    /**
     * 物料描述
     */
    @ApiModelProperty(value = "物料描述")
    @TableField("MATERIAL_DESC")
    private String materialDesc;

    /**
     * 胎胚编码
     */
    @ApiModelProperty(value = "胎胚编码")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    /**
     * 产品结构
     */
    @ApiModelProperty(value = "产品结构")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    /**
     * 花纹代号
     */
    @ApiModelProperty(value = "花纹代号")
    @TableField("PATTERN_CODE")
    private String patternCode;

    /**
     * 主物料(胎胚描述)
     */
    @ApiModelProperty(value = "主物料(胎胚描述)")
    @TableField("MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;

    /**
     * 制造示方书号
     */
    @ApiModelProperty(value = "制造示方书号")
    @TableField("EMBRYO_NO")
    private String embryoNo;

    /**
     * 物料组
     */
    @ApiModelProperty(value = "物料组")
    @TableField("MATERIAL_GROUP")
    private String materialGroup;

    /**
     * 单位
     */
    @ApiModelProperty(value = "单位")
    @TableField("UNIT")
    private String unit;

    /**
     * 物料类型
     */
    @ApiModelProperty(value = "物料类型")
    @TableField("MATERIAL_TYPE")
    private String materialType;

    /**
     * 是否删除
     */
    @ApiModelProperty(value = "是否删除(0-未删除 1-已删除)")
    @TableField("IS_DELETE")
    private String isDelete;

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
