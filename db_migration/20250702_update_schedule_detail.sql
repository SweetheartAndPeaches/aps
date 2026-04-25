-- =============================================
-- APS排程明细表结构更新
-- 日期: 2025-07-02
-- 说明: CxScheduleDetail实体类重构后数据库同步
-- 适用: MySQL 8.0+
-- 执行方式: 分步骤执行，每步独立执行
-- =============================================

-- =============================================
-- 步骤1: 删除已从子表移除的冗余字段（基础信息）
-- =============================================
ALTER TABLE t_cx_schedule_detail DROP COLUMN cx_batch_no;
ALTER TABLE t_cx_schedule_detail DROP COLUMN order_no;
ALTER TABLE t_cx_schedule_detail DROP COLUMN production_status;
ALTER TABLE t_cx_schedule_detail DROP COLUMN is_release;
ALTER TABLE t_cx_schedule_detail DROP COLUMN schedule_date;
ALTER TABLE t_cx_schedule_detail DROP COLUMN cx_machine_code;
ALTER TABLE t_cx_schedule_detail DROP COLUMN cx_machine_name;
ALTER TABLE t_cx_schedule_detail DROP COLUMN cx_machine_type;

-- =============================================
-- 步骤2: 删除硫化相关字段
-- =============================================
ALTER TABLE t_cx_schedule_detail DROP COLUMN lh_schedule_ids;
ALTER TABLE t_cx_schedule_detail DROP COLUMN lh_machine_code;
ALTER TABLE t_cx_schedule_detail DROP COLUMN lh_machine_name;
ALTER TABLE t_cx_schedule_detail DROP COLUMN lh_machine_qty;

-- =============================================
-- 步骤3: 删除物料信息字段
-- =============================================
ALTER TABLE t_cx_schedule_detail DROP COLUMN material_code;
ALTER TABLE t_cx_schedule_detail DROP COLUMN material_desc;
ALTER TABLE t_cx_schedule_detail DROP COLUMN embryo_code;
ALTER TABLE t_cx_schedule_detail DROP COLUMN main_material_desc;
ALTER TABLE t_cx_schedule_detail DROP COLUMN spec_dimension;
ALTER TABLE t_cx_schedule_detail DROP COLUMN structure_name;
ALTER TABLE t_cx_schedule_detail DROP COLUMN total_stock;
ALTER TABLE t_cx_schedule_detail DROP COLUMN bom_data_version;
ALTER TABLE t_cx_schedule_detail DROP COLUMN product_num;

-- =============================================
-- 步骤4: 删除计划/实际时间字段
-- =============================================
ALTER TABLE t_cx_schedule_detail DROP COLUMN plan_hours;
ALTER TABLE t_cx_schedule_detail DROP COLUMN actual_hours;
ALTER TABLE t_cx_schedule_detail DROP COLUMN plan_cars;
ALTER TABLE t_cx_schedule_detail DROP COLUMN actual_cars;
ALTER TABLE t_cx_schedule_detail DROP COLUMN start_time;
ALTER TABLE t_cx_schedule_detail DROP COLUMN end_time;

-- =============================================
-- 步骤5: 删除标识字段
-- =============================================
ALTER TABLE t_cx_schedule_detail DROP COLUMN is_key_product;
ALTER TABLE t_cx_schedule_detail DROP COLUMN is_precision;
ALTER TABLE t_cx_schedule_detail DROP COLUMN is_shutdown;
ALTER TABLE t_cx_schedule_detail DROP COLUMN is_trial;
ALTER TABLE t_cx_schedule_detail DROP COLUMN is_ending;
ALTER TABLE t_cx_schedule_detail DROP COLUMN is_last_ending_batch;
ALTER TABLE t_cx_schedule_detail DROP COLUMN is_opening;
ALTER TABLE t_cx_schedule_detail DROP COLUMN is_closing;

-- =============================================
-- 步骤6: 删除库存/余量字段
-- =============================================
ALTER TABLE t_cx_schedule_detail DROP COLUMN reserved_stock;
ALTER TABLE t_cx_schedule_detail DROP COLUMN ending_extra_inventory;
ALTER TABLE t_cx_schedule_detail DROP COLUMN required_cars;
ALTER TABLE t_cx_schedule_detail DROP COLUMN cx_remain_qty;
ALTER TABLE t_cx_schedule_detail DROP COLUMN lh_remain_qty;
ALTER TABLE t_cx_schedule_detail DROP COLUMN stock_hours;

-- =============================================
-- 步骤7: 删除班次信息字段
-- =============================================
ALTER TABLE t_cx_schedule_detail DROP COLUMN shift_order;
ALTER TABLE t_cx_schedule_detail DROP COLUMN shift_code;
ALTER TABLE t_cx_schedule_detail DROP COLUMN shift_name;
ALTER TABLE t_cx_schedule_detail DROP COLUMN day_shift_order;
ALTER TABLE t_cx_schedule_detail DROP COLUMN class1_start_time;
ALTER TABLE t_cx_schedule_detail DROP COLUMN class1_end_time;

-- =============================================
-- 步骤8: 添加main_id关联字段
-- =============================================
ALTER TABLE t_cx_schedule_detail ADD COLUMN main_id BIGINT COMMENT '所属主表ID(T_CX_SCHEDULE_RESULT.id)';

-- =============================================
-- 步骤9: 添加一班字段
-- =============================================
ALTER TABLE t_cx_schedule_detail ADD COLUMN class1_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '一班计划数';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class1_trip_no VARCHAR(50) COMMENT '一班车次号';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class1_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '一班车次容量（整车条数）';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class1_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '一班库存可供硫化时长';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class1_sequence INT DEFAULT 0 COMMENT '一班顺位';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class1_plan_start_time DATETIME COMMENT '一班计划开始时间';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class1_plan_end_time DATETIME COMMENT '一班计划结束时间';

-- =============================================
-- 步骤10: 添加二班字段
-- =============================================
ALTER TABLE t_cx_schedule_detail ADD COLUMN class2_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '二班计划数';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class2_trip_no VARCHAR(50) COMMENT '二班车次号';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class2_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '二班车次容量（整车条数）';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class2_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '二班库存可供硫化时长';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class2_sequence INT DEFAULT 0 COMMENT '二班顺位';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class2_plan_start_time DATETIME COMMENT '二班计划开始时间';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class2_plan_end_time DATETIME COMMENT '二班计划结束时间';

-- =============================================
-- 步骤11: 添加三班字段
-- =============================================
ALTER TABLE t_cx_schedule_detail ADD COLUMN class3_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '三班计划数';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class3_trip_no VARCHAR(50) COMMENT '三班车次号';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class3_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '三班车次容量（整车条数）';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class3_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '三班库存可供硫化时长';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class3_sequence INT DEFAULT 0 COMMENT '三班顺位';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class3_plan_start_time DATETIME COMMENT '三班计划开始时间';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class3_plan_end_time DATETIME COMMENT '三班计划结束时间';

-- =============================================
-- 步骤12: 添加四班字段
-- =============================================
ALTER TABLE t_cx_schedule_detail ADD COLUMN class4_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '四班计划数';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class4_trip_no VARCHAR(50) COMMENT '四班车次号';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class4_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '四班车次容量（整车条数）';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class4_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '四班库存可供硫化时长';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class4_sequence INT DEFAULT 0 COMMENT '四班顺位';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class4_plan_start_time DATETIME COMMENT '四班计划开始时间';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class4_plan_end_time DATETIME COMMENT '四班计划结束时间';

-- =============================================
-- 步骤13: 添加五班字段
-- =============================================
ALTER TABLE t_cx_schedule_detail ADD COLUMN class5_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '五班计划数';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class5_trip_no VARCHAR(50) COMMENT '五班车次号';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class5_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '五班车次容量（整车条数）';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class5_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '五班库存可供硫化时长';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class5_sequence INT DEFAULT 0 COMMENT '五班顺位';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class5_plan_start_time DATETIME COMMENT '五班计划开始时间';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class5_plan_end_time DATETIME COMMENT '五班计划结束时间';

-- =============================================
-- 步骤14: 添加六班字段
-- =============================================
ALTER TABLE t_cx_schedule_detail ADD COLUMN class6_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '六班计划数';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class6_trip_no VARCHAR(50) COMMENT '六班车次号';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class6_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '六班车次容量（整车条数）';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class6_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '六班库存可供硫化时长';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class6_sequence INT DEFAULT 0 COMMENT '六班顺位';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class6_plan_start_time DATETIME COMMENT '六班计划开始时间';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class6_plan_end_time DATETIME COMMENT '六班计划结束时间';

-- =============================================
-- 步骤15: 添加七班字段
-- =============================================
ALTER TABLE t_cx_schedule_detail ADD COLUMN class7_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '七班计划数';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class7_trip_no VARCHAR(50) COMMENT '七班车次号';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class7_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '七班车次容量（整车条数）';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class7_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '七班库存可供硫化时长';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class7_sequence INT DEFAULT 0 COMMENT '七班顺位';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class7_plan_start_time DATETIME COMMENT '七班计划开始时间';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class7_plan_end_time DATETIME COMMENT '七班计划结束时间';

-- =============================================
-- 步骤16: 添加八班字段
-- =============================================
ALTER TABLE t_cx_schedule_detail ADD COLUMN class8_plan_qty DECIMAL(10,2) DEFAULT 0 COMMENT '八班计划数';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class8_trip_no VARCHAR(50) COMMENT '八班车次号';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class8_trip_capacity DECIMAL(10,2) DEFAULT 0 COMMENT '八班车次容量（整车条数）';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class8_stock_hours DECIMAL(10,2) DEFAULT 0 COMMENT '八班库存可供硫化时长';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class8_sequence INT DEFAULT 0 COMMENT '八班顺位';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class8_plan_start_time DATETIME COMMENT '八班计划开始时间';
ALTER TABLE t_cx_schedule_detail ADD COLUMN class8_plan_end_time DATETIME COMMENT '八班计划结束时间';

-- =============================================
-- 步骤17: 添加索引
-- =============================================
ALTER TABLE t_cx_schedule_detail ADD INDEX idx_main_id (main_id);

-- =============================================
-- 验证: 查看最终表结构
-- =============================================
-- DESCRIBE t_cx_schedule_detail;
