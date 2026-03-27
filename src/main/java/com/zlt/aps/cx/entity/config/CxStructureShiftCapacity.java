package com.zlt.aps.cx.entity.config;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 结构整车配置实体
 * 
 * 定义每个结构的整车条数
 * 整车条数按结构不同而不同，可能是12条、18条等
 *
 * @author APS Team
 */
@Data
@TableName("T_CX_STRUCTURE_TRIP_CONFIG")
@ApiModel(value = "结构整车配置")
public class CxStructureShiftCapacity extends BaseEntity {

    @ApiModelProperty(value = "结构编码")
    @TableField("STRUCTURE_CODE")
    private String structureCode;

    @ApiModelProperty(value = "整车条数（该结构每车的条数，可能是12、18等）")
    @TableField("TRIP_QTY")
    private Integer tripQty;

    /**
     * 获取标准产能（条）
     * 标准产能 = 整车条数 × 1车
     */
    public Integer getStandardCapacity() {
        if (this.tripQty != null) {
            return this.tripQty;
        }
        return 12; // 默认12条
    }
}
