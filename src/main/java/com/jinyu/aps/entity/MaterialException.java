package com.jinyu.aps.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 材料异常记录实体类
 * 
 * 用于记录和管理材料异常情况，影响排程决策。
 *
 * @author APS Team
 */
@Data
@TableName(value = "t_cx_material_exception", keepGlobalPrefix = false)
@Schema(description = "材料异常记录")
public class MaterialException implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "异常单号")
    @TableField("exception_no")
    private String exceptionNo;

    @Schema(description = "材料编码")
    @TableField("material_code")
    private String materialCode;

    @Schema(description = "材料名称")
    @TableField("material_name")
    private String materialName;

    @Schema(description = "结构编码")
    @TableField("structure_code")
    private String structureCode;

    @Schema(description = "异常类型：QUALITY(质量问题)/SHORTAGE(缺料)/DELAY(到货延迟)/SPEC_CHANGE(规格变更)/OTHER(其他)")
    @TableField("exception_type")
    private String exceptionType;

    @Schema(description = "异常等级：LOW(低)/MEDIUM(中)/HIGH(高)/CRITICAL(紧急)")
    @TableField("exception_level")
    private String exceptionLevel;

    @Schema(description = "异常描述")
    @TableField("description")
    private String description;

    @Schema(description = "影响数量")
    @TableField("affected_quantity")
    private Integer affectedQuantity;

    @Schema(description = "影响开始日期")
    @TableField("affect_start_date")
    private LocalDate affectStartDate;

    @Schema(description = "影响结束日期")
    @TableField("affect_end_date")
    private LocalDate affectEndDate;

    @Schema(description = "处理方式：ADJUST_PLAN(调整计划)/CHANGE_MATERIAL(更换材料)/WAIT_SUPPLY(等待供应)/CANCEL_PLAN(取消计划)")
    @TableField("handling_method")
    private String handlingMethod;

    @Schema(description = "替代材料编码")
    @TableField("substitute_material")
    private String substituteMaterial;

    @Schema(description = "处理状态：PENDING(待处理)/PROCESSING(处理中)/RESOLVED(已解决)/CLOSED(已关闭)")
    @TableField("status")
    private String status;

    @Schema(description = "发现人")
    @TableField("discoverer")
    private String discoverer;

    @Schema(description = "发现时间")
    @TableField("discover_time")
    private LocalDateTime discoverTime;

    @Schema(description = "责任人")
    @TableField("responsible_person")
    private String responsiblePerson;

    @Schema(description = "处理人")
    @TableField("handler")
    private String handler;

    @Schema(description = "处理时间")
    @TableField("handle_time")
    private LocalDateTime handleTime;

    @Schema(description = "处理结果说明")
    @TableField("handle_result")
    private String handleResult;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
