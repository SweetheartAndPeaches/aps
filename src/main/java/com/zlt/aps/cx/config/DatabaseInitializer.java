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
        createParamConfigTable();
        createHolidayConfigTable();
        createKeyProductTable();

        // ==================== 二、核心业务表 ====================
        createMachineTable();
        createMdmMoldingMachineTable();
        createMaterialTable();
        createMdmMaterialInfoTable();
        createStockTable();
        createScheduleMainTable();
        createScheduleDetailTable();
        createScheduleResultTable();

        // ==================== 三、计划与任务表 ====================
        createLhScheduleResultTable();
        createDailyEmbryoTaskTable();
        createMdmMachineOnlineInfoTable();
        createMdmMachineFixedTable();
        createMdmWorkCalendarTable();
        createMdmStructureLhRatioTable();
        createMdmMonthSurplusTable();
        createMdmMonthPlanProductLhTable();
        createMdmSkuScheduleCategoryTable();

        // ==================== 三之一、月度计划表 ====================
        createFactoryMonthPlanProductionFinalResultTable();

        // ==================== 四、计算辅助表 ====================
        createTrialAllocationLogTable();
        createScheduleIntermediateTable();
        createShiftBalanceAdjustTable();
        createConstraintCheckRecordTable();
        createMachineStructureCapacityTable();
        createStructureAllocationTable();
        createStructurePriorityTable();
        createStructureTripConfigTable();
        createDevicePlanShutTable();
        createMaterialEndingTable();
        createMaterialExceptionTable();
        createOperatorLeaveTable();
        createTreadParkingConfigTable();

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
        initMdmMoldingMachineData();
        initMaterialData();
        initMdmMaterialInfoData();
        initStockData();
        initLhScheduleResultData();
        initMdmStructureLhRatioData();
        initMdmMachineOnlineInfoData();
        initKeyProductData();
        initStructureAllocationData();
        initMdmMonthPlanProductLhData();
        initMdmMonthSurplusData();
        initWorkCalendarData();
        initOperatorLeaveData();

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
        jdbcTemplate.execute("DROP TABLE IF EXISTS T_CX_SHIFT_CONFIG");
        jdbcTemplate.execute("CREATE TABLE T_CX_SHIFT_CONFIG (" +
                "ID BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "SHIFT_CODE VARCHAR(20) NOT NULL COMMENT '班次编码', " +
                "SHIFT_NAME VARCHAR(50) NOT NULL COMMENT '班次名称', " +
                "SHIFT_ORDER INT DEFAULT 0 COMMENT '班次序号', " +
                "START_TIME VARCHAR(20) NOT NULL COMMENT '开始时间', " +
                "END_TIME VARCHAR(20) NOT NULL COMMENT '结束时间', " +
                "SHIFT_HOURS INT DEFAULT 8 COMMENT '班次时长(小时)', " +
                "IS_CROSS_DAY SMALLINT DEFAULT 0 COMMENT '是否跨天', " +
                "SCHEDULE_DAY INT DEFAULT 1 COMMENT '排程天数(1-第一天,2-第二天,3-第三天)', " +
                "DAY_SHIFT_ORDER INT DEFAULT 1 COMMENT '当天班次序号', " +
                "CLASS_FIELD VARCHAR(20) COMMENT '对应结果表字段', " +
                "FACTORY_CODE VARCHAR(20) DEFAULT 'F001' COMMENT '工厂编号', " +
                "IS_ACTIVE SMALLINT DEFAULT 1 COMMENT '是否启用', " +
                "REMARK VARCHAR(500) COMMENT '备注', " +
                "CREATE_BY VARCHAR(50) COMMENT '创建人', " +
                "CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "UPDATE_BY VARCHAR(50) COMMENT '更新人', " +
                "UPDATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'" +
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
    private void createLhScheduleResultTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS T_LH_SCHEDULE_RESULT");
        jdbcTemplate.execute("CREATE TABLE T_LH_SCHEDULE_RESULT (" +
                "ID BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "FACTORY_CODE VARCHAR(50) COMMENT '工厂编号', " +
                "BATCH_NO VARCHAR(50) COMMENT '批次号', " +
                "ORDER_NO VARCHAR(50) COMMENT '工单号', " +
                "LH_MACHINE_CODE VARCHAR(50) COMMENT '硫化机台编号', " +
                "LEFT_RIGHT_MOULD VARCHAR(10) COMMENT '左右模', " +
                "LH_MACHINE_NAME VARCHAR(100) COMMENT '硫化机台名称', " +
                "MATERIAL_CODE VARCHAR(50) COMMENT '物料编号', " +
                "SPEC_CODE VARCHAR(50) COMMENT '规格代码', " +
                "EMBRYO_CODE VARCHAR(50) COMMENT '胎胚代码', " +
                "STRUCTURE_NAME VARCHAR(100) COMMENT '产品结构', " +
                "MATERIAL_DESC VARCHAR(200) COMMENT '物料描述', " +
                "MAIN_MATERIAL_DESC VARCHAR(200) COMMENT '主物料', " +
                "EMBRYO_STOCK INT COMMENT '胎胚库存', " +
                "SPEC_DESC VARCHAR(200) COMMENT '规格描述', " +
                "LH_TIME INT COMMENT '硫化时长(秒)', " +
                "DAILY_PLAN_QTY INT COMMENT '日计划数量', " +
                "SCHEDULE_DATE DATE COMMENT '排程日期', " +
                "SPEC_END_TIME DATETIME COMMENT '规格结束时间', " +
                "PRODUCTION_STATUS VARCHAR(20) DEFAULT 'PENDING' COMMENT '生产状态', " +
                "CLASS1_PLAN_QTY INT COMMENT '1班计划量', " +
                "CLASS1_START_TIME DATETIME COMMENT '1班开始时间', " +
                "CLASS1_END_TIME DATETIME COMMENT '1班结束时间', " +
                "CLASS1_ANALYSIS VARCHAR(500) COMMENT '1班原因分析', " +
                "CLASS1_FINISH_QTY INT COMMENT '1班完成量', " +
                "CLASS2_PLAN_QTY INT COMMENT '2班计划量', " +
                "CLASS2_START_TIME DATETIME COMMENT '2班开始时间', " +
                "CLASS2_END_TIME DATETIME COMMENT '2班结束时间', " +
                "CLASS2_ANALYSIS VARCHAR(500) COMMENT '2班原因分析', " +
                "CLASS2_FINISH_QTY INT COMMENT '2班完成量', " +
                "CLASS3_PLAN_QTY INT COMMENT '3班计划量', " +
                "CLASS3_START_TIME DATETIME COMMENT '3班开始时间', " +
                "CLASS3_END_TIME DATETIME COMMENT '3班结束时间', " +
                "CLASS3_ANALYSIS VARCHAR(500) COMMENT '3班原因分析', " +
                "CLASS3_FINISH_QTY INT COMMENT '3班完成量', " +
                "CLASS4_PLAN_QTY INT COMMENT '4班计划量', " +
                "CLASS4_START_TIME DATETIME COMMENT '4班开始时间', " +
                "CLASS4_END_TIME DATETIME COMMENT '4班结束时间', " +
                "CLASS4_ANALYSIS VARCHAR(500) COMMENT '4班原因分析', " +
                "CLASS4_FINISH_QTY INT COMMENT '4班完成量', " +
                "IS_DELIVERY VARCHAR(10) COMMENT '是否交期', " +
                "IS_RELEASE VARCHAR(10) COMMENT '是否发布', " +
                "PUBLISH_SUCCESS_COUNT DECIMAL(10,2) COMMENT '发布成功计数', " +
                "NEWEST_PUBLISH_TIME DATETIME COMMENT '最新发布时间', " +
                "DATA_SOURCE VARCHAR(20) COMMENT '数据来源', " +
                "MOULD_QTY INT COMMENT '使用模数', " +
                "SINGLE_MOULD_SHIFT_QTY INT COMMENT '单班硫化量', " +
                "MOULD_INFO VARCHAR(500) COMMENT '模具信息', " +
                "MOULD_METHOD VARCHAR(20) COMMENT '硫化方式', " +
                "CONSTRUCTION_STAGE VARCHAR(50) COMMENT '施工阶段', " +
                "EMBRYO_NO VARCHAR(50) COMMENT '制造示方书号', " +
                "TEXT_NO VARCHAR(50) COMMENT '文字示方书号', " +
                "LH_NO VARCHAR(50) COMMENT '硫化示方书号', " +
                "MONTH_PLAN_VERSION VARCHAR(50) COMMENT '月计划版本', " +
                "MACHINE_ORDER INT COMMENT '机台排序号', " +
                "IS_TRIAL VARCHAR(10) COMMENT '是否试制量试', " +
                "REAL_SCHEDULE_DATE DATE COMMENT '实际排程日期', " +
                "IS_FIRST VARCHAR(10) COMMENT '是否首排', " +
                "MOULD_SURPLUS_QTY INT COMMENT '硫化余量', " +
                "IS_END VARCHAR(10) COMMENT '是否收尾', " +
                "PRODUCTION_VERSION VARCHAR(50) COMMENT '排产版本', " +
                "MOULD_CODE VARCHAR(50) COMMENT '模具号', " +
                "IS_SPLIT VARCHAR(10) COMMENT '是否拆分', " +
                "SCHEDULE_ORDER VARCHAR(50) COMMENT '排程顺序', " +
                "SCHEDULE_TYPE VARCHAR(20) COMMENT '排程类型', " +
                "IS_CHANGE_MOULD VARCHAR(10) COMMENT '是否换模', " +
                "TOTAL_DAILY_PLAN_QTY INT COMMENT '总计划数量', " +
                "CREATE_BY VARCHAR(50) COMMENT '创建人', " +
                "CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "UPDATE_BY VARCHAR(50) COMMENT '更新人', " +
                "UPDATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间', " +
                "REMARK VARCHAR(500) COMMENT '备注'" +
                ") COMMENT='硫化排程结果表'");
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
     * 创建工厂月生产计划-最终排产计划定稿表
     * 数据来源：ERP/MES系统月度生产计划
     */
    private void createFactoryMonthPlanProductionFinalResultTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS T_MP_MONTH_PLAN_PROD_FINAL");
        jdbcTemplate.execute("CREATE TABLE T_MP_MONTH_PLAN_PROD_FINAL (" +
                "ID BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "PRODUCTION_NO VARCHAR(50) COMMENT '工单号', " +
                "FACTORY_CODE VARCHAR(50) COMMENT '工厂编码', " +
                "YEAR INT COMMENT '年份', " +
                "MONTH INT COMMENT '月份', " +
                "`YEAR_MONTH` INT COMMENT '年月YYYYMM', " +
                "MONTH_PLAN_VERSION VARCHAR(50) COMMENT '销售生产需求计划版本', " +
                "LAST_MONTH_PLAN_VERSION VARCHAR(50) COMMENT '最新需求计划版本', " +
                "PRODUCTION_VERSION VARCHAR(50) COMMENT '排产计划版本', " +
                "PRODUCT_TYPE_CODE VARCHAR(50) COMMENT '产品品类', " +
                "MATERIAL_CODE VARCHAR(50) COMMENT '物料编码', " +
                "MATERIAL_DESC VARCHAR(200) COMMENT '物料描述', " +
                "MES_MATERIAL_CODE VARCHAR(50) COMMENT 'MES物料编码', " +
                "STRUCTURE_NAME VARCHAR(100) COMMENT '产品结构', " +
                "PRO_SIZE VARCHAR(20) COMMENT '英寸', " +
                "PRODUCT_CATEGORY VARCHAR(50) COMMENT '产品分类', " +
                "PRODUCT_STATUS VARCHAR(20) COMMENT '产品状态', " +
                "STRUCTURE_TYPE VARCHAR(20) COMMENT '结构类型', " +
                "PRODUCTION_TYPE VARCHAR(50) COMMENT '排产分类', " +
                "EMBRYO_CODE VARCHAR(50) COMMENT '生胎代码', " +
                "MAIN_MATERIAL_DESC VARCHAR(200) COMMENT '主物料胎胚号', " +
                "CONSTRUCTION_STAGE VARCHAR(20) COMMENT '施工阶段', " +
                "IS_ZERO_RACK VARCHAR(10) COMMENT '是否零度材料', " +
                "EMBRYO_NO VARCHAR(50) COMMENT '制造示方书号', " +
                "TEXT_NO VARCHAR(50) COMMENT '文字示方书号', " +
                "LH_NO VARCHAR(50) COMMENT '硫化示方书号', " +
                "BRAND VARCHAR(50) COMMENT '品牌', " +
                "SPECIFICATIONS VARCHAR(100) COMMENT '规格', " +
                "MAIN_PATTERN VARCHAR(100) COMMENT '主花纹', " +
                "PATTERN VARCHAR(100) COMMENT '花纹', " +
                "MOULD_CAVITY_QTY INT COMMENT '型腔数量', " +
                "TYPE_BLOCK_QTY INT COMMENT '活块数量', " +
                "HEIGHT_QTY INT COMMENT '高优先级数量', " +
                "AVERAGE_SALE_QTY INT COMMENT '月均销量', " +
                "INVENTORY_SALES_RATIO DECIMAL(10,4) COMMENT '库销比', " +
                "DAY_VULCANIZATION_QTY INT COMMENT '日硫化量', " +
                "CX_MACHINE_CODE VARCHAR(500) COMMENT '成型机台信息', " +
                "MOULD_CHANGE_INFO VARCHAR(100) COMMENT '模具使用变化信息', " +
                "DYNAMIC_BALANCE_QTY VARCHAR(100) COMMENT '动平衡数量', " +
                "UNIFORMITY_QTY INT COMMENT '均匀性数量', " +
                "CURING_TIME INT COMMENT '单条硫化时间分钟', " +
                "PROD_REQ_PLAN INT COMMENT '生产需求计划净需求', " +
                "TRIAL_QTY INT COMMENT '试制量试计划需求量', " +
                "HEIGHT_PRODUCTION_QTY INT COMMENT '高优先级排产数量', " +
                "FACT_PROD_REQ_QTY INT COMMENT '实际生产需求含损耗', " +
                "TOTAL_QTY INT COMMENT '生产实际排产量', " +
                "MID_PRODUCTION_QTY INT COMMENT '中优先级排产数量', " +
                "CYCLE_PRODUCTION_QTY INT COMMENT '周期排产储备排产数量', " +
                "CONVENTION_PRODUCTION_QTY INT COMMENT '常规储备排产数量', " +
                "POSTPONE_PRODUCTION_QTY INT COMMENT '暂缓订单排产数量', " +
                "TRIAL_PRODUCTION_QTY INT COMMENT '试制量试排产量', " +
                "DIFFERENCE_QTY INT COMMENT '差异量未排产数量', " +
                "ADJUST_QTY1 INT COMMENT '第1周调整量', " +
                "ADJUST_QTY2 INT COMMENT '第2周调整量', " +
                "ADJUST_QTY3 INT COMMENT '第3周调整量', " +
                "ADJUST_QTY4 INT COMMENT '第4周调整量', " +
                "REASON VARCHAR(500) COMMENT '未排产原因', " +
                "BEGIN_DAY INT COMMENT '开始日期', " +
                "END_DAY INT COMMENT '结束日期', " +
                "DAY_1 INT COMMENT '第1天排产量', " +
                "DAY_2 INT COMMENT '第2天排产量', " +
                "DAY_3 INT COMMENT '第3天排产量', " +
                "DAY_4 INT COMMENT '第4天排产量', " +
                "DAY_5 INT COMMENT '第5天排产量', " +
                "DAY_6 INT COMMENT '第6天排产量', " +
                "DAY_7 INT COMMENT '第7天排产量', " +
                "DAY_8 INT COMMENT '第8天排产量', " +
                "DAY_9 INT COMMENT '第9天排产量', " +
                "DAY_10 INT COMMENT '第10天排产量', " +
                "DAY_11 INT COMMENT '第11天排产量', " +
                "DAY_12 INT COMMENT '第12天排产量', " +
                "DAY_13 INT COMMENT '第13天排产量', " +
                "DAY_14 INT COMMENT '第14天排产量', " +
                "DAY_15 INT COMMENT '第15天排产量', " +
                "DAY_16 INT COMMENT '第16天排产量', " +
                "DAY_17 INT COMMENT '第17天排产量', " +
                "DAY_18 INT COMMENT '第18天排产量', " +
                "DAY_19 INT COMMENT '第19天排产量', " +
                "DAY_20 INT COMMENT '第20天排产量', " +
                "DAY_21 INT COMMENT '第21天排产量', " +
                "DAY_22 INT COMMENT '第22天排产量', " +
                "DAY_23 INT COMMENT '第23天排产量', " +
                "DAY_24 INT COMMENT '第24天排产量', " +
                "DAY_25 INT COMMENT '第25天排产量', " +
                "DAY_26 INT COMMENT '第26天排产量', " +
                "DAY_27 INT COMMENT '第27天排产量', " +
                "DAY_28 INT COMMENT '第28天排产量', " +
                "DAY_29 INT COMMENT '第29天排产量', " +
                "DAY_30 INT COMMENT '第30天排产量', " +
                "DAY_31 INT COMMENT '第31天排产量', " +
                "TOTAL_VULCANIZATION_MINUTES DECIMAL(15,2) COMMENT '硫化总工时', " +
                "DISPLAY_SEQ INT COMMENT '显示顺序', " +
                "IS_RELEASE VARCHAR(10) COMMENT '发布状态', " +
                "IS_IMPORT VARCHAR(10) COMMENT '是否EXCEL导入', " +
                "PRODUCTION_SEQUENCE BIGINT COMMENT '排产顺序', " +
                "CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "UPDATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间', " +
                "CREATE_BY VARCHAR(50) COMMENT '创建人', " +
                "UPDATE_BY VARCHAR(50) COMMENT '更新人', " +
                "REMARK VARCHAR(500) COMMENT '备注', " +
                "INDEX IDX_YEAR_MONTH (`YEAR_MONTH`), " +
                "INDEX IDX_MATERIAL_CODE (MATERIAL_CODE), " +
                "INDEX IDX_PRODUCTION_NO (PRODUCTION_NO)" +
                ") COMMENT='工厂月生产计划-最终排产计划定稿表'");
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
     * 班次模式：第一天只有早班和中班，第二、三天有夜班、早班、中班
     */
    private void initShiftConfigData() {
        // 第一天班次配置 (D1) - 只有早班和中班
        jdbcTemplate.execute("INSERT INTO T_CX_SHIFT_CONFIG (SHIFT_CODE, SHIFT_NAME, SHIFT_ORDER, START_TIME, END_TIME, SHIFT_HOURS, IS_CROSS_DAY, SCHEDULE_DAY, DAY_SHIFT_ORDER, CLASS_FIELD, FACTORY_CODE, IS_ACTIVE) VALUES " +
                "('DAY_D1', '早班', 1, '06:00:00', '13:59:59', 8, 0, 1, 1, 'CLASS1', 'F001', 1), " +
                "('AFTERNOON_D1', '中班', 2, '14:00:00', '23:59:59', 10, 0, 1, 2, 'CLASS2', 'F001', 1)");
        
        // 第二天班次配置 (D2) - 有夜班、早班、中班
        jdbcTemplate.execute("INSERT INTO T_CX_SHIFT_CONFIG (SHIFT_CODE, SHIFT_NAME, SHIFT_ORDER, START_TIME, END_TIME, SHIFT_HOURS, IS_CROSS_DAY, SCHEDULE_DAY, DAY_SHIFT_ORDER, CLASS_FIELD, FACTORY_CODE, IS_ACTIVE) VALUES " +
                "('NIGHT_D2', '夜班', 1, '00:00:00', '05:59:59', 6, 0, 2, 1, 'CLASS3', 'F001', 1), " +
                "('DAY_D2', '早班', 2, '06:00:00', '13:59:59', 8, 0, 2, 2, 'CLASS4', 'F001', 1), " +
                "('AFTERNOON_D2', '中班', 3, '14:00:00', '23:59:59', 10, 0, 2, 3, 'CLASS5', 'F001', 1)");
        
        // 第三天班次配置 (D3) - 有夜班、早班、中班
        jdbcTemplate.execute("INSERT INTO T_CX_SHIFT_CONFIG (SHIFT_CODE, SHIFT_NAME, SHIFT_ORDER, START_TIME, END_TIME, SHIFT_HOURS, IS_CROSS_DAY, SCHEDULE_DAY, DAY_SHIFT_ORDER, CLASS_FIELD, FACTORY_CODE, IS_ACTIVE) VALUES " +
                "('NIGHT_D3', '夜班', 1, '00:00:00', '05:59:59', 6, 0, 3, 1, 'CLASS6', 'F001', 1), " +
                "('DAY_D3', '早班', 2, '06:00:00', '13:59:59', 8, 0, 3, 2, 'CLASS7', 'F001', 1), " +
                "('AFTERNOON_D3', '中班', 3, '14:00:00', '23:59:59', 10, 0, 3, 3, 'CLASS8', 'F001', 1)");
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
     * 初始化硫化排程结果数据
     * 使用相对日期，确保与测试排程日期匹配
     */
    private void initLhScheduleResultData() {
        // 硫化排程日期：当天和第二天，方便测试
        jdbcTemplate.execute("INSERT INTO T_LH_SCHEDULE_RESULT (FACTORY_CODE, BATCH_NO, SCHEDULE_DATE, MATERIAL_CODE, EMBRYO_CODE, MATERIAL_DESC, STRUCTURE_NAME, DAILY_PLAN_QTY, PRODUCTION_STATUS, MACHINE_ORDER, DATA_SOURCE, LH_TIME, MOULD_QTY, MOULD_SURPLUS_QTY) VALUES " +
                "('F001', 'LH2024010001', CURDATE(), 'MAT001', 'MAT001', '12R22.5-18PR-JA511', '12R22.5', 240, 'PENDING', 1, 'MONTH_PLAN', 750, 2, 500), " +
                "('F001', 'LH2024010002', CURDATE(), 'MAT002', 'MAT002', '11R22.5-16PR-JA511', '11R22.5', 180, 'PENDING', 2, 'MONTH_PLAN', 708, 2, 300), " +
                "('F001', 'LH2024010003', CURDATE(), 'MAT003', 'MAT003', '295/80R22.5-18PR-JA511', '295/80R22.5', 120, 'PENDING', 3, 'MONTH_PLAN', 792, 2, 400), " +
                "('F001', 'LH2024010004', CURDATE(), 'MAT004', 'MAT004', '275/80R22.5-16PR-JA511', '275/80R22.5', 200, 'PENDING', 4, 'MONTH_PLAN', 690, 2, 350), " +
                "('F001', 'LH2024010005', CURDATE(), 'MAT005', 'MAT005', '315/80R22.5-18PR-JA511', '315/80R22.5', 100, 'PENDING', 5, 'MONTH_PLAN', 840, 2, 200), " +
                "('F001', 'LH2024010006', CURDATE(), 'MAT006', 'MAT006', '385/65R22.5-20PR-JA511', '385/65R22.5', 150, 'PENDING', 6, 'MONTH_PLAN', 930, 2, 250), " +
                "('F001', 'LH2024010007', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'MAT001', 'MAT001', '12R22.5-18PR-JA511', '12R22.5', 240, 'PENDING', 1, 'MONTH_PLAN', 750, 2, 480), " +
                "('F001', 'LH2024010008', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'MAT002', 'MAT002', '11R22.5-16PR-JA511', '11R22.5', 180, 'PENDING', 2, 'MONTH_PLAN', 708, 2, 280), " +
                "('F001', 'LH2024010009', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'MAT003', 'MAT003', '295/80R22.5-18PR-JA511', '295/80R22.5', 120, 'PENDING', 3, 'MONTH_PLAN', 792, 2, 380), " +
                "('F001', 'LH2024010010', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'MAT004', 'MAT004', '275/80R22.5-16PR-JA511', '275/80R22.5', 200, 'PENDING', 4, 'MONTH_PLAN', 690, 2, 330), " +
                "('F001', 'LH2024010011', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'MAT005', 'MAT005', '315/80R22.5-18PR-JA511', '315/80R22.5', 100, 'PENDING', 5, 'MONTH_PLAN', 840, 2, 180), " +
                "('F001', 'LH2024010012', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'MAT006', 'MAT006', '385/65R22.5-20PR-JA511', '385/65R22.5', 150, 'PENDING', 6, 'MONTH_PLAN', 930, 2, 230)");
    }

    // ==================== 新增表创建方法 ====================

    /**
     * 创建成型机档案表 (主数据)
     */
    private void createMdmMoldingMachineTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS T_MDM_MOLDING_MACHINE");
        jdbcTemplate.execute("CREATE TABLE T_MDM_MOLDING_MACHINE (" +
                "ID BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "FACTORY_CODE VARCHAR(50) COMMENT '工厂编号', " +
                "CX_MACHINE_CODE VARCHAR(50) NOT NULL UNIQUE COMMENT '成型机编码', " +
                "CX_MACHINE_BRAND_CODE VARCHAR(50) COMMENT '成型机类型', " +
                "CX_MACHINE_TYPE_CODE VARCHAR(50) COMMENT '机型', " +
                "ROLL_OVER_TYPE VARCHAR(50) COMMENT '反包方式', " +
                "IS_ZERO_RACK VARCHAR(10) COMMENT '是否有零度供料架', " +
                "LH_MACHINE_MAX_QTY INT COMMENT '硫化机上限', " +
                "MAX_DAY_CAPACITY INT COMMENT '设备最大日产量', " +
                "LINE_NUMBER INT COMMENT '产线编号', " +
                "IS_ACTIVE INT DEFAULT 1 COMMENT '是否启用', " +
                "REMARK VARCHAR(500) COMMENT '备注', " +
                "CREATE_BY VARCHAR(50) COMMENT '创建人', " +
                "CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "UPDATE_BY VARCHAR(50) COMMENT '更新人', " +
                "UPDATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'" +
                ") COMMENT='成型机档案表'");
    }

    /**
     * 创建物料主数据表
     */
    private void createMdmMaterialInfoTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS T_MDM_MATERIAL_INFO");
        jdbcTemplate.execute("CREATE TABLE T_MDM_MATERIAL_INFO (" +
                "ID BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "MATERIAL_CODE VARCHAR(50) NOT NULL UNIQUE COMMENT '物料编码', " +
                "MATERIAL_NAME VARCHAR(200) COMMENT '物料名称', " +
                "SPECIFICATION VARCHAR(100) COMMENT '规格型号', " +
                "STRUCTURE_NAME VARCHAR(100) COMMENT '产品结构', " +
                "MAIN_PATTERN VARCHAR(100) COMMENT '主花纹', " +
                "PATTERN VARCHAR(100) COMMENT '花纹', " +
                "EMBRYO_CODE VARCHAR(50) COMMENT '胎胚代码', " +
                "SPEC_DESC VARCHAR(200) COMMENT '规格描述', " +
                "LH_TIME INT COMMENT '硫化时长(秒)', " +
                "MATERIAL_GROUP_CODE VARCHAR(50) COMMENT '物料分组编码', " +
                "IS_ACTIVE INT DEFAULT 1 COMMENT '是否启用', " +
                "CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "UPDATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'" +
                ") COMMENT='物料主数据表'");
    }

    /**
     * 创建成型机在机信息表
     */
    private void createMdmMachineOnlineInfoTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS T_MDM_CX_MACHINE_ONLINE_INFO");
        jdbcTemplate.execute("CREATE TABLE T_MDM_CX_MACHINE_ONLINE_INFO (" +
                "ID BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "ONLINE_DATE DATE COMMENT '上机日期', " +
                "CX_CODE VARCHAR(50) NOT NULL COMMENT '成型机编码', " +
                "MATERIAL_CODE VARCHAR(50) COMMENT '物料编码', " +
                "MES_MATERIAL_CODE VARCHAR(50) COMMENT 'MES物料编码', " +
                "SPEC_DESC VARCHAR(200) COMMENT '规格描述', " +
                "EMBRYO_SPEC VARCHAR(200) COMMENT '胎胚规格', " +
                "DATA_VERSION VARCHAR(100) COMMENT '数据版本', " +
                "COMPANY_CODE VARCHAR(20) COMMENT '公司编码', " +
                "FACTORY_CODE VARCHAR(20) COMMENT '工厂编码', " +
                "IS_DELETE INT DEFAULT 0 COMMENT '是否删除', " +
                "CREATE_BY VARCHAR(50) COMMENT '创建人', " +
                "CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "UPDATE_BY VARCHAR(50) COMMENT '更新人', " +
                "UPDATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间', " +
                "REMARK VARCHAR(500) COMMENT '备注'" +
                ") COMMENT='成型机在机信息表'");
    }

    /**
     * 创建成型机固定规格表
     */
    private void createMdmMachineFixedTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS T_MDM_CX_MACHINE_FIXED");
        jdbcTemplate.execute("CREATE TABLE T_MDM_CX_MACHINE_FIXED (" +
                "ID BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "CX_MACHINE_CODE VARCHAR(50) NOT NULL COMMENT '成型机编码', " +
                "STRUCTURE_NAME VARCHAR(100) COMMENT '固定结构', " +
                "FIXED_ORDER INT COMMENT '固定顺序', " +
                "IS_ACTIVE INT DEFAULT 1 COMMENT '是否启用', " +
                "CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
                ") COMMENT='成型机固定规格表'");
    }

    /**
     * 创建工作日历表
     */
    private void createMdmWorkCalendarTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS T_MDM_WORK_CALENDAR");
        jdbcTemplate.execute("CREATE TABLE T_MDM_WORK_CALENDAR (" +
                "ID BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "PROC_CODE VARCHAR(50) COMMENT '工序编码', " +
                "YEAR INT COMMENT '年份', " +
                "MONTH INT COMMENT '月份', " +
                "DAY INT COMMENT '日期', " +
                "PRODUCTION_DATE DATE COMMENT '生产日期', " +
                "FACTORY_CODE VARCHAR(50) COMMENT '工厂编码', " +
                "ONE_SHIFT_FLAG VARCHAR(1) COMMENT '一班开停产标志，0-停,1-开', " +
                "TWO_SHIFT_FLAG VARCHAR(1) COMMENT '二班开停产标志，0-停,1-开', " +
                "THREE_SHIFT_FLAG VARCHAR(1) COMMENT '三班开停产标志，0-停,1-开', " +
                "DAY_FLAG VARCHAR(1) COMMENT '日期开停产标志，0-停,1-开', " +
                "RATE INT COMMENT '比例', " +
                "IS_WORK_DAY INT DEFAULT 1 COMMENT '是否工作日', " +
                "DAY_TYPE VARCHAR(20) COMMENT '日期类型', " +
                "CREATE_BY VARCHAR(50) COMMENT '创建人', " +
                "CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "UPDATE_BY VARCHAR(50) COMMENT '更新人', " +
                "UPDATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间', " +
                "REMARK VARCHAR(200) COMMENT '备注'" +
                ") COMMENT='工作日历表'");
    }

    /**
     * 创建结构硫化机比例表
     */
    private void createMdmStructureLhRatioTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS T_MDM_STRUCTURE_LH_RATIO");
        jdbcTemplate.execute("CREATE TABLE T_MDM_STRUCTURE_LH_RATIO (" +
                "ID BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "FACTORY_CODE VARCHAR(50) COMMENT '工厂编号', " +
                "STRUCTURE_NAME VARCHAR(100) NOT NULL COMMENT '产品结构', " +
                "CX_MACHINE_TYPE_CODE VARCHAR(50) COMMENT '成型机机型编码', " +
                "LH_MACHINE_MAX_QTY INT COMMENT '最大硫化机数', " +
                "MAX_EMBRYO_QTY INT COMMENT '最大胎胚种类数', " +
                "TRIP_QTY INT DEFAULT 12 COMMENT '整车条数', " +
                "IS_ACTIVE INT DEFAULT 1 COMMENT '是否启用', " +
                "CREATE_BY VARCHAR(50) COMMENT '创建人', " +
                "CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "UPDATE_BY VARCHAR(50) COMMENT '更新人', " +
                "UPDATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间', " +
                "REMARK VARCHAR(500) COMMENT '备注'" +
                ") COMMENT='结构硫化机比例表'");
    }

    /**
     * 创建月度余量表
     */
    private void createMdmMonthSurplusTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_mdm_month_surplus");
        jdbcTemplate.execute("CREATE TABLE t_mdm_month_surplus (" +
                "ID BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "FACTORY_CODE VARCHAR(50) COMMENT '工厂编号', " +
                "PRODUCT_TYPE_CODE VARCHAR(50) COMMENT '产品品类', " +
                "YEAR DECIMAL(10,0) COMMENT '年份', " +
                "MONTH DECIMAL(10,0) COMMENT '月份', " +
                "REQUIRE_VERSION VARCHAR(50) COMMENT '需求版本号', " +
                "BRAND VARCHAR(50) COMMENT '品牌', " +
                "STRUCTURE_NAME VARCHAR(100) COMMENT '产品结构', " +
                "MATERIAL_CODE VARCHAR(50) COMMENT '物料编码', " +
                "MATERIAL_DESC VARCHAR(200) COMMENT '物料描述', " +
                "PLAN_SURPLUS_QTY DECIMAL(10,2) COMMENT '计划余量', " +
                "STOCK_CAPTURE_DATE TIMESTAMP COMMENT '库存抓取日', " +
                "REMARK VARCHAR(500) COMMENT '备注', " +
                "IS_DELETE INT DEFAULT 0 COMMENT '是否删除：0-未删除 1-已删除', " +
                "CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "UPDATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'" +
                ") COMMENT='月度余量表'");
    }

    /**
     * 创建月度计划产品硫化产能表
     */
    private void createMdmMonthPlanProductLhTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS T_MDM_MONTH_PLAN_PRODUCT_LH");
        jdbcTemplate.execute("CREATE TABLE T_MDM_MONTH_PLAN_PRODUCT_LH (" +
                "ID BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "FACTORY_CODE VARCHAR(50) COMMENT '工厂编号', " +
                "MATERIAL_CODE VARCHAR(50) COMMENT '物料编码', " +
                "MATERIAL_DESC VARCHAR(200) COMMENT '物料描述', " +
                "MES_CAPACITY DECIMAL(10,2) COMMENT 'MES产能', " +
                "STANDARD_CAPACITY DECIMAL(10,2) COMMENT '标准产能', " +
                "APS_CAPACITY DECIMAL(10,2) COMMENT 'APS产能', " +
                "VULCANIZATION_TIME INT COMMENT '硫化时长(秒)', " +
                "TYPE VARCHAR(50) COMMENT '类型', " +
                "CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "UPDATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'" +
                ") COMMENT='月度计划产品硫化产能表'");
    }

    /**
     * 创建SKU排程分类表
     */
    private void createMdmSkuScheduleCategoryTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS t_mdm_sku_schedule_category");
        jdbcTemplate.execute("CREATE TABLE t_mdm_sku_schedule_category (" +
                "ID BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "FACTORY_CODE VARCHAR(50) COMMENT '工厂编号', " +
                "MATERIAL_CODE VARCHAR(50) COMMENT '物料编码', " +
                "SCHEDULE_TYPE VARCHAR(20) COMMENT '排程类型', " +
                "GENRATE_DATE TIMESTAMP COMMENT '生成日期', " +
                "REMARK VARCHAR(500) COMMENT '备注', " +
                "IS_DELETE INT DEFAULT 0 COMMENT '是否删除：0-未删除 1-已删除', " +
                "CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "UPDATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间', " +
                "CREATE_BY VARCHAR(50) COMMENT '创建人', " +
                "UPDATE_BY VARCHAR(50) COMMENT '更新人'" +
                ") COMMENT='SKU排程分类表'");
    }

    /**
     * 创建排程结果表
     */
    private void createScheduleResultTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS T_CX_SCHEDULE_RESULT");
        jdbcTemplate.execute("CREATE TABLE T_CX_SCHEDULE_RESULT (" +
                "ID BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "CX_BATCH_NO VARCHAR(50) COMMENT '成型批次号', " +
                "SCHEDULE_DATE DATE COMMENT '排程日期', " +
                "CX_MACHINE_CODE VARCHAR(50) COMMENT '成型机台编号', " +
                "CX_MACHINE_NAME VARCHAR(100) COMMENT '成型机台名称', " +
                "EMBRYO_CODE VARCHAR(50) COMMENT '胎胚代码', " +
                "STRUCTURE_NAME VARCHAR(100) COMMENT '产品结构', " +
                "SPEC_DESC VARCHAR(200) COMMENT '规格描述', " +
                "PRODUCT_NUM DECIMAL(10,2) COMMENT '胎胚总计划量', " +
                "TOTAL_STOCK DECIMAL(10,2) COMMENT '胎胚库存', " +
                "PRODUCTION_STATUS VARCHAR(20) COMMENT '生产状态', " +
                "IS_RELEASE VARCHAR(10) COMMENT '是否发布', " +
                "DATA_SOURCE VARCHAR(20) COMMENT '数据来源', " +
                "CLASS1_PLAN_QTY DECIMAL(10,2) COMMENT '一班计划数', " +
                "CLASS2_PLAN_QTY DECIMAL(10,2) COMMENT '二班计划数', " +
                "CLASS3_PLAN_QTY DECIMAL(10,2) COMMENT '三班计划数', " +
                "CLASS4_PLAN_QTY DECIMAL(10,2) COMMENT '四班计划数', " +
                "CLASS5_PLAN_QTY DECIMAL(10,2) COMMENT '五班计划数', " +
                "CLASS6_PLAN_QTY DECIMAL(10,2) COMMENT '六班计划数', " +
                "CLASS7_PLAN_QTY DECIMAL(10,2) COMMENT '七班计划数', " +
                "CLASS8_PLAN_QTY DECIMAL(10,2) COMMENT '八班计划数', " +
                "CLASS1_FINISH_QTY DECIMAL(10,2) COMMENT '一班完成量', " +
                "CLASS2_FINISH_QTY DECIMAL(10,2) COMMENT '二班完成量', " +
                "CLASS3_FINISH_QTY DECIMAL(10,2) COMMENT '三班完成量', " +
                "CLASS4_FINISH_QTY DECIMAL(10,2) COMMENT '四班完成量', " +
                "CLASS5_FINISH_QTY DECIMAL(10,2) COMMENT '五班完成量', " +
                "CLASS6_FINISH_QTY DECIMAL(10,2) COMMENT '六班完成量', " +
                "CLASS7_FINISH_QTY DECIMAL(10,2) COMMENT '七班完成量', " +
                "CLASS8_FINISH_QTY DECIMAL(10,2) COMMENT '八班完成量', " +
                "CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "UPDATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'" +
                ") COMMENT='排程结果表'");
    }

    /**
     * 创建机台结构产能表
     */
    private void createMachineStructureCapacityTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS T_CX_MACHINE_STRUCTURE_CAPACITY");
        jdbcTemplate.execute("CREATE TABLE T_CX_MACHINE_STRUCTURE_CAPACITY (" +
                "ID BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "CX_MACHINE_CODE VARCHAR(50) NOT NULL COMMENT '成型机编码', " +
                "STRUCTURE_CODE VARCHAR(100) COMMENT '结构编码', " +
                "STRUCTURE_NAME VARCHAR(100) COMMENT '结构名称', " +
                "HOUR_CAPACITY DECIMAL(10,2) COMMENT '小时产能', " +
                "DAY_CAPACITY INT COMMENT '日产能', " +
                "IS_ACTIVE INT DEFAULT 1 COMMENT '是否启用', " +
                "CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
                ") COMMENT='机台结构产能表'");
    }

    /**
     * 创建结构分配表
     */
    private void createStructureAllocationTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS T_MP_STRUCTURE_ALLOCATION");
        jdbcTemplate.execute("CREATE TABLE T_MP_STRUCTURE_ALLOCATION (" +
                "ID BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "FACTORY_CODE VARCHAR(50) COMMENT '工厂编码', " +
                "YEAR INT COMMENT '年份', " +
                "MONTH INT COMMENT '月份', " +
                "MONTH_PLAN_VERSION VARCHAR(50) COMMENT '月计划版本', " +
                "PRODUCTION_VERSION VARCHAR(50) COMMENT '排产版本', " +
                "PLAN_TYPE VARCHAR(20) COMMENT '计划类型', " +
                "STRUCTURE_NAME VARCHAR(100) COMMENT '产品结构', " +
                "NET_QTY BIGINT COMMENT '排产净需求', " +
                "LOSS_QTY BIGINT COMMENT '排产净需求(含损耗)', " +
                "CX_MACHINE_CODE VARCHAR(50) COMMENT '成型机编码', " +
                "BEGIN_DAY INT COMMENT '开始日期', " +
                "END_DAY INT COMMENT '结束日期', " +
                "ALLOT_DAYS INT COMMENT '分配天数', " +
                "DAY_CAPACITY INT COMMENT '日产能', " +
                "IS_ACTIVE INT DEFAULT 1 COMMENT '是否启用', " +
                "CREATE_BY VARCHAR(50) COMMENT '创建人', " +
                "CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "UPDATE_BY VARCHAR(50) COMMENT '更新人', " +
                "UPDATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间', " +
                "REMARK VARCHAR(500) COMMENT '备注'" +
                ") COMMENT='结构分配表'");
    }

    /**
     * 创建结构优先级表
     */
    private void createStructurePriorityTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS T_CX_STRUCTURE_PRIORITY");
        jdbcTemplate.execute("CREATE TABLE T_CX_STRUCTURE_PRIORITY (" +
                "ID BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "STRUCTURE_NAME VARCHAR(100) COMMENT '结构名称', " +
                "PRIORITY INT COMMENT '优先级', " +
                "IS_ACTIVE INT DEFAULT 1 COMMENT '是否启用', " +
                "CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
                ") COMMENT='结构优先级表'");
    }

    /**
     * 创建结构车次配置表
     */
    private void createStructureTripConfigTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS T_CX_STRUCTURE_TRIP_CONFIG");
        jdbcTemplate.execute("CREATE TABLE T_CX_STRUCTURE_TRIP_CONFIG (" +
                "ID BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "STRUCTURE_CODE VARCHAR(100) COMMENT '结构编码', " +
                "STRUCTURE_NAME VARCHAR(100) COMMENT '结构名称', " +
                "SHIFT_CODE VARCHAR(20) COMMENT '班次编码', " +
                "TRIP_QTY INT COMMENT '整车条数', " +
                "IS_ACTIVE INT DEFAULT 1 COMMENT '是否启用', " +
                "CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
                ") COMMENT='结构车次配置表'");
    }

    /**
     * 创建设备计划停机表
     */
    private void createDevicePlanShutTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS T_MDM_DEVICE_PLAN_SHUT");
        jdbcTemplate.execute("CREATE TABLE T_MDM_DEVICE_PLAN_SHUT (" +
                "ID BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "FACTORY_CODE VARCHAR(50) COMMENT '工厂编号', " +
                "MACHINE_TYPE VARCHAR(50) COMMENT '机台类型', " +
                "MACHINE_CODE VARCHAR(50) COMMENT '机台编号', " +
                "MACHINE_STOP_TYPE VARCHAR(50) COMMENT '停机类型', " +
                "BEGIN_DATE DATETIME COMMENT '开始日期', " +
                "END_DATE DATETIME COMMENT '结束日期', " +
                "REMARK VARCHAR(500) COMMENT '备注', " +
                "IS_ACTIVE INT DEFAULT 1 COMMENT '是否有效', " +
                "CREATE_BY VARCHAR(50) COMMENT '创建人', " +
                "CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "UPDATE_BY VARCHAR(50) COMMENT '更新人', " +
                "UPDATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'" +
                ") COMMENT='设备计划停机表'");
    }

    /**
     * 创建物料收尾表
     */
    private void createMaterialEndingTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS T_CX_MATERIAL_ENDING");
        jdbcTemplate.execute("CREATE TABLE T_CX_MATERIAL_ENDING (" +
                "ID BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "FACTORY_CODE VARCHAR(50) COMMENT '工厂编码', " +
                "MATERIAL_CODE VARCHAR(50) COMMENT '物料编码', " +
                "MATERIAL_DESC VARCHAR(200) COMMENT '物料描述', " +
                "STRUCTURE_NAME VARCHAR(100) COMMENT '结构名称', " +
                "VULCANIZING_REMAINDER INT COMMENT '硫化余量（条）', " +
                "EMBRYO_STOCK INT COMMENT '胎胚库存（条）', " +
                "FORMING_REMAINDER INT COMMENT '成型余量', " +
                "DAILY_LH_CAPACITY INT COMMENT '日硫化产能', " +
                "DAILY_FORMING_CAPACITY INT COMMENT '日成型产能', " +
                "ESTIMATED_ENDING_DAYS DECIMAL(10,2) COMMENT '预计收尾天数', " +
                "PLANNED_ENDING_DATE DATE COMMENT '计划收尾日期', " +
                "IS_URGENT_ENDING INT DEFAULT 0 COMMENT '是否紧急收尾', " +
                "IS_NEAR_ENDING INT DEFAULT 0 COMMENT '是否10天内收尾', " +
                "DELAY_QUANTITY INT COMMENT '延误量', " +
                "DISTRIBUTED_QUANTITY INT COMMENT '平摊量', " +
                "NEED_MONTH_PLAN_ADJUST INT DEFAULT 0 COMMENT '是否需要调整月计划', " +
                "STAT_DATE DATE COMMENT '统计日期', " +
                "CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "UPDATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间', " +
                "REMARK VARCHAR(500) COMMENT '备注'" +
                ") COMMENT='物料收尾表'");
    }

    /**
     * 创建物料异常表
     */
    private void createMaterialExceptionTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS cx_material_exception");
        jdbcTemplate.execute("CREATE TABLE cx_material_exception (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "material_code VARCHAR(50) COMMENT '物料编码', " +
                "exception_type VARCHAR(50) COMMENT '异常类型', " +
                "exception_desc VARCHAR(500) COMMENT '异常描述', " +
                "status VARCHAR(20) COMMENT '状态', " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
                ") COMMENT='物料异常表'");
    }

    /**
     * 创建操作员请假表
     */
    private void createOperatorLeaveTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS cx_operator_leave");
        jdbcTemplate.execute("CREATE TABLE cx_operator_leave (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "machine_code VARCHAR(50) COMMENT '机台编码', " +
                "employee_code VARCHAR(50) COMMENT '员工编码', " +
                "employee_name VARCHAR(50) COMMENT '员工姓名', " +
                "shift_code VARCHAR(20) COMMENT '班次编码', " +
                "start_date DATE COMMENT '请假开始日期', " +
                "end_date DATE COMMENT '请假结束日期', " +
                "leave_type VARCHAR(20) COMMENT '请假类型', " +
                "affect_capacity INT DEFAULT 0 COMMENT '是否影响产能(0-否,1-是)', " +
                "approval_status VARCHAR(20) COMMENT '审批状态', " +
                "remark VARCHAR(500) COMMENT '备注', " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', " +
                "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'" +
                ") COMMENT='操作工请假记录表'");
    }

    /**
     * 初始化操作工请假测试数据
     */
    private void initOperatorLeaveData() {
        jdbcTemplate.execute("INSERT INTO cx_operator_leave (machine_code, employee_code, employee_name, shift_code, start_date, end_date, leave_type, affect_capacity, approval_status, remark) VALUES " +
                "('CX001', 'EMP001', '张三', 'NIGHT', '2026-09-05', '2026-09-07', 'ANNUAL', 1, 'APPROVED', '年假'), " +
                "('CX002', 'EMP002', '李四', 'MORNING', '2026-09-10', '2026-09-12', 'SICK', 0, 'APPROVED', '病假'), " +
                "('CX003', 'EMP003', '王五', 'AFTERNOON', '2026-09-15', '2026-09-16', 'PERSONAL', 1, 'PENDING', '事假待审批')");
    }

    /**
     * 创建胎面停放配置表
     */
    private void createTreadParkingConfigTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS cx_tread_parking_config");
        jdbcTemplate.execute("CREATE TABLE cx_tread_parking_config (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "structure_code VARCHAR(100) COMMENT '结构编码', " +
                "parking_time INT COMMENT '停放时间(分钟)', " +
                "is_enabled INT DEFAULT 1 COMMENT '是否启用', " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
                ") COMMENT='胎面停放配置表'");
    }

    /**
     * 创建参数配置表
     */
    private void createParamConfigTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS T_CX_PARAM_CONFIG");
        jdbcTemplate.execute("CREATE TABLE T_CX_PARAM_CONFIG (" +
                "ID BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "PARAM_CODE VARCHAR(50) NOT NULL UNIQUE COMMENT '参数编码', " +
                "PARAM_NAME VARCHAR(100) COMMENT '参数名称', " +
                "PARAM_VALUE VARCHAR(500) COMMENT '参数值', " +
                "PARAM_TYPE VARCHAR(20) COMMENT '参数类型', " +
                "DESCRIPTION VARCHAR(500) COMMENT '参数说明', " +
                "IS_ACTIVE INT DEFAULT 1 COMMENT '是否启用', " +
                "CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
                ") COMMENT='参数配置表'");
    }

    /**
     * 创建节假日配置表
     */
    private void createHolidayConfigTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS cx_holiday_config");
        jdbcTemplate.execute("CREATE TABLE cx_holiday_config (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "holiday_date DATE COMMENT '节假日日期', " +
                "holiday_name VARCHAR(100) COMMENT '节假日名称', " +
                "holiday_type VARCHAR(20) COMMENT '节假日类型', " +
                "is_work_day INT DEFAULT 0 COMMENT '是否工作日', " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
                ") COMMENT='节假日配置表'");
    }

    /**
     * 创建关键产品表
     */
    private void createKeyProductTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS T_CX_KEY_PRODUCT");
        jdbcTemplate.execute("CREATE TABLE T_CX_KEY_PRODUCT (" +
                "ID BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                "EMBRYO_CODE VARCHAR(50) COMMENT '胎胚代码', " +
                "STRUCTURE_NAME VARCHAR(100) COMMENT '结构名称', " +
                "PRIORITY INT COMMENT '优先级', " +
                "IS_ACTIVE INT DEFAULT 1 COMMENT '是否启用', " +
                "CREATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
                ") COMMENT='关键产品表'");
    }

    // ==================== 新增初始化数据方法 ====================

    /**
     * 初始化成型机档案数据
     */
    private void initMdmMoldingMachineData() {
        jdbcTemplate.execute("INSERT INTO T_MDM_MOLDING_MACHINE (CX_MACHINE_CODE, CX_MACHINE_BRAND_CODE, CX_MACHINE_TYPE_CODE, ROLL_OVER_TYPE, IS_ZERO_RACK, LH_MACHINE_MAX_QTY, MAX_DAY_CAPACITY, LINE_NUMBER, IS_ACTIVE) VALUES " +
                "('GM01', '软控', '三鼓', 'A型', '1', 4, 120, 1, 1), " +
                "('GM02', '软控', '三鼓', 'A型', '1', 4, 120, 1, 1), " +
                "('GM03', '软控', '三鼓', 'A型', '0', 4, 120, 2, 1), " +
                "('GM04', '赛象', '三鼓', 'B型', '1', 4, 120, 2, 1), " +
                "('GM05', '赛象', '三鼓', 'B型', '0', 4, 120, 3, 1)");
    }

    /**
     * 初始化物料主数据
     */
    private void initMdmMaterialInfoData() {
        jdbcTemplate.execute("INSERT INTO T_MDM_MATERIAL_INFO (MATERIAL_CODE, MATERIAL_NAME, STRUCTURE_NAME, MAIN_PATTERN, PATTERN, EMBRYO_CODE, LH_TIME, IS_ACTIVE) VALUES " +
                "('MAT001', '12R22.5-18PR-JA511', '12R22.5', 'JA511', 'JA511', 'MAT001', 750, 1), " +
                "('MAT002', '11R22.5-16PR-JA511', '11R22.5', 'JA511', 'JA511', 'MAT002', 708, 1), " +
                "('MAT003', '295/80R22.5-18PR-JA511', '295/80R22.5', 'JA511', 'JA511', 'MAT003', 792, 1), " +
                "('MAT004', '275/80R22.5-16PR-JA511', '275/80R22.5', 'JA511', 'JA511', 'MAT004', 690, 1), " +
                "('MAT005', '315/80R22.5-18PR-JA511', '315/80R22.5', 'JA511', 'JA511', 'MAT005', 840, 1), " +
                "('MAT006', '385/65R22.5-20PR-JA511', '385/65R22.5', 'JA511', 'JA511', 'MAT006', 930, 1)");
    }

    /**
     * 初始化结构硫化机比例数据
     */
    private void initMdmStructureLhRatioData() {
        jdbcTemplate.execute("INSERT INTO T_MDM_STRUCTURE_LH_RATIO (FACTORY_CODE, STRUCTURE_NAME, CX_MACHINE_TYPE_CODE, LH_MACHINE_MAX_QTY, MAX_EMBRYO_QTY, TRIP_QTY, IS_ACTIVE) VALUES " +
                "('F001', '12R22.5', '三鼓', 4, 4, 12, 1), " +
                "('F001', '11R22.5', '三鼓', 4, 4, 12, 1), " +
                "('F001', '295/80R22.5', '三鼓', 4, 4, 12, 1), " +
                "('F001', '275/80R22.5', '三鼓', 4, 4, 12, 1), " +
                "('F001', '315/80R22.5', '三鼓', 4, 4, 12, 1), " +
                "('F001', '385/65R22.5', '三鼓', 4, 4, 12, 1)");
    }

    /**
     * 初始化在机信息数据
     * 测试数据日期: 2026年9月
     */
    private void initMdmMachineOnlineInfoData() {
        // 清空表
        jdbcTemplate.execute("TRUNCATE TABLE T_MDM_CX_MACHINE_ONLINE_INFO");
        
        // 批量插入测试数据 - 2026年9月
        String[][] onlineData = {
            // H1505
            {"H1505", "2351000053", "215101523", "385/55R22.5 160K 20PR JT560 BL4EJY"},
            {"H1505", "2351000795", "215104811", "385/55R22.5 160K 20PR JW593 BL4EJY"},
            // H1504
            {"H1504", "2351000579", "215101878", "385/65R22.5 164K 24PR JT560 BL4EJY"},
            {"H1504", "2351000563", "215101877", "385/65R22.5 164K 24PR JY598 BL4EJY"},
            // H1503
            {"H1503", "2351000407", "215102615", "255/70R22.5 140/137M 16PR JF518 BL4EJY"},
            {"H1503", "2351000407", "215102615", "255/70R22.5 140/137M 16PR JF518 BL4EJY"},
            // H1502
            {"H1502", "2351000412", "215101880", "385/65R22.5 164K 24PR BT180 BL4EBL"},
            {"H1502", "2351000563", "215101877", "385/65R22.5 164K 24PR JY598 BL4EJY"},
            // H1501
            {"H1501", "2351000459", "215102626", "295/80R22.5 152/149M 18PR JF518 BL4EJY"},
            {"H1501", "2351000459", "215102626", "295/80R22.5 152/149M 18PR JF518 BL4EJY"},
            {"H1501", "2351000505", "215101325", "295/80R22.5 152/149L 18PR JD575 BL4EJY"},
            // H1405
            {"H1405", "2351000525", "215101734", "11R24.5 149/146L 16PR JF568 BL4HJY"},
            {"H1405", "2351000179", "215101731", "11R24.5 149/146L 16PR JD571 BL4HJY"},
            // H1404
            {"H1404", "2351000529", "215103396", "315/60R22.5 154/150L 18PR BF188 BL4EBL"},
            // H1403
            {"H1403", "2351000473", "215103491", "275/80R22.5 149/146L 18PR EDR51 BL4HEG"},
            {"H1403", "2351000487", "215101840", "275/80R22.5 146/143L 16PR JF568 BL4HJY"},
            // H1402
            {"H1402", "2351000335", "215101743", "245/70R19.5 144/142J 18PR JF518 BL3EJY"},
            // H1401
            {"H1401", "2351000443", "215102582", "215/75R17.5 135/133L 16PR JF518 BL3EJY"},
            // H1305
            {"H1305", "2351000045", "215103782", "315/80R22.5 161/157K 20PR JA665 BL0HJY"},
            {"H1305", "2351000040", "215101401", "315/80R22.5 156/153K 20PR JD758 BL0EJY"},
            // H1304
            {"H1304", "2351000395", "215101470", "295/80R22.5 152/149M 18PR BD175 BL4EBL"},
            {"H1304", "2351000484", "215103740", "295/80R22.5 154/149M 18PR UF195 BL4HEU"},
            // H1303
            {"H1303", "2351000437", "215102632", "315/70R22.5 156/150L 18PR JF518 BL4EJY"},
            {"H1303", "2351000437", "215102632", "315/70R22.5 156/150L 18PR JF518 BL4EJY"},
            // H1302
            {"H1302", "2351000412", "215101880", "385/65R22.5 164K 24PR BT180 BL4EBL"},
            {"H1302", "2351000210", "215104050", "385/65R22.5 164K 24PR JW593 BL4EJY"},
            // H1301
            {"H1301", "2351000485", "215101335", "315/70R22.5 156/150L 18PR JD575 BL4EJY"},
            {"H1301", "2351000400", "215102631", "315/70R22.5 156/150L 18PR JD577 BL4EJY"},
            // H1205
            {"H1205", "2351000083", "215101838", "385/65R22.5 164K 24PR JY598 BL0EJY"},
            // H1204
            {"H1204", "2351000459", "215102626", "295/80R22.5 152/149M 18PR JF518 BL4EJY"},
            {"H1204", "2351000482", "215103741", "295/80R22.5 154/149L 18PR UD188 BL4HEU"},
            // H1203
            {"H1203", "2351000409", "215102830", "315/70R22.5 156/150L 18PR EDR51 BL4EEG"},
            // H1202
            {"H1202", "2351000456", "215102568", "295/75R22.5 146/143L 16PR BF188 BL4HBL"},
            {"H1202", "2351000567", "215101611", "295/75R22.5 146/143L 16PR JD571 BL4HJY"},
            // H1201
            {"H1201", "2351000064", "215101729", "11R22.5 146/143L 16PR JD571 BL4HJY"},
            {"H1201", "2351000064", "215101729", "11R22.5 146/143L 16PR JD571 BL4HJY"},
            // H1105
            {"H1105", "2351000459", "215102626", "295/80R22.5 152/149M 18PR JF518 BL4EJY"},
            {"H1105", "2351000459", "215102626", "295/80R22.5 152/149M 18PR JF518 BL4EJY"},
            // H1104
            {"H1104", "2351000301", "215101489", "315/80R22.5 156/150L 18PR JD565 BL4EJY"},
            {"H1104", "2351000403", "215101814", "315/80R22.5 156/153L 20PR JF518 BL4EJY"},
            // H1103
            {"H1103", "2351000305", "215102639", "325/95R24 162/160K 22PR JA526 BT0HJY"},
            {"H1103", "2351000286", "215102640", "325/95R24 162/160K 22PR JD727 BT0HJY"},
            // H1102
            {"H1102", "2351000110", "215103997", "12R22.5 152/149L 18PR BF188 BL0HBL"},
            {"H1102", "2351000108", "215101166", "12R22.5 152/149K 18PR JA665 BL0HJY"},
            // H1101
            {"H1101", "2351000663", "215101837", "385/65R22.5 164K 24PR JT560 BL0EJY"}
        };
        
        // 批量插入数据
        for (String[] data : onlineData) {
            jdbcTemplate.update(
                "INSERT INTO T_MDM_CX_MACHINE_ONLINE_INFO " +
                "(ONLINE_DATE, CX_CODE, MATERIAL_CODE, MES_MATERIAL_CODE, SPEC_DESC, EMBRYO_SPEC, " +
                "DATA_VERSION, COMPANY_CODE, FACTORY_CODE, IS_DELETE, CREATE_BY, UPDATE_BY) VALUES " +
                "('2026-09-01', ?, ?, ?, ?, ?, 'APS_MES_AH01_20260901100530001', '116', '116', 0, 'MES', 'MES')",
                data[0], data[1], data[2], data[3], data[3]
            );
        }
        
        System.out.println("  已插入 " + onlineData.length + " 条在机信息测试数据 (2026年9月)");
    }

    /**
     * 初始化关键产品数据
     */
    private void initKeyProductData() {
        jdbcTemplate.execute("INSERT INTO T_CX_KEY_PRODUCT (EMBRYO_CODE, STRUCTURE_NAME, PRIORITY, IS_ACTIVE) VALUES " +
                "('MAT001', '12R22.5', 1, 1), " +
                "('MAT002', '11R22.5', 2, 1), " +
                "('MAT005', '315/80R22.5', 3, 1)");
    }

    /**
     * 初始化结构分配数据
     * 每个结构可以分配到多台机台
     */
    private void initStructureAllocationData() {
        jdbcTemplate.execute("INSERT INTO T_MP_STRUCTURE_ALLOCATION (FACTORY_CODE, YEAR, MONTH, STRUCTURE_NAME, CX_MACHINE_CODE, BEGIN_DAY, END_DAY, DAY_CAPACITY, IS_ACTIVE) VALUES " +
                // 12R22.5 结构分配到所有机台
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '12R22.5', 'GM01', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '12R22.5', 'GM02', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '12R22.5', 'GM03', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '12R22.5', 'GM04', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '12R22.5', 'GM05', 1, 31, 120, 1), " +
                // 11R22.5 结构分配到所有机台
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '11R22.5', 'GM01', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '11R22.5', 'GM02', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '11R22.5', 'GM03', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '11R22.5', 'GM04', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '11R22.5', 'GM05', 1, 31, 120, 1), " +
                // 295/80R22.5 结构
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '295/80R22.5', 'GM01', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '295/80R22.5', 'GM02', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '295/80R22.5', 'GM03', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '295/80R22.5', 'GM04', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '295/80R22.5', 'GM05', 1, 31, 120, 1), " +
                // 275/80R22.5 结构
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '275/80R22.5', 'GM01', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '275/80R22.5', 'GM02', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '275/80R22.5', 'GM03', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '275/80R22.5', 'GM04', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '275/80R22.5', 'GM05', 1, 31, 120, 1), " +
                // 315/80R22.5 结构
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '315/80R22.5', 'GM01', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '315/80R22.5', 'GM02', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '315/80R22.5', 'GM03', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '315/80R22.5', 'GM04', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '315/80R22.5', 'GM05', 1, 31, 120, 1), " +
                // 385/65R22.5 结构
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '385/65R22.5', 'GM01', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '385/65R22.5', 'GM02', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '385/65R22.5', 'GM03', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '385/65R22.5', 'GM04', 1, 31, 120, 1), " +
                "('F001', YEAR(CURDATE()), MONTH(CURDATE()), '385/65R22.5', 'GM05', 1, 31, 120, 1)");
    }

    /**
     * 初始化月度计划产品硫化产能数据
     */
    private void initMdmMonthPlanProductLhData() {
        jdbcTemplate.execute("INSERT INTO T_MDM_MONTH_PLAN_PRODUCT_LH (FACTORY_CODE, MATERIAL_CODE, MATERIAL_DESC, MES_CAPACITY, STANDARD_CAPACITY, APS_CAPACITY, VULCANIZATION_TIME, TYPE) VALUES " +
                "('F001', 'MAT001', '12R22.5-18PR-JA511', 240, 240, 240, 750, 'NORMAL'), " +
                "('F001', 'MAT002', '11R22.5-16PR-JA511', 180, 180, 180, 708, 'NORMAL'), " +
                "('F001', 'MAT003', '295/80R22.5-18PR-JA511', 120, 120, 120, 792, 'NORMAL'), " +
                "('F001', 'MAT004', '275/80R22.5-16PR-JA511', 200, 200, 200, 690, 'NORMAL'), " +
                "('F001', 'MAT005', '315/80R22.5-18PR-JA511', 100, 100, 100, 840, 'NORMAL'), " +
                "('F001', 'MAT006', '385/65R22.5-20PR-JA511', 150, 150, 150, 930, 'NORMAL')");
    }

    /**
     * 初始化月度余量数据
     */
    private void initMdmMonthSurplusData() {
        jdbcTemplate.execute("INSERT INTO t_mdm_month_surplus (FACTORY_CODE, PRODUCT_TYPE_CODE, YEAR, MONTH, REQUIRE_VERSION, BRAND, STRUCTURE_NAME, MATERIAL_CODE, MATERIAL_DESC, PLAN_SURPLUS_QTY, IS_DELETE) VALUES " +
                "('F001', 'TBR', 2026, 4, 'V1.0', 'JA', '12R22.5', 'MAT001', '12R22.5-18PR-JA511', 500, 0), " +
                "('F001', 'TBR', 2026, 4, 'V1.0', 'JA', '11R22.5', 'MAT002', '11R22.5-16PR-JA511', 300, 0), " +
                "('F001', 'TBR', 2026, 4, 'V1.0', 'JA', '295/80R22.5', 'MAT003', '295/80R22.5-18PR-JA511', 400, 0), " +
                "('F001', 'TBR', 2026, 4, 'V1.0', 'JA', '275/80R22.5', 'MAT004', '275/80R22.5-16PR-JA511', 350, 0), " +
                "('F001', 'TBR', 2026, 4, 'V1.0', 'JA', '315/80R22.5', 'MAT005', '315/80R22.5-18PR-JA511', 200, 0), " +
                "('F001', 'TBR', 2026, 4, 'V1.0', 'JA', '385/65R22.5', 'MAT006', '385/65R22.5-20PR-JA511', 250, 0)");
    }

    /**
     * 初始化工作日历数据
     */
    private void initWorkCalendarData() {
        // 添加未来7天的工作日历数据
        jdbcTemplate.execute("INSERT INTO T_MDM_WORK_CALENDAR (PROC_CODE, YEAR, MONTH, DAY, PRODUCTION_DATE, FACTORY_CODE, ONE_SHIFT_FLAG, TWO_SHIFT_FLAG, THREE_SHIFT_FLAG, DAY_FLAG, RATE, CREATE_BY) VALUES " +
                "('CX', YEAR(CURDATE()), MONTH(CURDATE()), DAY(CURDATE()), CURDATE(), 'F001', '1', '1', '1', '1', 100, 'SYSTEM'), " +
                "('CX', YEAR(DATE_ADD(CURDATE(), INTERVAL 1 DAY)), MONTH(DATE_ADD(CURDATE(), INTERVAL 1 DAY)), DAY(DATE_ADD(CURDATE(), INTERVAL 1 DAY)), DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'F001', '1', '1', '1', '1', 100, 'SYSTEM'), " +
                "('CX', YEAR(DATE_ADD(CURDATE(), INTERVAL 2 DAY)), MONTH(DATE_ADD(CURDATE(), INTERVAL 2 DAY)), DAY(DATE_ADD(CURDATE(), INTERVAL 2 DAY)), DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'F001', '1', '1', '1', '1', 100, 'SYSTEM'), " +
                "('CX', YEAR(DATE_ADD(CURDATE(), INTERVAL 3 DAY)), MONTH(DATE_ADD(CURDATE(), INTERVAL 3 DAY)), DAY(DATE_ADD(CURDATE(), INTERVAL 3 DAY)), DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'F001', '1', '1', '1', '1', 100, 'SYSTEM'), " +
                "('CX', YEAR(DATE_ADD(CURDATE(), INTERVAL 4 DAY)), MONTH(DATE_ADD(CURDATE(), INTERVAL 4 DAY)), DAY(DATE_ADD(CURDATE(), INTERVAL 4 DAY)), DATE_ADD(CURDATE(), INTERVAL 4 DAY), 'F001', '1', '1', '1', '1', 100, 'SYSTEM'), " +
                "('CX', YEAR(DATE_ADD(CURDATE(), INTERVAL 5 DAY)), MONTH(DATE_ADD(CURDATE(), INTERVAL 5 DAY)), DAY(DATE_ADD(CURDATE(), INTERVAL 5 DAY)), DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'F001', '1', '1', '1', '1', 100, 'SYSTEM'), " +
                "('CX', YEAR(DATE_ADD(CURDATE(), INTERVAL 6 DAY)), MONTH(DATE_ADD(CURDATE(), INTERVAL 6 DAY)), DAY(DATE_ADD(CURDATE(), INTERVAL 6 DAY)), DATE_ADD(CURDATE(), INTERVAL 6 DAY), 'F001', '1', '1', '1', '1', 100, 'SYSTEM')");
    }
}
