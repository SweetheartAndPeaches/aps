package com.jinyu.aps.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库初始化器 - MySQL版本
 * 在应用启动时创建表和初始化数据
 *
 * @author APS Team
 */
@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("========================================");
        System.out.println("  开始初始化MySQL数据库...");
        System.out.println("========================================");

        // 班次配置表
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_shift_config");
        jdbcTemplate.execute("CREATE TABLE t_cx_shift_config (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "shift_code VARCHAR(20) NOT NULL UNIQUE, " +
                "shift_name VARCHAR(50) NOT NULL, " +
                "start_time TIME NOT NULL, " +
                "end_time TIME NOT NULL, " +
                "sort_order INT DEFAULT 0, " +
                "is_active SMALLINT DEFAULT 1, " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        // 预警配置表
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_alert_config");
        jdbcTemplate.execute("CREATE TABLE t_cx_alert_config (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "config_code VARCHAR(50) NOT NULL UNIQUE, " +
                "config_name VARCHAR(100) NOT NULL, " +
                "config_value VARCHAR(500) NOT NULL, " +
                "config_type VARCHAR(20) DEFAULT 'NUMBER', " +
                "config_unit VARCHAR(20), " +
                "description VARCHAR(500), " +
                "is_active SMALLINT DEFAULT 1, " +
                "effective_date DATE, " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "create_by VARCHAR(50), " +
                "update_by VARCHAR(50))");

        // 成型机台表
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_machine");
        jdbcTemplate.execute("CREATE TABLE t_cx_machine (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "machine_code VARCHAR(50) NOT NULL UNIQUE, " +
                "machine_name VARCHAR(100), " +
                "machine_type VARCHAR(50), " +
                "wrapping_type VARCHAR(50), " +
                "has_zero_degree_feeder SMALLINT DEFAULT 0, " +
                "structure VARCHAR(50), " +
                "max_capacity_per_hour DECIMAL(10,2), " +
                "max_daily_capacity INT, " +
                "max_curing_machines INT, " +
                "fixed_structure1 VARCHAR(100), " +
                "fixed_structure2 VARCHAR(100), " +
                "fixed_structure3 VARCHAR(100), " +
                "restricted_structures TEXT, " +
                "production_restriction VARCHAR(500), " +
                "line_number INT, " +
                "status VARCHAR(20) DEFAULT 'RUNNING', " +
                "is_active SMALLINT DEFAULT 1, " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");

        // 物料表
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_material");
        jdbcTemplate.execute("CREATE TABLE t_cx_material (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "material_code VARCHAR(50) NOT NULL UNIQUE, " +
                "material_name VARCHAR(200), " +
                "specification VARCHAR(100), " +
                "product_structure VARCHAR(100), " +
                "main_pattern VARCHAR(100), " +
                "pattern VARCHAR(100), " +
                "category VARCHAR(50), " +
                "unit VARCHAR(20) DEFAULT '条', " +
                "vulcanize_time_minutes DECIMAL(10,2), " +
                "is_main_product SMALLINT DEFAULT 0, " +
                "is_active SMALLINT DEFAULT 1, " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        // 库存表
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_stock");
        jdbcTemplate.execute("CREATE TABLE t_cx_stock (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "material_code VARCHAR(50) NOT NULL, " +
                "current_stock INT NOT NULL DEFAULT 0, " +
                "planned_in_qty INT DEFAULT 0, " +
                "planned_out_qty INT DEFAULT 0, " +
                "available_stock INT DEFAULT 0, " +
                "vulcanize_machine_count INT DEFAULT 0, " +
                "vulcanize_mold_count INT DEFAULT 0, " +
                "stock_hours DECIMAL(10,2), " +
                "stock_hours_formula VARCHAR(500), " +
                "shift_end_available_hours DECIMAL(10,2), " +
                "alert_status VARCHAR(20) DEFAULT 'NORMAL', " +
                "alert_time TIMESTAMP, " +
                "is_ending_sku SMALLINT DEFAULT 0, " +
                "ending_date DATE, " +
                "calc_time TIMESTAMP, " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");

        // 排程主表
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_schedule_main");
        jdbcTemplate.execute("CREATE TABLE t_cx_schedule_main (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "schedule_code VARCHAR(50) NOT NULL UNIQUE, " +
                "schedule_date DATE NOT NULL, " +
                "schedule_type VARCHAR(20) DEFAULT 'NORMAL', " +
                "status VARCHAR(20) DEFAULT 'DRAFT', " +
                "total_machines INT DEFAULT 0, " +
                "total_quantity INT DEFAULT 0, " +
                "total_vehicles INT DEFAULT 0, " +
                "version INT DEFAULT 1, " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "create_by VARCHAR(50), " +
                "update_by VARCHAR(50), " +
                "confirm_time TIMESTAMP, " +
                "confirm_by VARCHAR(50), " +
                "remark VARCHAR(500))");

        // 排程明细表
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_schedule_detail");
        jdbcTemplate.execute("CREATE TABLE t_cx_schedule_detail (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "main_id BIGINT NOT NULL, " +
                "schedule_date DATE NOT NULL, " +
                "shift_code VARCHAR(20) NOT NULL, " +
                "machine_code VARCHAR(50) NOT NULL, " +
                "material_code VARCHAR(50) NOT NULL, " +
                "plan_quantity INT NOT NULL, " +
                "completed_quantity INT DEFAULT 0, " +
                "material_group_id VARCHAR(100), " +
                "trip_no INT, " +
                "trip_sequence INT, " +
                "trip_group_id VARCHAR(100), " +
                "trip_capacity INT DEFAULT 12, " +
                "trip_actual_qty INT DEFAULT 0, " +
                "trip_status VARCHAR(20) DEFAULT 'PENDING', " +
                "trip_create_time TIMESTAMP, " +
                "trip_complete_time TIMESTAMP, " +
                "sequence INT, " +
                "sequence_in_group INT, " +
                "stock_hours_at_calc DECIMAL(10,2), " +
                "production_mode VARCHAR(20), " +
                "is_ending SMALLINT DEFAULT 0, " +
                "is_starting SMALLINT DEFAULT 0, " +
                "is_trial SMALLINT DEFAULT 0, " +
                "is_precision SMALLINT DEFAULT 0, " +
                "is_continue SMALLINT DEFAULT 0, " +
                "status VARCHAR(20) DEFAULT 'PLANNED', " +
                "plan_start_time TIMESTAMP, " +
                "plan_end_time TIMESTAMP, " +
                "actual_start_time TIMESTAMP, " +
                "actual_end_time TIMESTAMP, " +
                "priority INT DEFAULT 0, " +
                "remark VARCHAR(500))");

        // 初始化班次数据
        jdbcTemplate.execute("INSERT INTO t_cx_shift_config (shift_code, shift_name, start_time, end_time, sort_order) VALUES " +
                "('NIGHT', '夜班', '00:00:00', '07:59:59', 1), " +
                "('DAY', '早班', '08:00:00', '15:59:59', 2), " +
                "('AFTERNOON', '中班', '16:00:00', '23:59:59', 3)");

        // 初始化预警配置数据
        jdbcTemplate.execute("INSERT INTO t_cx_alert_config (config_code, config_name, config_value, config_type, config_unit, description) VALUES " +
                "('INVENTORY_HIGH_HOURS', '胎胚库存高水位预警时长', '18', 'NUMBER', '小时', '胎胚库存可供硫化时长大于此值时触发高库存预警'), " +
                "('INVENTORY_LOW_HOURS', '胎胚库存低水位预警时长', '4', 'NUMBER', '小时', '胎胚库存可供硫化时长小于此值时触发低库存预警'), " +
                "('MAX_SKU_PER_MACHINE_PER_DAY', '单台成型机每天最大胎胚种类数', '4', 'NUMBER', '种', '硬性约束：超过则必须调整'), " +
                "('MAX_STRUCTURE_SWITCH_PER_DAY', '每日结构切换最大次数', '2', 'NUMBER', '次', '硬性约束：超过则禁止切换'), " +
                "('TRIP_DEFAULT_CAPACITY', '默认胎面整车容量', '12', 'NUMBER', '条', '每车默认装载胎胚数量')");

        // 初始化示例机台数据
        jdbcTemplate.execute("INSERT INTO t_cx_machine (machine_code, machine_name, machine_type, line_number, max_daily_capacity, status) VALUES " +
                "('GM01', '成型机01', '软控三鼓', 1, 120, 'RUNNING'), " +
                "('GM02', '成型机02', '软控三鼓', 1, 120, 'RUNNING'), " +
                "('GM03', '成型机03', '软控三鼓', 2, 120, 'RUNNING'), " +
                "('GM04', '成型机04', '赛象三鼓', 2, 120, 'RUNNING'), " +
                "('GM05', '成型机05', '赛象三鼓', 3, 120, 'RUNNING')");

        // 初始化示例物料数据
        jdbcTemplate.execute("INSERT INTO t_cx_material (material_code, material_name, product_structure, main_pattern, is_main_product) VALUES " +
                "('MAT001', '12R22.5-18PR-JA511', '12R22.5', 'JA511', 1), " +
                "('MAT002', '11R22.5-16PR-JA511', '11R22.5', 'JA511', 1), " +
                "('MAT003', '295/80R22.5-18PR-JA511', '295/80R22.5', 'JA511', 0), " +
                "('MAT004', '275/80R22.5-16PR-JA511', '275/80R22.5', 'JA511', 0)");

        // 初始化示例库存数据
        jdbcTemplate.execute("INSERT INTO t_cx_stock (material_code, current_stock, vulcanize_machine_count, vulcanize_mold_count, alert_status) VALUES " +
                "('MAT001', 500, 4, 16, 'NORMAL'), " +
                "('MAT002', 350, 3, 12, 'NORMAL'), " +
                "('MAT003', 200, 2, 8, 'LOW'), " +
                "('MAT004', 800, 3, 12, 'HIGH')");

        System.out.println("========================================");
        System.out.println("  MySQL数据库初始化完成!");
        System.out.println("========================================");
        System.out.println("  APS成型排程系统启动成功!");
        System.out.println("  Swagger文档地址: http://localhost:5000/api/swagger-ui/index.html");
        System.out.println("========================================");
    }
}
