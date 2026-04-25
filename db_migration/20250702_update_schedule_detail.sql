-- =============================================
-- APS排程明细表结构更新
-- 日期: 2025-07-02
-- 说明: CxScheduleDetail实体类重构后数据库同步
-- 适用: MySQL 8.0+
-- =============================================

-- =============================================
-- 步骤1: 删除已从子表移除的冗余字段
-- 注意: 如果某些字段不存在会报错，请注释掉对应行重试
-- =============================================

-- 基础信息字段（由主表提供）
ALTER TABLE t_cx_schedule_detail
    DROP COLUMN cx_batch_no,
    DROP COLUMN order_no,
    DROP COLUMN production_status,
    DROP COLUMN is_release,
    DROP COLUMN schedule_date,
    DROP COLUMN cx_machine_code,
    DROP COLUMN cx_machine_name,
    DROP COLUMN cx_machine_type;

-- 硫化相关字段（由主表提供）
ALTER TABLE t_cx_schedule_detail
    DROP COLUMN lh_schedule_ids,
    DROP COLUMN lh_machine_code,
    DROP COLUMN lh_machine_name,
    DROP COLUMN lh_machine_qty;

-- 物料信息字段（由主表提供）
ALTER TABLE t_cx_schedule_detail
    DROP COLUMN material_code,
    DROP COLUMN material_desc,
    DROP COLUMN embryo_code,
    DROP COLUMN main_material_desc,
    DROP COLUMN spec_dimension,
    DROP COLUMN structure_name,
    DROP COLUMN total_stock,
    DROP COLUMN bom_data_version,
    DROP COLUMN product_num;

-- 计划/实际时间字段
ALTER TABLE t_cx_schedule_detail
    DROP COLUMN plan_hours,
    DROP COLUMN actual_hours,
    DROP COLUMN plan_cars,
    DROP COLUMN actual_cars,
    DROP COLUMN start_time,
    DROP COLUMN end_time;

-- 标识字段
ALTER TABLE t_cx_schedule_detail
    DROP COLUMN is_key_product,
    DROP COLUMN is_precision,
    DROP COLUMN is_shutdown,
    DROP COLUMN is_trial,
    DROP COLUMN is_ending,
    DROP COLUMN is_last_ending_batch,
    DROP COLUMN is_opening,
    DROP COLUMN is_closing;

-- 库存/余量字段
ALTER TABLE t_cx_schedule_detail
    DROP COLUMN reserved_stock,
    DROP COLUMN ending_extra_inventory,
    DROP COLUMN required_cars,
    DROP COLUMN cx_remain_qty,
    DROP COLUMN lh_remain_qty,
    DROP COLUMN stock_hours;

-- 班次信息字段
ALTER TABLE t_cx_schedule_detail
    DROP COLUMN shift_order,
    DROP COLUMN shift_code,
    DROP COLUMN shift_name,
    DROP COLUMN day_shift_order,
    DROP COLUMN class1_start_time,
    DROP COLUMN class1_end_time;

-- =============================================
-- 步骤2: 添加子表核心字段
-- =============================================

-- main_id 关联字段
ALTER TABLE t_cx_schedule_detail
    ADD COLUMN main_id BIGINT COMMENT '所属主表ID(T_CX_SCHEDULE_RESULT.id)';

-- 一班
ALTER TABLE t_cx_schedule_detail
    ADD COLUMN class1_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '一班计划数',
    ADD COLUMN class1_trip_no VARCHAR(50) COMMENT '一班车次号',
    ADD COLUMN class1_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '一班车次容量（整车条数）',
    ADD COLUMN class1_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '一班库存可供硫化时长',
    ADD COLUMN class1_sequence INT DEFAULT 0 COMMENT '一班顺位',
    ADD COLUMN class1_plan_start_time DATETIME COMMENT '一班计划开始时间',
    ADD COLUMN class1_plan_end_time DATETIME COMMENT '一班计划结束时间';

-- 二班
ALTER TABLE t_cx_schedule_detail
    ADD COLUMN class2_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '二班计划数',
    ADD COLUMN class2_trip_no VARCHAR(50) COMMENT '二班车次号',
    ADD COLUMN class2_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '二班车次容量（整车条数）',
    ADD COLUMN class2_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '二班库存可供硫化时长',
    ADD COLUMN class2_sequence INT DEFAULT 0 COMMENT '二班顺位',
    ADD COLUMN class2_plan_start_time DATETIME COMMENT '二班计划开始时间',
    ADD COLUMN class2_plan_end_time DATETIME COMMENT '二班计划结束时间';

-- 三班
ALTER TABLE t_cx_schedule_detail
    ADD COLUMN class3_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '三班计划数',
    ADD COLUMN class3_trip_no VARCHAR(50) COMMENT '三班车次号',
    ADD COLUMN class3_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '三班车次容量（整车条数）',
    ADD COLUMN class3_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '三班库存可供硫化时长',
    ADD COLUMN class3_sequence INT DEFAULT 0 COMMENT '三班顺位',
    ADD COLUMN class3_plan_start_time DATETIME COMMENT '三班计划开始时间',
    ADD COLUMN class3_plan_end_time DATETIME COMMENT '三班计划结束时间';

-- 四班
ALTER TABLE t_cx_schedule_detail
    ADD COLUMN class4_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '四班计划数',
    ADD COLUMN class4_trip_no VARCHAR(50) COMMENT '四班车次号',
    ADD COLUMN class4_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '四班车次容量（整车条数）',
    ADD COLUMN class4_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '四班库存可供硫化时长',
    ADD COLUMN class4_sequence INT DEFAULT 0 COMMENT '四班顺位',
    ADD COLUMN class4_plan_start_time DATETIME COMMENT '四班计划开始时间',
    ADD COLUMN class4_plan_end_time DATETIME COMMENT '四班计划结束时间';

-- 五班
ALTER TABLE t_cx_schedule_detail
    ADD COLUMN class5_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '五班计划数',
    ADD COLUMN class5_trip_no VARCHAR(50) COMMENT '五班车次号',
    ADD COLUMN class5_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '五班车次容量（整车条数）',
    ADD COLUMN class5_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '五班库存可供硫化时长',
    ADD COLUMN class5_sequence INT DEFAULT 0 COMMENT '五班顺位',
    ADD COLUMN class5_plan_start_time DATETIME COMMENT '五班计划开始时间',
    ADD COLUMN class5_plan_end_time DATETIME COMMENT '五班计划结束时间';

-- 六班
ALTER TABLE t_cx_schedule_detail
    ADD COLUMN class6_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '六班计划数',
    ADD COLUMN class6_trip_no VARCHAR(50) COMMENT '六班车次号',
    ADD COLUMN class6_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '六班车次容量（整车条数）',
    ADD COLUMN class6_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '六班库存可供硫化时长',
    ADD COLUMN class6_sequence INT DEFAULT 0 COMMENT '六班顺位',
    ADD COLUMN class6_plan_start_time DATETIME COMMENT '六班计划开始时间',
    ADD COLUMN class6_plan_end_time DATETIME COMMENT '六班计划结束时间';

-- 七班
ALTER TABLE t_cx_schedule_detail
    ADD COLUMN class7_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '七班计划数',
    ADD COLUMN class7_trip_no VARCHAR(50) COMMENT '七班车次号',
    ADD COLUMN class7_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '七班车次容量（整车条数）',
    ADD COLUMN class7_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '七班库存可供硫化时长',
    ADD COLUMN class7_sequence INT DEFAULT 0 COMMENT '七班顺位',
    ADD COLUMN class7_plan_start_time DATETIME COMMENT '七班计划开始时间',
    ADD COLUMN class7_plan_end_time DATETIME COMMENT '七班计划结束时间';

-- 八班
ALTER TABLE t_cx_schedule_detail
    ADD COLUMN class8_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '八班计划数',
    ADD COLUMN class8_trip_no VARCHAR(50) COMMENT '八班车次号',
    ADD COLUMN class8_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '八班车次容量（整车条数）',
    ADD COLUMN class8_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '八班库存可供硫化时长',
    ADD COLUMN class8_sequence INT DEFAULT 0 COMMENT '八班顺位',
    ADD COLUMN class8_plan_start_time DATETIME COMMENT '八班计划开始时间',
    ADD COLUMN class8_plan_end_time DATETIME COMMENT '八班计划结束时间';

-- =============================================
-- 步骤3: 添加索引
-- =============================================
ALTER TABLE t_cx_schedule_detail
    ADD INDEX idx_main_id (main_id);

-- =============================================
-- 验证: 查看表结构
-- =============================================
-- DESCRIBE t_cx_schedule_detail;
