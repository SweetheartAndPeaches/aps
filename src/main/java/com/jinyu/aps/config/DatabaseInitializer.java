package com.jinyu.aps.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库初始化器 - MySQL版本
 * 在应用启动时创建表和初始化数据
 * 
 * 按照技术文档V5.0.0定义的完整表结构
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

        // ==================== 一、基础配置表 ====================
        createShiftConfigTable();
        createAlertConfigTable();

        // ==================== 二、核心业务表 ====================
        createMachineTable();
        createMaterialTable();
        createStockTable();
        createScheduleMainTable();
        createScheduleDetailTable();

        // ==================== 三、计划与任务表 ====================
        createVulcanizingPlanTable();
        createDailyEmbryoTaskTable();

        // ==================== 四、计算辅助表 ====================
        createTrialAllocationLogTable();
        createScheduleIntermediateTable();
        createShiftBalanceAdjustTable();
        createConstraintCheckRecordTable();

        // ==================== 五、特殊场景表 ====================
        createPrecisionPlanTable();
        createTrialPlanTable();
        createMachineStatusLogTable();
        createStructureSwitchTable();

        // ==================== 六、操作与日志表 ====================
        createDispatcherLogTable();
        createCompletionReportTable();
        createAlertRecordTable();

        // ==================== 初始化基础数据 ====================
        initShiftConfigData();
        initAlertConfigData();
        initMachineData();
        initMaterialData();
        initStockData();
        initVulcanizingPlanData();

        System.out.println("========================================");
        System.out.println("  MySQL数据库初始化完成!");
        System.out.println("========================================");
        System.out.println("  APS成型排程系统启动成功!");
        System.out.println("  Swagger文档地址: http://localhost:5000/api/swagger-ui/index.html");
        System.out.println("========================================");
    }

    // ==================== 一、基础配置表 ====================

    /**
     * 创建班次配置表
     */
    private void createShiftConfigTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_shift_config");
        jdbcTemplate.execute("CREATE TABLE t_cx_shift_config (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "shift_code VARCHAR(20) NOT NULL UNIQUE COMMENT '班次编码', " +
                "shift_name VARCHAR(50) NOT NULL COMMENT '班次名称', " +
                "start_time TIME NOT NULL COMMENT '开始时间', " +
                "end_time TIME NOT NULL COMMENT '结束时间', " +
                "sort_order INT DEFAULT 0 COMMENT '排序', " +
                "is_active SMALLINT DEFAULT 1 COMMENT '是否启用', " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
                ") COMMENT='班次配置表'");
    }

    /**
     * 创建预警配置表
     */
    private void createAlertConfigTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_alert_config");
        jdbcTemplate.execute("CREATE TABLE t_cx_alert_config (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "config_code VARCHAR(50) NOT NULL UNIQUE COMMENT '配置编码', " +
                "config_name VARCHAR(100) NOT NULL COMMENT '配置名称', " +
                "config_value VARCHAR(500) NOT NULL COMMENT '配置值', " +
                "config_type VARCHAR(20) DEFAULT 'NUMBER' COMMENT '配置类型', " +
                "config_unit VARCHAR(20) COMMENT '配置单位', " +
                "description VARCHAR(500) COMMENT '配置说明', " +
                "is_active SMALLINT DEFAULT 1 COMMENT '是否启用', " +
                "effective_date DATE COMMENT '生效日期', " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间', " +
                "create_by VARCHAR(50) COMMENT '创建人', " +
                "update_by VARCHAR(50) COMMENT '更新人'" +
                ") COMMENT='预警配置表'");
    }

    // ==================== 二、核心业务表 ====================

    /**
     * 创建成型机台表
     */
    private void createMachineTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_machine");
        jdbcTemplate.execute("CREATE TABLE t_cx_machine (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "machine_code VARCHAR(50) NOT NULL UNIQUE COMMENT '机台编号', " +
                "machine_name VARCHAR(100) COMMENT '机台名称', " +
                "machine_type VARCHAR(50) COMMENT '机台类型', " +
                "wrapping_type VARCHAR(50) COMMENT '反包方式', " +
                "has_zero_degree_feeder SMALLINT DEFAULT 0 COMMENT '是否有零度供料架', " +
                "structure VARCHAR(50) COMMENT '在产结构', " +
                "max_capacity_per_hour DECIMAL(10,2) COMMENT '每小时最大产能', " +
                "max_daily_capacity INT COMMENT '设备最大日产能', " +
                "max_curing_machines INT COMMENT '对应硫化机上限', " +
                "fixed_structure1 VARCHAR(100) COMMENT '固定规格1', " +
                "fixed_structure2 VARCHAR(100) COMMENT '固定规格2', " +
                "fixed_structure3 VARCHAR(100) COMMENT '固定规格3', " +
                "restricted_structures TEXT COMMENT '不可作业结构', " +
                "production_restriction VARCHAR(500) COMMENT '排产限制说明', " +
                "line_number INT COMMENT '产线编号', " +
                "status VARCHAR(20) DEFAULT 'RUNNING' COMMENT '状态', " +
                "is_active SMALLINT DEFAULT 1 COMMENT '是否启用', " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'" +
                ") COMMENT='成型机台表'");
    }

    /**
     * 创建物料主数据表
     */
    private void createMaterialTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_material");
        jdbcTemplate.execute("CREATE TABLE t_cx_material (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "material_code VARCHAR(50) NOT NULL UNIQUE COMMENT '胎胚物料编码', " +
                "material_name VARCHAR(200) COMMENT '物料名称', " +
                "specification VARCHAR(100) COMMENT '规格型号', " +
                "product_structure VARCHAR(100) COMMENT '产品结构', " +
                "main_pattern VARCHAR(100) COMMENT '主花纹', " +
                "pattern VARCHAR(100) COMMENT '花纹', " +
                "category VARCHAR(50) COMMENT '物料分类', " +
                "unit VARCHAR(20) DEFAULT '条' COMMENT '单位', " +
                "vulcanize_time_minutes DECIMAL(10,2) COMMENT '硫化时间(分钟)', " +
                "is_main_product SMALLINT DEFAULT 0 COMMENT '是否主销产品', " +
                "is_active SMALLINT DEFAULT 1 COMMENT '是否启用', " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
                ") COMMENT='物料主数据表'");
    }

    /**
     * 创建胎胚库存表
     */
    private void createStockTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_stock");
        jdbcTemplate.execute("CREATE TABLE t_cx_stock (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "material_code VARCHAR(50) NOT NULL COMMENT '胎胚物料编码', " +
                "current_stock INT NOT NULL DEFAULT 0 COMMENT '实时库存数量', " +
                "planned_in_qty INT DEFAULT 0 COMMENT '计划入库量', " +
                "planned_out_qty INT DEFAULT 0 COMMENT '计划出库量', " +
                "available_stock INT DEFAULT 0 COMMENT '可用库存', " +
                "vulcanize_machine_count INT DEFAULT 0 COMMENT '可用硫化机台数', " +
                "vulcanize_mold_count INT DEFAULT 0 COMMENT '总模数', " +
                "stock_hours DECIMAL(10,2) COMMENT '库存可供硫化时长(小时)', " +
                "stock_hours_formula VARCHAR(500) COMMENT '计算公式记录', " +
                "shift_end_available_hours DECIMAL(10,2) COMMENT '交班剩余可供硫化时长', " +
                "alert_status VARCHAR(20) DEFAULT 'NORMAL' COMMENT '预警状态', " +
                "alert_time TIMESTAMP COMMENT '预警触发时间', " +
                "is_ending_sku SMALLINT DEFAULT 0 COMMENT '是否收尾SKU', " +
                "ending_date DATE COMMENT '预计收尾日期', " +
                "calc_time TIMESTAMP COMMENT '计算时间', " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'" +
                ") COMMENT='胎胚库存表'");
    }

    /**
     * 创建排程主表
     */
    private void createScheduleMainTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_schedule_main");
        jdbcTemplate.execute("CREATE TABLE t_cx_schedule_main (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "schedule_code VARCHAR(50) NOT NULL UNIQUE COMMENT '排程单号', " +
                "schedule_date DATE NOT NULL COMMENT '计划日期', " +
                "schedule_type VARCHAR(20) DEFAULT 'NORMAL' COMMENT '排程类型', " +
                "status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态', " +
                "total_machines INT DEFAULT 0 COMMENT '参与排程机台数', " +
                "total_quantity INT DEFAULT 0 COMMENT '总计划量', " +
                "total_vehicles INT DEFAULT 0 COMMENT '总车次数', " +
                "version INT DEFAULT 1 COMMENT '版本号', " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间', " +
                "create_by VARCHAR(50) COMMENT '创建人', " +
                "update_by VARCHAR(50) COMMENT '更新人', " +
                "confirm_time TIMESTAMP COMMENT '确认时间', " +
                "confirm_by VARCHAR(50) COMMENT '确认人', " +
                "remark VARCHAR(500) COMMENT '备注'" +
                ") COMMENT='排程主表'");
    }

    /**
     * 创建排程明细表
     */
    private void createScheduleDetailTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_schedule_detail");
        jdbcTemplate.execute("CREATE TABLE t_cx_schedule_detail (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "main_id BIGINT NOT NULL COMMENT '所属主表ID', " +
                "schedule_date DATE NOT NULL COMMENT '计划日期', " +
                "shift_code VARCHAR(20) NOT NULL COMMENT '班次编码', " +
                "machine_code VARCHAR(50) NOT NULL COMMENT '成型机台编号', " +
                "material_code VARCHAR(50) NOT NULL COMMENT '胎胚物料编码', " +
                "plan_quantity INT NOT NULL COMMENT '计划量', " +
                "completed_quantity INT DEFAULT 0 COMMENT '完成量', " +
                "material_group_id VARCHAR(100) COMMENT '物料分组ID', " +
                "trip_no INT COMMENT '车次号', " +
                "trip_sequence INT COMMENT '车内序号', " +
                "trip_group_id VARCHAR(100) COMMENT '车次分组ID', " +
                "trip_capacity INT DEFAULT 12 COMMENT '本车次容量', " +
                "trip_actual_qty INT DEFAULT 0 COMMENT '本车次实际完成数量', " +
                "trip_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '车次状态', " +
                "trip_create_time TIMESTAMP COMMENT '车次创建时间', " +
                "trip_complete_time TIMESTAMP COMMENT '车次齐套时间', " +
                "sequence INT COMMENT '顺位', " +
                "sequence_in_group INT COMMENT '组内顺位', " +
                "stock_hours_at_calc DECIMAL(10,2) COMMENT '计算顺位时的库存可供硫化时长', " +
                "production_mode VARCHAR(20) COMMENT '生产方式', " +
                "is_ending SMALLINT DEFAULT 0 COMMENT '是否收尾', " +
                "is_starting SMALLINT DEFAULT 0 COMMENT '是否投产', " +
                "is_trial SMALLINT DEFAULT 0 COMMENT '是否试制', " +
                "is_precision SMALLINT DEFAULT 0 COMMENT '是否精度计划', " +
                "is_continue SMALLINT DEFAULT 0 COMMENT '是否续作', " +
                "status VARCHAR(20) DEFAULT 'PLANNED' COMMENT '状态', " +
                "plan_start_time TIMESTAMP COMMENT '计划开始时间', " +
                "plan_end_time TIMESTAMP COMMENT '计划结束时间', " +
                "actual_start_time TIMESTAMP COMMENT '实际开始时间', " +
                "actual_end_time TIMESTAMP COMMENT '实际结束时间', " +
                "priority INT DEFAULT 0 COMMENT '优先级', " +
                "remark VARCHAR(500) COMMENT '备注'" +
                ") COMMENT='排程明细表'");
    }

    // ==================== 三、计划与任务表 ====================

    /**
     * 创建硫化计划表
     */
    private void createVulcanizingPlanTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_vulcanizing_plan");
        jdbcTemplate.execute("CREATE TABLE t_cx_vulcanizing_plan (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "plan_code VARCHAR(50) NOT NULL UNIQUE COMMENT '计划单号', " +
                "plan_date DATE NOT NULL COMMENT '计划日期', " +
                "material_code VARCHAR(50) NOT NULL COMMENT '胎胚物料编码', " +
                "plan_quantity INT NOT NULL COMMENT '计划产量', " +
                "priority INT DEFAULT 0 COMMENT '优先级', " +
                "source VARCHAR(50) COMMENT '来源', " +
                "status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态', " +
                "assigned_quantity INT DEFAULT 0 COMMENT '已分配数量', " +
                "remainder_quantity INT DEFAULT 0 COMMENT '剩余未分配数量', " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间', " +
                "create_by VARCHAR(50) COMMENT '创建人'" +
                ") COMMENT='硫化计划表'");
    }

    /**
     * 创建日胎胚任务表
     */
    private void createDailyEmbryoTaskTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_daily_embryo_task");
        jdbcTemplate.execute("CREATE TABLE t_cx_daily_embryo_task (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "schedule_main_id BIGINT NOT NULL COMMENT '排程主表ID', " +
                "task_group_id VARCHAR(100) COMMENT '任务分组ID', " +
                "material_code VARCHAR(50) NOT NULL COMMENT '胎胚物料编码', " +
                "task_quantity INT NOT NULL COMMENT '任务量', " +
                "product_structure VARCHAR(100) COMMENT '产品结构', " +
                "is_main_product SMALLINT DEFAULT 0 COMMENT '是否主销产品', " +
                "priority INT DEFAULT 0 COMMENT '优先级', " +
                "sort_order INT COMMENT '排序', " +
                "assigned_quantity INT DEFAULT 0 COMMENT '已分配量', " +
                "remainder_quantity INT DEFAULT 0 COMMENT '剩余量', " +
                "is_fully_assigned SMALLINT DEFAULT 0 COMMENT '是否全部分配', " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
                ") COMMENT='日胎胚任务表'");
    }

    // ==================== 四、计算辅助表 ====================

    /**
     * 创建试错分配日志表
     */
    private void createTrialAllocationLogTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_trial_allocation_log");
        jdbcTemplate.execute("CREATE TABLE t_cx_trial_allocation_log (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "schedule_main_id BIGINT NOT NULL COMMENT '排程主表ID', " +
                "task_group_id VARCHAR(100) COMMENT '任务分组ID', " +
                "recursion_depth INT DEFAULT 0 COMMENT '递归深度', " +
                "task_index INT COMMENT '任务索引', " +
                "machine_code VARCHAR(50) COMMENT '机台编号', " +
                "material_code VARCHAR(50) COMMENT '分配的物料', " +
                "allocated_qty INT COMMENT '分配数量', " +
                "remainder_before INT COMMENT '分配前任务余量', " +
                "remainder_after INT COMMENT '分配后任务余量', " +
                "constraint_checks TEXT COMMENT '约束检查结果JSON', " +
                "is_feasible SMALLINT DEFAULT 1 COMMENT '是否可行', " +
                "is_best_solution SMALLINT DEFAULT 0 COMMENT '是否为最优方案', " +
                "load_diff INT COMMENT '当前方案负载差', " +
                "sku_diff INT COMMENT '当前方案种类差', " +
                "total_load INT COMMENT '当前方案总负载', " +
                "is_backtrack SMALLINT DEFAULT 0 COMMENT '是否回溯', " +
                "backtrack_reason VARCHAR(200) COMMENT '回溯原因', " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
                ") COMMENT='试错分配日志表'");
    }

    /**
     * 创建排程计算中间状态表
     */
    private void createScheduleIntermediateTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_schedule_intermediate");
        jdbcTemplate.execute("CREATE TABLE t_cx_schedule_intermediate (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "schedule_main_id BIGINT NOT NULL COMMENT '排程主表ID', " +
                "calc_stage VARCHAR(50) NOT NULL COMMENT '计算阶段', " +
                "input_data TEXT COMMENT '阶段输入数据JSON', " +
                "output_data TEXT COMMENT '阶段输出数据JSON', " +
                "stage_status VARCHAR(20) DEFAULT 'PROCESSING' COMMENT '阶段状态', " +
                "error_message VARCHAR(500) COMMENT '错误信息', " +
                "start_time TIMESTAMP COMMENT '阶段开始时间', " +
                "end_time TIMESTAMP COMMENT '阶段结束时间', " +
                "execution_time_ms INT COMMENT '执行耗时毫秒', " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
                ") COMMENT='排程计算中间状态表'");
    }

    /**
     * 创建班次均衡调整记录表
     */
    private void createShiftBalanceAdjustTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_shift_balance_adjust");
        jdbcTemplate.execute("CREATE TABLE t_cx_shift_balance_adjust (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "schedule_main_id BIGINT NOT NULL COMMENT '排程主表ID', " +
                "schedule_date DATE NOT NULL COMMENT '计划日期', " +
                "shift_code VARCHAR(20) NOT NULL COMMENT '班次编码', " +
                "qty_before INT COMMENT '调整前产量', " +
                "deviation_rate_before DECIMAL(5,2) COMMENT '调整前偏差率', " +
                "qty_after INT COMMENT '调整后产量', " +
                "deviation_rate_after DECIMAL(5,2) COMMENT '调整后偏差率', " +
                "adjust_qty INT COMMENT '调整数量', " +
                "adjust_type VARCHAR(20) COMMENT '调整类型', " +
                "adjust_mode VARCHAR(20) COMMENT '调整模式', " +
                "affected_materials TEXT COMMENT '受影响的物料列表JSON', " +
                "trigger_reason VARCHAR(200) COMMENT '触发原因', " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
                ") COMMENT='班次均衡调整记录表'");
    }

    /**
     * 创建约束检查记录表
     */
    private void createConstraintCheckRecordTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_constraint_check_record");
        jdbcTemplate.execute("CREATE TABLE t_cx_constraint_check_record (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "schedule_main_id BIGINT COMMENT '排程主表ID', " +
                "schedule_detail_id BIGINT COMMENT '排程明细ID', " +
                "rule_code VARCHAR(50) COMMENT '规则编码', " +
                "rule_name VARCHAR(100) COMMENT '规则名称', " +
                "constraint_type VARCHAR(50) COMMENT '约束类型', " +
                "check_result VARCHAR(20) COMMENT '检查结果', " +
                "actual_value VARCHAR(100) COMMENT '实际值', " +
                "limit_value VARCHAR(100) COMMENT '限制值', " +
                "error_message VARCHAR(500) COMMENT '错误信息', " +
                "suggestion VARCHAR(500) COMMENT '调整建议', " +
                "is_resolved SMALLINT DEFAULT 0 COMMENT '是否已解决', " +
                "resolve_time TIMESTAMP COMMENT '解决时间', " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
                ") COMMENT='约束检查记录表'");
    }

    // ==================== 五、特殊场景表 ====================

    /**
     * 创建精度计划表
     */
    private void createPrecisionPlanTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_precision_plan");
        jdbcTemplate.execute("CREATE TABLE t_cx_precision_plan (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "machine_code VARCHAR(50) NOT NULL COMMENT '机台编号', " +
                "schedule_date DATE NOT NULL COMMENT '精度计划日期', " +
                "shift_code VARCHAR(20) COMMENT '班次编码', " +
                "accuracy_duration INT COMMENT '精度时长分钟', " +
                "priority INT DEFAULT 1 COMMENT '优先级', " +
                "status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态', " +
                "curing_stop_required SMALLINT DEFAULT 0 COMMENT '是否需要硫化停机', " +
                "remark VARCHAR(500) COMMENT '备注', " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "create_by VARCHAR(50) COMMENT '创建人'" +
                ") COMMENT='精度计划表'");
    }

    /**
     * 创建试制计划表
     */
    private void createTrialPlanTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_trial_plan");
        jdbcTemplate.execute("CREATE TABLE t_cx_trial_plan (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "trial_code VARCHAR(50) NOT NULL UNIQUE COMMENT '试制单号', " +
                "material_code VARCHAR(50) NOT NULL COMMENT '试制胎胚物料编码', " +
                "trial_qty INT NOT NULL COMMENT '试制数量', " +
                "schedule_date DATE COMMENT '计划日期', " +
                "shift_code VARCHAR(20) COMMENT '班次编码', " +
                "machine_code VARCHAR(50) COMMENT '指定机台', " +
                "priority INT DEFAULT 999 COMMENT '优先级', " +
                "status VARCHAR(20) DEFAULT 'PLANNED' COMMENT '状态', " +
                "apply_date DATE COMMENT '申请日期', " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "create_by VARCHAR(50) COMMENT '创建人', " +
                "remark VARCHAR(500) COMMENT '备注'" +
                ") COMMENT='试制计划表'");
    }

    /**
     * 创建开产/停产记录表
     */
    private void createMachineStatusLogTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_machine_status_log");
        jdbcTemplate.execute("CREATE TABLE t_cx_machine_status_log (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "machine_code VARCHAR(50) NOT NULL COMMENT '机台编号', " +
                "operation_type VARCHAR(20) NOT NULL COMMENT '操作类型', " +
                "operation_date DATE NOT NULL COMMENT '操作日期', " +
                "shift_code VARCHAR(20) COMMENT '班次', " +
                "stop_reason VARCHAR(500) COMMENT '停产原因', " +
                "stop_mode VARCHAR(20) COMMENT '停产方式', " +
                "start_production_qty INT COMMENT '开产首班计划量', " +
                "reason VARCHAR(200) COMMENT '原因', " +
                "operator VARCHAR(50) COMMENT '操作人', " +
                "operation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间', " +
                "affect_schedule_id BIGINT COMMENT '影响的排程ID'" +
                ") COMMENT='开产/停产记录表'");
    }

    /**
     * 创建结构切换记录表
     */
    private void createStructureSwitchTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_structure_switch");
        jdbcTemplate.execute("CREATE TABLE t_cx_structure_switch (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "machine_code VARCHAR(50) NOT NULL COMMENT '机台编号', " +
                "switch_date DATE NOT NULL COMMENT '切换日期', " +
                "shift_code VARCHAR(20) COMMENT '班次编码', " +
                "old_structure VARCHAR(100) COMMENT '原结构', " +
                "new_structure VARCHAR(100) COMMENT '新结构', " +
                "switch_type VARCHAR(50) COMMENT '切换类型', " +
                "is_first_inspection SMALLINT DEFAULT 0 COMMENT '是否首检', " +
                "first_inspection_duration INT COMMENT '首检时长分钟', " +
                "switch_sequence INT COMMENT '当日切换顺序', " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "create_by VARCHAR(50) COMMENT '记录人'" +
                ") COMMENT='结构切换记录表'");
    }

    // ==================== 六、操作与日志表 ====================

    /**
     * 创建调度员操作日志表
     */
    private void createDispatcherLogTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_dispatcher_log");
        jdbcTemplate.execute("CREATE TABLE t_cx_dispatcher_log (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "operation_type VARCHAR(50) NOT NULL COMMENT '操作类型', " +
                "operation_desc VARCHAR(500) COMMENT '操作描述', " +
                "operator VARCHAR(50) NOT NULL COMMENT '操作人', " +
                "operation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间', " +
                "ip_address VARCHAR(50) COMMENT 'IP地址', " +
                "request_params TEXT COMMENT '请求参数JSON', " +
                "response_result TEXT COMMENT '响应结果JSON', " +
                "execution_time_ms INT COMMENT '执行耗时毫秒'" +
                ") COMMENT='调度员操作日志表'");
    }

    /**
     * 创建完成量回报表
     */
    private void createCompletionReportTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_completion_report");
        jdbcTemplate.execute("CREATE TABLE t_cx_completion_report (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "schedule_detail_id BIGINT NOT NULL COMMENT '排程明细ID', " +
                "report_date DATE NOT NULL COMMENT '回报日期', " +
                "shift_code VARCHAR(20) NOT NULL COMMENT '班次', " +
                "completed_qty INT NOT NULL COMMENT '完成数量', " +
                "complete_rate DECIMAL(5,2) COMMENT '完成率', " +
                "reporter VARCHAR(50) COMMENT '回报人', " +
                "report_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '回报时间', " +
                "is_confirmed SMALLINT DEFAULT 0 COMMENT '是否确认', " +
                "confirm_time TIMESTAMP COMMENT '确认时间', " +
                "remark VARCHAR(200) COMMENT '备注'" +
                ") COMMENT='完成量回报表'");
    }

    /**
     * 创建预警记录表
     */
    private void createAlertRecordTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_cx_alert_record");
        jdbcTemplate.execute("CREATE TABLE t_cx_alert_record (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "alert_type VARCHAR(50) NOT NULL COMMENT '预警类型', " +
                "alert_level VARCHAR(20) DEFAULT 'WARNING' COMMENT '预警级别', " +
                "material_code VARCHAR(50) COMMENT '相关物料编码', " +
                "material_group_id VARCHAR(100) COMMENT '相关物料分组', " +
                "alert_content VARCHAR(500) NOT NULL COMMENT '预警内容', " +
                "alert_value DECIMAL(10,2) COMMENT '预警值', " +
                "threshold_value DECIMAL(10,2) COMMENT '阈值', " +
                "suggestion VARCHAR(500) COMMENT '处理建议', " +
                "status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态', " +
                "trigger_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '触发时间', " +
                "resolve_time TIMESTAMP COMMENT '解决时间', " +
                "handler VARCHAR(50) COMMENT '处理人', " +
                "handle_remark VARCHAR(500) COMMENT '处理备注'" +
                ") COMMENT='预警记录表'");
    }

    // ==================== 初始化基础数据 ====================

    /**
     * 初始化班次配置数据
     */
    private void initShiftConfigData() {
        jdbcTemplate.execute("INSERT INTO t_cx_shift_config (shift_code, shift_name, start_time, end_time, sort_order) VALUES " +
                "('NIGHT', '夜班', '00:00:00', '07:59:59', 1), " +
                "('DAY', '早班', '08:00:00', '15:59:59', 2), " +
                "('AFTERNOON', '中班', '16:00:00', '23:59:59', 3)");
    }

    /**
     * 初始化预警配置数据
     */
    private void initAlertConfigData() {
        jdbcTemplate.execute("INSERT INTO t_cx_alert_config (config_code, config_name, config_value, config_type, config_unit, description) VALUES " +
                "('INVENTORY_HIGH_HOURS', '胎胚库存高水位预警时长', '18', 'NUMBER', '小时', '胎胚库存可供硫化时长大于此值时触发高库存预警'), " +
                "('INVENTORY_LOW_HOURS', '胎胚库存低水位预警时长', '4', 'NUMBER', '小时', '胎胚库存可供硫化时长小于此值时触发低库存预警'), " +
                "('MAX_SKU_PER_MACHINE_PER_DAY', '单台成型机每天最大胎胚种类数', '4', 'NUMBER', '种', '硬性约束：超过则必须调整'), " +
                "('MAX_STRUCTURE_SWITCH_PER_DAY', '每日结构切换最大次数', '2', 'NUMBER', '次', '硬性约束：超过则禁止切换'), " +
                "('TRIP_DEFAULT_CAPACITY', '默认胎面整车容量', '12', 'NUMBER', '条', '每车默认装载胎胚数量'), " +
                "('SHIFT_BALANCE_RATIO', '班次均衡分配比例', '1:2:1', 'STRING', '', '夜班:早班:中班的分配比例'), " +
                "('CONTINUOUS_SAME_STRUCTURE_PRIORITY', '连续同结构优先权重', '10', 'NUMBER', '', '续作时相同结构SKU的优先权重')");
    }

    /**
     * 初始化机台数据
     */
    private void initMachineData() {
        jdbcTemplate.execute("INSERT INTO t_cx_machine (machine_code, machine_name, machine_type, line_number, max_daily_capacity, max_capacity_per_hour, status, wrapping_type, has_zero_degree_feeder) VALUES " +
                "('GM01', '成型机01', '软控三鼓', 1, 120, 15.0, 'RUNNING', 'A型', 1), " +
                "('GM02', '成型机02', '软控三鼓', 1, 120, 15.0, 'RUNNING', 'A型', 1), " +
                "('GM03', '成型机03', '软控三鼓', 2, 120, 15.0, 'RUNNING', 'A型', 0), " +
                "('GM04', '成型机04', '赛象三鼓', 2, 120, 15.0, 'RUNNING', 'B型', 1), " +
                "('GM05', '成型机05', '赛象三鼓', 3, 120, 15.0, 'RUNNING', 'B型', 0)");
    }

    /**
     * 初始化物料数据
     */
    private void initMaterialData() {
        jdbcTemplate.execute("INSERT INTO t_cx_material (material_code, material_name, product_structure, main_pattern, pattern, is_main_product, vulcanize_time_minutes) VALUES " +
                "('MAT001', '12R22.5-18PR-JA511', '12R22.5', 'JA511', 'JA511', 1, 12.5), " +
                "('MAT002', '11R22.5-16PR-JA511', '11R22.5', 'JA511', 'JA511', 1, 11.8), " +
                "('MAT003', '295/80R22.5-18PR-JA511', '295/80R22.5', 'JA511', 'JA511', 0, 13.2), " +
                "('MAT004', '275/80R22.5-16PR-JA511', '275/80R22.5', 'JA511', 'JA511', 0, 11.5), " +
                "('MAT005', '315/80R22.5-18PR-JA511', '315/80R22.5', 'JA511', 'JA511', 1, 14.0), " +
                "('MAT006', '385/65R22.5-20PR-JA511', '385/65R22.5', 'JA511', 'JA511', 0, 15.5)");
    }

    /**
     * 初始化库存数据
     */
    private void initStockData() {
        jdbcTemplate.execute("INSERT INTO t_cx_stock (material_code, current_stock, vulcanize_machine_count, vulcanize_mold_count, alert_status, stock_hours) VALUES " +
                "('MAT001', 500, 4, 16, 'NORMAL', 16.0), " +
                "('MAT002', 350, 3, 12, 'NORMAL', 12.0), " +
                "('MAT003', 200, 2, 8, 'LOW', 5.0), " +
                "('MAT004', 800, 3, 12, 'HIGH', 24.0), " +
                "('MAT005', 150, 2, 8, 'LOW', 3.5), " +
                "('MAT006', 600, 4, 16, 'NORMAL', 18.0)");
    }

    /**
     * 初始化硫化计划数据
     */
    private void initVulcanizingPlanData() {
        jdbcTemplate.execute("INSERT INTO t_cx_vulcanizing_plan (plan_code, plan_date, material_code, plan_quantity, priority, source, status) VALUES " +
                "('VP2024010001', CURDATE(), 'MAT001', 240, 1, 'ERP', 'PENDING'), " +
                "('VP2024010002', CURDATE(), 'MAT002', 180, 2, 'ERP', 'PENDING'), " +
                "('VP2024010003', CURDATE(), 'MAT003', 120, 3, 'ERP', 'PENDING'), " +
                "('VP2024010004', CURDATE(), 'MAT004', 200, 2, 'ERP', 'PENDING'), " +
                "('VP2024010005', CURDATE(), 'MAT005', 100, 1, 'ERP', 'PENDING'), " +
                "('VP2024010006', CURDATE(), 'MAT006', 150, 3, 'ERP', 'PENDING')");
    }
}
