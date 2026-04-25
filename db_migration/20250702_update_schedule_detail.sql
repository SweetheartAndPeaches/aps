-- =============================================
-- APS排程明细表结构更新
-- 日期: 2025-07-02
-- 说明: CxScheduleDetail实体类重构后数据库同步
-- =============================================

-- =============================================
-- 步骤1: 删除已从子表移除的冗余字段（这些字段由主表T_CX_SCHEDULE_RESULT提供）
-- =============================================
ALTER TABLE t_cx_schedule_detail
    DROP COLUMN IF EXISTS cx_batch_no,
    DROP COLUMN IF EXISTS order_no,
    DROP COLUMN IF EXISTS production_status,
    DROP COLUMN IF EXISTS is_release,
    DROP COLUMN IF EXISTS schedule_date,
    DROP COLUMN IF EXISTS cx_machine_code,
    DROP COLUMN IF EXISTS cx_machine_name,
    DROP COLUMN IF EXISTS cx_machine_type,
    DROP COLUMN IF EXISTS lh_schedule_ids,
    DROP COLUMN IF EXISTS lh_machine_code,
    DROP COLUMN IF EXISTS lh_machine_name,
    DROP COLUMN IF EXISTS lh_machine_qty,
    DROP COLUMN IF EXISTS material_code,
    DROP COLUMN IF EXISTS material_desc,
    DROP COLUMN IF EXISTS embryo_code,
    DROP COLUMN IF EXISTS main_material_desc,
    DROP COLUMN IF EXISTS spec_dimension,
    DROP COLUMN IF EXISTS structure_name,
    DROP COLUMN IF EXISTS total_stock,
    DROP COLUMN IF EXISTS bom_data_version,
    DROP COLUMN IF EXISTS product_num,
    DROP COLUMN IF EXISTS plan_hours,
    DROP COLUMN IF EXISTS actual_hours,
    DROP COLUMN IF EXISTS plan_cars,
    DROP COLUMN IF EXISTS actual_cars,
    DROP COLUMN IF EXISTS start_time,
    DROP COLUMN IF EXISTS end_time,
    DROP COLUMN IF EXISTS is_key_product,
    DROP COLUMN IF EXISTS is_precision,
    DROP COLUMN IF EXISTS is_shutdown,
    DROP COLUMN IF EXISTS is_trial,
    DROP COLUMN IF EXISTS is_ending,
    DROP COLUMN IF EXISTS is_last_ending_batch,
    DROP COLUMN IF EXISTS is_opening,
    DROP COLUMN IF EXISTS is_closing,
    DROP COLUMN IF EXISTS reserved_stock,
    DROP COLUMN IF EXISTS ending_extra_inventory,
    DROP COLUMN IF EXISTS required_cars,
    DROP COLUMN IF EXISTS cx_remain_qty,
    DROP COLUMN IF EXISTS lh_remain_qty,
    DROP COLUMN IF EXISTS stock_hours,
    DROP COLUMN IF EXISTS shift_order,
    DROP COLUMN IF EXISTS shift_code,
    DROP COLUMN IF EXISTS shift_name,
    DROP COLUMN IF EXISTS day_shift_order,
    DROP COLUMN IF EXISTS class1_start_time,
    DROP COLUMN IF EXISTS class1_end_time;

-- =============================================
-- 步骤2: 添加子表新字段（如果还不存在）
-- 子表核心维度: main_id + CLASS1~CLASS8 (每个班次6个字段 + 2个时间字段)
-- =============================================

-- main_id 关联字段
ALTER TABLE t_cx_schedule_detail
    ADD COLUMN IF NOT EXISTS main_id BIGINT COMMENT '所属主表ID(T_CX_SCHEDULE_RESULT.id)';

-- 一班
ALTER TABLE t_cx_schedule_detail
    ADD COLUMN IF NOT EXISTS class1_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '一班计划数',
    ADD COLUMN IF NOT EXISTS class1_trip_no VARCHAR(50) COMMENT '一班车次号',
    ADD COLUMN IF NOT EXISTS class1_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '一班车次容量（整车条数）',
    ADD COLUMN IF NOT EXISTS class1_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '一班库存可供硫化时长',
    ADD COLUMN IF NOT EXISTS class1_sequence INT DEFAULT 0 COMMENT '一班顺位',
    ADD COLUMN IF NOT EXISTS class1_plan_start_time DATETIME COMMENT '一班计划开始时间',
    ADD COLUMN IF NOT EXISTS class1_plan_end_time DATETIME COMMENT '一班计划结束时间';

-- 二班
ALTER TABLE t_cx_schedule_detail
    ADD COLUMN IF NOT EXISTS class2_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '二班计划数',
    ADD COLUMN IF NOT EXISTS class2_trip_no VARCHAR(50) COMMENT '二班车次号',
    ADD COLUMN IF NOT EXISTS class2_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '二班车次容量（整车条数）',
    ADD COLUMN IF NOT EXISTS class2_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '二班库存可供硫化时长',
    ADD COLUMN IF NOT EXISTS class2_sequence INT DEFAULT 0 COMMENT '二班顺位',
    ADD COLUMN IF NOT EXISTS class2_plan_start_time DATETIME COMMENT '二班计划开始时间',
    ADD COLUMN IF NOT EXISTS class2_plan_end_time DATETIME COMMENT '二班计划结束时间';

-- 三班
ALTER TABLE t_cx_schedule_detail
    ADD COLUMN IF NOT EXISTS class3_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '三班计划数',
    ADD COLUMN IF NOT EXISTS class3_trip_no VARCHAR(50) COMMENT '三班车次号',
    ADD COLUMN IF NOT EXISTS class3_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '三班车次容量（整车条数）',
    ADD COLUMN IF NOT EXISTS class3_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '三班库存可供硫化时长',
    ADD COLUMN IF NOT EXISTS class3_sequence INT DEFAULT 0 COMMENT '三班顺位',
    ADD COLUMN IF NOT EXISTS class3_plan_start_time DATETIME COMMENT '三班计划开始时间',
    ADD COLUMN IF NOT EXISTS class3_plan_end_time DATETIME COMMENT '三班计划结束时间';

-- 四班
ALTER TABLE t_cx_schedule_detail
    ADD COLUMN IF NOT EXISTS class4_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '四班计划数',
    ADD COLUMN IF NOT EXISTS class4_trip_no VARCHAR(50) COMMENT '四班车次号',
    ADD COLUMN IF NOT EXISTS class4_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '四班车次容量（整车条数）',
    ADD COLUMN IF NOT EXISTS class4_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '四班库存可供硫化时长',
    ADD COLUMN IF NOT EXISTS class4_sequence INT DEFAULT 0 COMMENT '四班顺位',
    ADD COLUMN IF NOT EXISTS class4_plan_start_time DATETIME COMMENT '四班计划开始时间',
    ADD COLUMN IF NOT EXISTS class4_plan_end_time DATETIME COMMENT '四班计划结束时间';

-- 五班
ALTER TABLE t_cx_schedule_detail
    ADD COLUMN IF NOT EXISTS class5_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '五班计划数',
    ADD COLUMN IF NOT EXISTS class5_trip_no VARCHAR(50) COMMENT '五班车次号',
    ADD COLUMN IF NOT EXISTS class5_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '五班车次容量（整车条数）',
    ADD COLUMN IF NOT EXISTS class5_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '五班库存可供硫化时长',
    ADD COLUMN IF NOT EXISTS class5_sequence INT DEFAULT 0 COMMENT '五班顺位',
    ADD COLUMN IF NOT EXISTS class5_plan_start_time DATETIME COMMENT '五班计划开始时间',
    ADD COLUMN IF NOT EXISTS class5_plan_end_time DATETIME COMMENT '五班计划结束时间';

-- 六班
ALTER TABLE t_cx_schedule_detail
    ADD COLUMN IF NOT EXISTS class6_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '六班计划数',
    ADD COLUMN IF NOT EXISTS class6_trip_no VARCHAR(50) COMMENT '六班车次号',
    ADD COLUMN IF NOT EXISTS class6_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '六班车次容量（整车条数）',
    ADD COLUMN IF NOT EXISTS class6_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '六班库存可供硫化时长',
    ADD COLUMN IF NOT EXISTS class6_sequence INT DEFAULT 0 COMMENT '六班顺位',
    ADD COLUMN IF NOT EXISTS class6_plan_start_time DATETIME COMMENT '六班计划开始时间',
    ADD COLUMN IF NOT EXISTS class6_plan_end_time DATETIME COMMENT '六班计划结束时间';

-- 七班
ALTER TABLE t_cx_schedule_detail
    ADD COLUMN IF NOT EXISTS class7_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '七班计划数',
    ADD COLUMN IF NOT EXISTS class7_trip_no VARCHAR(50) COMMENT '七班车次号',
    ADD COLUMN IF NOT EXISTS class7_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '七班车次容量（整车条数）',
    ADD COLUMN IF NOT EXISTS class7_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '七班库存可供硫化时长',
    ADD COLUMN IF NOT EXISTS class7_sequence INT DEFAULT 0 COMMENT '七班顺位',
    ADD COLUMN IF NOT EXISTS class7_plan_start_time DATETIME COMMENT '七班计划开始时间',
    ADD COLUMN IF NOT EXISTS class7_plan_end_time DATETIME COMMENT '七班计划结束时间';

-- 八班
ALTER TABLE t_cx_schedule_detail
    ADD COLUMN IF NOT EXISTS class8_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '八班计划数',
    ADD COLUMN IF NOT EXISTS class8_trip_no VARCHAR(50) COMMENT '八班车次号',
    ADD COLUMN IF NOT EXISTS class8_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '八班车次容量（整车条数）',
    ADD COLUMN IF NOT EXISTS class8_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '八班库存可供硫化时长',
    ADD COLUMN IF NOT EXISTS class8_sequence INT DEFAULT 0 COMMENT '八班顺位',
    ADD COLUMN IF NOT EXISTS class8_plan_start_time DATETIME COMMENT '八班计划开始时间',
    ADD COLUMN IF NOT EXISTS class8_plan_end_time DATETIME COMMENT '八班计划结束时间';

-- =============================================
-- 步骤3: 添加索引
-- =============================================
ALTER TABLE t_cx_schedule_detail
    ADD INDEX IF NOT EXISTS idx_main_id (main_id);

-- =============================================
-- 验证: 查看表结构
-- =============================================
-- DESCRIBE t_cx_schedule_detail;
