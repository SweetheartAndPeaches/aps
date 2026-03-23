package com.jinyu.aps.entity.mdm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jinyu.aps.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 结构名称字典对象
 * 对应表：T_MDM_STRUCTURE_NAME
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_MDM_STRUCTURE_NAME")
@Schema(description = "结构名称字典对象")
public class MdmStructureName extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 结构编码
     */
    @Schema(description = "结构编码")
    @TableField("STRUCTURE_CODE")
    private String structureCode;

    /**
     * 结构名称
     */
    @Schema(description = "结构名称")
    @TableField("STRUCTURE_NAME")
    private String structureName;
}
