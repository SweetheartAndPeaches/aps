package com.zlt.aps.cx.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库初始化器 - MySQL版本
 * 在应用启动时创建表和初始化数据
 * 
 * 按照技术文档V5.0.0定义的完整表结构
 * 
 * 注意：此初始化器仅在非测试环境执行
 *
 * @author APS Team
 */
@Component
@Profile("!test")
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

        // ==================== 三之一、月度计划表 ====================
        createMonthPlanFinalTable();

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

    // ==================== 三之一、月度计划表 ====================

    /**
     * 创建月度生产计划定稿表
     * 数据来源：ERP/MES系统月度生产计划
     */
    private void createMonthPlanFinalTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_mp_month_plan_prod_final");
        jdbcTemplate.execute("CREATE TABLE t_mp_month_plan_prod_final (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "production_no VARCHAR(50) COMMENT '工单号', " +
                "factory_code VARCHAR(50) COMMENT '工厂编码', " +
                "`year` INT COMMENT '年份', " +
                "`month` INT COMMENT '月份', " +
                "`year_month` INT COMMENT '年月YYYYMM', " +
                "month_plan_version VARCHAR(50) COMMENT '销售生产需求计划版本', " +
                "last_month_plan_version VARCHAR(50) COMMENT '最新需求计划版本', " +
                "production_version VARCHAR(50) COMMENT '排产计划版本', " +
                "product_type_code VARCHAR(50) COMMENT '产品品类', " +
                "material_code VARCHAR(50) COMMENT '物料编码', " +
                "material_desc VARCHAR(200) COMMENT '物料描述', " +
                "mes_material_code VARCHAR(50) COMMENT 'MES物料编码', " +
                "structure_name VARCHAR(100) COMMENT '产品结构', " +
                "pro_size VARCHAR(20) COMMENT '英寸', " +
                "product_category VARCHAR(50) COMMENT '产品分类', " +
                "product_status VARCHAR(20) COMMENT '产品状态', " +
                "structure_type VARCHAR(20) COMMENT '结构类型', " +
                "production_type VARCHAR(50) COMMENT '排产分类', " +
                "embryo_code VARCHAR(50) COMMENT '生胎代码', " +
                "main_material_desc VARCHAR(200) COMMENT '主物料胎胚号', " +
                "construction_stage VARCHAR(20) COMMENT '施工阶段', " +
                "is_zero_rack VARCHAR(10) COMMENT '是否零度材料', " +
                "embryo_no VARCHAR(50) COMMENT '制造示方书号', " +
                "text_no VARCHAR(50) COMMENT '文字示方书号', " +
                "lh_no VARCHAR(50) COMMENT '硫化示方书号', " +
                "brand VARCHAR(50) COMMENT '品牌', " +
                "specifications VARCHAR(100) COMMENT '规格', " +
                "main_pattern VARCHAR(100) COMMENT '主花纹', " +
                "pattern VARCHAR(100) COMMENT '花纹', " +
                "mould_cavity_qty INT COMMENT '型腔数量', " +
                "type_block_qty INT COMMENT '活块数量', " +
                "height_qty INT COMMENT '高优先级数量', " +
                "average_sale_qty INT COMMENT '月均销量', " +
                "inventory_sales_ratio DECIMAL(10,4) COMMENT '库销比', " +
                "day_vulcanization_qty INT COMMENT '日硫化量', " +
                "cx_machine_code VARCHAR(500) COMMENT '成型机台信息', " +
                "mould_change_info VARCHAR(100) COMMENT '模具使用变化信息', " +
                "dynamic_balance_qty VARCHAR(100) COMMENT '动平衡数量', " +
                "uniformity_qty INT COMMENT '均匀性数量', " +
                "curing_time INT COMMENT '单条硫化时间分钟', " +
                "prod_req_plan INT COMMENT '生产需求计划净需求', " +
                "trial_qty INT COMMENT '试制量试计划需求量', " +
                "height_production_qty INT COMMENT '高优先级排产数量', " +
                "fact_prod_req_qty INT COMMENT '实际生产需求含损耗', " +
                "total_qty INT COMMENT '生产实际排产量', " +
                "mid_production_qty INT COMMENT '中优先级排产数量', " +
                "cycle_production_qty INT COMMENT '周期排产储备排产数量', " +
                "convention_production_qty INT COMMENT '常规储备排产数量', " +
                "postpone_production_qty INT COMMENT '暂缓订单排产数量', " +
                "trial_production_qty INT COMMENT '试制量试排产量', " +
                "difference_qty INT COMMENT '差异量未排产数量', " +
                "adjust_qty1 INT COMMENT '第1周调整量', " +
                "adjust_qty2 INT COMMENT '第2周调整量', " +
                "adjust_qty3 INT COMMENT '第3周调整量', " +
                "adjust_qty4 INT COMMENT '第4周调整量', " +
                "reason VARCHAR(500) COMMENT '未排产原因', " +
                "begin_day INT COMMENT '开始日期', " +
                "end_day INT COMMENT '结束日期', " +
                "day_1 INT COMMENT '第1天排产量', " +
                "day_2 INT COMMENT '第2天排产量', " +
                "day_3 INT COMMENT '第3天排产量', " +
                "day_4 INT COMMENT '第4天排产量', " +
                "day_5 INT COMMENT '第5天排产量', " +
                "day_6 INT COMMENT '第6天排产量', " +
                "day_7 INT COMMENT '第7天排产量', " +
                "day_8 INT COMMENT '第8天排产量', " +
                "day_9 INT COMMENT '第9天排产量', " +
                "day_10 INT COMMENT '第10天排产量', " +
                "day_11 INT COMMENT '第11天排产量', " +
                "day_12 INT COMMENT '第12天排产量', " +
                "day_13 INT COMMENT '第13天排产量', " +
                "day_14 INT COMMENT '第14天排产量', " +
                "day_15 INT COMMENT '第15天排产量', " +
                "day_16 INT COMMENT '第16天排产量', " +
                "day_17 INT COMMENT '第17天排产量', " +
                "day_18 INT COMMENT '第18天排产量', " +
                "day_19 INT COMMENT '第19天排产量', " +
                "day_20 INT COMMENT '第20天排产量', " +
                "day_21 INT COMMENT '第21天排产量', " +
                "day_22 INT COMMENT '第22天排产量', " +
                "day_23 INT COMMENT '第23天排产量', " +
                "day_24 INT COMMENT '第24天排产量', " +
                "day_25 INT COMMENT '第25天排产量', " +
                "day_26 INT COMMENT '第26天排产量', " +
                "day_27 INT COMMENT '第27天排产量', " +
                "day_28 INT COMMENT '第28天排产量', " +
                "day_29 INT COMMENT '第29天排产量', " +
                "day_30 INT COMMENT '第30天排产量', " +
                "day_31 INT COMMENT '第31天排产量', " +
                "total_vulcanization_minutes DECIMAL(15,2) COMMENT '硫化总工时', " +
                "display_seq INT COMMENT '显示顺序', " +
                "is_release VARCHAR(10) COMMENT '发布状态', " +
                "is_import VARCHAR(10) COMMENT '是否EXCEL导入', " +
                "production_sequence BIGINT COMMENT '排产顺序', " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间', " +
                "create_by VARCHAR(50) COMMENT '创建人', " +
                "update_by VARCHAR(50) COMMENT '更新人', " +
                "remark VARCHAR(500) COMMENT '备注', " +
                "INDEX idx_year_month (`year_month`), " +
                "INDEX idx_material_code (material_code), " +
                "INDEX idx_production_no (production_no)" +
                ") COMMENT='月度生产计划定稿表'");
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
        // 机台数据：部分机台设置在产结构（structure字段），用于测试续作排产
        jdbcTemplate.execute("INSERT INTO t_cx_machine (machine_code, machine_name, machine_type, line_number, max_daily_capacity, max_capacity_per_hour, status, wrapping_type, has_zero_degree_feeder, structure) VALUES " +
                "('GM01', '成型机01', '软控三鼓', 1, 120, 15.0, 'RUNNING', 'A型', 1, '12R22.5'), " +  // 在产结构：12R22.5
                "('GM02', '成型机02', '软控三鼓', 1, 120, 15.0, 'RUNNING', 'A型', 1, '11R22.5'), " +  // 在产结构：11R22.5
                "('GM03', '成型机03', '软控三鼓', 2, 120, 15.0, 'RUNNING', 'A型', 0, '295/80R22.5'), " + // 在产结构：295/80R22.5
                "('GM04', '成型机04', '赛象三鼓', 2, 120, 15.0, 'RUNNING', 'B型', 1, NULL), " +  // 无在产结构
                "('GM05', '成型机05', '赛象三鼓', 3, 120, 15.0, 'RUNNING', 'B型', 0, NULL)");   // 无在产结构
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
     * 使用相对日期，确保与测试排程日期匹配
     */
    private void initVulcanizingPlanData() {
        // 硫化计划日期：当天和第二天，方便测试
        jdbcTemplate.execute("INSERT INTO t_cx_vulcanizing_plan (plan_code, plan_date, material_code, plan_quantity, priority, source, status) VALUES " +
                "('VP2024010001', CURDATE(), 'MAT001', 240, 1, 'ERP', 'PENDING'), " +
                "('VP2024010002', CURDATE(), 'MAT002', 180, 2, 'ERP', 'PENDING'), " +
                "('VP2024010003', CURDATE(), 'MAT003', 120, 3, 'ERP', 'PENDING'), " +
                "('VP2024010004', CURDATE(), 'MAT004', 200, 2, 'ERP', 'PENDING'), " +
                "('VP2024010005', CURDATE(), 'MAT005', 100, 1, 'ERP', 'PENDING'), " +
                "('VP2024010006', CURDATE(), 'MAT006', 150, 3, 'ERP', 'PENDING'), " +
                "('VP2024010007', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'MAT001', 240, 1, 'ERP', 'PENDING'), " +
                "('VP2024010008', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'MAT002', 180, 2, 'ERP', 'PENDING'), " +
                "('VP2024010009', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'MAT003', 120, 3, 'ERP', 'PENDING'), " +
                "('VP2024010010', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'MAT004', 200, 2, 'ERP', 'PENDING'), " +
                "('VP2024010011', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'MAT005', 100, 1, 'ERP', 'PENDING'), " +
                "('VP2024010012', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'MAT006', 150, 3, 'ERP', 'PENDING')");
    }
}
