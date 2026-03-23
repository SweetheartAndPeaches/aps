package com.jinyu.aps.entity.mdm;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.jinyu.aps.entity.base.ApsBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成型机台信息对象
 * 对应表：T_MDM_MOLDING_MACHINE
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_MDM_MOLDING_MACHINE")
@Schema(description = "成型机台信息对象")
public class MdmMoldingMachine extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 成型机编码
     */
    @Schema(description = "成型机编码")
    @TableField("CX_MACHINE_CODE")
    private String cxMachineCode;

    /**
     * 成型机名称
     */
    @Schema(description = "成型机名称")
    @TableField("CX_MACHINE_NAME")
    private String cxMachineName;

    /**
     * 成型机类型编码
     */
    @Schema(description = "成型机类型编码")
    @TableField("CX_MACHINE_TYPE_CODE")
    private String cxMachineTypeCode;

    /**
     * 成型机类型名称
     */
    @Schema(description = "成型机类型名称")
    @TableField("CX_MACHINE_TYPE_NAME")
    private String cxMachineTypeName;

    /**
     * 机型型号
     */
    @Schema(description = "机型型号")
    @TableField("CX_MACHINE_MODEL")
    private String cxMachineModel;

    /**
     * 反包方式
     */
    @Schema(description = "反包方式")
    @TableField("WRAPPING_TYPE")
    private String wrappingType;

    /**
     * 是否有零度供料架：0-否 1-是
     */
    @Schema(description = "是否有零度供料架：0-否 1-是")
    @TableField("HAS_ZERO_DEGREE_FEEDER")
    private Integer hasZeroDegreeFeeder;

    /**
     * 当前在产结构
     */
    @Schema(description = "当前在产结构")
    @TableField("CURRENT_STRUCTURE")
    private String currentStructure;

    /**
     * 每小时产能（条）
     */
    @Schema(description = "每小时产能（条）")
    @TableField("PRODUCTION_CAPACITY")
    private BigDecimal productionCapacity;

    /**
     * 设备最大日产能（条）
     */
    @Schema(description = "设备最大日产能（条）")
    @TableField("MAX_DAILY_CAPACITY")
    private Integer maxDailyCapacity;

    /**
     * 对应硫化机上限数量
     */
    @Schema(description = "对应硫化机上限数量")
    @TableField("MAX_CURING_MACHINES")
    private Integer maxCuringMachines;

    /**
     * 产线编号
     */
    @Schema(description = "产线编号")
    @TableField("LINE_NUMBER")
    private Integer lineNumber;

    /**
     * 机台状态：0-正常 1-维护中 2-故障 3-停用
     */
    @Schema(description = "机台状态：0-正常 1-维护中 2-故障 3-停用")
    @TableField("MAINTAIN_STATUS")
    private String maintainStatus;

    /**
     * 维护开始时间
     */
    @Schema(description = "维护开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("MAINTAIN_START_TIME")
    private LocalDateTime maintainStartTime;

    /**
     * 维护结束时间
     */
    @Schema(description = "维护结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("MAINTAIN_END_TIME")
    private LocalDateTime maintainEndTime;

    /**
     * 排产限制说明
     */
    @Schema(description = "排产限制说明")
    @TableField("PRODUCTION_RESTRICTION")
    private String productionRestriction;

    /**
     * 是否启用：0-禁用 1-启用
     */
    @Schema(description = "是否启用：0-禁用 1-启用")
    @TableField("IS_ACTIVE")
    private Integer isActive;
}
