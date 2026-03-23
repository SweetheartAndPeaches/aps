package com.jinyu.aps.entity.config;

import com.baomidou.mybatisplus.annotation.*;
import com.jinyu.aps.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;

/**
 * 班次配置表
 * 对应表：T_CX_SHIFT_CONFIG
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_CX_SHIFT_CONFIG")
@Schema(description = "班次配置对象")
public class CxShiftConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 班次编码：NIGHT-夜班/DAY-早班/AFTERNOON-中班
     */
    @Schema(description = "班次编码：NIGHT-夜班/DAY-早班/AFTERNOON-中班")
    @TableField("SHIFT_CODE")
    private String shiftCode;

    /**
     * 班次名称
     */
    @Schema(description = "班次名称")
    @TableField("SHIFT_NAME")
    private String shiftName;

    /**
     * 开始时间
     */
    @Schema(description = "开始时间")
    @TableField("START_TIME")
    private LocalTime startTime;

    /**
     * 结束时间
     */
    @Schema(description = "结束时间")
    @TableField("END_TIME")
    private LocalTime endTime;

    /**
     * 排序（用于排班顺序）
     */
    @Schema(description = "排序（用于排班顺序）")
    @TableField("SORT_ORDER")
    private Integer sortOrder;

    /**
     * 是否启用
     */
    @Schema(description = "是否启用")
    @TableField("IS_ACTIVE")
    private Integer isActive;
}
