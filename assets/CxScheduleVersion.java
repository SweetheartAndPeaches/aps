package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 排程版本表 T_CX_SCHEDULE_VERSION
 */
@ApiModel(value = "排程版本对象", description = "排程版本表")
@Data
@TableName(value = "T_CX_SCHEDULE_VERSION")
public class CxScheduleVersion extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "主键ID", name = "id")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /** 排程主表ID */
    @ApiModelProperty(value = "排程主表ID", name = "mainId")
    @TableField(value = "MAIN_ID")
    private Long mainId;

    /** 排程明细ID（为空表示整批调整） */
    @ApiModelProperty(value = "排程明细ID", name = "detailId")
    @TableField(value = "DETAIL_ID")
    private Long detailId;

    /** 版本号 */
    @ApiModelProperty(value = "版本号", name = "versionNo")
    @TableField(value = "VERSION_NO")
    private Integer versionNo;

    /** 操作类型：CREATE创建/ADJUST调整/DELETE删除/SWAP转机台/INSERT插单 */
    @ApiModelProperty(value = "操作类型", name = "operationType")
    @TableField(value = "OPERATION_TYPE")
    private String operationType;

    /** 操作描述 */
    @ApiModelProperty(value = "操作描述", name = "operationDesc")
    @TableField(value = "OPERATION_DESC")
    private String operationDesc;

    /** 操作人 */
    @ApiModelProperty(value = "操作人", name = "operator")
    @TableField(value = "OPERATOR")
    private String operator;

    /** 操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "操作时间", name = "operationTime")
    @TableField(value = "OPERATION_TIME")
    private LocalDateTime operationTime;

    /** 变更前内容 */
    @ApiModelProperty(value = "变更前内容", name = "changeBefore")
    @TableField(value = "CHANGE_BEFORE")
    private String changeBefore;

    /** 变更后内容 */
    @ApiModelProperty(value = "变更后内容", name = "changeAfter")
    @TableField(value = "CHANGE_AFTER")
    private String changeAfter;

    /** 变更原因 */
    @ApiModelProperty(value = "变更原因", name = "changeReason")
    @TableField(value = "CHANGE_REASON")
    private String changeReason;

    /** 完整快照（用于回滚） */
    @ApiModelProperty(value = "完整快照", name = "snapshot")
    @TableField(value = "SNAPSHOT")
    private String snapshot;
}
