package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 物料收尾管理实体类
 * 对应数据库表：T_CX_MATERIAL_ENDING
 *
 * 用于跟踪每个物料的收尾进度，支持紧急收尾判断。
 * 收尾管理规则（严格依据月计划收尾日）：
 * 1. 收尾日判断：从月计划day_1到day_31找到最后一个有排产的日期
 * 2. 收尾前10天检查：
 *    - 计算：成型余量 = 硫化余量 - 胎胚库存
 *    - 判断：能否在收尾日前完成？
 * 3. 延误量追赶：
 *    - 如果做不完，计算延误量，平摊到未来3天
 *    - 检查未来3天满产是否能追上
 * 4. 满产判断：
 *    - 如果未来3天满产仍追不上，通知月计划调整（调用硫化接口）
 *
 * @author APS Team
 */
@Data
@TableName(value = "t_cx_material_ending", keepGlobalPrefix = false)
@ApiModel(value = "物料收尾管理")
public class CxMaterialEnding implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 工厂编码
     */
    @ApiModelProperty(value = "工厂编码")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    /**
     * 物料编码
     */
    @ApiModelProperty(value = "物料编码")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    /**
     * 物料描述
     */
    @ApiModelProperty(value = "物料描述")
    @TableField("MATERIAL_DESC")
    private String materialDesc;

    /**
     * 结构名称（关联的结构）
     */
    @ApiModelProperty(value = "结构名称")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    /**
     * 硫化余量（条）- 从月度计划余量表获取
     */
    @ApiModelProperty(value = "硫化余量（条）")
    @TableField("VULCANIZING_REMAINDER")
    private Integer vulcanizingRemainder;

    /**
     * 胎胚库存（条）- 使用有效库存：库存量 - 超期库存 - 不良数量 + 修正数量
     */
    @ApiModelProperty(value = "胎胚库存（条）")
    @TableField("EMBRYO_STOCK")
    private Integer embryoStock;

    /**
     * 成型余量 = 硫化余量 - 胎胚库存（需要生产的量）
     */
    @ApiModelProperty(value = "成型余量 = 硫化余量 - 胎胚库存")
    @TableField("FORMING_REMAINDER")
    private Integer formingRemainder;

    /**
     * 日硫化产能（满算力，单位：条）
     * 计算公式：成型供的硫化机中该物料的日产汇总（配比塞满的情况）
     */
    @ApiModelProperty(value = "日硫化产能（满算力，单位：条）")
    @TableField("DAILY_LH_CAPACITY")
    private Integer dailyLhCapacity;

    /**
     * 日成型产能（单位：条）
     */
    @ApiModelProperty(value = "日成型产能（单位：条）")
    @TableField("DAILY_FORMING_CAPACITY")
    private Integer dailyFormingCapacity;

    /**
     * 预计收尾天数
     */
    @ApiModelProperty(value = "预计收尾天数")
    @TableField("ESTIMATED_ENDING_DAYS")
    private BigDecimal estimatedEndingDays;

    /**
     * 计划收尾日期
     */
    @ApiModelProperty(value = "计划收尾日期")
    @TableField("PLANNED_ENDING_DATE")
    private LocalDate plannedEndingDate;

    /**
     * 是否紧急收尾（3天内）
     */
    @ApiModelProperty(value = "是否紧急收尾（3天内）")
    @TableField("IS_URGENT_ENDING")
    private Integer isUrgentEnding;

    /**
     * 是否10天内收尾
     */
    @ApiModelProperty(value = "是否10天内收尾")
    @TableField("IS_NEAR_ENDING")
    private Integer isNearEnding;

    /**
     * 延误量（条）- 会延误的量
     */
    @ApiModelProperty(value = "延误量（条）")
    @TableField("DELAY_QUANTITY")
    private Integer delayQuantity;

    /**
     * 平摊到未来3天的量（每天需要额外生产的量）
     */
    @ApiModelProperty(value = "平摊到未来3天的量")
    @TableField("DISTRIBUTED_QUANTITY")
    private Integer distributedQuantity;

    /**
     * 是否需要调整月计划（0-否，1-是）
     */
    @ApiModelProperty(value = "是否需要调整月计划（0-否，1-是）")
    @TableField("NEED_MONTH_PLAN_ADJUST")
    private Integer needMonthPlanAdjust;

    /**
     * 统计日期
     */
    @ApiModelProperty(value = "统计日期")
    @TableField("STAT_DATE")
    private LocalDate statDate;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间")
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @ApiModelProperty(value = "更新时间")
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;
}
