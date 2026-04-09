package com.zlt.aps.mdm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 结构整车配置实体
 * 
 * 定义每个结构的整车胎面条数配置
 *
 * @author APS Team
 */
@Data
@TableName("t_mdm_structure_tread_config")
@ApiModel(value = "结构整车配置")
public class MdmStructureTreadConfig extends BaseEntity {

    @ApiModelProperty(value = "结构")
    @TableField(value = "STRUCTURE_CODE")
    private String structureCode;

    @ApiModelProperty(value = "整车胎面条数")
    @TableField(value = "TREAD_COUNT")
    private Integer treadCount;

    @ApiModelProperty(value = "版本号")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    @ApiModelProperty(value = "分公司编码")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    @ApiModelProperty(value = "厂别")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;
}
