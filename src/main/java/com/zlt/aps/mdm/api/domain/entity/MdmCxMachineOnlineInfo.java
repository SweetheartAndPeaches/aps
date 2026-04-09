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
 * 成型机台在机信息
 *
 * @author APS Team
 */
@Data
@ApiModel(description = "成型机台在机信息")
@TableName("T_MDM_CX_MACHINE_ONLINE_INFO")
public class MdmCxMachineOnlineInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 信息日期
     */
    @ApiModelProperty(value = "信息日期")
    @TableField("INFO_DATE")
    private Date infoDate;

    /**
     * 成型机台编码
     */
    @ApiModelProperty(value = "成型机台编码")
    @TableField("CX_MACHINE_CODE")
    private String cxMachineCode;

    /**
     * 胎胚编码
     */
    @ApiModelProperty(value = "胎胚编码")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    /**
     * 物料编码
     */
    @ApiModelProperty(value = "物料编码")
    @TableField("MATERIAL_CODE")
    private String materialCode;

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
     * 在机数量
     */
    @ApiModelProperty(value = "在机数量")
    @TableField("ONLINE_QTY")
    private Integer onlineQty;

    /**
     * 生产开始时间
     */
    @ApiModelProperty(value = "生产开始时间")
    @TableField("PRODUCTION_START_TIME")
    private Date productionStartTime;

    /**
     * 预计结束时间
     */
    @ApiModelProperty(value = "预计结束时间")
    @TableField("EXPECTED_END_TIME")
    private Date expectedEndTime;

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
