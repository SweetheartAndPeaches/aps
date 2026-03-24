package com.zlt.aps.cx.entity.mdm;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "成型在机信息")
public class MdmCxMachineOnlineInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @Schema(description = "在机日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("ONLINE_DATE")
    private LocalDate onlineDate;

    @Schema(description = "成型机台编码")
    @TableField("CX_CODE")
    private String cxCode;

    @Schema(description = "在机物料编码（NC系统）")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    @Schema(description = "在机物料编码（MES系统）")
    @TableField("MES_MATERIAL_CODE")
    private String mesMaterialCode;

    @Schema(description = "在机物料描述")
    @TableField("SPEC_DESC")
    private String specDesc;

    @Schema(description = "在机胎胚描述")
    @TableField("EMBRYO_SPEC")
    private String embryoSpec;

    @Schema(description = "版本号")
    @TableField("DATA_VERSION")
    private String dataVersion;

    @Schema(description = "分公司编码")
    @TableField("COMPANY_CODE")
    private String companyCode;

    @Schema(description = "厂别")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    @Schema(description = "删除标识")
    @TableField("IS_DELETE")
    private Integer isDelete;

    @Schema(description = "创建人")
    @TableField("CREATE_BY")
    private String createBy;

    @Schema(description = "创建时间")
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新人")
    @TableField("UPDATE_BY")
    private String updateBy;

    @Schema(description = "更新时间")
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "备注")
    @TableField("REMARK")
    private String remark;
}
