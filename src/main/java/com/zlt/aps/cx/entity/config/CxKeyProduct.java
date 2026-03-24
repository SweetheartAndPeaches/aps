package com.zlt.aps.cx.entity.config;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 关键产品配置实体
 * 
 * 定义关键产品列表，用于开产首班排除等场景判断
 *
 * @author APS Team
 */
@Data
@TableName("T_CX_KEY_PRODUCT")
@Schema(description = "关键产品配置")
public class CxKeyProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @Schema(description = "胎胚编码")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    @Schema(description = "胎胚名称")
    @TableField("EMBRYO_NAME")
    private String embryoName;

    @Schema(description = "结构编码")
    @TableField("STRUCTURE_CODE")
    private String structureCode;

    @Schema(description = "结构名称")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    @Schema(description = "优先级")
    @TableField("PRIORITY")
    private Integer priority;

    @Schema(description = "是否启用：0-禁用 1-启用")
    @TableField("IS_ACTIVE")
    private Integer isActive;

    @Schema(description = "备注")
    @TableField("REMARK")
    private String remark;

    @Schema(description = "创建时间")
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
