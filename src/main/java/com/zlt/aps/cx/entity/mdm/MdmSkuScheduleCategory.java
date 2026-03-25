package com.zlt.aps.cx.entity.mdm;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SKU排产分类实体
 * 
 * 用于判断产品是否为主销产品
 * SCHEDULE_TYPE = '01' 表示主销产品（月均销量>=500条）
 *
 * @author APS Team
 */
@Data
@TableName("t_mdm_sku_schedule_category")
@ApiModel(value = "SKU排产分类")
public class MdmSkuScheduleCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "工厂编号")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    @ApiModelProperty(value = "物料编码")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    @ApiModelProperty(value = "排产类型：01-主销产品、02-常规产品、03-常规周期产品、04-波动性产品、05-按单排产产品")
    @TableField("SCHEDULE_TYPE")
    private String scheduleType;

    @ApiModelProperty(value = "生成日期")
    @TableField("GENRATE_DATE")
    private LocalDateTime genrateDate;

    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;

    @ApiModelProperty(value = "是否删除：0-未删除 1-已删除")
    @TableField("IS_DELETE")
    private Integer isDelete;

    @ApiModelProperty(value = "创建时间")
    @TableField("CREATE_TIME")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField("UPDATE_TIME")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "创建人")
    @TableField("CREATE_BY")
    private String createBy;

    @ApiModelProperty(value = "修改人")
    @TableField("UPDATE_BY")
    private String updateBy;

    /**
     * 是否为主销产品
     * 主销产品：月均销量 >= 500条
     */
    public boolean isMainProduct() {
        return "01".equals(this.scheduleType);
    }
}
