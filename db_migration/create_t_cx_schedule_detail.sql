-- =============================================
-- APS排程明细表建表语句
-- 日期: 2025-07-02
-- 说明: CxScheduleDetail实体类完整结构
-- =============================================

CREATE TABLE IF NOT EXISTS t_cx_schedule_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    main_id BIGINT COMMENT '所属主表ID(T_CX_SCHEDULE_RESULT.id)',

    -- 一班字段
    class1_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '一班计划数',
    class1_trip_no VARCHAR(50) COMMENT '一班车次号',
    class1_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '一班车次容量（整车条数）',
    class1_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '一班库存可供硫化时长',
    class1_sequence INT DEFAULT 0 COMMENT '一班顺位',
    class1_plan_start_time DATETIME COMMENT '一班计划开始时间',
    class1_plan_end_time DATETIME COMMENT '一班计划结束时间',

    -- 二班字段
    class2_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '二班计划数',
    class2_trip_no VARCHAR(50) COMMENT '二班车次号',
    class2_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '二班车次容量（整车条数）',
    class2_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '二班库存可供硫化时长',
    class2_sequence INT DEFAULT 0 COMMENT '二班顺位',
    class2_plan_start_time DATETIME COMMENT '二班计划开始时间',
    class2_plan_end_time DATETIME COMMENT '二班计划结束时间',

    -- 三班字段
    class3_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '三班计划数',
    class3_trip_no VARCHAR(50) COMMENT '三班车次号',
    class3_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '三班车次容量（整车条数）',
    class3_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '三班库存可供硫化时长',
    class3_sequence INT DEFAULT 0 COMMENT '三班顺位',
    class3_plan_start_time DATETIME COMMENT '三班计划开始时间',
    class3_plan_end_time DATETIME COMMENT '三班计划结束时间',

    -- 四班字段
    class4_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '四班计划数',
    class4_trip_no VARCHAR(50) COMMENT '四班车次号',
    class4_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '四班车次容量（整车条数）',
    class4_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '四班库存可供硫化时长',
    class4_sequence INT DEFAULT 0 COMMENT '四班顺位',
    class4_plan_start_time DATETIME COMMENT '四班计划开始时间',
    class4_plan_end_time DATETIME COMMENT '四班计划结束时间',

    -- 五班字段
    class5_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '五班计划数',
    class5_trip_no VARCHAR(50) COMMENT '五班车次号',
    class5_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '五班车次容量（整车条数）',
    class5_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '五班库存可供硫化时长',
    class5_sequence INT DEFAULT 0 COMMENT '五班顺位',
    class5_plan_start_time DATETIME COMMENT '五班计划开始时间',
    class5_plan_end_time DATETIME COMMENT '五班计划结束时间',

    -- 六班字段
    class6_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '六班计划数',
    class6_trip_no VARCHAR(50) COMMENT '六班车次号',
    class6_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '六班车次容量（整车条数）',
    class6_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '六班库存可供硫化时长',
    class6_sequence INT DEFAULT 0 COMMENT '六班顺位',
    class6_plan_start_time DATETIME COMMENT '六班计划开始时间',
    class6_plan_end_time DATETIME COMMENT '六班计划结束时间',

    -- 七班字段
    class7_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '七班计划数',
    class7_trip_no VARCHAR(50) COMMENT '七班车次号',
    class7_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '七班车次容量（整车条数）',
    class7_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '七班库存可供硫化时长',
    class7_sequence INT DEFAULT 0 COMMENT '七班顺位',
    class7_plan_start_time DATETIME COMMENT '七班计划开始时间',
    class7_plan_end_time DATETIME COMMENT '七班计划结束时间',

    -- 八班字段
    class8_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '八班计划数',
    class8_trip_no VARCHAR(50) COMMENT '八班车次号',
    class8_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '八班车次容量（整车条数）',
    class8_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '八班库存可供硫化时长',
    class8_sequence INT DEFAULT 0 COMMENT '八班顺位',
    class8_plan_start_time DATETIME COMMENT '八班计划开始时间',
    class8_plan_end_time DATETIME COMMENT '八班计划结束时间',

    -- 通用字段
    create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '删除标志',

    PRIMARY KEY (id),
    INDEX idx_main_id (main_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='APS排程明细表';
