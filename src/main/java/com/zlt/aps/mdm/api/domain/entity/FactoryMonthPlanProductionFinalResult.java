package com.zlt.aps.mdm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 工厂月生产计划-最终排产计划定稿
 *
 * @author APS Team
 */
@Data
@ApiModel(description = "工厂月生产计划-最终排产计划定稿")
@TableName("T_MP_MONTH_PLAN_PROD_FINAL")
public class FactoryMonthPlanProductionFinalResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 工厂编号
     */
    @ApiModelProperty(value = "工厂编号")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    /**
     * 年月(YYYYMM)
     */
    @ApiModelProperty(value = "年月(YYYYMM)")
    @TableField("YEAR_MONTH")
    private Integer yearMonth;

    /**
     * 物料编号
     */
    @ApiModelProperty(value = "物料编号")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    /**
     * 物料描述
     */
    @ApiModelProperty(value = "物料描述")
    @TableField("MATERIAL_DESC")
    private String materialDesc;

    /**
     * 生产版本
     */
    @ApiModelProperty(value = "生产版本")
    @TableField("PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 计划总量
     */
    @ApiModelProperty(value = "计划总量")
    @TableField("PLAN_QTY")
    private BigDecimal planQty;

    /**
     * 工单号
     */
    @ApiModelProperty(value = "工单号")
    @TableField("PRODUCTION_NO")
    private String productionNo;

    /**
     * 生产顺序
     */
    @ApiModelProperty(value = "生产顺序")
    @TableField("PRODUCTION_SEQUENCE")
    private Integer productionSequence;

    /**
     * 是否发布(0-未发布 1-已发布)
     */
    @ApiModelProperty(value = "是否发布(0-未发布 1-已发布)")
    @TableField("IS_RELEASED")
    private String isReleased;

    /**
     * 发布状态
     */
    @ApiModelProperty(value = "发布状态")
    @TableField("RELEASE_STATUS")
    private String releaseStatus;

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
