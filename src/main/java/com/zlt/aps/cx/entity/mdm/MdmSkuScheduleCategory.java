package com.zlt.aps.cx.entity.mdm;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "SKU排产分类")
public class MdmSkuScheduleCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @Schema(description = "工厂编号")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    @Schema(description = "物料编码")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    @Schema(description = "排产类型：01-主销产品、02-常规产品、03-常规周期产品、04-波动性产品、05-按单排产产品")
    @TableField("SCHEDULE_TYPE")
    private String scheduleType;

    @Schema(description = "生成日期")
    @TableField("GENRATE_DATE")
    private LocalDateTime genrateDate;

    @Schema(description = "备注")
    @TableField("REMARK")
    private String remark;

    @Schema(description = "是否删除：0-未删除 1-已删除")
    @TableField("IS_DELETE")
    private Integer isDelete;

    @Schema(description = "创建时间")
    @TableField("CREATE_TIME")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField("UPDATE_TIME")
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    @TableField("CREATE_BY")
    private String createBy;

    @Schema(description = "修改人")
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
