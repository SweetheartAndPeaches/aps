package com.zlt.aps.cx.entity.base;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * APS基础实体类
 * 扩展BaseEntity，增加工厂编号等APS系统特有字段
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "APS基础实体类")
public class ApsBaseEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号（分厂编号）
     * 用于多工厂场景下的数据隔离
     */
    @ApiModelProperty(value = "工厂编号")
    @TableField("FACTORY_CODE")
    private String factoryCode;
}
