package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 材料异常记录实体类
 * 对应数据库表：T_CX_MATERIAL_EXCEPTION
 * 
 * 用于记录和管理材料异常情况，影响排程决策。
 *
 * @author APS Team
 */
@Data
@TableName(value = "T_CX_MATERIAL_EXCEPTION", keepGlobalPrefix = false)
@Schema(description = "材料异常记录")
public class CxMaterialException implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @Schema(description = "异常单号")
    @TableField("EXCEPTION_NO")
    private String exceptionNo;

    @Schema(description = "材料编码")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    @Schema(description = "材料名称")
    @TableField("MATERIAL_NAME")
    private String materialName;

    @Schema(description = "结构编码")
    @TableField("STRUCTURE_CODE")
    private String structureCode;

    @Schema(description = "异常类型：QUALITY(质量问题)/SHORTAGE(缺料)/DELAY(到货延迟)/SPEC_CHANGE(规格变更)/OTHER(其他)")
    @TableField("EXCEPTION_TYPE")
    private String exceptionType;

    @Schema(description = "异常等级：LOW(低)/MEDIUM(中)/HIGH(高)/CRITICAL(紧急)")
    @TableField("EXCEPTION_LEVEL")
    private String exceptionLevel;

    @Schema(description = "异常描述")
    @TableField("DESCRIPTION")
    private String description;

    @Schema(description = "影响数量")
    @TableField("AFFECTED_QUANTITY")
    private Integer affectedQuantity;

    @Schema(description = "影响开始日期")
    @TableField("AFFECT_START_DATE")
    private LocalDate affectStartDate;

    @Schema(description = "影响结束日期")
    @TableField("AFFECT_END_DATE")
    private LocalDate affectEndDate;

    @Schema(description = "处理方式：ADJUST_PLAN(调整计划)/CHANGE_MATERIAL(更换材料)/WAIT_SUPPLY(等待供应)/CANCEL_PLAN(取消计划)")
    @TableField("HANDLING_METHOD")
    private String handlingMethod;

    @Schema(description = "替代材料编码")
    @TableField("SUBSTITUTE_MATERIAL")
    private String substituteMaterial;

    @Schema(description = "处理状态：PENDING(待处理)/PROCESSING(处理中)/RESOLVED(已解决)/CLOSED(已关闭)")
    @TableField("STATUS")
    private String status;

    @Schema(description = "发现人")
    @TableField("DISCOVERER")
    private String discoverer;

    @Schema(description = "发现时间")
    @TableField("DISCOVER_TIME")
    private LocalDateTime discoverTime;

    @Schema(description = "责任人")
    @TableField("RESPONSIBLE_PERSON")
    private String responsiblePerson;

    @Schema(description = "处理人")
    @TableField("HANDLER")
    private String handler;

    @Schema(description = "处理时间")
    @TableField("HANDLE_TIME")
    private LocalDateTime handleTime;

    @Schema(description = "处理结果说明")
    @TableField("HANDLE_RESULT")
    private String handleResult;

    @Schema(description = "创建时间")
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
