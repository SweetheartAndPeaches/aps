/*
 Navicat Premium Data Transfer

 Source Server         : 双钱开发环境
 Source Server Type    : MySQL
 Source Server Version : 80100
 Source Host           : 192.168.2.124:3306
 Source Schema         : jy_aps

 Target Server Type    : MySQL
 Target Server Version : 80100
 File Encoding         : 65001

 Date: 03/04/2026 19:00:18
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_cx_close_out_range
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_close_out_range`;
CREATE TABLE `t_cx_close_out_range`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `RANGE_NAME` varchar(300) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '当前维护的范围系数名称描述',
  `CLOSE_OUT_RANGE_MINIMUM` decimal(8, 0) NULL DEFAULT NULL COMMENT '收尾范围(下限),一个范围区间只能有一个系数值，需要考虑范围不重叠',
  `CLOSE_OUT_RANGE_MAXIMUM` decimal(8, 0) NULL DEFAULT NULL COMMENT '收尾范围(上限)一个范围区间只能有一个系数值，需要考虑范围不重叠',
  `RANGE_VALUE` decimal(6, 1) NULL DEFAULT NULL COMMENT '系数值',
  `CREATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `CREATE_TIME` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `IS_DELETE` int NULL DEFAULT 0 COMMENT '删除标识（0未删除；1已删除）',
  `REMARK` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注说明字段',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `INDEXCX_CLOSE_OUT_RANGE`(`RANGE_NAME` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '成型收尾范围系数' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_cx_close_out_range
-- ----------------------------

-- ----------------------------
-- Table structure for t_cx_dispatcher_log
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_dispatcher_log`;
CREATE TABLE `t_cx_dispatcher_log`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应自增序列为：SEQ_T_CX_DISPATCHER_LOG',
  `SCHEDULE_ID` bigint NULL DEFAULT NULL COMMENT '排程ID，对应排产表的ID',
  `OPER_TYPE` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '操作类型：0--转机台、1--调量。对应数据字典：DISPATCHER_OPER_TYPE',
  `SCHEDULE_DATE` datetime NULL DEFAULT NULL COMMENT '排程日期',
  `SAP_CODE` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'SAP品号',
  `EMBRYO_CODE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胎胚代码',
  `EMBRYO_VERSION` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '0' COMMENT '胎胚版本',
  `BEFORE_LH_MACHINE_CODE` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '操作前硫化机台编号，多个逗号分割',
  `BEFORE_CX_MACHINE_CODE` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '操作前成型机台编号',
  `BEFORE_CLASS1_PLAN` decimal(8, 0) NULL DEFAULT NULL COMMENT '操作前一班计划数',
  `BEFORE_CLASS2_PLAN` decimal(8, 0) NULL DEFAULT NULL COMMENT '操作前二班计划数',
  `BEFORE_CLASS3_PLAN` decimal(8, 0) NULL DEFAULT NULL COMMENT '操作前三班计划数',
  `BEFORE_CLASS4_PLAN` decimal(8, 0) NULL DEFAULT NULL COMMENT '操作前次日一班计划数',
  `BEFORE_CLASS5_PLAN` decimal(8, 0) NULL DEFAULT NULL COMMENT '操作前次日二班计划数',
  `AFTER_LH_MACHINE_CODE` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '操作前硫化机台编号，多个逗号分割',
  `AFTER_CX_MACHINE_CODE` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '操作前成型机台编号',
  `AFTER_CLASS1_PLAN` decimal(8, 0) NULL DEFAULT NULL COMMENT '操作前一班计划数',
  `AFTER_CLASS2_PLAN` decimal(8, 0) NULL DEFAULT NULL COMMENT '操作前二班计划数',
  `AFTER_CLASS3_PLAN` decimal(8, 0) NULL DEFAULT NULL COMMENT '操作前三班计划数',
  `AFTER_CLASS4_PLAN` decimal(8, 0) NULL DEFAULT NULL COMMENT '操作前次日一班计划数',
  `AFTER_CLASS5_PLAN` decimal(8, 0) NULL DEFAULT NULL COMMENT '操作前次日二班计划数',
  `REMARK` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注',
  `IS_DELETE` int NULL DEFAULT NULL COMMENT '删除标识：0--正常，1-删除',
  `CREATE_BY` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建者',
  `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `UPDATE_BY` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '更新者',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `IDX_CX_DISPATCHER_DATE`(`SCHEDULE_DATE` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '成型调度员排程操作日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_cx_dispatcher_log
-- ----------------------------

-- ----------------------------
-- Table structure for t_cx_embryo_month_plan_surplus
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_embryo_month_plan_surplus`;
CREATE TABLE `t_cx_embryo_month_plan_surplus`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `MONTH_PLAN_VERSION` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '主计划版本号',
  `YEAR` varchar(4) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '主计划所属年份',
  `MONTH` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '主计划所属月份',
  `BOM_DATA_VERSION` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'BOM信息中所使用的版本',
  `MATERIAL_CODE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胎胚代码',
  `MONTH_PLAN_QTY` decimal(8, 0) NULL DEFAULT NULL COMMENT '月度计划量',
  `LAST_MONTH_STOCK` decimal(8, 0) NULL DEFAULT NULL COMMENT '胎胚月结库存，月结库存获取时更新到该字段',
  `MONTH_FINISH_QTY` decimal(8, 0) NULL DEFAULT NULL COMMENT '月度完成量',
  `MONTH_REMAIN_QTY` decimal(8, 0) NULL DEFAULT NULL COMMENT '月剩余量',
  `CREATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `CREATE_TIME` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `IS_DELETE` int NULL DEFAULT 0 COMMENT '删除标识（0未删除；1已删除）',
  `REMARK` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注说明字段',
  `DATA_SOURCE` varchar(8) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '数据来源：0：主计划；1:APS插单；插单数据主计划版本更新不进行删除',
  `FACTORY_CODE` char(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '分厂编号',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `IDX_CX_SULPLUS_INDEX`(`YEAR` ASC, `MONTH` ASC, `MATERIAL_CODE` ASC) USING BTREE,
  INDEX `IDX_CX_SULPLUS_INDEX_1`(`YEAR` ASC, `MONTH` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '成型工序胎胚计划量汇总表用于判断月度剩余量' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_cx_embryo_month_plan_surplus
-- ----------------------------

-- ----------------------------
-- Table structure for t_cx_holiday_setting
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_holiday_setting`;
CREATE TABLE `t_cx_holiday_setting`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `HOLIDAY_NAME` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '假日名称',
  `HOLIDAY_DAY` datetime NULL DEFAULT NULL COMMENT '假日日期',
  `REMARK` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注说明字段',
  `IS_DELETE` int NULL DEFAULT 0 COMMENT '删除标识（0未删除；1已删除）',
  `CREATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `CREATE_TIME` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `IDX_KEY_CX_HOLIDAY_SETTING`(`HOLIDAY_DAY` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '假日设定表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_cx_holiday_setting
-- ----------------------------

-- ----------------------------
-- Table structure for t_cx_key_product
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_key_product`;
CREATE TABLE `t_cx_key_product`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `EMBRYO_CODE` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '胎胚编码',
  `STRUCTURE_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '结构名称',
  `IS_ACTIVE` int NULL DEFAULT NULL COMMENT '是否启用：0-禁用 1-启用',
  `CREATE_BY` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `UPDATE_BY` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `REMARK` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `IS_DELETE` int NULL DEFAULT 0 COMMENT '删除标志：0-正常 1-已删除',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `IDX_EMBRYO_CODE`(`EMBRYO_CODE` ASC) USING BTREE COMMENT '胎胚编码索引',
  INDEX `IDX_STRUCTURE_NAME`(`STRUCTURE_NAME` ASC) USING BTREE COMMENT '结构名称索引',
  INDEX `IDX_IS_ACTIVE`(`IS_ACTIVE` ASC) USING BTREE COMMENT '启用状态索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '关键产品配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_cx_key_product
-- ----------------------------

-- ----------------------------
-- Table structure for t_cx_loss_setting
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_loss_setting`;
CREATE TABLE `t_cx_loss_setting`  (
  `ID` bigint NOT NULL AUTO_INCREMENT,
  `EMBRYO_CODE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胎胚',
  `LOSS_RATE` decimal(6, 2) NULL DEFAULT NULL COMMENT '损耗率',
  `MACHINE_CODE` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '成型机编号',
  `create_by` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '0' COMMENT '删除标识（0未删除；1已删除）',
  `remark` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注说明字段',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_cx_loss_setting
-- ----------------------------

-- ----------------------------
-- Table structure for t_cx_machine_info
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_machine_info`;
CREATE TABLE `t_cx_machine_info`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `MACHINE_NAME` varchar(60) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '机台名称',
  `MACHINE_CODE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '机台编号',
  `MACHINE_TYPE` varchar(60) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '机台名称',
  `PRODUCT_TYPE` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '机台类别：荷兰VMI一次法成型机245、荷兰VMI一次法成型机246、机电二次法12--18成型机、等类型。\r\n            数据字典维护',
  `DIMENSION_MINIMUM` decimal(6, 2) NULL DEFAULT NULL COMMENT '寸口范围下限',
  `DIMENSION_MAXIMUM` decimal(6, 2) NULL DEFAULT NULL COMMENT '寸口范围上限',
  `LH_GROUP` varchar(400) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注说明字段',
  `CLOTH_TYPE` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '机台类型,数据维护在数据字典；1=一次法；2=二次法',
  `CLASS_SHIFT` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '班制，如：三班制，两班制；对应数据字典CLASS_SHIFT',
  `QUATA` decimal(8, 0) NULL DEFAULT NULL COMMENT '生产定额，是指单班一次能生产的量，单位：条',
  `QUOTA_RATIO` decimal(6, 2) NULL DEFAULT 1.00 COMMENT '定额系数，配置系数，该机台定额都需要乘以该系数',
  `OPERATOR_QTY` decimal(3, 0) NULL DEFAULT NULL COMMENT '操作人员数量',
  `STATUS` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '机台状态，0--启用，1--禁用。对应数据字典STATUS',
  `CREATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `CREATE_TIME` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `DEL_FLAG` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '0' COMMENT '删除标识（0未删除；1已删除）',
  `REMARK` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注说明字段',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `INDEX_CX_MACHINE_INFO`(`MACHINE_CODE` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '成型机台信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_cx_machine_info
-- ----------------------------

-- ----------------------------
-- Table structure for t_cx_material_ending
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_material_ending`;
CREATE TABLE `t_cx_material_ending`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `FACTORY_CODE` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工厂编码',
  `MATERIAL_CODE` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '物料编码',
  `MATERIAL_DESC` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '物料描述',
  `STRUCTURE_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '结构名称',
  `VULCANIZING_REMAINDER` int NULL DEFAULT NULL COMMENT '硫化余量（条）-从月度计划余量表获取',
  `EMBRYO_STOCK` int NULL DEFAULT NULL COMMENT '胎胚库存（条）-使用有效库存',
  `FORMING_REMAINDER` int NULL DEFAULT NULL COMMENT '成型余量=硫化余量 - 胎胚库存',
  `DAILY_LH_CAPACITY` int NULL DEFAULT NULL COMMENT '日硫化产能（满算力，单位：条）',
  `DAILY_FORMING_CAPACITY` int NULL DEFAULT NULL COMMENT '日成型产能（单位：条）',
  `ESTIMATED_ENDING_DAYS` decimal(10, 2) NULL DEFAULT NULL COMMENT '预计收尾天数',
  `PLANNED_ENDING_DATE` date NULL DEFAULT NULL COMMENT '计划收尾日期',
  `IS_URGENT_ENDING` int NULL DEFAULT NULL COMMENT '是否紧急收尾（3 天内）',
  `IS_NEAR_ENDING` int NULL DEFAULT NULL COMMENT '是否 10 天内收尾',
  `DELAY_QUANTITY` int NULL DEFAULT NULL COMMENT '延误量（条）',
  `DISTRIBUTED_QUANTITY` int NULL DEFAULT NULL COMMENT '平摊到未来 3 天的量',
  `NEED_MONTH_PLAN_ADJUST` int NULL DEFAULT NULL COMMENT '是否需要调整月计划（0-否，1-是）',
  `STAT_DATE` date NULL DEFAULT NULL COMMENT '统计日期',
  `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `REMARK` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE INDEX `UK_MATERIAL_STAT_DATE`(`MATERIAL_CODE` ASC, `STAT_DATE` ASC) USING BTREE COMMENT '物料 + 统计日期唯一索引',
  INDEX `IDX_FACTORY_CODE`(`FACTORY_CODE` ASC) USING BTREE COMMENT '工厂编码索引',
  INDEX `IDX_STRUCTURE_NAME`(`STRUCTURE_NAME` ASC) USING BTREE COMMENT '结构名称索引',
  INDEX `IDX_IS_URGENT_ENDING`(`IS_URGENT_ENDING` ASC) USING BTREE COMMENT '紧急收尾标记索引',
  INDEX `IDX_IS_NEAR_ENDING`(`IS_NEAR_ENDING` ASC) USING BTREE COMMENT '临近收尾标记索引',
  INDEX `IDX_STAT_DATE`(`STAT_DATE` ASC) USING BTREE COMMENT '统计日期索引'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '物料收尾管理表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_cx_material_ending
-- ----------------------------

-- ----------------------------
-- Table structure for t_cx_month_stock
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_month_stock`;
CREATE TABLE `t_cx_month_stock`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `STOCK_MONTH` datetime NULL DEFAULT NULL COMMENT '库存所属月份：yyyy-mm',
  `BOM_DATA_VERSION` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'BOM信息中所使用的版本',
  `EMBRYO_CODE` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胎胚代码',
  `STOCK_NUM` decimal(10, 0) NULL DEFAULT NULL COMMENT '库存量',
  `OVER_TIME_STOCK` decimal(10, 0) NULL DEFAULT NULL COMMENT '超期库存',
  `REMARK` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注',
  `IS_DELETE` int NULL DEFAULT 0 COMMENT '删除标识：0--正常，1-删除',
  `CREATE_BY` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建者',
  `CREATE_TIME` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_BY` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '更新者',
  `UPDATE_TIME` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `FACTORY_CODE` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '分厂编号',
  INDEX `AK_KEY_CX_MONTH_STOCK`(`ID` ASC) USING BTREE,
  INDEX `INDEX_CX_MONTH_STOCK`(`STOCK_MONTH` ASC, `BOM_DATA_VERSION` ASC, `EMBRYO_CODE` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '每月1号早8库存数据，需要跟MES沟通是否可以提供' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_cx_month_stock
-- ----------------------------

-- ----------------------------
-- Table structure for t_cx_param_config
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_param_config`;
CREATE TABLE `t_cx_param_config`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `PARAM_CODE` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '参数编码',
  `PARAM_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '参数名称',
  `PARAM_VALUE` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '参数值',
  `IS_ACTIVE` int NULL DEFAULT NULL COMMENT '是否启用：0-禁用 1-启用',
  `REGULAR_EXPRESSION` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '参数值对应的正则表达式',
  `ERROR_TIPS` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '参数值根据正则表达式校验失败后的错误提示',
  `CREATE_BY` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `UPDATE_BY` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `REMARK` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `IS_DELETE` int NULL DEFAULT 0 COMMENT '删除标志：0-正常 1-已删除',
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE INDEX `UK_PARAM_CODE`(`PARAM_CODE` ASC) USING BTREE COMMENT '参数编码唯一索引',
  INDEX `IDX_IS_ACTIVE`(`IS_ACTIVE` ASC) USING BTREE COMMENT '启用状态索引'
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '排程参数配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_cx_param_config
-- ----------------------------
INSERT INTO `t_cx_param_config` VALUES (1, 'DAY_VULCANIZATION_MODE', '日硫化量计算模式', 'STANDARD_CAPACITY', 1, '^[A-Z_]+$', '参数值必须是大写字母和下划线组合', 'admin', '2026-04-03 16:05:44', '', NULL, '可选值：STANDARD_CAPACITY-标准产能，FULL_CAPACITY-满负荷产能', 0);
INSERT INTO `t_cx_param_config` VALUES (2, 'LOSS_RATE', '默认损耗率', '0.02', 1, '^0\\.[0-9]{1,2}$', '损耗率必须是 0 到 1 之间的小数', 'admin', '2026-04-03 16:05:44', '', NULL, '成型工序默认损耗率 2%', 0);

-- ----------------------------
-- Table structure for t_cx_params
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_params`;
CREATE TABLE `t_cx_params`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `PARAM_CODE` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '参数code',
  `PARAM_NAME` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '参数名称',
  `PARAM_VALUE` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '参数值',
  `REGULAR_EXPRESSION` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '参数值对应的正则表达式',
  `ERROR_TIPS` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '参数值根据正则表达式校验是失败后的错误提示',
  `CREATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `CREATE_TIME` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `IS_DELETE` int NULL DEFAULT 0 COMMENT '删除标识（0未删除；1已删除）',
  `REMARK` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注说明字段',
  `DEL_FLAG` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '删除标识',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `INDEX_CX_PARAMS`(`PARAM_CODE` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '成型参数信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_cx_params
-- ----------------------------

-- ----------------------------
-- Table structure for t_cx_persion_train_setting
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_persion_train_setting`;
CREATE TABLE `t_cx_persion_train_setting`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `SCHEDULE_DATE` datetime NULL DEFAULT NULL COMMENT '排程日期',
  `MOULD_METHOD` smallint NOT NULL DEFAULT 0 COMMENT '成型法:来源于数据字典molding_method',
  `QUOTA_CLASS1` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '多个使用/分割，1班机台-定额',
  `QUOTA_CLASS2` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '多个使用/分割，2班机台-定额',
  `QUOTA_CLASS3` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '多个使用/分割，3班机台-定额',
  `CREATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `CREATE_TIME` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `IS_DELETE` int NULL DEFAULT 0 COMMENT '删除标识（0未删除；1已删除）',
  `REMARK` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注说明字段',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `INDEX_CX_QUOTA_SETTING`(`SCHEDULE_DATE` ASC, `MOULD_METHOD` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 152 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '成型定额设定表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_cx_persion_train_setting
-- ----------------------------

-- ----------------------------
-- Table structure for t_cx_precision_plan
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_precision_plan`;
CREATE TABLE `t_cx_precision_plan`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `MACHINE_NAME` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '机台名称',
  `MACHINE_CODE` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '成型机台',
  `PLAN_DATE` date NOT NULL COMMENT '计划日期',
  `PLAN_SHIFT` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '班次(早班/中班)',
  `PLAN_START_TIME` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `PLAN_END_TIME` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `ESTIMATED_HOURS` decimal(3, 1) NULL DEFAULT 4.0 COMMENT '持续时间(小时)',
  `LAST_PRECISION_DATE` datetime NULL DEFAULT NULL COMMENT '上次精度日期',
  `DUE_DATE` datetime NULL DEFAULT NULL COMMENT '到期日期',
  `CREATE_BY` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `CREATE_TIME` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_BY` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '修改人',
  `UPDATE_TIME` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `IS_DELETE` decimal(1, 0) NULL DEFAULT 0 COMMENT '删除标识（0未删除；1已删除）',
  `REMARK` varchar(900) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `FACTORY_CODE` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工厂',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `IDX_T_CX_PRECISION_MACHINE`(`MACHINE_CODE` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '成型精度计划' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_cx_precision_plan
-- ----------------------------
INSERT INTO `t_cx_precision_plan` VALUES (1, 'H1504', 'H1504', '2026-04-07', '2', '2026-04-09 00:00:00', '2026-04-10 00:00:00', 24.0, '2026-03-30 00:00:00', '2026-04-23 00:00:00', 'admin', '2026-04-03 18:11:10', 'admin', '2026-04-03 18:32:33', 1, '', '116');
INSERT INTO `t_cx_precision_plan` VALUES (2, 'H1504', 'H1504', '2026-04-07', '2', '2026-04-09 00:00:00', '2026-04-10 00:00:00', 24.0, '2026-03-30 00:00:00', '2026-04-23 00:00:00', 'admin', '2026-04-03 18:32:58', 'admin', '2026-04-03 18:32:58', 1, '', '116');
INSERT INTO `t_cx_precision_plan` VALUES (3, 'H1504', 'H1504', '2026-04-07', '2', '2026-04-09 00:00:00', '2026-04-10 00:00:00', 24.0, '2026-03-30 00:00:00', '2026-04-23 00:00:00', 'admin', '2026-04-03 18:33:08', 'admin', '2026-04-03 18:33:08', 0, '', '116');

-- ----------------------------
-- Table structure for t_cx_product_stock_limit
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_product_stock_limit`;
CREATE TABLE `t_cx_product_stock_limit`  (
  `ID` bigint NOT NULL AUTO_INCREMENT,
  `LIMIT_NAME` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '限制描述',
  `TYPE` char(5) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胎胚类型',
  `LIMIT_TYPE` char(5) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '设定限制类型;1:库存上限；2：上限预警值；3：库存下限；4：下限预警值',
  `STOCK_NUM` int NULL DEFAULT NULL COMMENT '库存',
  `SHIFT_PARAMS` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '库位',
  `create_by` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '0' COMMENT '删除标识（0未删除；1已删除）',
  `remark` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注说明字段',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_cx_product_stock_limit
-- ----------------------------

-- ----------------------------
-- Table structure for t_cx_quota_setting
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_quota_setting`;
CREATE TABLE `t_cx_quota_setting`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `SCHEDULE_DATE` datetime NULL DEFAULT NULL COMMENT '排程日期',
  `MACHINE_NAME` varchar(60) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '机台名称',
  `MACHINE_CODE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '机台编号',
  `MACHINE_TYPE` varchar(60) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '机台名称',
  `PRODUCT_TYPE` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '机台类别：荷兰VMI一次法成型机245、荷兰VMI一次法成型机246、机电二次法12--18成型机、等类型。\r\n            数据字典维护',
  `TYPE` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '机型：荷兰VMI一次法成型机245、荷兰VMI一次法成型机246、机电二次法12--18成型机、等类型。',
  `QUOTA_MORNING` decimal(6, 2) NULL DEFAULT 1.00 COMMENT '定额系数，配置系数，该机台定额都需要乘以该系数',
  `QUOTA_NIGHT` decimal(6, 2) NULL DEFAULT 1.00 COMMENT '定额系数，配置系数，该机台定额都需要乘以该系数',
  `CREATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `CREATE_TIME` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `DEL_FLAG` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '0' COMMENT '删除标识（0未删除；1已删除）',
  `REMARK` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注说明字段',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `INDEX_CX_QUOTA_SETTING`(`MACHINE_CODE` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '成型定额设定表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_cx_quota_setting
-- ----------------------------

-- ----------------------------
-- Table structure for t_cx_schedule_limit
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_schedule_limit`;
CREATE TABLE `t_cx_schedule_limit`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `SCHEDULE_DATE` datetime NULL DEFAULT NULL COMMENT 'ID',
  `MACHINE_NAME` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '机台名称',
  `MACHINE_CODE` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '机台编号',
  `EMBRYO_CODE` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胎胚代码',
  `SPEC_DIMENSION` decimal(10, 2) NULL DEFAULT NULL COMMENT '外胎规格尺口',
  `MAX_STOCK` decimal(4, 2) NULL DEFAULT NULL COMMENT '最大备库库存',
  `CREATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `CREATE_TIME` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `IS_DELETE` int NULL DEFAULT 0 COMMENT '删除标识（0未删除；1已删除）',
  `REMARK` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注说明字段',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `INDEX_CX_SCHEDULE_LIMIT`(`MACHINE_CODE` ASC, `EMBRYO_CODE` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '成型排产库设存设定' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_cx_schedule_limit
-- ----------------------------

-- ----------------------------
-- Table structure for t_cx_schedule_result
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_schedule_result`;
CREATE TABLE `t_cx_schedule_result`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `CX_BATCH_NO` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '成型批次号',
  `ORDER_NO` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工单号',
  `PRODUCTION_STATUS` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生产状态：0-未生产；1-生产中；2-已收尾',
  `IS_RELEASE` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否发布：0--未发布，1--已发布',
  `SCHEDULE_DATE` datetime NULL DEFAULT NULL COMMENT '排程日期',
  `CX_MACHINE_CODE` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '成型机台编号',
  `CX_MACHINE_NAME` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '成型机台名称',
  `CX_MACHINE_TYPE` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '成型机台类型',
  `LH_SCHEDULE_IDS` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '硫化排程任务序号',
  `LH_MACHINE_CODE` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '硫化机台编号',
  `LH_MACHINE_NAME` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '硫化机台名称',
  `LH_MACHINE_QTY` decimal(10, 2) NULL DEFAULT NULL COMMENT '硫化机使用总模数',
  `SAP_CODE` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '外胎代码',
  `SPEC_DESC` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '外胎规格描述',
  `EMBRYO_CODE` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '胎胚代码',
  `SPEC_DIMENSION` decimal(10, 2) NULL DEFAULT NULL COMMENT '胎胚寸口',
  `STRUCTURE_NAME` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '结构',
  `TOTAL_STOCK` decimal(10, 2) NULL DEFAULT NULL COMMENT '胎胚库存',
  `BOM_DATA_VERSION` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '施工版本信息',
  `PRODUCT_NUM` decimal(10, 2) NULL DEFAULT NULL COMMENT '胎胚总计划量',
  `CLASS1_PLAN_QTY` decimal(10, 2) NULL DEFAULT NULL COMMENT '一班计划数',
  `CLASS1_ANALYSIS_INPUT` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '一班原因分析手工输入',
  `CLASS1_FINISH_QTY` decimal(10, 2) NULL DEFAULT NULL COMMENT '一班完成量',
  `CLASS1_ANALYSIS` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '一班原因分析',
  `CLASS2_PLAN_QTY` decimal(10, 2) NULL DEFAULT NULL COMMENT '二班计划数',
  `CLASS2_ANALYSIS_INPUT` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '二班原因分析手工输入',
  `CLASS2_FINISH_QTY` decimal(10, 2) NULL DEFAULT NULL COMMENT '二班完成量',
  `CLASS2_ANALYSIS` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '二班原因分析',
  `CLASS3_PLAN_QTY` decimal(10, 2) NULL DEFAULT NULL COMMENT '三班计划数',
  `CLASS3_ANALYSIS_INPUT` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '三班原因分析手工输入',
  `CLASS3_FINISH_QTY` decimal(10, 2) NULL DEFAULT NULL COMMENT '三班完成量',
  `CLASS3_ANALYSIS` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '三班原因分析',
  `CLASS4_PLAN_QTY` decimal(10, 2) NULL DEFAULT NULL COMMENT '四班计划数',
  `CLASS4_ANALYSIS_INPUT` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '四班原因分析手工输入',
  `CLASS4_FINISH_QTY` decimal(10, 2) NULL DEFAULT NULL COMMENT '四班完成量',
  `CLASS4_ANALYSIS` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '四班原因分析',
  `CLASS5_PLAN_QTY` decimal(10, 2) NULL DEFAULT NULL COMMENT '五班计划数',
  `CLASS5_ANALYSIS_INPUT` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '五班原因分析手工输入',
  `CLASS5_FINISH_QTY` decimal(10, 2) NULL DEFAULT NULL COMMENT '五班完成量',
  `CLASS5_ANALYSIS` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '五班原因分析',
  `CLASS6_PLAN_QTY` decimal(10, 2) NULL DEFAULT NULL COMMENT '六班计划数',
  `CLASS6_ANALYSIS_INPUT` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '六班原因分析手工输入',
  `CLASS6_FINISH_QTY` decimal(10, 2) NULL DEFAULT NULL COMMENT '六班完成量',
  `CLASS6_ANALYSIS` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '六班原因分析',
  `CLASS7_PLAN_QTY` decimal(10, 2) NULL DEFAULT NULL COMMENT '七班计划数',
  `CLASS7_ANALYSIS_INPUT` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '七班原因分析手工输入',
  `CLASS7_FINISH_QTY` decimal(10, 2) NULL DEFAULT NULL COMMENT '七班完成量',
  `CLASS7_ANALYSIS` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '七班原因分析',
  `CLASS8_PLAN_QTY` decimal(10, 2) NULL DEFAULT NULL COMMENT '八班计划数',
  `CLASS8_ANALYSIS_INPUT` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '八班原因分析手工输入',
  `CLASS8_FINISH_QTY` decimal(10, 2) NULL DEFAULT NULL COMMENT '八班完成量',
  `CLASS8_ANALYSIS` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '八班原因分析',
  `MARK_CLOSE_OUT_TIP` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '收尾提示标识：0-提示收尾；1-不需要提示',
  `DATA_SOURCE` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '数据来源：0-自动排程；1-插单；2-导入',
  `SPECIAL_REQUIREMENTS` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '特殊要求',
  `IS_DELETE` tinyint(1) NULL DEFAULT 0 COMMENT '删除标志：0-未删除 1-已删除',
  `CREATE_BY` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `UPDATE_BY` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `IDX_CX_BATCH_NO`(`CX_BATCH_NO` ASC) USING BTREE,
  INDEX `IDX_ORDER_NO`(`ORDER_NO` ASC) USING BTREE,
  INDEX `IDX_SCHEDULE_DATE`(`SCHEDULE_DATE` ASC) USING BTREE,
  INDEX `IDX_CX_MACHINE_CODE`(`CX_MACHINE_CODE` ASC) USING BTREE,
  INDEX `IDX_EMBRYO_CODE`(`EMBRYO_CODE` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '成型排程结果表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_cx_schedule_result
-- ----------------------------
INSERT INTO `t_cx_schedule_result` VALUES (24, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1503', NULL, NULL, NULL, NULL, NULL, NULL, '3302002318', '11R22.5 146/143L 16PR EAM68 BL4HEG', '215103975', NULL, 'T122', 22.00, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (25, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1201', NULL, NULL, NULL, NULL, NULL, NULL, '3302002160', '11R22.5 146/143L 16PR UA922 BL4HEU', '215103975', NULL, 'T122', 22.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (26, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1503', NULL, NULL, NULL, NULL, NULL, NULL, '3302001143', '11R22.5 146/143L 16PR JD571 BL4HJY', '215101729', NULL, 'T105', 34.00, NULL, NULL, 50.00, NULL, NULL, 'SPQT', 50.00, NULL, NULL, 'SPQT', 60.00, NULL, NULL, 'SPQT', 60.00, NULL, NULL, 'SPQT', 60.00, NULL, NULL, 'SPQT', 80.00, NULL, NULL, 'SPQT', 60.00, NULL, NULL, 'SPQT', NULL, NULL, NULL, 'SPQT', NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (27, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1201', NULL, NULL, NULL, NULL, NULL, NULL, '3302002169', '11R22.5 144/142L 14PR QD571 BL4HGQ', '215101729', NULL, 'T105', 34.00, NULL, NULL, NULL, NULL, NULL, 'SPQT', NULL, NULL, NULL, 'SPQT', NULL, NULL, NULL, 'SPQT', NULL, NULL, NULL, 'SPQT', NULL, NULL, NULL, 'SPQT', NULL, NULL, NULL, 'SPQT', 20.00, NULL, NULL, 'SPQT', 60.00, NULL, NULL, 'SPQT', NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (28, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1201', NULL, NULL, NULL, NULL, NULL, NULL, '3302002245', '11R22.5 146/143L 16PR EAR539 BL4EEG', '215101828', NULL, 'T105', 22.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (29, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1503', NULL, NULL, NULL, NULL, NULL, NULL, '3302001067', '11R22.5 146/143L 16PR AF508 BL4HAM', '215101828', NULL, 'T105', 22.00, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (30, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1503', NULL, NULL, NULL, NULL, NULL, NULL, '3302001546', '11R22.5 144/142M 14PR AT159 BL4HAM', '215102719', NULL, 'T105', 20.00, NULL, NULL, 12.00, NULL, NULL, NULL, 22.00, NULL, NULL, NULL, 6.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (31, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1201', NULL, NULL, NULL, NULL, NULL, NULL, '3302001556', '11R22.5 146/143L 16PR BD170 BL4HBL', '215102643', NULL, 'T105', 0.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (32, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1503', NULL, NULL, NULL, NULL, NULL, NULL, '3302002479', '11R22.5 146/143L 16PR JD570 BL4HJY', '215102643', NULL, 'T105', 0.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (33, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1405', NULL, NULL, NULL, NULL, NULL, NULL, '', '11R24.5 146/143L 14PR JD571 BL4HJY', '215101731', NULL, 'T105', 25.00, NULL, NULL, 50.00, NULL, NULL, 'SPQT ', 50.00, NULL, NULL, 'SPQT ', 40.00, NULL, NULL, 'SPQT ', 50.00, NULL, NULL, 'SPQT ', 50.00, NULL, NULL, 'SPQT ', 50.00, NULL, NULL, 'SPQT ', 50.00, NULL, NULL, 'SPQT ', 40.00, NULL, NULL, 'SPQT ', NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (34, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1405', NULL, NULL, NULL, NULL, NULL, NULL, '3302001243', '11R24.5 149/146L 16PR AD515 BL4HAM', '215101888', NULL, 'T105', 15.00, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (35, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1405', NULL, NULL, NULL, NULL, NULL, NULL, '3302002094', '11R24.5 149/146L 16PR UF195 BL4HEU', '215101734', NULL, 'T105', 22.00, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 60.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 60.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (36, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1102', NULL, NULL, NULL, NULL, NULL, NULL, '3302002343', '12R22.5 152/149L 18PR BF188 BL0HBL', '215103997', NULL, 'T105', 23.00, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (37, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1102', NULL, NULL, NULL, NULL, NULL, NULL, '3302002344', '12R22.5 152/149L 18PR JF518 BL0HJY', '215103998', NULL, 'T105', 0.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (38, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1102', NULL, NULL, NULL, NULL, NULL, NULL, '3302002311', '12R22.5 152/149L 18PR BD175 BL0HBL', '215103962', NULL, 'T105', 16.00, NULL, NULL, 20.00, NULL, NULL, NULL, 12.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (39, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1102', NULL, NULL, NULL, NULL, NULL, NULL, '3302001245', '12R22.5 152/149K 18PR BA220 BL0HBL', '215101166', NULL, 'T122', 0.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (40, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1102', NULL, NULL, NULL, NULL, NULL, NULL, '3302000193', '12R22.5 152/149K 18PR JD755 BL0HJY', '215101178', NULL, 'T122', 17.00, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 13.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (41, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1102', NULL, NULL, NULL, NULL, NULL, NULL, '3302000196', '12R22.5 152/149K 18PR BD210 BL0HBL', '215101180', NULL, 'T122', 17.00, NULL, NULL, 9.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (42, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1401', NULL, NULL, NULL, NULL, NULL, NULL, '3302002568', '215/75R17.5 128/126M 16PR JF568 BL3EJY DL', '215102582', NULL, 'T105', 33.00, NULL, NULL, 75.00, NULL, NULL, NULL, 75.00, NULL, NULL, NULL, 90.00, NULL, NULL, NULL, 75.00, NULL, NULL, NULL, 90.00, NULL, NULL, NULL, 90.00, NULL, NULL, NULL, 75.00, NULL, NULL, NULL, 105.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (43, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1401', NULL, NULL, NULL, NULL, NULL, NULL, '3302002566', '215/75R17.5 128/126M 16PR JD575 BL3EJY DL', '215101222', NULL, 'T105', 0.00, NULL, NULL, NULL, NULL, NULL, NULL, 15.00, NULL, NULL, NULL, 15.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 15.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 15.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (44, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1401', NULL, NULL, NULL, NULL, NULL, NULL, '3302002531', '215/75R17.5 128/126M 16PR EDR50 BL3EEG DL', '215103006', NULL, 'T105', 8.00, NULL, NULL, 15.00, NULL, NULL, NULL, 15.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 15.00, NULL, NULL, NULL, 21.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (45, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1402', NULL, NULL, NULL, NULL, NULL, NULL, '3302001162', '245/70R19.5 144/142J 18PR BF188 BL3EBL', '215101744', NULL, 'T101', 18.00, NULL, NULL, 28.00, NULL, NULL, NULL, 28.00, NULL, NULL, NULL, 42.00, NULL, NULL, NULL, 42.00, NULL, NULL, NULL, 28.00, NULL, NULL, NULL, 28.00, NULL, NULL, NULL, 42.00, NULL, NULL, NULL, 42.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (46, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1402', NULL, NULL, NULL, NULL, NULL, NULL, '3302001161', '245/70R19.5 144/142J 18PR JF518 BL3EJY', '215101743', NULL, 'T105', 14.00, NULL, NULL, 28.00, NULL, NULL, NULL, 14.00, NULL, NULL, NULL, 28.00, NULL, NULL, NULL, 14.00, NULL, NULL, NULL, 28.00, NULL, NULL, NULL, 14.00, NULL, NULL, NULL, 28.00, NULL, NULL, NULL, 14.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (47, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1402', NULL, NULL, NULL, NULL, NULL, NULL, '3302001573', '245/70R19.5 144/142J 18PR BD175 BL3EBL', '215102642', NULL, 'T101', 19.00, NULL, NULL, 28.00, NULL, NULL, NULL, 14.00, NULL, NULL, NULL, 28.00, NULL, NULL, NULL, 14.00, NULL, NULL, NULL, 14.00, NULL, NULL, NULL, 28.00, NULL, NULL, NULL, 14.00, NULL, NULL, NULL, 28.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (48, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1402', NULL, NULL, NULL, NULL, NULL, NULL, '3302001205', '245/70R19.5 144/142J 18PR JD575 BL3EJY', '215101783', NULL, 'T101', 7.00, NULL, NULL, 14.00, NULL, NULL, NULL, 28.00, NULL, NULL, NULL, 14.00, NULL, NULL, NULL, 28.00, NULL, NULL, NULL, 14.00, NULL, NULL, NULL, 28.00, NULL, NULL, NULL, 14.00, NULL, NULL, NULL, 28.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (49, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1503', NULL, NULL, NULL, NULL, NULL, NULL, '3302001311', '255/70R22.5 140/137M 16PR AT505 BL4HAM', '215102615', NULL, 'T105', 0.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (50, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1202', NULL, NULL, NULL, NULL, NULL, NULL, '3302002209', '295/75R22.5 146/143L 16PR QD571 BL4HGQ', '215101611', NULL, 'T105', 85.00, NULL, NULL, 60.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 60.00, NULL, NULL, NULL, 60.00, NULL, NULL, NULL, 70.00, NULL, NULL, NULL, 60.00, NULL, NULL, NULL, 60.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (51, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1201', NULL, NULL, NULL, NULL, NULL, NULL, '3302002282', '295/75R22.5 146/143L 16PR EDL15 BL4EEG', '215101611', NULL, 'T105', 85.00, NULL, NULL, 10.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (52, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1202', NULL, NULL, NULL, NULL, NULL, NULL, '3302002499', '295/75R22.5 146/143M 16PR AT502 BL4HAM FE', '215102780', NULL, 'T105', 16.00, NULL, NULL, 30.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (53, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1202', NULL, NULL, NULL, NULL, NULL, NULL, '3302001071', '295/75R22.5 146/143L 16PR AF508 BL4HAM', '215102568', NULL, 'T105', 83.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 60.00, NULL, NULL, NULL, 60.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (54, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1201', NULL, NULL, NULL, NULL, NULL, NULL, '3302002497', '295/75R22.5 144/141L 14PR QF568 BL4HGQ FE', '215102568', NULL, 'T105', 83.00, NULL, NULL, 80.00, NULL, NULL, NULL, 70.00, NULL, NULL, NULL, 70.00, NULL, NULL, NULL, 80.00, NULL, NULL, NULL, 70.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (55, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1202', NULL, NULL, NULL, NULL, NULL, NULL, '3302001654', '295/75R22.5 146/143L 16PR AD170 BL4HAM', '215102644', NULL, 'T105', 15.00, NULL, NULL, 21.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (56, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1501', NULL, NULL, NULL, NULL, NULL, NULL, '3302002053', '295/80R22.5 152/149L 18PR AA267 BL4HAM', '215103130', NULL, 'T122', 6.00, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (57, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1204', NULL, NULL, NULL, NULL, NULL, NULL, '3302001139', '295/80R22.5 152/149J 18PR JD756 BL4HJY', '215101726', NULL, 'T133', 26.00, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (58, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1204', NULL, NULL, NULL, NULL, NULL, NULL, '3302001585', '295/80R22.5 152/149M 18PR BF188 BL4EBL', '215102626', NULL, 'T105', 269.00, NULL, NULL, 90.00, NULL, NULL, 'SPQT', 90.00, NULL, NULL, 'SPQT', 90.00, NULL, NULL, 'SPQT', 80.00, NULL, NULL, 'SPQT', 80.00, NULL, NULL, 'SPQT', 90.00, NULL, NULL, 'SPQT', 80.00, NULL, NULL, 'SPQT', 80.00, NULL, NULL, 'SPQT', NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (59, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1105', NULL, NULL, NULL, NULL, NULL, NULL, '3302002061', '295/80R22.5 154/149M 18PR ESL01 BL4EEG', '215102626', NULL, 'T105', 269.00, NULL, NULL, 80.00, NULL, NULL, 'SPQT', 80.00, NULL, NULL, 'SPQT', 80.00, NULL, NULL, 'SPQT', 90.00, NULL, NULL, 'SPQT', 100.00, NULL, NULL, 'SPQT', 110.00, NULL, NULL, 'SPQT', 100.00, NULL, NULL, 'SPQT', 100.00, NULL, NULL, 'SPQT', NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (60, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1501', NULL, NULL, NULL, NULL, NULL, NULL, '3302001404', '295/80R22.5 154/149M 18PR JF568 BL4EJY', '215102626', NULL, 'T105', 269.00, NULL, NULL, 80.00, NULL, NULL, 'SPQT', 80.00, NULL, NULL, 'SPQT', 80.00, NULL, NULL, 'SPQT', 80.00, NULL, NULL, 'SPQT', 60.00, NULL, NULL, 'SPQT', 70.00, NULL, NULL, 'SPQT', 80.00, NULL, NULL, 'SPQT', 70.00, NULL, NULL, 'SPQT', NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (61, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1105', NULL, NULL, NULL, NULL, NULL, NULL, '3302002417', '295/80R22.5 154/149M 18PR EAR30 BL4EEG', '215104191', NULL, 'T105', 24.00, NULL, NULL, 40.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (62, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1304', NULL, NULL, NULL, NULL, NULL, NULL, '3302002218', '295/80R22.5 154/149M 18PR UF195 BL4HEU', '215103740', NULL, 'T105', 12.00, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (63, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1105', NULL, NULL, NULL, NULL, NULL, NULL, '3302002059', '295/80R22.5 154/149L 18PR EDL11 BL4EEG', '215102624', NULL, 'T105', 0.00, NULL, NULL, 10.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (64, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1304', NULL, NULL, NULL, NULL, NULL, NULL, '3302000750', '295/80R22.5 152/149M 18PR BD175 BL4EBL', '215101470', NULL, 'T105', 120.00, NULL, NULL, 70.00, NULL, NULL, NULL, 80.00, NULL, NULL, NULL, 80.00, NULL, NULL, NULL, 80.00, NULL, NULL, NULL, 80.00, NULL, NULL, NULL, 60.00, NULL, NULL, NULL, 90.00, NULL, NULL, NULL, 90.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (65, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1501', NULL, NULL, NULL, NULL, NULL, NULL, '3302001002', '295/80R22.5 152/149M 18PR AD506 BL4EAM', '215101470', NULL, 'T105', 120.00, NULL, NULL, 40.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (66, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1304', NULL, NULL, NULL, NULL, NULL, NULL, '3302000442', '295/80R22.5 152/149L 18PR JD575 BL4EJY', '215101325', NULL, 'T105', 0.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (67, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1304', NULL, NULL, NULL, NULL, NULL, NULL, '3302002060', '295/80R22.5 154/149L 18PR EDR51 BL4EEG', '215103003', NULL, 'T105', 51.00, NULL, NULL, 50.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 27.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (68, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1204', NULL, NULL, NULL, NULL, NULL, NULL, '3302002217', '295/80R22.5 154/149L 18PR UD188 BL4HEU', '215103741', NULL, 'T105', 27.00, NULL, NULL, 40.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (69, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1404', NULL, NULL, NULL, NULL, NULL, NULL, '3302001587', '315/60R22.5 152/148L 18PR BD177 BL4EBL', '215102627', NULL, 'T122', 0.00, NULL, NULL, 4.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (70, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1404', NULL, NULL, NULL, NULL, NULL, NULL, '3302001587', '315/60R22.5 152/148L 18PR BD177 BL4EBL', '215102628', NULL, 'T122', 44.00, NULL, NULL, 54.00, NULL, NULL, NULL, 45.00, NULL, NULL, NULL, 45.00, NULL, NULL, NULL, 54.00, NULL, NULL, NULL, 45.00, NULL, NULL, NULL, 45.00, NULL, NULL, NULL, 54.00, NULL, NULL, NULL, 45.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (71, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1404', NULL, NULL, NULL, NULL, NULL, NULL, '3302002070', '315/60R22.5 154/150L 18PR BF188 BL4EBL', '215103396', NULL, 'T122', 18.00, NULL, NULL, 18.00, NULL, NULL, NULL, 18.00, NULL, NULL, NULL, 18.00, NULL, NULL, NULL, 18.00, NULL, NULL, NULL, 18.00, NULL, NULL, NULL, 18.00, NULL, NULL, NULL, 18.00, NULL, NULL, NULL, 18.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (72, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1404', NULL, NULL, NULL, NULL, NULL, NULL, '3302002070', '315/60R22.5 154/150L 18PR BF188 BL4EBL', '215103396', NULL, 'T122', 18.00, NULL, NULL, 4.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (73, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1404', NULL, NULL, NULL, NULL, NULL, NULL, '3302002071', '315/60R22.5 154/150L 18PR JF518 BL4EJY', '215103395', NULL, 'T122', 0.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 18.00, NULL, NULL, NULL, 18.00, NULL, NULL, NULL, 18.00, NULL, NULL, NULL, 18.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (74, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1404', NULL, NULL, NULL, NULL, NULL, NULL, '3302000831', '315/60R22.5 152/148L 18PR JD575 BL4EJY', '215101520', NULL, 'T122', 20.00, NULL, NULL, 18.00, NULL, NULL, NULL, 18.00, NULL, NULL, NULL, 18.00, NULL, NULL, NULL, 18.00, NULL, NULL, NULL, 18.00, NULL, NULL, NULL, 18.00, NULL, NULL, NULL, 18.00, NULL, NULL, NULL, 18.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (75, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1303', NULL, NULL, NULL, NULL, NULL, NULL, '3302001590', '315/70R22.5 156/150L 18PR BF188 BL4EBL', '215102632', NULL, 'T105', 95.00, NULL, NULL, 80.00, NULL, NULL, NULL, 80.00, NULL, NULL, NULL, 90.00, NULL, NULL, NULL, 100.00, NULL, NULL, NULL, 100.00, NULL, NULL, NULL, 100.00, NULL, NULL, NULL, 100.00, NULL, NULL, NULL, 100.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (76, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1303', NULL, NULL, NULL, NULL, NULL, NULL, '3302001589', '315/70R22.5 156/150L 18PR BD177 BL4EBL', '215102631', NULL, 'T105', 35.00, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (77, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1301', NULL, NULL, NULL, NULL, NULL, NULL, '3302001716', '315/70R22.5 156/150L 18PR EDL11 BL4EEG', '215102631', NULL, 'T105', 35.00, NULL, NULL, 30.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (78, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1301', NULL, NULL, NULL, NULL, NULL, NULL, '3302002705', '315/70R22.5 156/150L 18PR EDR53 BL4EEG', '215101335', NULL, 'T105', 90.00, NULL, NULL, 80.00, NULL, NULL, 'SPQT', 80.00, NULL, NULL, 'SPQT', 80.00, NULL, NULL, 'SPQT', 80.00, NULL, NULL, 'SPQT', 80.00, NULL, NULL, 'SPQT', 80.00, NULL, NULL, 'SPQT', 80.00, NULL, NULL, 'SPQT', 80.00, NULL, NULL, 'SPQT', NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (79, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1203', NULL, NULL, NULL, NULL, NULL, NULL, '3302001717', '315/70R22.5 156/150L 18PR EDR51 BL4EEG', '215102830', NULL, 'T105', 22.00, NULL, NULL, 40.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (80, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1203', NULL, NULL, NULL, NULL, NULL, NULL, '3302000467', '315/70R22.5 156/150L 18PR BD175 BL4EBL', '215101337', NULL, 'T105', 58.00, NULL, NULL, 100.00, NULL, NULL, NULL, 90.00, NULL, NULL, NULL, 100.00, NULL, NULL, NULL, 90.00, NULL, NULL, NULL, 100.00, NULL, NULL, NULL, 100.00, NULL, NULL, NULL, 90.00, NULL, NULL, NULL, 100.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (81, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1203', NULL, NULL, NULL, NULL, NULL, NULL, '3302001206', '315/70R22.5 156/150L 18PR BD165 BL4EBL', '215101336', NULL, 'T105', 9.00, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (82, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1301', NULL, NULL, NULL, NULL, NULL, NULL, '3302002348', '315/70R22.5 156/150L 18PR BW292 BL4EBL', '215101922', NULL, 'T601', 39.00, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (83, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1203', NULL, NULL, NULL, NULL, NULL, NULL, '3302002363', '315/70R22.5 156/150L 18PR EDW85 BL4EEG', '215104041', NULL, 'T601', 0.00, NULL, NULL, 2.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (84, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1305', NULL, NULL, NULL, NULL, NULL, NULL, '3302002305', '315/80R22.5 156/150J 20PR BD280 BL0EBL DL', '215101401', NULL, 'T133', 51.00, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (85, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1305', NULL, NULL, NULL, NULL, NULL, NULL, '3302002325', '315/80R22.5 156/150L 20PR BA220 BL0EBL DL', '215103782', NULL, 'T122', 27.00, NULL, NULL, 10.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (86, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1305', NULL, NULL, NULL, NULL, NULL, NULL, '3302000915', '315/80R22.5 156/153K 20PR JD755 BL0EJY', '215101545', NULL, 'T133', 25.00, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (87, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1305', NULL, NULL, NULL, NULL, NULL, NULL, '3302000921', '315/80R22.5 156/153K 20PR BD210 BL0EBL', '215101548', NULL, 'T133', 25.00, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (88, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1403', NULL, NULL, NULL, NULL, NULL, NULL, '3302000611', '315/80R22.5 156/153L 20PR EG801 BL0EEG', '215101411', NULL, 'T105', 26.00, NULL, NULL, 40.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (89, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1403', NULL, NULL, NULL, NULL, NULL, NULL, '3302000995', '315/80R22.5 156/153L 20PR BD175 BL0EBL', '215101595', NULL, 'T105', 0.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (90, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1403', NULL, NULL, NULL, NULL, NULL, NULL, '3302001446', '315/80R22.5 156/153K 20PR JW592 BL0EJY', '215102417', NULL, 'T601', 39.00, NULL, NULL, 30.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, 30.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (91, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1104', NULL, NULL, NULL, NULL, NULL, NULL, '3302001236', '315/80R22.5 156/153L 20PR JF518 BL4EJY', '215101814', NULL, 'T105', 58.00, NULL, NULL, 50.00, NULL, NULL, 'SPQT', 50.00, NULL, NULL, 'SPQT', 50.00, NULL, NULL, 'SPQT', 50.00, NULL, NULL, 'SPQT', 40.00, NULL, NULL, 'SPQT', 50.00, NULL, NULL, 'SPQT', 50.00, NULL, NULL, 'SPQT', 50.00, NULL, NULL, 'SPQT', NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (92, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1104', NULL, NULL, NULL, NULL, NULL, NULL, '3302000795', '315/80R22.5 156/150L 18PR JD565 BL4EJY', '215101489', NULL, 'T105', 0.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (93, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1104', NULL, NULL, NULL, NULL, NULL, NULL, '3302000787', '315/80R22.5 156/153K 20PR JD575 BL4EJY', '215101486', NULL, 'T105', 16.00, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (94, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1104', NULL, NULL, NULL, NULL, NULL, NULL, '3302002332', '315/80R22.5 156/150L 20PR EDR51 BL4EEG DL', '215103353', NULL, 'T105', 9.00, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (95, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1104', NULL, NULL, NULL, NULL, NULL, NULL, '3302000609', '315/80R22.5 156/153L 20PR BD175 BL4EBL', '215101407', NULL, 'T105', 46.00, NULL, NULL, 50.00, NULL, NULL, 'SPQT', 40.00, NULL, NULL, 'SPQT', 50.00, NULL, NULL, 'SPQT', 50.00, NULL, NULL, 'SPQT', 50.00, NULL, NULL, 'SPQT', 40.00, NULL, NULL, 'SPQT', 50.00, NULL, NULL, 'SPQT', 50.00, NULL, NULL, 'SPQT', NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (96, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1103', NULL, NULL, NULL, NULL, NULL, NULL, '3202000565', '325/95R24 162/160K 22PR JD727 BT0HJY', '215102640', NULL, 'T122', 42.00, NULL, NULL, 36.00, NULL, NULL, NULL, 45.00, NULL, NULL, NULL, 45.00, NULL, NULL, NULL, 36.00, NULL, NULL, NULL, 45.00, NULL, NULL, NULL, 45.00, NULL, NULL, NULL, 36.00, NULL, NULL, NULL, 45.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (97, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1103', NULL, NULL, NULL, NULL, NULL, NULL, '3202000220', '325/95R24 162/160K 22PR JA661 BT0HJY', '215100460', NULL, 'T122', 21.00, NULL, NULL, 18.00, NULL, NULL, NULL, 18.00, NULL, NULL, NULL, 36.00, NULL, NULL, NULL, 36.00, NULL, NULL, NULL, 27.00, NULL, NULL, NULL, 27.00, NULL, NULL, NULL, 36.00, NULL, NULL, NULL, 36.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (98, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1103', NULL, NULL, NULL, NULL, NULL, NULL, '3202000564', '325/95R24 162/160K 22PR JA526 BT0HJY', '215102639', NULL, 'T122', 25.00, NULL, NULL, 36.00, NULL, NULL, NULL, 36.00, NULL, NULL, NULL, 27.00, NULL, NULL, NULL, 36.00, NULL, NULL, NULL, 36.00, NULL, NULL, NULL, 36.00, NULL, NULL, NULL, 36.00, NULL, NULL, NULL, 27.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (99, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1103', NULL, NULL, NULL, NULL, NULL, NULL, '3202000570', '325/95R24 162/160K 22PR UD223 BT0HEU', '215102737', NULL, 'T122', 13.00, NULL, NULL, 17.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (100, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1505', NULL, NULL, NULL, NULL, NULL, NULL, '3302000745', '385/55R22.5 160K 20PR BT160 BL4EBL', '215101523', NULL, 'T105', 4.00, NULL, NULL, 80.00, NULL, NULL, 'SPQT ', 80.00, NULL, NULL, 'SPQT ', 80.00, NULL, NULL, 'SPQT ', 80.00, NULL, NULL, 'SPQT ', 70.00, NULL, NULL, 'SPQT ', 80.00, NULL, NULL, 'SPQT ', 80.00, NULL, NULL, 'SPQT ', 80.00, NULL, NULL, 'SPQT ', NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (101, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1505', NULL, NULL, NULL, NULL, NULL, NULL, '3302002278', '385/55R22.5 160K 20PR BF196 BL4EBL', '215103930', NULL, 'T105', 9.00, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (102, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1505', NULL, NULL, NULL, NULL, NULL, NULL, '3302002676', '385/55R22.5 160K 20PR BW293 BL4EBL', '215104811', NULL, 'T601', 6.00, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (103, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1504', NULL, NULL, NULL, NULL, NULL, NULL, '3302001888', '385/65R22.5 164K 24PR ETL23 BL4EEG', '215101878', NULL, 'T105', 33.00, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 60.00, NULL, NULL, NULL, 70.00, NULL, NULL, NULL, 60.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (104, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1302', NULL, NULL, NULL, NULL, NULL, NULL, '3302001321', '385/65R22.5 164K 24PR AT502 BL4EAM', '215101879', NULL, 'T105', 32.00, NULL, NULL, 50.00, NULL, NULL, 'SPQT', 50.00, NULL, NULL, 'SPQT', 50.00, NULL, NULL, 'SPQT', 50.00, NULL, NULL, 'SPQT', 50.00, NULL, NULL, 'SPQT', 60.00, NULL, NULL, 'SPQT', 70.00, NULL, NULL, 'SPQT', 60.00, NULL, NULL, 'SPQT', NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (105, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1504', NULL, NULL, NULL, NULL, NULL, NULL, '3302001313', '385/65R22.5 164K 24PR JY598 BL4EJY', '215101877', NULL, 'T105', 105.00, NULL, NULL, 60.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 60.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (106, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1502', NULL, NULL, NULL, NULL, NULL, NULL, '3302002641', '385/65R22.5 164K 24PR BT169 BL4EBL', '215101877', NULL, 'T105', 105.00, NULL, NULL, 60.00, NULL, NULL, NULL, 60.00, NULL, NULL, NULL, 60.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 60.00, NULL, NULL, NULL, 60.00, NULL, NULL, NULL, 70.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (107, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1302', NULL, NULL, NULL, NULL, NULL, NULL, '3302001316', '385/65R22.5 164K 24PR BT180 BL4EBL', '215101880', NULL, 'T105', 66.00, NULL, NULL, 50.00, NULL, NULL, NULL, 60.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 60.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (108, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1502', NULL, NULL, NULL, NULL, NULL, NULL, '3302001322', '385/65R22.5 164K 24PR AT503 BL4EAM', '215101880', NULL, 'T105', 66.00, NULL, NULL, 50.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 50.00, NULL, NULL, NULL, 40.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (109, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1504', NULL, NULL, NULL, NULL, NULL, NULL, '3302002215', '385/65R22.5 160K 20PR QA626 BL4HGQ', '215102793', NULL, 'T122', 12.00, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (110, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1302', NULL, NULL, NULL, NULL, NULL, NULL, '3302002366', '385/65R22.5 164K 24PR JW593 BL4EJY', '215104050', NULL, 'T601', 8.00, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (111, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1502', NULL, NULL, NULL, NULL, NULL, NULL, '3302002413', '385/65R22.5 164K 24PR EAW86 BL4EEG', '215104169', NULL, 'T601', 18.00, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (112, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1205', NULL, NULL, NULL, NULL, NULL, NULL, '3302001271', '385/65R22.5 164K 24PR JY598 BL0EJY', '215101838', NULL, 'T122', 73.00, NULL, NULL, 70.00, NULL, NULL, NULL, 60.00, NULL, NULL, NULL, 70.00, NULL, NULL, NULL, 70.00, NULL, NULL, NULL, 70.00, NULL, NULL, NULL, 80.00, NULL, NULL, NULL, 80.00, NULL, NULL, NULL, 70.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (113, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1101', NULL, NULL, NULL, NULL, NULL, NULL, '3302002527', '385/65R22.5 164K 24PR ETL23 BL0EEG', '215101837', NULL, 'T105', 141.00, NULL, NULL, 110.00, NULL, NULL, 'SPQT', 100.00, NULL, NULL, 'SPQT', 100.00, NULL, NULL, 'SPQT', 110.00, NULL, NULL, 'SPQT', 100.00, NULL, NULL, 'SPQT', 90.00, NULL, NULL, 'SPQT', 100.00, NULL, NULL, 'SPQT', 100.00, NULL, NULL, 'SPQT', NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (114, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1205', NULL, NULL, NULL, NULL, NULL, NULL, '3302001317', '385/65R22.5 164K 24PR BT160 BL0EBL', '215101837', NULL, 'T105', 141.00, NULL, NULL, 20.00, NULL, NULL, 'SPQT', 20.00, NULL, NULL, 'SPQT', 20.00, NULL, NULL, 'SPQT', 20.00, NULL, NULL, 'SPQT', NULL, NULL, NULL, 'SPQT', NULL, NULL, NULL, 'SPQT', NULL, NULL, NULL, 'SPQT', NULL, NULL, NULL, 'SPQT', NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);
INSERT INTO `t_cx_schedule_result` VALUES (115, NULL, NULL, NULL, NULL, '2026-03-27 00:00:00', 'H1205', NULL, NULL, NULL, NULL, NULL, NULL, '3302002542', '385/65R22.5 164K 24PR BA226 BL0EBL', '215104472', NULL, 'T122', 8.00, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 10.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, 20.00, NULL, NULL, NULL, NULL, '2', NULL, 0, NULL, '2026-04-03 17:40:28', NULL, NULL);

-- ----------------------------
-- Table structure for t_cx_schedule_result_ref
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_schedule_result_ref`;
CREATE TABLE `t_cx_schedule_result_ref`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `CX_BATCH_NO` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '自动排程批次号信息，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号',
  `FACTORY_CODE` char(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '分厂编号',
  `ORDER_NO` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '成型排程工单号，自动生成，批次号+4位定长自增序号',
  `SCHEDULE_DATE` datetime NULL DEFAULT NULL COMMENT '排程日期',
  `PRODUCTION_STATUS` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '生产状态:0-未生产；1-生产中；2-已收尾',
  `IS_RELEASE` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE',
  `CX_MACHINE_CODE` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '成型机台编号',
  `CX_MACHINE_QTY` int NULL DEFAULT NULL COMMENT '成型机定额',
  `CX_MACHINE_NAME` varchar(60) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '成型机台名称',
  `CX_MACHINE_TYPE` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '成型机台类型：1一次法；2二次法',
  `CX_MACHINE_PRO_RANGE` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '成型机寸口范围：15/17',
  `LH_SCHEDULE_IDS` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '硫化排程任务ID，多个/分割',
  `LH_MACHINE_CODE` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '硫化机台编号，多个/分割',
  `LH_MACHINE_NAME` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '硫化机台名称，多个/分割',
  `LH_MACHINE_QTY` decimal(6, 1) NULL DEFAULT NULL COMMENT '硫化机台使用模数',
  `SAP_CODE` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '外胎代码',
  `SPEC_DESC` varchar(300) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '外胎规格描述',
  `LH_SINGLE_TIRE_TIME` decimal(6, 1) NULL DEFAULT NULL COMMENT '单胎硫化时长(分钟，从施工信息中获取)',
  `SINGLE_SHIFT_LH_QTY` decimal(8, 0) NULL DEFAULT NULL COMMENT '单班硫化量',
  `EMBRYO_CODE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胎胚代码',
  `SPEC_DIMENSION` decimal(10, 2) NULL DEFAULT NULL COMMENT '胎胚寸口',
  `PRODUCT_NUM` decimal(8, 0) NULL DEFAULT NULL COMMENT '胎胚计划量合计',
  `TOTAL_STOCK` decimal(8, 0) NULL DEFAULT NULL COMMENT '胎胚库存',
  `CLASS3_PLANNED_QTY` int NULL DEFAULT NULL COMMENT '成型排程当日早班计划量，用于半部件计算预计库存',
  `CLASS1_PLAN_QTY` decimal(8, 0) NULL DEFAULT NULL COMMENT '一班计划数',
  `CLASS1_START_TIME` datetime NULL DEFAULT NULL COMMENT '一班胎胚开始生产的时间',
  `CLASS1_END_TIME` datetime NULL DEFAULT NULL COMMENT '一班胎胚结束生产的时间',
  `CLASS1_MACHINE_QUOTA` decimal(8, 0) NULL DEFAULT NULL COMMENT '一班成型定额',
  `LH_CLASS1_PLAN` decimal(8, 0) NULL DEFAULT NULL COMMENT '一班硫化消耗',
  `CLASS1_SORT` decimal(3, 0) NULL DEFAULT NULL COMMENT '一班计划顺序',
  `CLASS1_ANALYSIS_INPUT` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '一班原因分析手工输入',
  `CLASS1_FINISH_QTY` char(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '一班完成量',
  `CLASS1_FINISH_RATE` char(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '一班完成率',
  `CLASS1_ANALYSIS` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '一班原因分析',
  `CLASS2_PLAN_QTY` decimal(8, 0) NULL DEFAULT NULL COMMENT '二班计划数',
  `CLASS2_START_TIME` datetime NULL DEFAULT NULL COMMENT '二班胎胚开始生产的时间',
  `CLASS2_END_TIME` datetime NULL DEFAULT NULL COMMENT '二班胎胚结束生产的时间',
  `CLASS2_MACHINE_QUOTA` decimal(8, 0) NULL DEFAULT NULL COMMENT '二班成型定额',
  `LH_CLASS2_PLAN` decimal(8, 0) NULL DEFAULT NULL COMMENT '二班硫化消耗',
  `CLASS2_SORT` decimal(8, 0) NULL DEFAULT NULL COMMENT '二班计划顺序',
  `CLASS2_ANALYSIS_INPUT` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '二班原因分析手工输入',
  `CLASS2_MODIFY_QTY` decimal(8, 0) NULL DEFAULT NULL COMMENT '二班修正后计划量',
  `CLASS2_FINISH_QTY` char(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '二班完成量',
  `CLASS2_FINISH_RATE` char(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '二班完成率',
  `CLASS2_ANALYSIS` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '二班原因分析',
  `CLASS3_PLAN_QTY` decimal(8, 0) NULL DEFAULT NULL COMMENT '三班计划数',
  `CLASS3_START_TIME` datetime NULL DEFAULT NULL COMMENT '三班胎胚开始生产的时间',
  `CLASS3_END_TIME` datetime NULL DEFAULT NULL COMMENT '三班胎胚结束生产的时间',
  `LH_CLASS3_PLAN` decimal(8, 0) NULL DEFAULT NULL COMMENT '三班硫化消耗',
  `CLASS3_MACHINE_QUOTA` decimal(8, 0) NULL DEFAULT NULL COMMENT '三班成型定额',
  `CLASS3_SORT` decimal(3, 0) NULL DEFAULT NULL COMMENT '三班计划顺序',
  `CLASS3_ANALYSIS_INPUT` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '三班原因分析手工输入',
  `CLASS3_FINISH_QTY` char(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '三班完成量',
  `CLASS3_FINISH_RATE` char(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '三班完成率',
  `CLASS3_ANALYSIS` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '三班原因分析',
  `CLASS4_PLAN_QTY` decimal(8, 0) NULL DEFAULT NULL COMMENT '次日一班计划数',
  `CLASS4_START_TIME` datetime NULL DEFAULT NULL COMMENT '次日一班胎胚开始生产的时间',
  `CLASS4_END_TIME` datetime NULL DEFAULT NULL COMMENT '次日一班胎胚结束生产的时间',
  `CLASS4_MACHINE_QUOTA` decimal(8, 0) NULL DEFAULT NULL COMMENT '次日一班成型定额',
  `LH_CLASS4_PLAN` decimal(8, 0) NULL DEFAULT NULL COMMENT '次日一班硫化消耗',
  `CLASS4_SORT` decimal(3, 0) NULL DEFAULT NULL COMMENT '次日一班计划顺序',
  `CLASS4_ANALYSIS_INPUT` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '次日一班原因分析手工输入',
  `CLASS4_FINISH_QTY` char(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '次日一班完成量',
  `CLASS4_FINISH_RATE` char(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '次日一班完成率',
  `CLASS4_ANALYSIS` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '次日一班原因分析',
  `CLASS5_PLAN_QTY` decimal(8, 0) NULL DEFAULT NULL COMMENT '次日二班计划数',
  `CLASS5_START_TIME` datetime NULL DEFAULT NULL COMMENT '次日二班胎胚开始生产的时间',
  `CLASS5_END_TIME` datetime NULL DEFAULT NULL COMMENT '次日二班胎胚结束生产的时间',
  `CLASS5_MACHINE_QUOTA` decimal(8, 0) NULL DEFAULT NULL COMMENT '次日二班成型定额',
  `LH_CLASS5_PLAN` decimal(8, 0) NULL DEFAULT NULL COMMENT '次日二班硫化消耗',
  `CLASS5_SORT` decimal(3, 0) NULL DEFAULT NULL COMMENT '次日二班计划顺序',
  `CLASS5_ANALYSIS_INPUT` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '次日二班原因分析手工输入',
  `CLASS5_FINISH_QTY` char(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '次日二完成量',
  `CLASS5_FINISH_RATE` char(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '次日二完成率',
  `CLASS5_ANALYSIS` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '次日二班原因分析',
  `CLASS6_PLAN_QTY` decimal(8, 0) NULL DEFAULT NULL COMMENT '次日三班计划数',
  `CLASS6_END_TIME` datetime NULL DEFAULT NULL COMMENT '次日三班胎胚结束生产的时间',
  `CLASS6_START_TIME` datetime NULL DEFAULT NULL COMMENT '次日三班胎胚开始生产的时间',
  `CLASS6_MACHINE_QUOTA` decimal(8, 0) NULL DEFAULT NULL COMMENT '次日三班成型定额',
  `LH_CLASS6_PLAN` decimal(8, 0) NULL DEFAULT NULL COMMENT '次日三班硫化消耗',
  `CLASS6_SORT` decimal(3, 0) NULL DEFAULT NULL COMMENT '次日三班计划顺序',
  `CLASS6_ANALYSIS_INPUT` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '次日三班原因分析手工输入',
  `CLASS6_FINISH_QTY` char(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '次日三完成量',
  `CLASS6_FINISH_RATE` char(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '次日三完成率',
  `CLASS6_ANALYSIS` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '次日三班原因分析',
  `MARK_CLOSE_OUT_TIP` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '收尾提示标识：0-提示收尾；1-不需要提示)',
  `DATA_SOURCE` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '0' COMMENT '数据来源：0>自动排程；1>插单；2>导入。插单数据可以进行计划调整',
  `BOM_DATA_VERSION` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '0' COMMENT '施工版本信息',
  `SPECIAL_REQUIREMENTS` varchar(3000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '特殊要求',
  `CREATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `CREATE_TIME` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `REMARK` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注说明字段',
  `PUBLISH_SUCCESS_COUNT` decimal(4, 0) NULL DEFAULT 0 COMMENT '发布成功计数器，每次发布成功进行累加。如果大于1发，发布状态只能到待发布',
  `NEWEST_PUBLISH_TIME` datetime NULL DEFAULT NULL COMMENT '保留最新的一次发布成功时间',
  `IS_DELETE` int NULL DEFAULT NULL COMMENT '是否删除:0否，1是',
  `DEL_FLAG` int NULL DEFAULT NULL COMMENT '班部件是否删除标识 :0否，1是，用于半部件计算取数',
  `SPEC_CODE` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '外胎代码',
  `PREVIOUS_TIRE_TIME` datetime NULL DEFAULT NULL COMMENT '生胎欠胎时间',
  `CLASS2_MODIFY_SORT` decimal(8, 0) NULL DEFAULT NULL COMMENT '二班修正后计划顺序',
  `CLASS3_MODIFY_SORT` decimal(8, 0) NULL DEFAULT NULL COMMENT '三班修正后计划顺序',
  `CLASS3_MODIFY_QTY` decimal(8, 0) NULL DEFAULT NULL COMMENT '三班修正后计划量',
  `WORK_SHIFTS` int NULL DEFAULT NULL COMMENT '班制',
  `STORAGE_LOCATION` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '库位',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `IDX_CX_SCHEDULE_EMBRYO_CODE`(`EMBRYO_CODE` ASC) USING BTREE,
  INDEX `IDX_CX_SCHEDULE_SCHEDULE_DATE`(`SCHEDULE_DATE` ASC) USING BTREE,
  INDEX `IDX_CX_DATE_MACHINE_SAP_CODE`(`SCHEDULE_DATE` ASC, `CX_MACHINE_CODE` ASC, `SAP_CODE` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 387171 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '成型排程结果表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_cx_schedule_result_ref
-- ----------------------------

-- ----------------------------
-- Table structure for t_cx_schedule_stop_info
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_schedule_stop_info`;
CREATE TABLE `t_cx_schedule_stop_info`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `CX_BATCH_NO` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '自动排程批次号信息，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号',
  `SPEC` varchar(300) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '外胎规格',
  `SAP_CODE` varchar(300) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '外胎代码',
  `EMBRYO_CODE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胎胚代码',
  `ORDER_NO` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '成型排程工单号，自动生成，批次号+4位定长自增序号',
  `SCHEDULE_DATE` datetime NULL DEFAULT NULL COMMENT '排程日期',
  `STOP_REASON` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '字典',
  `CREATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `CREATE_TIME` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `IS_DELETE` int NULL DEFAULT 0 COMMENT '删除标识（0未删除；1已删除）',
  `REMARK` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注说明字段',
  `SPEC_CODE` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '外胎代码',
  `BOM_DATA_VERSION` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '0' COMMENT '施工版本信息',
  `UN_SCHEDULE_NUM` int NULL DEFAULT NULL COMMENT '未排数量',
  `FACTORY_CODE` char(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '分厂编号',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '成型机台自动停排信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_cx_schedule_stop_info
-- ----------------------------

-- ----------------------------
-- Table structure for t_cx_share_mold_info
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_share_mold_info`;
CREATE TABLE `t_cx_share_mold_info`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `GROUP_NAME` varchar(90) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '所属组别信息',
  `SAP_CODE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '外胎代码',
  `EMBRYO_CODE` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胎胚代码',
  `SHARE_MOLD_NUM` decimal(6, 0) NULL DEFAULT NULL COMMENT '共用模具数量',
  `CREATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `CREATE_TIME` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `IS_DELETE` int NULL DEFAULT 0 COMMENT '删除标识（0未删除；1已删除）',
  `REMARK` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注说明字段',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `IDX_CX_SHARE_MOLD_INFO`(`SAP_CODE` ASC, `EMBRYO_CODE` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '成型（外胎20220325）胎胚共用模具信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_cx_share_mold_info
-- ----------------------------

-- ----------------------------
-- Table structure for t_cx_shift_config
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_shift_config`;
CREATE TABLE `t_cx_shift_config`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `SHIFT_CODE` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '班次编码',
  `SHIFT_NAME` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '班次名称',
  `SHIFT_ORDER` int NULL DEFAULT NULL COMMENT '班次序号',
  `START_TIME` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '开始时间（HH:mm:ss 格式）',
  `END_TIME` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '结束时间（HH:mm:ss 格式）',
  `SHIFT_HOURS` int NULL DEFAULT NULL COMMENT '班次时长（小时）',
  `IS_CROSS_DAY` tinyint(1) NULL DEFAULT NULL COMMENT '是否跨天：0-否 1-是',
  `SCHEDULE_DAY` int NULL DEFAULT NULL COMMENT '排程天数：1-第一天 2-第二天 3-第三天',
  `DAY_SHIFT_ORDER` int NULL DEFAULT NULL COMMENT '当天班次序号：该天第几个班',
  `CLASS_FIELD` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '对应结果表字段：CLASS1~CLASS8',
  `FACTORY_CODE` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工厂编号',
  `IS_ACTIVE` tinyint(1) NULL DEFAULT NULL COMMENT '是否启用：0-禁用 1-启用',
  `REMARK` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `IS_DELETE` tinyint(1) NULL DEFAULT 0 COMMENT '删除标志：0-未删除 1-已删除',
  `CREATE_BY` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `UPDATE_BY` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `IDX_SHIFT_CODE`(`SHIFT_CODE` ASC) USING BTREE,
  INDEX `IDX_FACTORY_CODE`(`FACTORY_CODE` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '班次配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_cx_shift_config
-- ----------------------------
INSERT INTO `t_cx_shift_config` VALUES (1, 'DAY_D1', '早班', 1, '06:00:00', '13:59:59', 8, 0, 1, 1, 'CLASS1', '116', 1, NULL, 0, NULL, NULL, NULL, NULL);
INSERT INTO `t_cx_shift_config` VALUES (2, 'AFTERNOON_D1', '中班', 2, '14:00:00', '23:59:59', 10, 0, 1, 2, 'CLASS2', '116', 1, NULL, 0, NULL, NULL, NULL, NULL);
INSERT INTO `t_cx_shift_config` VALUES (3, 'NIGHT_D2', '夜班', 1, '00:00:00', '05:59:59', 6, 0, 2, 1, 'CLASS3', '116', 1, NULL, 0, NULL, NULL, NULL, NULL);
INSERT INTO `t_cx_shift_config` VALUES (4, 'DAY_D2', '早班', 2, '06:00:00', '13:59:59', 8, 0, 2, 2, 'CLASS4', '116', 1, NULL, 0, NULL, NULL, NULL, NULL);
INSERT INTO `t_cx_shift_config` VALUES (5, 'AFTERNOON_D2', '中班', 3, '14:00:00', '23:59:59', 10, 0, 2, 3, 'CLASS5', '116', 1, NULL, 0, NULL, NULL, NULL, NULL);
INSERT INTO `t_cx_shift_config` VALUES (6, 'NIGHT_D3', '夜班', 1, '00:00:00', '05:59:59', 6, 0, 3, 1, 'CLASS6', '116', 1, NULL, 0, NULL, NULL, NULL, NULL);
INSERT INTO `t_cx_shift_config` VALUES (7, 'DAY_D3', '早班', 2, '06:00:00', '13:59:59', 8, 0, 3, 2, 'CLASS7', '116', 1, NULL, 0, NULL, NULL, NULL, NULL);
INSERT INTO `t_cx_shift_config` VALUES (8, 'AFTERNOON_D3', '中班', 3, '14:00:00', '23:59:59', 10, 0, 3, 3, 'CLASS8', '116', 1, NULL, 0, NULL, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for t_cx_spec_color
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_spec_color`;
CREATE TABLE `t_cx_spec_color`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `SPEC_DESC` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '规格型号',
  `COLOR_TYPE` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '颜色类型；0-字体颜色，1-背景颜色（对应数据字典BIG_ROLL_COLOR）',
  `COLOR_CODE` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '颜色代码，例如：#000000',
  `STATUS` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '状态，0--启用，1--禁用。',
  `REMARK` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注',
  `IS_DELETE` int NULL DEFAULT NULL COMMENT '删除标识：0--正常，1-删除',
  `CREATE_BY` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建者',
  `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `UPDATE_BY` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '更新者',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `IDX_CX_SPEC_COLOR`(`SPEC_DESC` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '规格字体颜色设置' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_cx_spec_color
-- ----------------------------

-- ----------------------------
-- Table structure for t_cx_specify_machine
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_specify_machine`;
CREATE TABLE `t_cx_specify_machine`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `SAP_CODE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'SAP品号(不需要外SAP+胎胚确定唯一，SAP品号可以为空)',
  `EMBRYO_CODE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胎胚代码(不允许为空)',
  `MACHINE_CODE` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '成型机台编号',
  `LINE_TYPE` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '线路，数据维护在数据字典：0-生产线、1-备用线',
  `JOB_TYPE` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '作业类型，数据维护在数据字典：0-限制作业；1-不可作业',
  `CREATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `CREATE_TIME` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `DEL_FLAG` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '0' COMMENT '删除标识（0未删除；1已删除）',
  `REMARK` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注说明字段',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `INDEX_CX_SPECIFY_MACHINE`(`EMBRYO_CODE` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '定点机台表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_cx_specify_machine
-- ----------------------------

-- ----------------------------
-- Table structure for t_cx_stock
-- ----------------------------
DROP TABLE IF EXISTS `t_cx_stock`;
CREATE TABLE `t_cx_stock`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `STOCK_DATE` datetime NULL DEFAULT NULL COMMENT '库存日期，格式：yyyy-MM-dd',
  `EMBRYO_CODE` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胎胚代码',
  `STOCK_NUM` decimal(10, 0) NULL DEFAULT NULL COMMENT '库存量',
  `OVER_TIME_STOCK` decimal(10, 0) NULL DEFAULT NULL COMMENT '超期库存',
  `MODIFY_NUM` decimal(10, 0) NULL DEFAULT NULL COMMENT '修正数量',
  `BAD_NUM` decimal(10, 0) NULL DEFAULT NULL COMMENT '不良数量',
  `CREATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `CREATE_TIME` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `IS_DELETE` int NULL DEFAULT 0 COMMENT '删除标识（0未删除；1已删除）',
  `REMARK` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注说明字段',
  `FACTORY_CODE` char(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '分厂编号',
  `IS_ENDING_SKU` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '是否收尾SKU',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `INDEX_CX_STOCK`(`STOCK_DATE` ASC, `EMBRYO_CODE` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 33709 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '成型库存信息表，用于获取排程前的真实库存' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_cx_stock
-- ----------------------------
INSERT INTO `t_cx_stock` VALUES (33710, '2026-03-25 00:00:00', '215100460', 21, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33711, '2026-03-25 00:00:00', '215101178', 17, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33712, '2026-03-25 00:00:00', '215101180', 17, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33713, '2026-03-25 00:00:00', '215101335', 90, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33714, '2026-03-25 00:00:00', '215101336', 9, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33715, '2026-03-25 00:00:00', '215101337', 58, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33716, '2026-03-25 00:00:00', '215101401', 51, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33717, '2026-03-25 00:00:00', '215101407', 46, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33718, '2026-03-25 00:00:00', '215101411', 26, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33719, '2026-03-25 00:00:00', '215101470', 120, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33720, '2026-03-25 00:00:00', '215101486', 16, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33721, '2026-03-25 00:00:00', '215101520', 20, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33722, '2026-03-25 00:00:00', '215101523', 4, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33723, '2026-03-25 00:00:00', '215101545', 25, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33724, '2026-03-25 00:00:00', '215101548', 25, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33725, '2026-03-25 00:00:00', '215101611', 85, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33726, '2026-03-25 00:00:00', '215101677', 9, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33727, '2026-03-25 00:00:00', '215101726', 26, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33728, '2026-03-25 00:00:00', '215101729', 34, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33729, '2026-03-25 00:00:00', '215101731', 25, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33730, '2026-03-25 00:00:00', '215101734', 22, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33731, '2026-03-25 00:00:00', '215101743', 14, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33732, '2026-03-25 00:00:00', '215101744', 18, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33733, '2026-03-25 00:00:00', '215101783', 7, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33734, '2026-03-25 00:00:00', '215101814', 58, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33735, '2026-03-25 00:00:00', '215101828', 22, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33736, '2026-03-25 00:00:00', '215101837', 141, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33737, '2026-03-25 00:00:00', '215101838', 73, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33738, '2026-03-25 00:00:00', '215101877', 105, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33739, '2026-03-25 00:00:00', '215101878', 33, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33740, '2026-03-25 00:00:00', '215101879', 32, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33741, '2026-03-25 00:00:00', '215101880', 66, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33742, '2026-03-25 00:00:00', '215101888', 15, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33743, '2026-03-25 00:00:00', '215101922', 39, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33744, '2026-03-25 00:00:00', '215102358', 2, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33745, '2026-03-25 00:00:00', '215102417', 39, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33746, '2026-03-25 00:00:00', '215102568', 83, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33747, '2026-03-25 00:00:00', '215102582', 33, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33748, '2026-03-25 00:00:00', '215102626', 269, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33749, '2026-03-25 00:00:00', '215102628', 44, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33750, '2026-03-25 00:00:00', '215102631', 35, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33751, '2026-03-25 00:00:00', '215102632', 95, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33752, '2026-03-25 00:00:00', '215102639', 25, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33753, '2026-03-25 00:00:00', '215102640', 42, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33754, '2026-03-25 00:00:00', '215102642', 19, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33755, '2026-03-25 00:00:00', '215102644', 15, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33756, '2026-03-25 00:00:00', '215102719', 20, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33757, '2026-03-25 00:00:00', '215102737', 13, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33758, '2026-03-25 00:00:00', '215102780', 16, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33759, '2026-03-25 00:00:00', '215102793', 12, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33760, '2026-03-25 00:00:00', '215102830', 22, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33761, '2026-03-25 00:00:00', '215103003', 51, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33762, '2026-03-25 00:00:00', '215103006', 8, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33763, '2026-03-25 00:00:00', '215103130', 6, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33764, '2026-03-25 00:00:00', '215103353', 9, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33765, '2026-03-25 00:00:00', '215103396', 18, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33766, '2026-03-25 00:00:00', '215103740', 12, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33767, '2026-03-25 00:00:00', '215103741', 27, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33768, '2026-03-25 00:00:00', '215103782', 27, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33769, '2026-03-25 00:00:00', '215103930', 9, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33770, '2026-03-25 00:00:00', '215103962', 16, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33771, '2026-03-25 00:00:00', '215103967', 17, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33772, '2026-03-25 00:00:00', '215103975', 22, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33773, '2026-03-25 00:00:00', '215103997', 23, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33774, '2026-03-25 00:00:00', '215104050', 8, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33775, '2026-03-25 00:00:00', '215104169', 18, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33776, '2026-03-25 00:00:00', '215104191', 24, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33777, '2026-03-25 00:00:00', '215104472', 8, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);
INSERT INTO `t_cx_stock` VALUES (33778, '2026-03-25 00:00:00', '215104811', 6, NULL, NULL, NULL, NULL, '2026-04-03 17:44:59', NULL, NULL, 0, NULL, 'TBR', NULL);

-- ----------------------------
-- Table structure for t_lh_cx_linkage_confirm
-- ----------------------------
DROP TABLE IF EXISTS `t_lh_cx_linkage_confirm`;
CREATE TABLE `t_lh_cx_linkage_confirm`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `FACTORY_CODE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '分厂编号',
  `BATCH_NO` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '自动排程批次号信息，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号',
  `ORDER_NO` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '工单号，自动生成（工序+日期+三位顺序号001,002）',
  `ADJUST_BATCH_NO` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '调整批次号',
  `LH_SCHEDULE_ID` bigint NOT NULL COMMENT '硫化排程ID',
  `LH_MACHINE_CODE` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '硫化机台编号',
  `SPEC_CODE` varchar(60) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '规格代码',
  `EMBRYO_CODE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胎胚代码',
  `SPEC_DESC` varchar(600) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '规格描述信息',
  `ADJUST_TYPE` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '调整类型',
  `ADJUST_QTY` int NULL DEFAULT NULL COMMENT '调整量',
  `SCHEDULE_DATE` datetime NULL DEFAULT NULL COMMENT '排程日期',
  `CX_SCHEDULE_ID` bigint NOT NULL COMMENT '成型排程ID',
  `ORI_CX_MACHINE_CODE` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '原成型机台编号',
  `ORI_CX_SPEC_CODE` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '原成型规格代码',
  `ORI_CX_EMBRYO_CODE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '原成型生胎代码',
  `ORI_CX_CLASS1_PLAN_QTY` int NULL DEFAULT NULL COMMENT '原成型一班计划数',
  `ORI_CX_CLASS2_PLAN_QTY` int NULL DEFAULT NULL COMMENT '原成型二班计划数',
  `ORI_CX_CLASS3_PLAN_QTY` int NULL DEFAULT NULL COMMENT '原成型三班计划数',
  `ORI_CX_CLASS4_PLAN_QTY` int NULL DEFAULT NULL COMMENT '原成型次日一班计划数',
  `ORI_CX_CLASS5_PLAN_QTY` int NULL DEFAULT NULL COMMENT '原成型次日二班计划数',
  `ORI_CX_CLASS6_PLAN_QTY` int NULL DEFAULT NULL COMMENT '原成型次日三班计划数',
  `NEW_CX_MACHINE_CODE` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '新成型机台编号',
  `NEW_CX_SPEC_CODE` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '新成型规格代码',
  `NEW_CX_EMBRYO_CODE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '新成型生胎代码',
  `NEW_CX_CLASS1_PLAN_QTY` int NULL DEFAULT NULL COMMENT '新成型一班计划数',
  `NEW_CX_CLASS2_PLAN_QTY` int NULL DEFAULT NULL COMMENT '新成型二班计划数',
  `NEW_CX_CLASS3_PLAN_QTY` int NULL DEFAULT NULL COMMENT '新成型三班计划数',
  `NEW_CX_CLASS4_PLAN_QTY` int NULL DEFAULT NULL COMMENT '新成型次日一班计划数',
  `NEW_CX_CLASS5_PLAN_QTY` int NULL DEFAULT NULL COMMENT '新成型次日二班计划数',
  `NEW_CX_CLASS6_PLAN_QTY` int NULL DEFAULT NULL COMMENT '新成型次日三班计划数',
  `IS_CONFIRM` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '是否确认',
  `CREATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `CREATE_TIME` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_BY` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `IS_DELETE` int NULL DEFAULT 0 COMMENT '删除标识（0未删除；1已删除）',
  `REMARK` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注说明字段',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `IDX_ADJUST_BATCH_NO`(`ADJUST_BATCH_NO` ASC, `LH_SCHEDULE_ID` ASC) USING BTREE,
  INDEX `IDX_ADJUST_SCHEDULE_DATE`(`SCHEDULE_DATE` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '硫化成型联动确认' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_lh_cx_linkage_confirm
-- ----------------------------

-- ----------------------------
-- Table structure for t_mdm_cx_machine_fixed
-- ----------------------------
DROP TABLE IF EXISTS `t_mdm_cx_machine_fixed`;
CREATE TABLE `t_mdm_cx_machine_fixed`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `FACTORY_CODE` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工厂编号',
  `CX_MACHINE_CODE` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '成型机编码',
  `FIXED_STRUCTURE1` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '固定结构1 多个以,分隔拼接',
  `FIXED_STRUCTURE2` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '固定结构1 多个以,分隔拼接',
  `FIXED_STRUCTURE3` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '固定结构1 多个以,分隔拼接',
  `FIXED_MATERIAL_CODE` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '固定SKU  多个以,分隔拼接',
  `FIXED_MATERIAL_DESC` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '固定物料描述  多个以,分隔拼接',
  `DISABLE_STRUCTURE` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '不可作业结构  多个以,分隔拼接',
  `DISABLE_MATERIAL_CODE` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '不可作业SKU  多个以,分隔拼接',
  `DISABLE_MATERIAL_DESC` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '不可作业物料描述  多个以,分隔拼接',
  `REMARK` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `IS_DELETE` decimal(1, 0) NOT NULL DEFAULT 0 COMMENT '是否删除（0：默认未删除 1：已删除）',
  `CREATE_TIME` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `CREATE_BY` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `UPDATE_BY` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `IDX_MDM_CX_MACHINE_FIXED_MOLDING_MACHINE_CODE`(`CX_MACHINE_CODE` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 145 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '成型固定机台表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_mdm_cx_machine_fixed
-- ----------------------------
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (1, '116', '111', '1', '2', '3', '3302002083', NULL, '22', '3302002083', NULL, NULL, 1, '2025-12-29 09:36:17', '2025-12-29 09:36:17', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (2, '116', 'A1', '', '', '', '', NULL, '', '', NULL, NULL, 1, '2025-12-29 10:33:01', '2025-12-29 10:33:14', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (3, '116', '66666', '315/80R22.5EDR53', '295/80R22.5EAM62+', '295/80R22.5BA220+', '3302002083,3302002254,3302002084,3302002082,3302002562,3302002566,3302002568,3302002558,3302002556,3302002557', NULL, '315/70R22.5EDR53', '3302002083,3302002254', NULL, NULL, 1, '2025-12-29 11:10:09', '2025-12-30 11:27:06', 'admin', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (4, '116', 'psl12250001', 'ST235/80R16,315/80R22.5-JF518四层', '315/80R22.5-JY711零度,315/70R22.5', '315/80R22.5-JD758零度,315/70R25.5', '3302002083,3302002254', NULL, '295/80R22.5,315/60R22.5', '3302002082,3302002562', NULL, NULL, 1, '2025-12-29 17:23:48', '2025-12-31 09:39:47', 'vege', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (5, '116', 'sp122511001', '10.00R20 149/146K 18PR BD260 BT0HBL', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, '2025-12-31 09:37:21', '2025-12-31 14:51:50', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (6, '116', 'H1304', '10.00R20-JD727载重,11.00R20-JD727载重,11.00R20-JY601标载', '10.00R20 149/146K 18PR BD260 BT0HBL,10.00R20 149/146K 18PR JA767 BT0HJY,10.00R20 149/146K 18PR JD727 BT0HJY', '10.00R20 149/146K 18PR JD727 BT0HJY,11.00R20 152/149J 18PR JD681 BT0HJY', '3302000207,3302000315', NULL, '10.00R20-JD727载重,11.00R20-JD727载重,11.00R20-JY601标载', '3302000207,3302000315', NULL, NULL, 1, '2026-01-05 14:10:49', '2026-01-06 13:26:32', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (7, '116', 'H1404', '445/45R19.5', '12R22.5-JY601标载零度', '11R24.5-JD571', NULL, NULL, '285/75R24.5', '3302000302', NULL, NULL, 1, '2026-01-07 14:56:02', '2026-01-13 20:49:26', 'admin', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (9, '116', '01', '123,222,333', '', '', 'qqq,www,eee', NULL, '111', 'aaa', NULL, NULL, 1, '2026-01-09 09:22:07', '2026-01-09 09:22:07', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (10, '116', 'H1505', '10.00R20-JD727载重', '', '', '', NULL, '', '', NULL, NULL, 1, '2026-01-13 11:51:38', '2026-01-13 11:51:38', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (11, '116', 'H1101', '10.00R20-JD727载重,11.00R20-JD727载重,11.00R20-JY601标载', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, '2026-01-13 13:43:24', '2026-01-13 15:43:01', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (12, '116', 'H1504', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, '2026-01-13 14:05:39', '2026-01-13 14:05:44', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (13, '116', 'H1504', '295/75R22.5', NULL, NULL, NULL, NULL, NULL, '3202000563', NULL, NULL, 1, '2026-01-13 15:03:10', '2026-01-13 20:06:52', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (14, '116', 'H1504', NULL, NULL, NULL, NULL, NULL, NULL, '3302000258,3302000026', NULL, NULL, 1, '2026-01-13 20:55:47', '2026-01-14 09:11:48', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (15, '116', 'H1505', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, '2026-01-14 14:47:02', '2026-01-14 14:47:07', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (16, '116', 'H1505', NULL, NULL, NULL, NULL, NULL, '11.00R20-JD727载重,295/75R22.5', NULL, NULL, NULL, 1, '2026-01-15 14:49:47', '2026-01-15 14:52:14', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (17, '116', 'H1502', '275/70R22.5', '245/70R19.5', NULL, NULL, NULL, '435/50R19.5', '3302001538,3302002582', NULL, NULL, 1, '2026-01-15 16:16:16', '2026-01-15 17:05:36', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (18, '116', 'H1405', '245/70R19.5', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, '2026-01-15 17:09:48', '2026-01-15 17:09:48', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (19, '116', 'H1503', NULL, NULL, NULL, NULL, NULL, '295/75R22.5', NULL, NULL, NULL, 1, '2026-01-15 17:30:11', '2026-01-15 17:30:11', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (20, '116', 'H1502', NULL, NULL, NULL, NULL, NULL, '13R22.5-JD758零度,11R24.5-JD571,275/70R22.5', NULL, NULL, NULL, 1, '2026-01-15 17:30:40', '2026-01-15 17:32:45', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (21, '116', 'H1504', NULL, NULL, NULL, NULL, NULL, '11R22.5-JD571四层', NULL, NULL, NULL, 1, '2026-01-15 17:30:59', '2026-01-15 17:30:59', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (22, '116', 'H1402', '11R22.5-JY601零度', '12.00R20-JD727', '12R22.5-JY601标载零度', '3302002758', NULL, '285/75R24.5', '3302002104', NULL, NULL, 1, '2026-01-15 17:31:38', '2026-01-15 17:36:38', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (23, '116', 'H1101', '11R22.5-JD571四层\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n385/65R22.5-JY598零度,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '', '3302001270', NULL, '', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (24, '116', 'H1102', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度', '13R22.5-JD758零度,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n385/65R22.5-JY598零度,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '', NULL, '', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (25, '116', 'H1103', '11R24.5-JD571,\n285/75R24.5,\n325/95R24', '11R22.5-JD571四层,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n385/65R22.5-JY598零度,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '3302001146', NULL, '', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (26, '116', 'H1104', '315/80R22.5-JF518四层', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n385/55R22.5,\n385/65R22.5-JY598四层,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '3302000609,\n315/80R22.5 156/153L 20PR JF518 BL4EJY', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (27, '116', 'H1105', '275/80R22.5 ,\n315/70R22.5,\n315/80R22.5-JF518四层,\n295/80R22.5', '', '', '3302001586', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度,\n295/60R22.5', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (28, '116', 'H1201', '11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5', '255/70R22.5,\n275/70R22.5,\n295/60R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '3302001273,3302001143', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (29, '116', 'H1202', '11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5', '255/70R22.5,\n275/70R22.5,\n295/60R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '', NULL, '9.00R20-JY601,\n11.00R20-JY601标载\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (30, '116', 'H1203', '315/70R22.5', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '3302000465', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (31, '116', 'H1204', '315/70R22.5', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '3302001586,\n295/80R22.5 152/149J 18PR JD756 BL4HJY', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (32, '116', 'H1205', '11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n385/65R22.5-JY598零度', '295/80R22.5,385/65R22.5-JY598四层', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层', '', NULL, '', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (33, '116', 'H1301', '385/65R22.5-JY598四层', '11R22.5-JD571四层,\n295/75R22.5', '255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层', '', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (34, '116', 'H1302', '385/65R22.5-JY598四层', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '3302001315', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '3302001462', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (35, '116', 'H1304', '295/80R22.5,315/70R22.5', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层', '', '3302001586,\n295/80R22.5 152/149J 18PR JD756 BL4HJY', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (36, '116', 'H1305', '315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n13R22.5-JD758零度', '11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n385/65R22.5-JY598零度', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n295/80R22.5,\n315/70R22.5', '', NULL, '', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (37, '116', 'H1401', '205/75R17.5,\n215/75R17.5,\n235/75R17.5,\n245/70R17.5', '225/70R19.5,\n245/70R19.5,\n265/70R19.5,\n285/70R19.5,\nST235/80R16,\nST235/85R16', '', '3302001572', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (38, '116', 'H1402', '225/70R19.5,\n245/70R19.5,\n265/70R19.5,\n285/70R19.5', '205/75R17.5,\n215/75R17.5,\n235/75R17.5,\n245/70R17.5,\nST235/80R16,\nST235/85R16', '', '', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (39, '116', 'H1403', '13R22.5-JD758零度,\n255/70R22.5,\n275/70R22.5,\n385/55R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n295/80R22.5', '11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n385/65R22.5-JY598零度,\n11R22.5-JD571四层,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层', '', '3302001273', NULL, '', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (40, '116', 'H1404', '385/55R22.5,\n315/60R22.5,\n295/60R22.5,\n275/70R22.5-RFID,\n13R22.5-JD758零度', '11R22.5-JY601零度\n12R22.5-JY601标载零度\n12R22.5-JY711载重零度\n385/65R22.5-JY598零度\n11R22.5-JD571四层\n275/80R22.5\n295/75R22.5\n425/65R22.5\n385/65R22.5-JT599四层\n385/65R22.5-JY598四层\n255/70R22.5\n315/70R22.5\n315/80R22.5-JF518四层\n295/80R22.5', '', '3302001582', NULL, '', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (41, '116', 'H1405', '11R24.5-JD571,285/75R24.5', '325/95R24', '11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n385/65R22.5-JY598零度,\n11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n255/70R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n295/80R22.5,\n385/55R22.5,\n315/60R22.5,\n295/60R22.5,\n275/70R22.5,\n13R22.5-JD758零度', '3302001146', NULL, '', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (42, '116', 'H1501', '325/95R24', '11R24.5-JD571,\n285/75R24.5,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n385/65R22.5-JY598零度,\n11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n255/70R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n295/80R22.5,\n385/55R22.5,\n295/60R22.5,\n275/70R22.5-RFID,\n13R22.5-JD758零度', '', '3302001586,3302001273', NULL, '', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (43, '116', 'H1502', '11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n385/65R22.5-JY598零度', '385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n315/70R22.5,\n315/80R22.5-JF518四层', '111R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n255/70R22.5,\n295/80R22.5,\n385/55R22.5,\n315/60R22.5,\n295/60R22.5,\n275/70R22.5,\n13R22.5-JD758零度', '', NULL, '', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (44, '116', 'H1503', '205/75R17.5,\n215/75R17.5,\n235/75R17.5,\n245/70R17.5,\n225/70R19.5,\n245/70R19.5,\n265/70R19.5,\n285/70R19.5,\nST235/80R16,\nST235/85R16', '385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n315/70R22.5,\n315/80R22.5-JF518四层,\n11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5,\n425/65R22.5,\n255/70R22.5,\n295/80R22.5,\n385/55R22.5,\n295/60R22.5,\n275/70R22.5', '', '3302001143', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (45, '116', 'H1504', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n295/80R22.5,\n315/70R22.5', '', '', '3302001586', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (46, '116', 'H1505', '255/70R22.5,\n275/70R22.5-RFID,\n295/60R22.5,\n385/55R22.5', '11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5,\n315/80R22.5-JF518四层,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n295/80R22.5,\n315/70R22.5', '', '3302000837', NULL, '', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (47, '116', 'H1101', '11R22.5-JD571四层，\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n385/65R22.5-JY598零度,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '', '3302001270', '385/65R22.5 164K 24PR JT560 BL0EJY', '', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (48, '116', 'H1102', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度', '13R22.5-JD758零度,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n385/65R22.5-JY598零度,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '', '', '', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (49, '116', 'H1103', '11R24.5-JD571,\n285/75R24.5,\n325/95R24', '11R22.5-JD571四层,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n385/65R22.5-JY598零度,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '3302001146', '11R24.5 149/146L 16PR JD571 BL4HJY', '', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (50, '116', 'H1104', '315/80R22.5-JF518四层', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n385/55R22.5,\n385/65R22.5-JY598四层,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '3302000609,3302001236', '315/80R22.5 156/153L 20PR BD175 BL4EBL,315/80R22.5 156/153L 20PR JF518 BL4EJY', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (51, '116', 'H1105', '275/80R22.5 ,\n315/70R22.5,\n315/80R22.5-JF518四层,\n295/80R22.5', '', '', '3302001586', '295/80R22.5 152/149M 18PR JF518 BL4EJY', '9.00R20-JY601,\n11.00R20-JY601标载,,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度,\n295/60R22.5', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (52, '116', 'H1201', '11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5', '', '', '3302001273,3302001143', '275/80R22.5 146/143L 16PR JF568 BL4HJY,11R22.5 146/143L 16PR JD571 BL4HJY', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度，\n255/70R22.5,\n275/70R22.5,\n295/60R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (53, '116', 'H1202', '11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5', '', '', '', '', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度，\n255/70R22.5,\n275/70R22.5,\n295/60R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (54, '116', 'H1203', '315/70R22.5', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '3302000465', '315/70R22.5 156/150L 18PR JD575 BL4EJY', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (55, '116', 'H1204', '315/70R22.5', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '3302001586,3302001139', '295/80R22.5 152/149M 18PR JF518 BL4EJY,295/80R22.5 152/149J 18PR JD756 BL4HJY', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (56, '116', 'H1205', '11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n385/65R22.5-JY598零度', '295/80R22.5,385/65R22.5-JY598四层', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '', '', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (57, '116', 'H1301', '385/65R22.5-JY598四层', '11R22.5-JD571四层,\n295/75R22.5', '255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (58, '116', 'H1302', '385/65R22.5-JY598四层', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '3302001315', '385/65R22.5 164K 24PR BT160 BL4EBL', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '3302001462', '385/65R22.5 164K 24PR JA626 BL4EJY', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (59, '116', 'H1304', '295/80R22.5,315/70R22.5', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层', '', '3302001586,3302001139', '295/80R22.5 152/149M 18PR JF518 BL4EJY,295/80R22.5 152/149J 18PR JD756 BL4HJY', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (60, '116', 'H1305', '315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n13R22.5-JD758零度', '11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n385/65R22.5-JY598零度', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n295/80R22.5,\n315/70R22.5', '', '', '', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (61, '116', 'H1401', '205/75R17.5,\n215/75R17.5,\n235/75R17.5,\n245/70R17.5', '225/70R19.5,\n245/70R19.5,\n265/70R19.5,\n285/70R19.5,\nST235/80R16,\nST235/85R16', '', '3302001572', '235/75R17.5 143/141L 18PR JF518 BL3EJY', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (62, '116', 'H1402', '225/70R19.5,\n245/70R19.5,\n265/70R19.5,\n285/70R19.5', '205/75R17.5,\n215/75R17.5,\n235/75R17.5,\n245/70R17.5,\nST235/80R16,\nST235/85R16', '', '', '', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (63, '116', 'H1403', '13R22.5-JD758零度,\n255/70R22.5,\n275/70R22.5,\n385/55R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n295/80R22.5', '11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n385/65R22.5-JY598零度,\n11R22.5-JD571四层,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层', '', '3302001273', '275/80R22.5 146/143L 16PR JF568 BL4HJY', '', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (64, '116', 'H1404', '385/55R22.5,\n315/60R22.5,\n295/60R22.5,\n275/70R22.5-RFID,\n13R22.5-JD758零度', '11R22.5-JY601零度\n12R22.5-JY601标载零度\n12R22.5-JY711载重零度\n385/65R22.5-JY598零度\n11R22.5-JD571四层\n275/80R22.5\n295/75R22.5\n425/65R22.5\n385/65R22.5-JT599四层\n385/65R22.5-JY598四层\n255/70R22.5\n315/70R22.5\n315/80R22.5-JF518四层\n295/80R22.5', '', '3302001582', '295/60R22.5 150/147L 18PR JD577 BL4EJY', '', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (65, '116', 'H1405', '11R24.5-JD571,285/75R24.5', '325/95R24', '11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n385/65R22.5-JY598零度,\n11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n255/70R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n295/80R22.5,\n385/55R22.5,\n315/60R22.5,\n295/60R22.5,\n275/70R22.5,\n13R22.5-JD758零度', '3302001146', '11R24.5 149/146L 16PR JD571 BL4HJY', '', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (66, '116', 'H1501', '325/95R24', '11R24.5-JD571,\n285/75R24.5,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n385/65R22.5-JY598零度,\n11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n255/70R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n295/80R22.5,\n385/55R22.5,\n295/60R22.5,\n275/70R22.5-RFID,\n13R22.5-JD758零度', '', '3302001586,3302001273', '295/80R22.5 152/149M 18PR JF518 BL4EJY,275/80R22.5 146/143L 16PR JF568 BL4HJY', '', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (67, '116', 'H1502', '11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n385/65R22.5-JY598零度', '385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n315/70R22.5,\n315/80R22.5-JF518四层', '111R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n255/70R22.5,\n295/80R22.5,\n385/55R22.5,\n315/60R22.5,\n295/60R22.5,\n275/70R22.5,\n13R22.5-JD758零度', '', '', '', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (68, '116', 'H1503', '205/75R17.5,\n215/75R17.5,\n235/75R17.5,\n245/70R17.5,\n225/70R19.5,\n245/70R19.5,\n265/70R19.5,\n285/70R19.5,\nST235/80R16,\nST235/85R16', '385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n315/70R22.5,\n315/80R22.5-JF518四层,\n11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5,\n425/65R22.5,\n255/70R22.5,\n295/80R22.5,\n385/55R22.5,\n295/60R22.5,\n275/70R22.5', '', '3302001143', '11R22.5 146/143L 16PR JD571 BL4HJY', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (69, '116', 'H1504', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n295/80R22.5,\n315/70R22.5', '', '', '3302001586', '295/80R22.5 152/149M 18PR JF518 BL4EJY', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (70, '116', 'H1505', '255/70R22.5,\n275/70R22.5-RFID,\n295/60R22.5,\n385/55R22.5', '11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5,\n315/80R22.5-JF518四层,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n295/80R22.5,\n315/70R22.5', '', '3302000837', '385/55R22.5 160K 20PR JT560 BL4EJY', '', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (71, '116', 'H1303', '435/50R19.5,445/45R19.5', '425/65R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n295/80R22.5,\n385/65R22.5-JY598四层', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n385/55R22.5,\n385/65R22.5-JT599四层', '3302000906,3302000836', '435/50R19.5 160J 20PR JT560 BL4EJY,445/45R19.5 160J 20PR JT560 BL4EJY', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', '', NULL, 1, '2026-01-28 21:36:42', '2026-01-30 15:14:12', 'testing01', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (72, '116', 'H1303', '435/50R19.5,445/45R19.5', '425/65R22.5,315/70R22.5,315/80R22.5-JF518四层,295/80R22.5,385/65R22.5-JY598四层', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,385/55R22.5,385/65R22.5-JT599四层', '3302000906,3302000836', '435/50R19.5 160J 20PR JT560 BL4EJY,445/45R19.5 160J 20PR JT560 BL4EJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (73, '116', 'H1501', '325/95R24', '11R24.5-JD571,285/75R24.5,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,385/65R22.5-JY598零度,11R22.5-JD571四层,275/80R22.5,295/75R22.5,425/65R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层,255/70R22.5,315/70R22.5,315/80R22.5-JF518四层,295/80R22.5,385/55R22.5,295/60R22.5,275/70R22.5-RFID,13R22.5-JD758零度', '', '3302001586,3302001273', '295/80R22.5 152/149M 18PR JF518 BL4EJY,275/80R22.5 146/143L 16PR JF568 BL4HJY', '', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (74, '116', 'H1402', '225/70R19.5,245/70R19.5,265/70R19.5,285/70R19.5', '205/75R17.5,215/75R17.5,235/75R17.5,245/70R17.5,ST235/80R16,ST235/85R16', '', '', '', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (75, '116', 'H1204', '315/70R22.5', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,295/80R22.5,315/80R22.5-JF518四层,385/55R22.5,385/65R22.5-JY598四层,425/65R22.5,385/65R22.5-JT599四层', '', '3302001586,3302001139', '295/80R22.5 152/149M 18PR JF518 BL4EJY,295/80R22.5 152/149J 18PR JD756 BL4HJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (76, '116', 'H1105', '275/80R22.5,315/70R22.5,315/80R22.5-JF518四层,295/80R22.5', '', '', '3302001586', '295/80R22.5 152/149M 18PR JF518 BL4EJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度,295/60R22.5', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (77, '116', 'H1502', '11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,385/65R22.5-JY598零度', '385/65R22.5-JT599四层,385/65R22.5-JY598四层,12R22.5-JY601标载零度,12R22.5-JY711载重零度,315/70R22.5,315/80R22.5-JF518四层', '111R22.5-JD571四层,275/80R22.5,295/75R22.5,425/65R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层,255/70R22.5,295/80R22.5,385/55R22.5,315/60R22.5,295/60R22.5,275/70R22.5,13R22.5-JD758零度', '', '', '', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (78, '116', 'H1403', '13R22.5-JD758零度,255/70R22.5,275/70R22.5,385/55R22.5,315/70R22.5,315/80R22.5-JF518四层,295/80R22.5', '11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,385/65R22.5-JY598零度,11R22.5-JD571四层,275/80R22.5,295/60R22.5,295/75R22.5,425/65R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层', '', '3302001273', '275/80R22.5 146/143L 16PR JF568 BL4HJY', '', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (79, '116', 'H1304', '295/80R22.5,315/70R22.5', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,315/80R22.5-JF518四层,385/55R22.5,425/65R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层', '', '3302001586,3302001139', '295/80R22.5 152/149M 18PR JF518 BL4EJY,295/80R22.5 152/149J 18PR JD756 BL4HJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (80, '116', 'H1205', '11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,385/65R22.5-JY598零度', '295/80R22.5,385/65R22.5-JY598四层', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,315/70R22.5,315/80R22.5-JF518四层,385/55R22.5,425/65R22.5,385/65R22.5-JT599四层', '', '', '', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (81, '116', 'H1301', '385/65R22.5-JY598四层', '11R22.5-JD571四层,295/75R22.5', '255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/80R22.5,315/70R22.5,315/80R22.5-JF518四层,385/55R22.5,425/65R22.5,385/65R22.5-JT599四层', '', '', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (82, '116', 'H1202', '11R22.5-JD571四层,275/80R22.5,295/75R22.5', '', '', '', '', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度，\n255/70R22.5,275/70R22.5,295/60R22.5,295/80R22.5,315/70R22.5,315/80R22.5-JF518四层,385/55R22.5,385/65R22.5-JY598四层,425/65R22.5,385/65R22.5-JT599四层', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (83, '116', 'H1103', '11R24.5-JD571,285/75R24.5,325/95R24', '11R22.5-JD571四层,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度\n255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,295/80R22.5,315/70R22.5,315/80R22.5-JD758零度,315/80R22.5-JY711零度,315/80R22.5-JF518四层,385/55R22.5,385/65R22.5-JY598四层,385/65R22.5-JY598零度,425/65R22.5,385/65R22.5-JT599四层', '', '3302001146', '11R24.5 149/146L 16PR JD571 BL4HJY', '', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (84, '116', 'H1401', '205/75R17.5,215/75R17.5,235/75R17.5,245/70R17.5', '225/70R19.5,245/70R19.5,265/70R19.5,285/70R19.5,ST235/80R16,ST235/85R16', '', '3302001572', '235/75R17.5 143/141L 18PR JF518 BL3EJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (85, '116', 'H1302', '385/65R22.5-JY598四层', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,295/80R22.5,315/70R22.5,315/80R22.5-JF518四层,385/55R22.5,425/65R22.5,385/65R22.5-JT599四层', '', '3302001315', '385/65R22.5 164K 24PR BT160 BL4EBL', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '3302001462', '385/65R22.5 164K 24PR JA626 BL4EJY', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (86, '116', 'H1203', '315/70R22.5', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,295/80R22.5,315/70R22.5,315/80R22.5-JF518四层,385/55R22.5,385/65R22.5-JY598四层,425/65R22.5,385/65R22.5-JT599四层', '', '3302000465', '315/70R22.5 156/150L 18PR JD575 BL4EJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (87, '116', 'H1104', '315/80R22.5-JF518四层', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,295/80R22.5,315/70R22.5,385/55R22.5,385/65R22.5-JY598四层,425/65R22.5,385/65R22.5-JT599四层', '', '3302000609,3302001236', '315/80R22.5 156/153L 20PR BD175 BL4EBL,315/80R22.5 156/153L 20PR JF518 BL4EJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (88, '116', 'H1101', '11R22.5-JD571四层，\n11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,295/80R22.5,315/70R22.5,315/80R22.5-JD758零度,315/80R22.5-JY711零度,315/80R22.5-JF518四层,385/55R22.5,385/65R22.5-JY598四层,385/65R22.5-JY598零度,425/65R22.5,385/65R22.5-JT599四层', '', '', '3302001270', '385/65R22.5 164K 24PR JT560 BL0EJY', '', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (89, '116', 'H1201', '11R22.5-JD571四层,275/80R22.5,295/75R22.5', '', '', '3302001273,3302001143', '275/80R22.5 146/143L 16PR JF568 BL4HJY,11R22.5 146/143L 16PR JD571 BL4HJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度，\n255/70R22.5,275/70R22.5,295/60R22.5,295/80R22.5,315/70R22.5,315/80R22.5-JF518四层,385/55R22.5,385/65R22.5-JY598四层,425/65R22.5,385/65R22.5-JT599四层', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (90, '116', 'H1102', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重\n12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度', '13R22.5-JD758零度,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,295/80R22.5,315/70R22.5,315/80R22.5-JD758零度,315/80R22.5-JY711零度,315/80R22.5-JF518四层,385/55R22.5,385/65R22.5-JY598四层,385/65R22.5-JY598零度,425/65R22.5,385/65R22.5-JT599四层', '', '', '', '', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (91, '116', 'H1505', '255/70R22.5,275/70R22.5-RFID,295/60R22.5,385/55R22.5', '11R22.5-JD571四层,275/80R22.5,295/75R22.5,315/80R22.5-JF518四层,425/65R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层,295/80R22.5,315/70R22.5', '', '3302000837', '385/55R22.5 160K 20PR JT560 BL4EJY', '', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (92, '116', 'H1503', '205/75R17.5,215/75R17.5,235/75R17.5,245/70R17.5,225/70R19.5,245/70R19.5,265/70R19.5,285/70R19.5,ST235/80R16,ST235/85R16', '385/65R22.5-JT599四层,385/65R22.5-JY598四层,315/70R22.5,315/80R22.5-JF518四层,11R22.5-JD571四层,275/80R22.5,295/75R22.5,425/65R22.5,255/70R22.5,295/80R22.5,385/55R22.5,295/60R22.5,275/70R22.5', '', '3302001143', '11R22.5 146/143L 16PR JD571 BL4HJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (93, '116', 'H1404', '385/55R22.5,315/60R22.5,295/60R22.5,275/70R22.5-RFID,13R22.5-JD758零度', '11R22.5-JY601零度\n12R22.5-JY601标载零度\n12R22.5-JY711载重零度\n385/65R22.5-JY598零度\n11R22.5-JD571四层\n275/80R22.5\n295/75R22.5\n425/65R22.5\n385/65R22.5-JT599四层\n385/65R22.5-JY598四层\n255/70R22.5\n315/70R22.5\n315/80R22.5-JF518四层\n295/80R22.5', '', '3302001582', '295/60R22.5 150/147L 18PR JD577 BL4EJY', '', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (94, '116', 'H1305', '315/80R22.5-JD758零度,315/80R22.5-JY711零度,13R22.5-JD758零度', '11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,385/65R22.5-JY598零度', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,315/80R22.5-JF518四层,385/55R22.5,425/65R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层,295/80R22.5,315/70R22.5', '', '', '', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (95, '116', 'H1405', '11R24.5-JD571,285/75R24.5', '325/95R24', '11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,385/65R22.5-JY598零度,11R22.5-JD571四层,275/80R22.5,295/75R22.5,425/65R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层,255/70R22.5,315/70R22.5,315/80R22.5-JF518四层,295/80R22.5,385/55R22.5,315/60R22.5,295/60R22.5,275/70R22.5,13R22.5-JD758零度', '3302001146', '11R24.5 149/146L 16PR JD571 BL4HJY', '', '', '', NULL, 1, '2026-03-04 15:14:15', '2026-03-04 15:14:15', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (96, '116', 'H1303', '435/50R19.5,445/45R19.5', '425/65R22.5,315/70R22.5,315/80R22.5-JF518四层,295/80R22.5,385/65R22.5-JY598四层', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,385/55R22.5,385/65R22.5-JT599四层', '3302000906,3302000836', '435/50R19.5 160J 20PR JT560 BL4EJY,445/45R19.5 160J 20PR JT560 BL4EJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (97, '116', 'H1501', '325/95R24', '11R24.5-JD571,285/75R24.5,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,385/65R22.5-JY598零度,11R22.5-JD571四层,275/80R22.5,295/75R22.5,425/65R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层,255/70R22.5,315/70R22.5,315/80R22.5-JF518四层,295/80R22.5,385/55R22.5,295/60R22.5,275/70R22.5-RFID,13R22.5-JD758零度', '', '3302001586,3302001273', '295/80R22.5 152/149M 18PR JF518 BL4EJY,275/80R22.5 146/143L 16PR JF568 BL4HJY', '', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (98, '116', 'H1402', '225/70R19.5,245/70R19.5,265/70R19.5,285/70R19.5', '205/75R17.5,215/75R17.5,235/75R17.5,245/70R17.5,ST235/80R16,ST235/85R16', '', '', '', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (99, '116', 'H1204', '315/70R22.5', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,295/80R22.5,315/80R22.5-JF518四层,385/55R22.5,385/65R22.5-JY598四层,425/65R22.5,385/65R22.5-JT599四层', '', '3302001586,3302001139', '295/80R22.5 152/149M 18PR JF518 BL4EJY,295/80R22.5 152/149J 18PR JD756 BL4HJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (100, '116', 'H1105', '275/80R22.5,315/70R22.5,315/80R22.5-JF518四层,295/80R22.5', '', '', '3302001586', '295/80R22.5 152/149M 18PR JF518 BL4EJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度,295/60R22.5', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (101, '116', 'H1502', '11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,385/65R22.5-JY598零度', '385/65R22.5-JT599四层,385/65R22.5-JY598四层,12R22.5-JY601标载零度,12R22.5-JY711载重零度,315/70R22.5,315/80R22.5-JF518四层', '111R22.5-JD571四层,275/80R22.5,295/75R22.5,425/65R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层,255/70R22.5,295/80R22.5,385/55R22.5,315/60R22.5,295/60R22.5,275/70R22.5,13R22.5-JD758零度', '', '', '', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (102, '116', 'H1403', '13R22.5-JD758零度,255/70R22.5,275/70R22.5,385/55R22.5,315/70R22.5,315/80R22.5-JF518四层,295/80R22.5', '11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,385/65R22.5-JY598零度,11R22.5-JD571四层,275/80R22.5,295/60R22.5,295/75R22.5,425/65R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层', '', '3302001273', '275/80R22.5 146/143L 16PR JF568 BL4HJY', '', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (103, '116', 'H1304', '295/80R22.5,315/70R22.5', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,315/80R22.5-JF518四层,385/55R22.5,425/65R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层', '', '3302001586,3302001139', '295/80R22.5 152/149M 18PR JF518 BL4EJY,295/80R22.5 152/149J 18PR JD756 BL4HJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (104, '116', 'H1205', '11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,385/65R22.5-JY598零度', '295/80R22.5,385/65R22.5-JY598四层', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,315/70R22.5,315/80R22.5-JF518四层,385/55R22.5,425/65R22.5,385/65R22.5-JT599四层', '', '', '', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (105, '116', 'H1301', '385/65R22.5-JY598四层', '11R22.5-JD571四层,295/75R22.5', '255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/80R22.5,315/70R22.5,315/80R22.5-JF518四层,385/55R22.5,425/65R22.5,385/65R22.5-JT599四层', '', '', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (106, '116', 'H1202', '11R22.5-JD571四层,275/80R22.5,295/75R22.5', '', '', '', '', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度，\n255/70R22.5,275/70R22.5,295/60R22.5,295/80R22.5,315/70R22.5,315/80R22.5-JF518四层,385/55R22.5,385/65R22.5-JY598四层,425/65R22.5,385/65R22.5-JT599四层', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (107, '116', 'H1103', '11R24.5-JD571,285/75R24.5,325/95R24', '11R22.5-JD571四层,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度\n255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,295/80R22.5,315/70R22.5,315/80R22.5-JD758零度,315/80R22.5-JY711零度,315/80R22.5-JF518四层,385/55R22.5,385/65R22.5-JY598四层,385/65R22.5-JY598零度,425/65R22.5,385/65R22.5-JT599四层', '', '3302001146', '11R24.5 149/146L 16PR JD571 BL4HJY', '', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (108, '116', 'H1401', '205/75R17.5,215/75R17.5,235/75R17.5,245/70R17.5', '225/70R19.5,245/70R19.5,265/70R19.5,285/70R19.5,ST235/80R16,ST235/85R16', '', '3302001572', '235/75R17.5 143/141L 18PR JF518 BL3EJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (109, '116', 'H1302', '385/65R22.5-JY598四层', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,295/80R22.5,315/70R22.5,315/80R22.5-JF518四层,385/55R22.5,425/65R22.5,385/65R22.5-JT599四层', '', '3302001315', '385/65R22.5 164K 24PR BT160 BL4EBL', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '3302001462', '385/65R22.5 164K 24PR JA626 BL4EJY', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (110, '116', 'H1203', '315/70R22.5', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,295/80R22.5,315/70R22.5,315/80R22.5-JF518四层,385/55R22.5,385/65R22.5-JY598四层,425/65R22.5,385/65R22.5-JT599四层', '', '3302000465', '315/70R22.5 156/150L 18PR JD575 BL4EJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (111, '116', 'H1104', '315/80R22.5-JF518四层', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,295/80R22.5,315/70R22.5,385/55R22.5,385/65R22.5-JY598四层,425/65R22.5,385/65R22.5-JT599四层', '', '3302000609,3302001236', '315/80R22.5 156/153L 20PR BD175 BL4EBL,315/80R22.5 156/153L 20PR JF518 BL4EJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (112, '116', 'H1101', '11R22.5-JD571四层,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,295/80R22.5,315/70R22.5,315/80R22.5-JD758零度,315/80R22.5-JY711零度,315/80R22.5-JF518四层,385/55R22.5,385/65R22.5-JY598四层,385/65R22.5-JY598零度,425/65R22.5,385/65R22.5-JT599四层', '', '', '3302001270', '385/65R22.5 164K 24PR JT560 BL0EJY', '', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (113, '116', 'H1201', '11R22.5-JD571四层,275/80R22.5,295/75R22.5', '', '', '3302001273,3302001143', '275/80R22.5 146/143L 16PR JF568 BL4HJY,11R22.5 146/143L 16PR JD571 BL4HJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度，\n255/70R22.5,275/70R22.5,295/60R22.5,295/80R22.5,315/70R22.5,315/80R22.5-JF518四层,385/55R22.5,385/65R22.5-JY598四层,425/65R22.5,385/65R22.5-JT599四层', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (114, '116', 'H1102', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重\n12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度', '13R22.5-JD758零度,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,295/80R22.5,315/70R22.5,315/80R22.5-JD758零度,315/80R22.5-JY711零度,315/80R22.5-JF518四层,385/55R22.5,385/65R22.5-JY598四层,385/65R22.5-JY598零度,425/65R22.5,385/65R22.5-JT599四层', '', '', '', '', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (115, '116', 'H1505', '255/70R22.5,275/70R22.5-RFID,295/60R22.5,385/55R22.5', '11R22.5-JD571四层,275/80R22.5,295/75R22.5,315/80R22.5-JF518四层,425/65R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层,295/80R22.5,315/70R22.5', '', '3302000837', '385/55R22.5 160K 20PR JT560 BL4EJY', '', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (116, '116', 'H1503', '205/75R17.5,215/75R17.5,235/75R17.5,245/70R17.5,225/70R19.5,245/70R19.5,265/70R19.5,285/70R19.5,ST235/80R16,ST235/85R16', '385/65R22.5-JT599四层,385/65R22.5-JY598四层,315/70R22.5,315/80R22.5-JF518四层,11R22.5-JD571四层,275/80R22.5,295/75R22.5,425/65R22.5,255/70R22.5,295/80R22.5,385/55R22.5,295/60R22.5,275/70R22.5', '', '3302001143', '11R22.5 146/143L 16PR JD571 BL4HJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (117, '116', 'H1404', '385/55R22.5,315/60R22.5,295/60R22.5,275/70R22.5-RFID,13R22.5-JD758零度', '11R22.5-JY601零度\n12R22.5-JY601标载零度\n12R22.5-JY711载重零度\n385/65R22.5-JY598零度\n11R22.5-JD571四层\n275/80R22.5\n295/75R22.5\n425/65R22.5\n385/65R22.5-JT599四层\n385/65R22.5-JY598四层\n255/70R22.5\n315/70R22.5\n315/80R22.5-JF518四层\n295/80R22.5', '', '3302001582', '295/60R22.5 150/147L 18PR JD577 BL4EJY', '', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (118, '116', 'H1305', '315/80R22.5-JD758零度,315/80R22.5-JY711零度,13R22.5-JD758零度', '11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,385/65R22.5-JY598零度', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,315/80R22.5-JF518四层,385/55R22.5,425/65R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层,295/80R22.5,315/70R22.5', '', '', '', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (119, '116', 'H1405', '11R24.5-JD571,285/75R24.5', '325/95R24', '11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,385/65R22.5-JY598零度,11R22.5-JD571四层,275/80R22.5,295/75R22.5,425/65R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层,255/70R22.5,315/70R22.5,315/80R22.5-JF518四层,295/80R22.5,385/55R22.5,315/60R22.5,295/60R22.5,275/70R22.5,13R22.5-JD758零度', '3302001146', '11R24.5 149/146L 16PR JD571 BL4HJY', '', '', '', '测试环境数据', 1, '2026-03-04 15:21:56', '2026-03-04 15:21:56', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (120, '116', 'H1501', '325/95R24,295/80R22.5', '11R22.5-JD571四层,11R22.5-JY601零度,11R24.5-JD571,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,255/70R22.5,275/80R22.5,285/75R24.5,295/60R22.5,295/75R22.5,315/70R22.5,315/80R22.5-JF518四层,385/55R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层,385/65R22.5-JY598零度,425/65R22.5', '', '3302001586,3302001273', '295/80R22.5 152/149M 18PR JF518 BL4EJY,275/80R22.5 146/143L 16PR JF568 BL4HJY', '', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-04-01 14:36:43', 'testing01', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (121, '116', 'H1402', '225/70R19.5,245/70R19.5,285/70R19.5', '205/75R17.5,215/75R17.5,235/75R17.5,245/70R17.5,ST235/80R16,ST235/85R16', '', '', '', '11.00R20-JD727载重,11.00R20-JY601标载,11R22.5-JY601零度,12.00R20-JD727,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度,9.00R20-JY601,435/50R19.5,445/45R19.5', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-03-31 15:17:56', 'testing01', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (122, '116', 'H1303', '435/50R19.5,445/45R19.5', '315/70R22.5,315/80R22.5-JF518四层,385/65R22.5-JY598四层,425/65R22.5', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,385/55R22.5,385/65R22.5-JT599四层', '3302000906,3302000836', '435/50R19.5 160J 20PR JT560 BL4EJY,445/45R19.5 160J 20PR JT560 BL4EJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-04-01 14:54:15', 'testing01', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (123, '116', 'H1204', '315/70R22.5,295/80R22.5', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,315/80R22.5-JF518四层,385/55R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层,425/65R22.5', '', '3302001586,3302001139', '295/80R22.5 152/149M 18PR JF518 BL4EJY,295/80R22.5 152/149J 18PR JD756 BL4HJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-04-01 14:36:18', 'testing01', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (124, '116', 'H1105', '275/80R22.5,315/70R22.5,315/80R22.5-JF518四层,295/80R22.5', '', '', '3302001586', '295/80R22.5 152/149M 18PR JF518 BL4EJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度,295/60R22.5', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-04-01 14:36:02', 'testing01', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (125, '116', 'H1502', '11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,385/65R22.5-JY598零度', '385/65R22.5-JT599四层,385/65R22.5-JY598四层,12R22.5-JY601标载零度,12R22.5-JY711载重零度,315/70R22.5,315/80R22.5-JF518四层', '111R22.5-JD571四层,275/80R22.5,295/75R22.5,425/65R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层,255/70R22.5,295/80R22.5,385/55R22.5,315/60R22.5,295/60R22.5,275/70R22.5,13R22.5-JD758零度', '', '', '', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-03-30 16:40:49', 'testing01', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (126, '116', 'H1403', '13R22.5-JD758零度,255/70R22.5,275/70R22.5,315/70R22.5,315/80R22.5-JF518四层,385/55R22.5', '11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,385/65R22.5-JY598零度,11R22.5-JD571四层,275/80R22.5,295/60R22.5,295/75R22.5,425/65R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层', '', '3302001273', '275/80R22.5 146/143L 16PR JF568 BL4HJY', '', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-04-01 14:22:39', 'testing01', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (127, '116', 'H1304', '315/70R22.5', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,315/80R22.5-JF518四层,385/55R22.5,425/65R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层', '', '3302001586,3302001139', '295/80R22.5 152/149M 18PR JF518 BL4EJY,295/80R22.5 152/149J 18PR JD756 BL4HJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-04-01 14:22:31', 'testing01', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (128, '116', 'H1205', '11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,385/65R22.5-JY598零度', '385/65R22.5-JY598四层', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,315/70R22.5,315/80R22.5-JF518四层,385/55R22.5,425/65R22.5,385/65R22.5-JT599四层', '', '', '', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-04-01 14:24:27', 'testing01', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (129, '116', 'H1301', '385/65R22.5-JY598四层', '11R22.5-JD571四层,295/75R22.5', '255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/80R22.5,315/70R22.5,315/80R22.5-JF518四层,385/55R22.5,425/65R22.5,385/65R22.5-JT599四层', '', '', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-03-30 16:40:49', 'testing01', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (130, '116', 'H1202', '11R22.5-JD571四层,275/80R22.5,295/75R22.5', '', '', '', '', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度,255/70R22.5,275/70R22.5,295/60R22.5,295/80R22.5,315/70R22.5,315/80R22.5-JF518四层,385/55R22.5,385/65R22.5-JY598四层,425/65R22.5,385/65R22.5-JT599四层', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-03-30 16:40:49', 'testing01', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (131, '116', 'H1103', '11R24.5-JD571,285/75R24.5,325/95R24', '11R22.5-JD571四层,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,255/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,315/70R22.5,315/80R22.5-JD758零度,315/80R22.5-JF518四层,315/80R22.5-JY711零度', '', '3302001146', '11R24.5 149/146L 16PR JD571 BL4HJY', '', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-04-01 14:23:39', 'testing01', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (132, '116', 'H1401', '205/75R17.5,215/75R17.5,235/75R17.5,245/70R17.5', '225/70R19.5,245/70R19.5,285/70R19.5,ST235/80R16,ST235/85R16', '', '3302001572', '235/75R17.5 143/141L 18PR JF518 BL3EJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-03-31 15:18:27', 'testing01', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (133, '116', 'H1302', '385/65R22.5-JY598四层', '255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,315/70R22.5,315/80R22.5-JF518四层,385/55R22.5,385/65R22.5-JT599四层,425/65R22.5', '', '3302001315', '385/65R22.5 164K 24PR BT160 BL4EBL', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '3302001462', '385/65R22.5 164K 24PR JA626 BL4EJY', NULL, 0, '2026-03-30 16:40:48', '2026-04-01 14:24:35', 'testing01', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (134, '116', 'H1203', '', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,315/70R22.5,315/80R22.5-JF518四层,385/55R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层,425/65R22.5,285/70R19.5', '', '3302000465', '315/70R22.5 156/150L 18PR JD575 BL4EJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-04-01 14:55:23', 'testing01', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (135, '116', 'H1104', '315/80R22.5-JF518四层', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,315/70R22.5,385/55R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层,425/65R22.5', '', '3302000609,3302001236', '315/80R22.5 156/153L 20PR BD175 BL4EBL,315/80R22.5 156/153L 20PR JF518 BL4EJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-04-01 14:23:45', 'testing01', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (136, '116', 'H1101', '11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,315/70R22.5,315/80R22.5-JD758零度,315/80R22.5-JF518四层,315/80R22.5-JY711零度,385/55R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层,385/65R22.5-JY598零度,425/65R22.5', '10.00R20-JD727载重,10.00R20-JY601标载,11.00R20-JD727载重,11.00R20-JY601标载', '', '3302001270', '385/65R22.5 164K 24PR JT560 BL0EJY', '', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-04-01 14:22:17', 'testing01', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (137, '116', 'H1201', '11R22.5-JD571四层,275/80R22.5,295/75R22.5', '', '', '3302001273,3302001143', '275/80R22.5 146/143L 16PR JF568 BL4HJY,11R22.5 146/143L 16PR JD571 BL4HJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度,255/70R22.5,275/70R22.5,295/60R22.5,295/80R22.5,315/70R22.5,315/80R22.5-JF518四层,385/55R22.5,385/65R22.5-JY598四层,425/65R22.5,385/65R22.5-JT599四层', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-03-30 16:40:49', 'testing01', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (138, '116', 'H1102', '11.00R20-JD727载重,11.00R20-JY601标载,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,12.00R20-JD727', '13R22.5-JD758零度,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,315/70R22.5,315/80R22.5-JD758零度,315/80R22.5-JF518四层,315/80R22.5-JY711零度,385/55R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层,385/65R22.5-JY598零度,425/65R22.5', '', '', '', '', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-04-01 14:23:25', 'testing01', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (139, '116', 'H1505', '255/70R22.5,275/70R22.5-RFID,295/60R22.5,385/55R22.5', '11R22.5-JD571四层,275/80R22.5,295/75R22.5,315/70R22.5,315/80R22.5-JF518四层,385/65R22.5-JT599四层,385/65R22.5-JY598四层,425/65R22.5', '', '3302000837', '385/55R22.5 160K 20PR JT560 BL4EJY', '', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-04-01 14:24:44', 'testing01', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (140, '116', 'H1503', '205/75R17.5,215/75R17.5,225/70R19.5,235/75R17.5,245/70R17.5,245/70R19.5,285/70R19.5,ST235/80R16,ST235/85R16', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,315/70R22.5,315/80R22.5-JF518四层,385/55R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层,425/65R22.5', '', '3302001143', '11R22.5 146/143L 16PR JD571 BL4HJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-04-01 14:24:52', 'testing01', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (141, '116', 'H1404', '385/55R22.5,315/60R22.5,295/60R22.5,275/70R22.5-RFID,13R22.5-JD758零度', '11R22.5-JD571四层,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,255/70R22.5,275/80R22.5,295/75R22.5,315/70R22.5,315/80R22.5-JF518四层,385/65R22.5-JT599四层,385/65R22.5-JY598四层,385/65R22.5-JY598零度,425/65R22.5', '', '3302001582', '295/60R22.5 150/147L 18PR JD577 BL4EJY', '', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-04-01 14:25:06', 'testing01', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (142, '116', 'H1305', '315/80R22.5-JD758零度,315/80R22.5-JY711零度,13R22.5-JD758零度', '11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,385/65R22.5-JY598零度', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,315/80R22.5-JF518四层,385/55R22.5,425/65R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层,295/80R22.5,315/70R22.5', '', '', '', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-03-30 16:40:49', 'testing01', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (143, '116', 'H1504', '11R22.5-JD571四层,255/70R22.5,275/70R22.5,275/80R22.5,295/60R22.5,295/75R22.5,315/70R22.5,315/80R22.5-JF518四层,385/55R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层,425/65R22.5,295/80R22.5', '', '', '3302001586', '295/80R22.5 152/149M 18PR JF518 BL4EJY', '9.00R20-JY601,11.00R20-JY601标载,11.00R20-JD727载重,12.00R20-JD727,11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,13R22.5-JD758零度,315/80R22.5-JD758零度,315/80R22.5-JY711零度,325/95R24,385/65R22.5-JY598零度', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-04-01 14:36:58', 'testing01', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed` VALUES (144, '116', 'H1405', '11R24.5-JD571,285/75R24.5', '325/95R24', '11R22.5-JY601零度,12R22.5-JY601标载零度,12R22.5-JY711载重零度,385/65R22.5-JY598零度,11R22.5-JD571四层,275/80R22.5,295/75R22.5,425/65R22.5,385/65R22.5-JT599四层,385/65R22.5-JY598四层,255/70R22.5,315/70R22.5,315/80R22.5-JF518四层,295/80R22.5,385/55R22.5,315/60R22.5,295/60R22.5,275/70R22.5,13R22.5-JD758零度', '3302001146', '11R24.5 149/146L 16PR JD571 BL4HJY', '', '', '', NULL, 0, '2026-03-30 16:40:48', '2026-03-30 16:40:49', 'testing01', 'testing01');

-- ----------------------------
-- Table structure for t_mdm_cx_machine_fixed_20260303
-- ----------------------------
DROP TABLE IF EXISTS `t_mdm_cx_machine_fixed_20260303`;
CREATE TABLE `t_mdm_cx_machine_fixed_20260303`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `FACTORY_CODE` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工厂编号',
  `CX_MACHINE_CODE` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '成型机编码',
  `FIXED_STRUCTURE1` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '固定结构1 多个以,分隔拼接',
  `FIXED_STRUCTURE2` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '固定结构1 多个以,分隔拼接',
  `FIXED_STRUCTURE3` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '固定结构1 多个以,分隔拼接',
  `FIXED_MATERIAL_CODE` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '固定SKU  多个以,分隔拼接',
  `FIXED_MATERIAL_DESC` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '固定物料描述  多个以,分隔拼接',
  `DISABLE_STRUCTURE` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '不可作业结构  多个以,分隔拼接',
  `DISABLE_MATERIAL_CODE` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '不可作业SKU  多个以,分隔拼接',
  `DISABLE_MATERIAL_DESC` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '不可作业物料描述  多个以,分隔拼接',
  `REMARK` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `IS_DELETE` decimal(1, 0) NOT NULL DEFAULT 0 COMMENT '是否删除（0：默认未删除 1：已删除）',
  `CREATE_TIME` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `CREATE_BY` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `UPDATE_BY` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `IDX_MDM_CX_MACHINE_FIXED_MOLDING_MACHINE_CODE`(`CX_MACHINE_CODE` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 71 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '成型固定机台表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_mdm_cx_machine_fixed_20260303
-- ----------------------------
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (1, '116', '111', '1', '2', '3', '3302002083', NULL, '22', '3302002083', NULL, NULL, 1, '2025-12-29 09:36:17', '2025-12-29 09:36:17', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (2, '116', 'A1', '', '', '', '', NULL, '', '', NULL, NULL, 1, '2025-12-29 10:33:01', '2025-12-29 10:33:14', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (3, '116', '66666', '315/80R22.5EDR53', '295/80R22.5EAM62+', '295/80R22.5BA220+', '3302002083,3302002254,3302002084,3302002082,3302002562,3302002566,3302002568,3302002558,3302002556,3302002557', NULL, '315/70R22.5EDR53', '3302002083,3302002254', NULL, NULL, 1, '2025-12-29 11:10:09', '2025-12-30 11:27:06', 'admin', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (4, '116', 'psl12250001', 'ST235/80R16,315/80R22.5-JF518四层', '315/80R22.5-JY711零度,315/70R22.5', '315/80R22.5-JD758零度,315/70R25.5', '3302002083,3302002254', NULL, '295/80R22.5,315/60R22.5', '3302002082,3302002562', NULL, NULL, 1, '2025-12-29 17:23:48', '2025-12-31 09:39:47', 'vege', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (5, '116', 'sp122511001', '10.00R20 149/146K 18PR BD260 BT0HBL', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, '2025-12-31 09:37:21', '2025-12-31 14:51:50', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (6, '116', 'H1304', '10.00R20-JD727载重,11.00R20-JD727载重,11.00R20-JY601标载', '10.00R20 149/146K 18PR BD260 BT0HBL,10.00R20 149/146K 18PR JA767 BT0HJY,10.00R20 149/146K 18PR JD727 BT0HJY', '10.00R20 149/146K 18PR JD727 BT0HJY,11.00R20 152/149J 18PR JD681 BT0HJY', '3302000207,3302000315', NULL, '10.00R20-JD727载重,11.00R20-JD727载重,11.00R20-JY601标载', '3302000207,3302000315', NULL, NULL, 1, '2026-01-05 14:10:49', '2026-01-06 13:26:32', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (7, '116', 'H1404', '445/45R19.5', '12R22.5-JY601标载零度', '11R24.5-JD571', NULL, NULL, '285/75R24.5', '3302000302', NULL, NULL, 1, '2026-01-07 14:56:02', '2026-01-13 20:49:26', 'admin', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (9, '116', '01', '123,222,333', '', '', 'qqq,www,eee', NULL, '111', 'aaa', NULL, NULL, 1, '2026-01-09 09:22:07', '2026-01-09 09:22:07', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (10, '116', 'H1505', '10.00R20-JD727载重', '', '', '', NULL, '', '', NULL, NULL, 1, '2026-01-13 11:51:38', '2026-01-13 11:51:38', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (11, '116', 'H1101', '10.00R20-JD727载重,11.00R20-JD727载重,11.00R20-JY601标载', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, '2026-01-13 13:43:24', '2026-01-13 15:43:01', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (12, '116', 'H1504', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, '2026-01-13 14:05:39', '2026-01-13 14:05:44', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (13, '116', 'H1504', '295/75R22.5', NULL, NULL, NULL, NULL, NULL, '3202000563', NULL, NULL, 1, '2026-01-13 15:03:10', '2026-01-13 20:06:52', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (14, '116', 'H1504', NULL, NULL, NULL, NULL, NULL, NULL, '3302000258,3302000026', NULL, NULL, 1, '2026-01-13 20:55:47', '2026-01-14 09:11:48', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (15, '116', 'H1505', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, '2026-01-14 14:47:02', '2026-01-14 14:47:07', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (16, '116', 'H1505', NULL, NULL, NULL, NULL, NULL, '11.00R20-JD727载重,295/75R22.5', NULL, NULL, NULL, 1, '2026-01-15 14:49:47', '2026-01-15 14:52:14', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (17, '116', 'H1502', '275/70R22.5', '245/70R19.5', NULL, NULL, NULL, '435/50R19.5', '3302001538,3302002582', NULL, NULL, 1, '2026-01-15 16:16:16', '2026-01-15 17:05:36', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (18, '116', 'H1405', '245/70R19.5', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, '2026-01-15 17:09:48', '2026-01-15 17:09:48', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (19, '116', 'H1503', NULL, NULL, NULL, NULL, NULL, '295/75R22.5', NULL, NULL, NULL, 1, '2026-01-15 17:30:11', '2026-01-15 17:30:11', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (20, '116', 'H1502', NULL, NULL, NULL, NULL, NULL, '13R22.5-JD758零度,11R24.5-JD571,275/70R22.5', NULL, NULL, NULL, 1, '2026-01-15 17:30:40', '2026-01-15 17:32:45', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (21, '116', 'H1504', NULL, NULL, NULL, NULL, NULL, '11R22.5-JD571四层', NULL, NULL, NULL, 1, '2026-01-15 17:30:59', '2026-01-15 17:30:59', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (22, '116', 'H1402', '11R22.5-JY601零度', '12.00R20-JD727', '12R22.5-JY601标载零度', '3302002758', NULL, '285/75R24.5', '3302002104', NULL, NULL, 1, '2026-01-15 17:31:38', '2026-01-15 17:36:38', 'jy_test', 'jy_test');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (23, '116', 'H1101', '11R22.5-JD571四层\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n385/65R22.5-JY598零度,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '', '3302001270', NULL, '', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (24, '116', 'H1102', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度', '13R22.5-JD758零度,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n385/65R22.5-JY598零度,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '', NULL, '', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (25, '116', 'H1103', '11R24.5-JD571,\n285/75R24.5,\n325/95R24', '11R22.5-JD571四层,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n385/65R22.5-JY598零度,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '3302001146', NULL, '', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (26, '116', 'H1104', '315/80R22.5-JF518四层', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n385/55R22.5,\n385/65R22.5-JY598四层,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '3302000609,\n315/80R22.5 156/153L 20PR JF518 BL4EJY', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (27, '116', 'H1105', '275/80R22.5 ,\n315/70R22.5,\n315/80R22.5-JF518四层,\n295/80R22.5', '', '', '3302001586', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度,\n295/60R22.5', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (28, '116', 'H1201', '11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5', '255/70R22.5,\n275/70R22.5,\n295/60R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '3302001273,3302001143', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (29, '116', 'H1202', '11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5', '255/70R22.5,\n275/70R22.5,\n295/60R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '', NULL, '9.00R20-JY601,\n11.00R20-JY601标载\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (30, '116', 'H1203', '315/70R22.5', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '3302000465', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (31, '116', 'H1204', '315/70R22.5', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '3302001586,\n295/80R22.5 152/149J 18PR JD756 BL4HJY', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (32, '116', 'H1205', '11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n385/65R22.5-JY598零度', '295/80R22.5,385/65R22.5-JY598四层', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层', '', NULL, '', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (33, '116', 'H1301', '385/65R22.5-JY598四层', '11R22.5-JD571四层,\n295/75R22.5', '255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层', '', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (34, '116', 'H1302', '385/65R22.5-JY598四层', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '3302001315', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '3302001462', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (35, '116', 'H1304', '295/80R22.5,315/70R22.5', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层', '', '3302001586,\n295/80R22.5 152/149J 18PR JD756 BL4HJY', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (36, '116', 'H1305', '315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n13R22.5-JD758零度', '11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n385/65R22.5-JY598零度', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n295/80R22.5,\n315/70R22.5', '', NULL, '', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (37, '116', 'H1401', '205/75R17.5,\n215/75R17.5,\n235/75R17.5,\n245/70R17.5', '225/70R19.5,\n245/70R19.5,\n265/70R19.5,\n285/70R19.5,\nST235/80R16,\nST235/85R16', '', '3302001572', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (38, '116', 'H1402', '225/70R19.5,\n245/70R19.5,\n265/70R19.5,\n285/70R19.5', '205/75R17.5,\n215/75R17.5,\n235/75R17.5,\n245/70R17.5,\nST235/80R16,\nST235/85R16', '', '', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (39, '116', 'H1403', '13R22.5-JD758零度,\n255/70R22.5,\n275/70R22.5,\n385/55R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n295/80R22.5', '11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n385/65R22.5-JY598零度,\n11R22.5-JD571四层,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层', '', '3302001273', NULL, '', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (40, '116', 'H1404', '385/55R22.5,\n315/60R22.5,\n295/60R22.5,\n275/70R22.5-RFID,\n13R22.5-JD758零度', '11R22.5-JY601零度\n12R22.5-JY601标载零度\n12R22.5-JY711载重零度\n385/65R22.5-JY598零度\n11R22.5-JD571四层\n275/80R22.5\n295/75R22.5\n425/65R22.5\n385/65R22.5-JT599四层\n385/65R22.5-JY598四层\n255/70R22.5\n315/70R22.5\n315/80R22.5-JF518四层\n295/80R22.5', '', '3302001582', NULL, '', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (41, '116', 'H1405', '11R24.5-JD571,285/75R24.5', '325/95R24', '11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n385/65R22.5-JY598零度,\n11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n255/70R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n295/80R22.5,\n385/55R22.5,\n315/60R22.5,\n295/60R22.5,\n275/70R22.5,\n13R22.5-JD758零度', '3302001146', NULL, '', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (42, '116', 'H1501', '325/95R24', '11R24.5-JD571,\n285/75R24.5,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n385/65R22.5-JY598零度,\n11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n255/70R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n295/80R22.5,\n385/55R22.5,\n295/60R22.5,\n275/70R22.5-RFID,\n13R22.5-JD758零度', '', '3302001586,3302001273', NULL, '', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (43, '116', 'H1502', '11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n385/65R22.5-JY598零度', '385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n315/70R22.5,\n315/80R22.5-JF518四层', '111R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n255/70R22.5,\n295/80R22.5,\n385/55R22.5,\n315/60R22.5,\n295/60R22.5,\n275/70R22.5,\n13R22.5-JD758零度', '', NULL, '', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (44, '116', 'H1503', '205/75R17.5,\n215/75R17.5,\n235/75R17.5,\n245/70R17.5,\n225/70R19.5,\n245/70R19.5,\n265/70R19.5,\n285/70R19.5,\nST235/80R16,\nST235/85R16', '385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n315/70R22.5,\n315/80R22.5-JF518四层,\n11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5,\n425/65R22.5,\n255/70R22.5,\n295/80R22.5,\n385/55R22.5,\n295/60R22.5,\n275/70R22.5', '', '3302001143', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (45, '116', 'H1504', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n295/80R22.5,\n315/70R22.5', '', '', '3302001586', NULL, '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (46, '116', 'H1505', '255/70R22.5,\n275/70R22.5-RFID,\n295/60R22.5,\n385/55R22.5', '11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5,\n315/80R22.5-JF518四层,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n295/80R22.5,\n315/70R22.5', '', '3302000837', NULL, '', '', NULL, NULL, 1, '2026-01-19 18:40:30', '2026-01-19 18:40:30', 'admin', 'admin');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (47, '116', 'H1101', '11R22.5-JD571四层，\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n385/65R22.5-JY598零度,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '', '3302001270', '385/65R22.5 164K 24PR JT560 BL0EJY', '', '', '', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (48, '116', 'H1102', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度', '13R22.5-JD758零度,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n385/65R22.5-JY598零度,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '', '', '', '', '', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (49, '116', 'H1103', '11R24.5-JD571,\n285/75R24.5,\n325/95R24', '11R22.5-JD571四层,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n385/65R22.5-JY598零度,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '3302001146', '11R24.5 149/146L 16PR JD571 BL4HJY', '', '', '', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (50, '116', 'H1104', '315/80R22.5-JF518四层', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n385/55R22.5,\n385/65R22.5-JY598四层,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '3302000609,3302001236', '315/80R22.5 156/153L 20PR BD175 BL4EBL,315/80R22.5 156/153L 20PR JF518 BL4EJY', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', '', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (51, '116', 'H1105', '275/80R22.5 ,\n315/70R22.5,\n315/80R22.5-JF518四层,\n295/80R22.5', '', '', '3302001586', '295/80R22.5 152/149M 18PR JF518 BL4EJY', '9.00R20-JY601,\n11.00R20-JY601标载,,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度,\n295/60R22.5', '', '', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (52, '116', 'H1201', '11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5', '', '', '3302001273,3302001143', '275/80R22.5 146/143L 16PR JF568 BL4HJY,11R22.5 146/143L 16PR JD571 BL4HJY', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度，\n255/70R22.5,\n275/70R22.5,\n295/60R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (53, '116', 'H1202', '11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5', '', '', '', '', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度，\n255/70R22.5,\n275/70R22.5,\n295/60R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (54, '116', 'H1203', '315/70R22.5', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '3302000465', '315/70R22.5 156/150L 18PR JD575 BL4EJY', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', '', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (55, '116', 'H1204', '315/70R22.5', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n385/65R22.5-JY598四层,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '3302001586,3302001139', '295/80R22.5 152/149M 18PR JF518 BL4EJY,295/80R22.5 152/149J 18PR JD756 BL4HJY', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', '', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (56, '116', 'H1205', '11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n385/65R22.5-JY598零度', '295/80R22.5,385/65R22.5-JY598四层', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '', '', '', '', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (57, '116', 'H1301', '385/65R22.5-JY598四层', '11R22.5-JD571四层,\n295/75R22.5', '255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', '', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (58, '116', 'H1302', '385/65R22.5-JY598四层', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n295/80R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层', '', '3302001315', '385/65R22.5 164K 24PR BT160 BL4EBL', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '3302001462', '385/65R22.5 164K 24PR JA626 BL4EJY', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (59, '116', 'H1304', '295/80R22.5,315/70R22.5', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层', '', '3302001586,3302001139', '295/80R22.5 152/149M 18PR JF518 BL4EJY,295/80R22.5 152/149J 18PR JD756 BL4HJY', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', '', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (60, '116', 'H1305', '315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n13R22.5-JD758零度', '11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n385/65R22.5-JY598零度', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n295/80R22.5,\n315/70R22.5', '', '', '', '', '', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (61, '116', 'H1401', '205/75R17.5,\n215/75R17.5,\n235/75R17.5,\n245/70R17.5', '225/70R19.5,\n245/70R19.5,\n265/70R19.5,\n285/70R19.5,\nST235/80R16,\nST235/85R16', '', '3302001572', '235/75R17.5 143/141L 18PR JF518 BL3EJY', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', '', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (62, '116', 'H1402', '225/70R19.5,\n245/70R19.5,\n265/70R19.5,\n285/70R19.5', '205/75R17.5,\n215/75R17.5,\n235/75R17.5,\n245/70R17.5,\nST235/80R16,\nST235/85R16', '', '', '', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', '', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (63, '116', 'H1403', '13R22.5-JD758零度,\n255/70R22.5,\n275/70R22.5,\n385/55R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n295/80R22.5', '11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n385/65R22.5-JY598零度,\n11R22.5-JD571四层,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层', '', '3302001273', '275/80R22.5 146/143L 16PR JF568 BL4HJY', '', '', '', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (64, '116', 'H1404', '385/55R22.5,\n315/60R22.5,\n295/60R22.5,\n275/70R22.5-RFID,\n13R22.5-JD758零度', '11R22.5-JY601零度\n12R22.5-JY601标载零度\n12R22.5-JY711载重零度\n385/65R22.5-JY598零度\n11R22.5-JD571四层\n275/80R22.5\n295/75R22.5\n425/65R22.5\n385/65R22.5-JT599四层\n385/65R22.5-JY598四层\n255/70R22.5\n315/70R22.5\n315/80R22.5-JF518四层\n295/80R22.5', '', '3302001582', '295/60R22.5 150/147L 18PR JD577 BL4EJY', '', '', '', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (65, '116', 'H1405', '11R24.5-JD571,285/75R24.5', '325/95R24', '11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n385/65R22.5-JY598零度,\n11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n255/70R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n295/80R22.5,\n385/55R22.5,\n315/60R22.5,\n295/60R22.5,\n275/70R22.5,\n13R22.5-JD758零度', '3302001146', '11R24.5 149/146L 16PR JD571 BL4HJY', '', '', '', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (66, '116', 'H1501', '325/95R24', '11R24.5-JD571,\n285/75R24.5,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n385/65R22.5-JY598零度,\n11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n255/70R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n295/80R22.5,\n385/55R22.5,\n295/60R22.5,\n275/70R22.5-RFID,\n13R22.5-JD758零度', '', '3302001586,3302001273', '295/80R22.5 152/149M 18PR JF518 BL4EJY,275/80R22.5 146/143L 16PR JF568 BL4HJY', '', '', '', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (67, '116', 'H1502', '11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n385/65R22.5-JY598零度', '385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n315/70R22.5,\n315/80R22.5-JF518四层', '111R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n255/70R22.5,\n295/80R22.5,\n385/55R22.5,\n315/60R22.5,\n295/60R22.5,\n275/70R22.5,\n13R22.5-JD758零度', '', '', '', '', '', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (68, '116', 'H1503', '205/75R17.5,\n215/75R17.5,\n235/75R17.5,\n245/70R17.5,\n225/70R19.5,\n245/70R19.5,\n265/70R19.5,\n285/70R19.5,\nST235/80R16,\nST235/85R16', '385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n315/70R22.5,\n315/80R22.5-JF518四层,\n11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5,\n425/65R22.5,\n255/70R22.5,\n295/80R22.5,\n385/55R22.5,\n295/60R22.5,\n275/70R22.5', '', '3302001143', '11R22.5 146/143L 16PR JD571 BL4HJY', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', '', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (69, '116', 'H1504', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n315/80R22.5-JF518四层,\n385/55R22.5,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n295/80R22.5,\n315/70R22.5', '', '', '3302001586', '295/80R22.5 152/149M 18PR JF518 BL4EJY', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', '', NULL, 1, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (70, '116', 'H1505', '255/70R22.5,\n275/70R22.5-RFID,\n295/60R22.5,\n385/55R22.5', '11R22.5-JD571四层,\n275/80R22.5,\n295/75R22.5,\n315/80R22.5-JF518四层,\n425/65R22.5,\n385/65R22.5-JT599四层,\n385/65R22.5-JY598四层,\n295/80R22.5,\n315/70R22.5', '', '3302000837', '385/55R22.5 160K 20PR JT560 BL4EJY', '', '', '', NULL, 0, '2026-01-20 09:42:47', '2026-01-30 15:14:12', 'jy_test', 'testing01');
INSERT INTO `t_mdm_cx_machine_fixed_20260303` VALUES (71, '116', 'H1303', '435/50R19.5,445/45R19.5', '425/65R22.5,\n315/70R22.5,\n315/80R22.5-JF518四层,\n295/80R22.5,\n385/65R22.5-JY598四层', '11R22.5-JD571四层,\n255/70R22.5,\n275/70R22.5,\n275/80R22.5,\n295/60R22.5,\n295/75R22.5,\n385/55R22.5,\n385/65R22.5-JT599四层', '3302000906,3302000836', '435/50R19.5 160J 20PR JT560 BL4EJY,445/45R19.5 160J 20PR JT560 BL4EJY', '9.00R20-JY601,\n11.00R20-JY601标载,\n11.00R20-JD727载重,\n12.00R20-JD727,\n11R22.5-JY601零度,\n12R22.5-JY601标载零度,\n12R22.5-JY711载重零度,\n13R22.5-JD758零度,\n315/80R22.5-JD758零度,\n315/80R22.5-JY711零度,\n325/95R24,\n385/65R22.5-JY598零度', '', '', NULL, 0, '2026-01-28 21:36:42', '2026-01-30 15:14:12', 'testing01', 'testing01');

-- ----------------------------
-- Table structure for t_mdm_cx_machine_online_info
-- ----------------------------
DROP TABLE IF EXISTS `t_mdm_cx_machine_online_info`;
CREATE TABLE `t_mdm_cx_machine_online_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ONLINE_DATE` date NULL DEFAULT NULL COMMENT '在机日期',
  `CX_CODE` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '成型机台',
  `MATERIAL_CODE` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '在机物料编码（NC）',
  `MES_MATERIAL_CODE` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '在机物料编码（MES）',
  `SPEC_DESC` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '在机物料描述',
  `EMBRYO_SPEC` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '在机胎胚描述',
  `DATA_VERSION` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '版本号',
  `COMPANY_CODE` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分公司编码',
  `FACTORY_CODE` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '厂别',
  `IS_DELETE` decimal(1, 0) NULL DEFAULT NULL COMMENT '删除标识',
  `CREATE_BY` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `UPDATE_BY` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '修改人',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `REMARK` varchar(900) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_mdm_cx_machine_online_cxjt`(`CX_CODE` ASC) USING BTREE,
  INDEX `idx_mdm_cx_machine_online_fgs`(`COMPANY_CODE` ASC) USING BTREE,
  INDEX `idx_mdm_cx_machine_online_fc`(`FACTORY_CODE` ASC) USING BTREE,
  INDEX `idx_mdm_cx_machine_online_v`(`DATA_VERSION` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6367 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'S0-1202-成型在机信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_mdm_cx_machine_online_info
-- ----------------------------
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6367, '2026-03-26', 'H1503', '215103975', '3302002318', '11R22.5 146/143L 16PR EAM68 BL4HEG', '11R22.5 146/143L 16PR EAM68 BL4HEG', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 20');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6368, '2026-03-26', 'H1503', '215101729', '3302001143', '11R22.5 146/143L 16PR JD571 BL4HJY', '11R22.5 146/143L 16PR JD571 BL4HJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 60');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6369, '2026-03-26', 'H1503', '215101828', '3302001067', '11R22.5 146/143L 16PR AF508 BL4HAM', '11R22.5 146/143L 16PR JF568 BL4HJY TW', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 10');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6370, '2026-03-26', 'H1503', '215102719', '3302001546', '11R22.5 144/142M 14PR AT159 BL4HAM', '11R22.5 146/143M 16PR JT582 BL4HJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 6');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6371, '2026-03-26', 'H1405', '215101731', NULL, '11R24.5 146/143L 14PR JD571 BL4HJY', '11R24.5 149/146L 16PR JD571 BL4HJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 40');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6372, '2026-03-26', 'H1405', '215101888', '3302001243', '11R24.5 149/146L 16PR AD515 BL4HAM', '11R24.5 149/146L 16PR JD720 BL4HJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 20');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6373, '2026-03-26', 'H1405', '215101734', '3302002094', '11R24.5 149/146L 16PR UF195 BL4HEU', '11R24.5 149/146L 16PR JF568 BL4HJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 50');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6374, '2026-03-26', 'H1102', '215103997', '3302002343', '12R22.5 152/149L 18PR BF188 BL0HBL', '12R22.5 152/149L 18PR BF188 BL0HBL', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 50');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6375, '2026-03-26', 'H1102', '215103998', '3302002344', '12R22.5 152/149L 18PR JF518 BL0HJY', '12R22.5 152/149L 18PR JF518 BL0HJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 10');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6376, '2026-03-26', 'H1102', '215101178', '3302000193', '12R22.5 152/149K 18PR JD755 BL0HJY', '12R22.5 152/149K 18PR JD755 BL0HJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 20');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6377, '2026-03-26', 'H1401', '215102582', '3302002568', '215/75R17.5 128/126M 16PR JF568 BL3EJY DL', '215/75R17.5 135/133L 16PR JF518 BL3EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 90');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6378, '2026-03-26', 'H1401', '215101222', '3302002566', '215/75R17.5 128/126M 16PR JD575 BL3EJY DL', '215/75R17.5 135/133L 16PR JD575 BL3EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 15');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6379, '2026-03-26', 'H1401', '215103006', '3302002531', '215/75R17.5 128/126M 16PR EDR50 BL3EEG DL', '215/75R17.5 135/133L 16PR EDR50 BL3EEG', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 30');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6380, '2026-03-26', 'H1402', '215101744', '3302001162', '245/70R19.5 144/142J 18PR BF188 BL3EBL', '245/70R19.5 144/142J 18PR BF188 BL3EBL', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 42');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6381, '2026-03-26', 'H1402', '215101743', '3302001161', '245/70R19.5 144/142J 18PR JF518 BL3EJY', '245/70R19.5 144/142J 18PR JF518 BL3EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 28');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6382, '2026-03-26', 'H1402', '215102642', '3302001573', '245/70R19.5 144/142J 18PR BD175 BL3EBL', '245/70R19.5 144/142J 18PR BD175 BL3EBL', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 28');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6383, '2026-03-26', 'H1402', '215101783', '3302001205', '245/70R19.5 144/142J 18PR JD575 BL3EJY', '245/70R19.5 144/142J 18PR JD575 BL3EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 14');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6384, '2026-03-26', 'H1202', '215101611', '3302002209', '295/75R22.5 146/143L 16PR QD571 BL4HGQ', '295/75R22.5 146/143L 16PR JD571 BL4HJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 50');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6385, '2026-03-26', 'H1201', '215101611', '3302002282', '295/75R22.5 146/143L 16PR EDL15 BL4EEG', '295/75R22.5 146/143L 16PR JD571 BL4HJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 30');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6386, '2026-03-26', 'H1202', '215102780', '3302002499', '295/75R22.5 146/143M 16PR AT502 BL4HAM FE', '295/75R22.5 146/143M 16PR BT159 BL4HBL', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 40');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6387, '2026-03-26', 'H1201', '215102568', '3302002497', '295/75R22.5 144/141L 14PR QF568 BL4HGQ FE', '295/75R22.5 146/143L 16PR BF188 BL4HBL', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 70');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6388, '2026-03-26', 'H1501', '215103130', '3302002053', '295/80R22.5 152/149L 18PR AA267 BL4HAM', '295/80R22.5 152/149L 18PR JA767 BL4HJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 20');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6389, '2026-03-26', 'H1204', '215101726', '3302001139', '295/80R22.5 152/149J 18PR JD756 BL4HJY', '295/80R22.5 152/149J 18PR JD756 BL4HJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 20');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6390, '2026-03-26', 'H1204', '215102626', '3302001585', '295/80R22.5 152/149M 18PR BF188 BL4EBL', '295/80R22.5 152/149M 18PR JF518 BL4EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 90');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6391, '2026-03-26', 'H1105', '215102626', '3302002061', '295/80R22.5 154/149M 18PR ESL01 BL4EEG', '295/80R22.5 152/149M 18PR JF518 BL4EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 80');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6392, '2026-03-26', 'H1501', '215102626', '3302001404', '295/80R22.5 154/149M 18PR JF568 BL4EJY', '295/80R22.5 152/149M 18PR JF518 BL4EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 80');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6393, '2026-03-26', 'H1105', '215104191', '3302002417', '295/80R22.5 154/149M 18PR EAR30 BL4EEG', '295/80R22.5 154/149M 18PR EAR30 BL4EEG', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 30');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6394, '2026-03-26', 'H1304', '215103740', '3302002218', '295/80R22.5 154/149M 18PR UF195 BL4HEU', '295/80R22.5 154/149M 18PR UF195 BL4HEU', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 10');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6395, '2026-03-26', 'H1105', '215102624', '3302002059', '295/80R22.5 154/149L 18PR EDL11 BL4EEG', '295/80R22.5 152/149L 18PR BD177 BL4EBL', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 20');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6396, '2026-03-26', 'H1304', '215101470', '3302000750', '295/80R22.5 152/149M 18PR BD175 BL4EBL', '295/80R22.5 152/149M 18PR BD175 BL4EBL', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 80');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6397, '2026-03-26', 'H1501', '215101470', '3302001002', '295/80R22.5 152/149M 18PR AD506 BL4EAM', '295/80R22.5 152/149M 18PR BD175 BL4EBL', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 30');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6398, '2026-03-26', 'H1304', '215103003', '3302002060', '295/80R22.5 154/149L 18PR EDR51 BL4EEG', '295/80R22.5 152/149L 18PR EDR51 BL4EEG', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 50');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6399, '2026-03-26', 'H1204', '215103741', '3302002217', '295/80R22.5 154/149L 18PR UD188 BL4HEU', '295/80R22.5 154/149L 18PR UD188 BL4HEU', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 30');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6400, '2026-03-26', 'H1404', '215102628', '3302001587', '315/60R22.5 152/148L 18PR BD177 BL4EBL', '315/60R22.5 152/148L 18PR JD577 BL4EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 45');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6401, '2026-03-26', 'H1404', '215103396', '3302002070', '315/60R22.5 154/150L 18PR BF188 BL4EBL', '315/60R22.5 154/150L 18PR BF188 BL4EBL', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 18');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6402, '2026-03-26', 'H1404', '215101520', '3302000831', '315/60R22.5 152/148L 18PR JD575 BL4EJY', '315/60R22.5 152/148L 18PR JD575 BL4EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 18');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6403, '2026-03-26', 'H1303', '215102632', '3302001590', '315/70R22.5 156/150L 18PR BF188 BL4EBL', '315/70R22.5 156/150L 18PR JF518 BL4EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 90');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6404, '2026-03-26', 'H1301', '215102631', '3302001716', '315/70R22.5 156/150L 18PR EDL11 BL4EEG', '315/70R22.5 156/150L 18PR JD577 BL4EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 50');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6405, '2026-03-26', 'H1301', '215101335', '3302002705', '315/70R22.5 156/150L 18PR EDR53 BL4EEG', '315/70R22.5 156/150L 18PR JD575 BL4EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 80');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6406, '2026-03-26', 'H1203', '215102830', '3302001717', '315/70R22.5 156/150L 18PR EDR51 BL4EEG', '315/70R22.5 156/150L 18PR EDR51 BL4EEG', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 30');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6407, '2026-03-26', 'H1203', '215101337', '3302000467', '315/70R22.5 156/150L 18PR BD175 BL4EBL', '315/70R22.5 156/150L 18PR BD175 BL4EBL', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 100');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6408, '2026-03-26', 'H1203', '215101336', '3302001206', '315/70R22.5 156/150L 18PR BD165 BL4EBL', '315/70R22.5 156/150L 18PR JD565 BL4EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 20');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6409, '2026-03-26', 'H1301', '215101922', '3302002348', '315/70R22.5 156/150L 18PR BW292 BL4EBL', '315/70R22.5 156/150L 18PR JW592 BL4EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 10');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6410, '2026-03-26', 'H1305', '215101401', '3302002305', '315/80R22.5 156/150J 20PR BD280 BL0EBL DL', '315/80R22.5 156/153K 20PR JD758 BL0EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 40');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6411, '2026-03-26', 'H1305', '215103782', '3302002325', '315/80R22.5 156/150L 20PR BA220 BL0EBL DL', '315/80R22.5 161/157K 20PR JA665 BL0HJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 20');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6412, '2026-03-26', 'H1305', '215101545', '3302000915', '315/80R22.5 156/153K 20PR JD755 BL0EJY', '315/80R22.5 156/153K 20PR JD755 BL0EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 30');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6413, '2026-03-26', 'H1305', '215101548', '3302000921', '315/80R22.5 156/153K 20PR BD210 BL0EBL', '315/80R22.5 156/153K 20PR BD210 BL0EBL', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 20');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6414, '2026-03-26', 'H1403', '215101411', '3302000611', '315/80R22.5 156/153L 20PR EG801 BL0EEG', '315/80R22.5 156/153L 20PR JY711 BL0EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 30');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6415, '2026-03-26', 'H1403', '215102417', '3302001446', '315/80R22.5 156/153K 20PR JW592 BL0EJY', '315/80R22.5 156/153K 20PR JW592 BL0EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 40');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6416, '2026-03-26', 'H1104', '215101814', '3302001236', '315/80R22.5 156/153L 20PR JF518 BL4EJY', '315/80R22.5 156/153L 20PR JF518 BL4EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 50');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6417, '2026-03-26', 'H1104', '215101486', '3302000787', '315/80R22.5 156/153K 20PR JD575 BL4EJY', '315/80R22.5 156/153K 20PR JD575 BL4EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 20');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6418, '2026-03-26', 'H1104', '215103353', '3302002332', '315/80R22.5 156/150L 20PR EDR51 BL4EEG DL', '315/80R22.5 156/153L 20PR EDR51 BL4EEG', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 10');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6419, '2026-03-26', 'H1104', '215101407', '3302000609', '315/80R22.5 156/153L 20PR BD175 BL4EBL', '315/80R22.5 156/153L 20PR BD175 BL4EBL', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 50');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6420, '2026-03-26', 'H1103', '215102640', '3202000565', '325/95R24 162/160K 22PR JD727 BT0HJY', '325/95R24 162/160K 22PR JD727 BT0HJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 45');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6421, '2026-03-26', 'H1103', '215100460', '3202000220', '325/95R24 162/160K 22PR JA661 BT0HJY', '325/95R24 162/160K 22PR JA661 BT0HJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 36');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6422, '2026-03-26', 'H1103', '215102639', '3202000564', '325/95R24 162/160K 22PR JA526 BT0HJY', '325/95R24 162/160K 22PR JA526 BT0HJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 27');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6423, '2026-03-26', 'H1505', '215101523', '3302000745', '385/55R22.5 160K 20PR BT160 BL4EBL', '385/55R22.5 160K 20PR JT560 BL4EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 80');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6424, '2026-03-26', 'H1505', '215103930', '3302002278', '385/55R22.5 160K 20PR BF196 BL4EBL', '385/55R22.5 160K 20PR BF196 BL4EBL', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 10');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6425, '2026-03-26', 'H1505', '215104811', '3302002676', '385/55R22.5 160K 20PR BW293 BL4EBL', '385/55R22.5 160K 20PR JW593 BL4EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 20');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6426, '2026-03-26', 'H1504', '215101878', '3302001888', '385/65R22.5 164K 24PR ETL23 BL4EEG', '385/65R22.5 164K 24PR JT560 BL4EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 50');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6427, '2026-03-26', 'H1302', '215101879', '3302001321', '385/65R22.5 164K 24PR AT502 BL4EAM', '385/65R22.5 164K 24PR BT160 BL4EBL', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 50');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6428, '2026-03-26', 'H1504', '215101877', '3302001313', '385/65R22.5 164K 24PR JY598 BL4EJY', '385/65R22.5 164K 24PR JY598 BL4EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 50');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6429, '2026-03-26', 'H1502', '215101877', '3302002641', '385/65R22.5 164K 24PR BT169 BL4EBL', '385/65R22.5 164K 24PR JY598 BL4EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 60');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6430, '2026-03-26', 'H1302', '215101880', '3302001316', '385/65R22.5 164K 24PR BT180 BL4EBL', '385/65R22.5 164K 24PR BT180 BL4EBL', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 50');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6431, '2026-03-26', 'H1502', '215101880', '3302001322', '385/65R22.5 164K 24PR AT503 BL4EAM', '385/65R22.5 164K 24PR BT180 BL4EBL', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 40');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6432, '2026-03-26', 'H1504', '215102793', '3302002215', '385/65R22.5 160K 20PR QA626 BL4HGQ', '385/65R22.5 164K 24PR JA626 BL4EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 20');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6433, '2026-03-26', 'H1302', '215104050', '3302002366', '385/65R22.5 164K 24PR JW593 BL4EJY', '385/65R22.5 164K 24PR JW593 BL4EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 20');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6434, '2026-03-26', 'H1502', '215104169', '3302002413', '385/65R22.5 164K 24PR EAW86 BL4EEG', '385/65R22.5 164K 24PR EAW86 BL4EEG', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 20');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6435, '2026-03-26', 'H1205', '215101838', '3302001271', '385/65R22.5 164K 24PR JY598 BL0EJY', '385/65R22.5 164K 24PR JY598 BL0EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 70');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6436, '2026-03-26', 'H1101', '215101837', '3302002527', '385/65R22.5 164K 24PR ETL23 BL0EEG', '385/65R22.5 164K 24PR JT560 BL0EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 100');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6437, '2026-03-26', 'H1205', '215101837', '3302001317', '385/65R22.5 164K 24PR BT160 BL0EBL', '385/65R22.5 164K 24PR JT560 BL0EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 20');
INSERT INTO `t_mdm_cx_machine_online_info` VALUES (6438, '2026-03-26', 'H1205', '215104472', '3302002542', '385/65R22.5 164K 24PR BA226 BL0EBL', '385/65R22.5 164K 24PR JA626 BL0EJY', 'V1.0', 'TBR', 'TBR', 0, NULL, '2026-04-03 18:34:35', NULL, '2026-04-03 18:34:35', '03/26夜班计划量: 10');

-- ----------------------------
-- Table structure for t_mdm_cx_sche_finish_qty
-- ----------------------------
DROP TABLE IF EXISTS `t_mdm_cx_sche_finish_qty`;
CREATE TABLE `t_mdm_cx_sche_finish_qty`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ORDER_NO` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '成型工单号',
  `SCHEDULE_DATE` date NULL DEFAULT NULL COMMENT '排程日期',
  `CX_MACHINE_CODE` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '机台编号',
  `MATERIAL_CODE` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '物料编码（NC）',
  `MES_MATERIAL_CODE` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '物料编码（MES）',
  `CLASS1_FINISH_QTY` decimal(8, 0) NULL DEFAULT NULL COMMENT '一班(夜班)完成量',
  `CLASS2_FINISH_QTY` decimal(8, 0) NULL DEFAULT NULL COMMENT '二班(早班)完成量',
  `CLASS3_FINISH_QTY` decimal(8, 0) NULL DEFAULT NULL COMMENT '三班(中班)完成量',
  `CLASS1_UN_REASON` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '一班(夜班)未完成原因',
  `CLASS2_UN_REASON` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '二班(早班)未完成原因',
  `CLASS3_UN_REASON` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '三班(中班)未完成原因',
  `CLASS1_PERSON` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '一班(夜班)作业人员',
  `CLASS2_PERSON` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '二班(早班)作业人员',
  `CLASS3_PERSON` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '三班(中班)作业人员',
  `EMBRYO_CODE` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '成型胎胚物料编码',
  `DATA_VERSION` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '版本号',
  `COMPANY_CODE` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分公司编码',
  `FACTORY_CODE` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '厂别',
  `CREATE_BY` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `UPDATE_BY` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '修改人',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `IS_DELETE` decimal(1, 0) NULL DEFAULT NULL COMMENT '删除标识',
  `REMARK` varchar(900) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `idx_mdm_cx_sche_finish_qty_fgs`(`COMPANY_CODE` ASC) USING BTREE,
  INDEX `idx_mdm_cx_sche_finish_qty_fc`(`FACTORY_CODE` ASC) USING BTREE,
  INDEX `idx_mdm_cx_sche_finish_qty_v`(`DATA_VERSION` ASC) USING BTREE,
  INDEX `idx_mdm_cx_sche_finish_qty_v2`(`CX_MACHINE_CODE` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'S0-2602-成型排程完成量回报接口' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_mdm_cx_sche_finish_qty
-- ----------------------------

-- ----------------------------
-- Table structure for t_mdm_mes_cx_day_finish_qty
-- ----------------------------
DROP TABLE IF EXISTS `t_mdm_mes_cx_day_finish_qty`;
CREATE TABLE `t_mdm_mes_cx_day_finish_qty`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `FINISH_DATE` date NULL DEFAULT NULL COMMENT '完成日期',
  `DAY_FINISH_QTY` decimal(8, 0) NULL DEFAULT NULL COMMENT '胚胎日完成量',
  `EMBRYO_CODE` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '成型胚胎物料编码',
  `EXAMPLE_TYPE` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '示方类型',
  `BOM_DATA_VERSION` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '胚胎施工版本号',
  `DATA_VERSION` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '版本号',
  `COMPANY_CODE` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分公司编码',
  `FACTORY_CODE` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '厂别',
  `CREATE_BY` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `UPDATE_BY` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '修改人',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `IS_DELETE` decimal(1, 0) NULL DEFAULT NULL COMMENT '删除标识',
  `REMARK` varchar(900) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `idx_mdm_mes_cx_day_finish_qty_fgs`(`COMPANY_CODE` ASC) USING BTREE,
  INDEX `idx_mdm_mes_cx_day_finish_qty_fc`(`FACTORY_CODE` ASC) USING BTREE,
  INDEX `idx_mdm_mes_cx_day_finish_qty_v`(`DATA_VERSION` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 67 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'S0-3002-成型排程日完成量' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_mdm_mes_cx_day_finish_qty
-- ----------------------------
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (1, '2026-03-31', 43, '215103997', 'S', 'VTMCB22502186SA11', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (2, '2026-03-31', 21, '215102793', 'S', 'VTMCB22501506SA28', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (3, '2026-03-31', 102, '215102582', 'S', 'VTMAM17500145SA11', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (4, '2026-03-31', 20, '215103998', 'S', 'VTMCB22502187SA12', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (5, '2026-03-31', 19, '215103740', 'S', 'VTMCB22502026SA14', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (6, '2026-03-31', 9, '215100460', 'S', 'VTMAM24000025SA10', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (7, '2026-03-31', 19, '215101337', 'S', 'VTMCB22500576SA15', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (8, '2026-03-31', 41, '215101731', 'S', 'VTMCB24500034SA24', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (9, '2026-03-31', 5, '215102597', 'S', 'VTMCB22501368SA12', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (10, '2026-03-31', 30, '215103741', 'S', 'VTMCB22502027SA13', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (11, '2026-03-31', 21, '215104811', 'S', 'VTMCB22502577SA02', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (12, '2026-03-31', 20, '215101401', 'S', 'VTMCB22500640SA07', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (13, '2026-03-31', 14, '215101783', 'S', 'VTMAM19500089SA08', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (14, '2026-03-31', 35, '215101828', 'S', 'VTMCB22500929SA15', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (15, '2026-03-31', 78, '215101523', 'S', 'VTMCB22500724SA11', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (16, '2026-03-31', 8, '215101828', 'S', 'VTMCB22500929SA16', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (17, '2026-03-31', 15, '215101489', 'S', 'VTMCB22500708SA11', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (18, '2026-03-31', 17, '215104191', 'S', 'VTMCB22502290SA11', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (19, '2026-03-31', 97, '215101611', 'S', 'VTMCB22500791SA25', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (20, '2026-03-31', 22, '215101487', 'S', 'VTMCB22500706SA16', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (21, '2026-03-31', 42, '215103975', 'S', 'VTMCB22502175SA09', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (22, '2026-03-31', 56, '215102626', 'S', 'VTMCB22501390SA18', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (23, '2026-03-31', 18, '215101548', 'S', 'VTMCB22500738SA03', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (24, '2026-03-31', 7, '215101922', 'S', 'VTMCB22500995SA09', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (25, '2026-03-31', 25, '215101837', 'S', 'VTMCB22500937SA05', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (26, '2026-03-31', 4, '215101520', 'S', 'VTMCB22500723SA05', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (27, '2026-03-31', 39, '215101744', 'S', 'VTMAM19500084SA08', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (28, '2026-03-31', 33, '215102642', 'S', 'VTMAM19500127SA07', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (29, '2026-03-31', 10, '215101486', 'S', 'VTMCB22500705SA17', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (30, '2026-03-31', 8, '215103396', 'S', 'VTMCB22501835SA13', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (31, '2026-03-31', 42, '215102628', 'S', 'VTMCB22501392SA13', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (32, '2026-03-31', 9, '215103491', 'S', 'VTMCB22501877SA15', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (33, '2026-03-31', 30, '215101743', 'S', 'VTMAM19500083SA07', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (34, '2026-03-31', 36, '215101166', 'S', 'VTMCB22500475SA13', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (35, '2026-03-31', 112, '215102615', 'S', 'VTMCB22501383SA12', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (36, '2026-03-31', 47, '215101879', 'S', 'VTMCB22500966SA27', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (37, '2026-03-31', 47, '215101407', 'S', 'VTMCB22500646SA16', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (38, '2026-03-31', 30, '215103782', 'S', 'VTMCB22502057SA08', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (39, '2026-03-31', 48, '215101325', 'S', 'VTMCB22500564SA16', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (40, '2026-03-31', 47, '215101880', 'S', 'VTMCB22500967SA30', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (41, '2026-03-31', 59, '215101729', 'S', 'VTMCB22500864SA17', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (42, '2026-03-31', 17, '215101888', 'S', 'VTMCB24500060SA22', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (43, '2026-03-31', 19, '215101729', 'S', 'VTMCB22500864SA16', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (44, '2026-03-31', 10, '215104169', 'S', 'VTMCB22502285SA11', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (45, '2026-03-31', 54, '215101877', 'S', 'VTMCB22500964SA28', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (46, '2026-03-31', 74, '215101840', 'S', 'VTMCB22500940SA32', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (47, '2026-03-31', 49, '215101734', 'S', 'VTMCB24500037SA25', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (48, '2026-03-31', 11, '215103930', 'S', 'VTMCB22502152SA07', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (49, '2026-03-31', 70, '215101838', 'S', 'VTMCB22500938SA11', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (50, '2026-03-31', 6, '215104984', 'X', 'VTMCB22502702XA03', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (51, '2026-03-31', 28, '215101545', 'S', 'VTMCB22500737SA08', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (52, '2026-03-31', 17, '215101726', 'S', 'VTMCB22500861SA13', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (53, '2026-03-31', 88, '215101335', 'S', 'VTMCB22500574SA15', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (54, '2026-03-31', 45, '215102640', 'S', 'VTMAM24000060SA09', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (55, '2026-03-31', 52, '215101814', 'S', 'VTMCB22500917SA18', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (56, '2026-03-31', 36, '215102639', 'S', 'VTMAM24000059SA10', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (57, '2026-03-31', 30, '215102830', 'S', 'VTMCB22501534SA14', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (58, '2026-03-31', 20, '215103130', 'S', 'VTMCB22501692SA15', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (59, '2026-03-31', 32, '215102568', 'S', 'VTMCB22501352SA19', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (60, '2026-03-31', 40, '215101878', 'S', 'VTMCB22500965SA30', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (61, '2026-03-31', 104, '215101470', 'S', 'VTMCB22500695SA16', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (62, '2026-03-31', 116, '215102632', 'S', 'VTMCB22501396SA18', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (63, '2026-03-31', 29, '215102631', 'S', 'VTMCB22501395SA16', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (64, '2026-03-31', 31, '215101222', 'S', 'VTMAM17500035SA09', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (65, '2026-03-31', 10, '215104472', 'S', 'VTMCB22502437SA05', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);
INSERT INTO `t_mdm_mes_cx_day_finish_qty` VALUES (66, '2026-03-31', 18, '215104050', 'S', 'VTMCB22502219SA13', '2026040101', '116', '116', 'MES', '2026-04-02 18:12:47', 'MES', '2026-04-02 18:12:47', 0, NULL);

-- ----------------------------
-- Table structure for t_mdm_mes_cx_stock
-- ----------------------------
DROP TABLE IF EXISTS `t_mdm_mes_cx_stock`;
CREATE TABLE `t_mdm_mes_cx_stock`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `STOCK_DATE` date NULL DEFAULT NULL COMMENT '库存日期',
  `EMBRYO_CODE` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '胎胚物料编码',
  `STOCK_NUM` decimal(10, 0) NULL DEFAULT NULL COMMENT '胎胚库存',
  `LITI_STOCK` decimal(10, 0) NULL DEFAULT NULL COMMENT '立体库',
  `EMBRYO_CAR` decimal(10, 0) NULL DEFAULT NULL COMMENT '胎胚车',
  `LH_STOCK` decimal(10, 0) NULL DEFAULT NULL COMMENT '硫化库存',
  `AVAILABLE_STOCK` decimal(10, 0) NULL DEFAULT NULL COMMENT '可用库存',
  `BOM_DATA_VERSION` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '胎胚版本',
  `EXAMPLE_TYPE` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '示方类型',
  `UNAVAILABLE_STOCK` decimal(10, 0) NULL DEFAULT NULL COMMENT '不可用库存',
  `DATA_VERSION` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '版本号',
  `COMPANY_CODE` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分公司编码',
  `FACTORY_CODE` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '厂别',
  `CREATE_BY` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `CREATE_TIME` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `UPDATE_BY` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '修改人',
  `UPDATE_TIME` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `IS_DELETE` decimal(1, 0) NULL DEFAULT NULL COMMENT '删除标识',
  `REMARK` varchar(900) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`ID`) USING BTREE,
  INDEX `idx_mdm_mes_cx_stock_fgs`(`COMPANY_CODE` ASC) USING BTREE,
  INDEX `idx_mdm_mes_cx_stock_fc`(`FACTORY_CODE` ASC) USING BTREE,
  INDEX `idx_mdm_mes_cx_stock_v`(`DATA_VERSION` ASC) USING BTREE,
  INDEX `idx_mdm_mes_cx_stock_pt`(`EMBRYO_CODE` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'S0-2402-生胎库存' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_mdm_mes_cx_stock
-- ----------------------------

-- ----------------------------
-- Table structure for t_mes_cx_day_finish_qty
-- ----------------------------
DROP TABLE IF EXISTS `t_mes_cx_day_finish_qty`;
CREATE TABLE `t_mes_cx_day_finish_qty`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应序列SEQ_CX_FINISH_QTY',
  `FINISH_DATE` datetime NULL DEFAULT NULL COMMENT '排程日期',
  `EMBRYO_CODE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胎胚代码',
  `BOM_DATA_VERSION` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胎胚施工版本',
  `DAY_FINISH_QTY` int NULL DEFAULT NULL COMMENT '胎胚日完成量',
  `COMPANY_CODE` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '分公司代码',
  `FACTORY_CODE` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '分厂厂别代码',
  `REMARK` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注',
  `DATA_VERSION` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '版本号',
  `CREATE_DATE` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `UPDATE_DATE` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `IS_DELETE` int NULL DEFAULT 0 COMMENT '删除标识：0--正常，1-删除',
  `SAP_CODE` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胎胚SAP品号',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '成型排程日完成量回报接口' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_mes_cx_day_finish_qty
-- ----------------------------

-- ----------------------------
-- Table structure for t_mes_cx_part_finish_qty
-- ----------------------------
DROP TABLE IF EXISTS `t_mes_cx_part_finish_qty`;
CREATE TABLE `t_mes_cx_part_finish_qty`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应序列SEQ_MES_CX_FINISH_QTY',
  `STAT_DATE` datetime NULL DEFAULT NULL COMMENT '日期',
  `EMBRYO_CODE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胎胚代码',
  `FINISH_QTY` int NULL DEFAULT NULL COMMENT '完成量(8点-12点)',
  `DATA_VERSION` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '版本号',
  `CREATE_DATE` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `UPDATE_DATE` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `IS_DELETE` int NULL DEFAULT 0 COMMENT '删除标识：0--正常，1-删除',
  `SAP_CODE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'SAP品号',
  `BOM_DATA_VERSION` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '成型8-12点完成量添加胎胚版本',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '成型8-12点的完成量' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_mes_cx_part_finish_qty
-- ----------------------------

-- ----------------------------
-- Table structure for t_mes_cx_schedule_result
-- ----------------------------
DROP TABLE IF EXISTS `t_mes_cx_schedule_result`;
CREATE TABLE `t_mes_cx_schedule_result`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `CX_BATCH_NO` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '自动排程批次号信息，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号',
  `ORDER_NO` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '成型排程工单号，自动生成，批次号+4位定长自增序号',
  `SCHEDULE_DATE` datetime NULL DEFAULT NULL COMMENT '排程日期',
  `PRODUCTION_STATUS` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '生产状态:0-未生产；1-生产中；2-已收尾',
  `IS_RELEASE` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE',
  `CX_MACHINE_CODE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '成型机台编号',
  `CX_MACHINE_NAME` varchar(60) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '成型机台名称',
  `LH_MACHINE_CODE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '硫化机台编号',
  `LH_MACHINE_NAME` varchar(60) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '硫化机台名称',
  `LH_MACHINE_QTY` double NULL DEFAULT NULL COMMENT '硫化机台数量',
  `MINIMUM_LH_MACHINE_REQ_QTY` int NULL DEFAULT NULL COMMENT '最小硫化机需求数',
  `AVAILABLE_MOLD_QTY` int NULL DEFAULT NULL COMMENT '可用模具数量',
  `MAXIMUM_CLASS_QTY` int NULL DEFAULT NULL COMMENT '最大班数',
  `WORK_SHIFTS` int NULL DEFAULT NULL COMMENT '班制',
  `STORAGE_LOCATION` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '库存地点编码',
  `STORAGE_LOCATION_DESC` varchar(300) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '库存地点中文描述',
  `SAP_CODE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'SAP品号',
  `SPEC_DESC` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '规格型号',
  `SPEC_DIMENSION` double NULL DEFAULT NULL COMMENT '外胎规格尺寸信息',
  `EMBRYO_CODE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胎胚代码',
  `LH_MIDDLE_NIGHT_FINISH_QTY` int NULL DEFAULT NULL COMMENT '硫化中夜班产量\r\n            （昨天16点到今天8点）',
  `CX_MONTH_FINISH_QTY` int NULL DEFAULT NULL COMMENT '成型产量\r\n            （本月成型总量）',
  `REJECT_QTY` int NULL DEFAULT NULL COMMENT '废次品数量',
  `NEWEST_PLAN_QTY` int NULL DEFAULT NULL COMMENT '最新计划数（初稿）',
  `CLASS3_PLANNED_QTY` int NULL DEFAULT NULL COMMENT '三班（8点-16点）计划量',
  `SINGLE_SHIFT_LH_QTY` int NULL DEFAULT NULL COMMENT '单班硫化量',
  `TOTAL_STOCK` int NULL DEFAULT NULL COMMENT '总库存数量',
  `EXTENDED_STOCK` int NULL DEFAULT NULL COMMENT '超期库存数量',
  `MONTH_STOCK` int NULL DEFAULT NULL COMMENT '月结库存数量',
  `ACTUAL_OVER_PRODUCTION` int NULL DEFAULT NULL COMMENT '实际超欠产',
  `EXPECTED_OVER_PRODUCTION` int NULL DEFAULT NULL COMMENT '预计超欠产',
  `DIFFERENCE_OVER_PRODUCTION` int NULL DEFAULT NULL COMMENT '超欠产差额（实际-预计）',
  `CLASS1_AVAILABLE_LH_SHIFT` double NULL DEFAULT NULL COMMENT '一班可硫化班次',
  `CLASS1_PLAN_QTY` int NULL DEFAULT NULL COMMENT '一班计划数',
  `CLASS1_ANALYSIS_INPUT` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '一班原因分析手工输入',
  `CLASS1_ANALYSIS` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '一班原因分析',
  `CLASS2_AVAILABLE_LH_SHIFT` double NULL DEFAULT NULL COMMENT '二班可硫化班次',
  `CLASS2_PLAN_QTY` int NULL DEFAULT NULL COMMENT '二班计划数',
  `CLASS2_ANALYSIS_INPUT` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '二班原因分析手工输入',
  `CLASS2_ANALYSIS` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '二班原因分析',
  `CLASS3_AVAILABLE_LH_SHIFT` double NULL DEFAULT NULL COMMENT '三班可硫化班次',
  `CLASS3_PLAN_QTY` int NULL DEFAULT NULL COMMENT '三班计划数',
  `CLASS3_ANALYSIS_INPUT` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '三班原因分析手工输入',
  `CLASS3_ANALYSIS` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '三班原因分析',
  `CLASS4_AVAILABLE_LH_SHIFT` double NULL DEFAULT NULL COMMENT '次日一班可硫化班次',
  `CLASS4_PLAN_QTY` int NULL DEFAULT NULL COMMENT '次日一班计划数',
  `CLASS4_ANALYSIS_INPUT` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '次日一班原因分析手工输入',
  `CLASS4_ANALYSIS` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '次日一班原因分析',
  `CLASS5_AVAILABLE_LH_SHIFT` double NULL DEFAULT NULL COMMENT '次日二班可硫化班次',
  `CLASS5_PLAN_QTY` int NULL DEFAULT NULL COMMENT '次日二班计划数',
  `CLASS5_ANALYSIS_INPUT` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '次日二班原因分析手工输入',
  `CLASS5_ANALYSIS` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '次日二班原因分析',
  `MARK_CLOSE_OUT_TIP` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '收尾提示标识(0:提示收尾；1:不需要提示)',
  `REMARK` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注说明字段',
  `DATA_VERSION` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '版本号',
  `CREATE_DATE` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `UPDATE_DATE` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `IS_DELETE` int NULL DEFAULT 0 COMMENT '删除标识：0--正常，1-删除',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '成型排程下发接口' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_mes_cx_schedule_result
-- ----------------------------

-- ----------------------------
-- Table structure for t_mes_cx_shift_finish_qty
-- ----------------------------
DROP TABLE IF EXISTS `t_mes_cx_shift_finish_qty`;
CREATE TABLE `t_mes_cx_shift_finish_qty`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应序列SEQ_CX_FINISH_QTY',
  `ORDER_NO` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '成型排程工单号，自动生成，批次号+4位定长自增序号',
  `SCHEDULE_DATE` datetime NULL DEFAULT NULL COMMENT '排程日期',
  `CX_MACHINE_CODE` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '成型机台编号',
  `SAP_CODE` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'SAP品号',
  `SAP_CLASS1_FINISH_QTY` int NULL DEFAULT NULL COMMENT '一班(中班)完成量',
  `SAP_CLASS2_FINISH_QTY` int NULL DEFAULT NULL COMMENT '二班(夜班)完成量',
  `SAP_CLASS3_FINISH_QTY` int NULL DEFAULT NULL COMMENT '三班(白班)完成量',
  `SAP_CLASS4_FINISH_QTY` int NULL DEFAULT NULL COMMENT '四班(次日一班)完成量',
  `SAP_CLASS5_FINISH_QTY` int NULL DEFAULT NULL COMMENT '五班(次日二班)完成量',
  `EMBRYO_CODE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胎胚代码',
  `CLASS1_FINISH_QTY` int NULL DEFAULT NULL COMMENT '一班(中班)完成量',
  `CLASS2_FINISH_QTY` int NULL DEFAULT NULL COMMENT '二班(夜班)完成量',
  `CLASS3_FINISH_QTY` int NULL DEFAULT NULL COMMENT '三班(白班)完成量',
  `CLASS4_FINISH_QTY` int NULL DEFAULT NULL COMMENT '四班(次日一班)完成量',
  `CLASS5_FINISH_QTY` int NULL DEFAULT NULL COMMENT '五班(次日二班)完成量',
  `REMARK` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注',
  `DATA_VERSION` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '版本号',
  `CREATE_DATE` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `UPDATE_DATE` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `IS_DELETE` int NULL DEFAULT 0 COMMENT '删除标识：0--正常，1-删除',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '成型排程各班次完成量回报接口' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_mes_cx_shift_finish_qty
-- ----------------------------

-- ----------------------------
-- Table structure for t_mes_cx_stock
-- ----------------------------
DROP TABLE IF EXISTS `t_mes_cx_stock`;
CREATE TABLE `t_mes_cx_stock`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应自增序列为：SEQ_NC_STOCK',
  `STOCK_DATE` datetime NULL DEFAULT NULL COMMENT '库存日期，格式：yyyy-MM-dd',
  `EMBRYO_CODE` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胎胚代码',
  `STOCK_NUM` int NULL DEFAULT NULL COMMENT '库存量',
  `LITI_STOCK` int NULL DEFAULT NULL COMMENT '立体库',
  `EMBRYO_CAR` int NULL DEFAULT NULL COMMENT '胎胚车',
  `LH_STOCK` int NULL DEFAULT NULL COMMENT '硫化库存',
  `OVER_TIME_STOCK` int NULL DEFAULT NULL COMMENT '超期库存',
  `REMARK` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注说明字段',
  `DATA_VERSION` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '版本号',
  `CREATE_DATE` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `UPDATE_DATE` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `IS_DELETE` int NULL DEFAULT 0 COMMENT '删除标识：0--正常，1-删除',
  `AVAILABLE_STOCK` int NULL DEFAULT NULL COMMENT '可用库存',
  `UNAVAILABLE_STOCK` int NULL DEFAULT NULL COMMENT '不可用库存',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '胎胚库存同步接口' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_mes_cx_stock
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
