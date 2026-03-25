package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 成型在机信息实体
 * 
 * 来自MES系统的成型机台在机物料信息
 * 用于判断续作任务：当前机台正在做的胎胚，今天继续做的就是续作
 *
 * @author APS Team
 */
@Data
@TableName("T_MDM_CX_MACHINE_ONLINE_INFO")
@ApiModel(value = "成型在机信息")
public class MdmCxMachineOnlineInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "在机日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("ONLINE_DATE")
    private LocalDate onlineDate;

    @ApiModelProperty(value = "成型机台编码")
    @TableField("CX_CODE")
    private String cxCode;

    @ApiModelProperty(value = "在机物料编码（NC系统）")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    @ApiModelProperty(value = "在机物料编码（MES系统）")
    @TableField("MES_MATERIAL_CODE")
    private String mesMaterialCode;

    @ApiModelProperty(value = "在机物料描述")
    @TableField("SPEC_DESC")
    private String specDesc;

    @ApiModelProperty(value = "在机胎胚描述")
    @TableField("EMBRYO_SPEC")
    private String embryoSpec;

    @ApiModelProperty(value = "版本号")
    @TableField("DATA_VERSION")
    private String dataVersion;

    @ApiModelProperty(value = "分公司编码")
    @TableField("COMPANY_CODE")
    private String companyCode;

    @ApiModelProperty(value = "厂别")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    @ApiModelProperty(value = "删除标识")
    @TableField("IS_DELETE")
    private Integer isDelete;

    @ApiModelProperty(value = "创建人")
    @TableField("CREATE_BY")
    private String createBy;

    @ApiModelProperty(value = "创建时间")
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新人")
    @TableField("UPDATE_BY")
    private String updateBy;

    @ApiModelProperty(value = "更新时间")
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;
}
