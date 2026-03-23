# 金宇轮胎APS系统 - 成型排程架构重设计文档

> 版本：V2.0.0
> 日期：2024年3月
> 状态：完全重写方案

---

## 📋 一、实体类清单总览

### 1.1 基础实体类

| 类名 | 表名 | 说明 | 状态 |
|------|------|------|------|
| `BaseEntity` | - | 基础实体（公共字段） | 待创建 |
| `ApsBaseEntity` | - | APS基础实体（扩展字段） | 待创建 |

### 1.2 主数据实体类（MDM前缀）

| 类名 | 表名 | 说明 | 字段数 | 状态 |
|------|------|------|--------|------|
| `MdmMoldingMachine` | T_MDM_MOLDING_MACHINE | 成型机台信息 | 20+ | 待创建 |
| `MdmMaterialInfo` | T_MDM_MATERIAL_INFO | 物料信息（SKU） | 40+ | 已提供 |
| `MdmConstructionInfo` | T_MDM_CONSTRUCTION_INFO | 投产胎胚施工信息 | 100+ | 已提供 |
| `MdmBomInfo` | T_MDM_BOM_INFO | BOM示方书 | 20+ | 已提供 |
| `MdmSkuConstructionRef` | T_MDM_SKU_CONSTRUCTION_REF | SKU与施工关系 | 30+ | 已提供 |
| `MdmSkuStructureRef` | T_MDM_SKU_STRUCTURE_REF | SKU与结构关系 | 10+ | 已提供 |
| `MdmCxMachineFixed` | T_MDM_CX_MACHINE_FIXED | 成型固定机台 | 15+ | 已提供 |
| `MdmStructureLhRatio` | T_MDM_STRUCTURE_LH_RATIO | 成型结构硫化配比 | 8 | 已提供 |
| `MdmStructureName` | T_MDM_STRUCTURE_NAME | 结构名称字典 | 5 | 已提供 |
| `MdmWorkCalendar` | T_MDM_WORK_CALENDAR | 工作日历 | 15+ | 已提供 |
| `MdmWorkWearInfo` | T_MDM_WORK_WEAR_INFO | 磨损信息 | 10+ | 已提供 |
| `MdmMaterialConsumeDetail` | T_MDM_MATERIAL_CONSUME_DETAIL | 物料消耗明细 | 10+ | 已提供 |

### 1.3 排程核心实体类

| 类名 | 表名 | 说明 | 字段数 | 状态 |
|------|------|------|--------|------|
| `CxScheduleResult` | T_CX_SCHEDULE_RESULT | 成型排程结果主表 | 60+ | 已提供 |
| `CxScheduleDetail` | T_CX_SCHEDULE_DETAIL | 排程明细表（车次） | 20+ | 已提供 |
| `CxScheduleVersion` | T_CX_SCHEDULE_VERSION | 排程版本表 | 10+ | 已提供 |
| `CxPrecisionPlan` | T_CX_PRECISION_PLAN | 精度计划表 | 15+ | 已提供 |
| `LhScheduleResult` | T_LH_SCHEDULE_RESULT | 硫化排程结果表 | 80+ | 已提供 |
| `FactoryMonthPlanProductionFinalResult` | T_FACTORY_MONTH_PLAN... | 月计划最终结果 | 100+ | 已提供 |

### 1.4 配置与库存实体类

| 类名 | 表名 | 说明 | 字段数 | 状态 |
|------|------|------|--------|------|
| `CxShiftConfig` | T_CX_SHIFT_CONFIG | 班次配置 | 8 | 已提供 |
| `CxTreadConfig` | T_CX_TREAD_CONFIG | 胎面配置 | 5 | 已提供 |
| `CxParams` | T_CX_PARAMS | 成型参数配置 | 10 | 已提供 |
| `CxStock` | T_CX_STOCK | 成型库存信息 | 15+ | 已提供 |

---

## 🏗️ 二、项目架构设计

### 2.1 包结构设计

```
com.jinyu.aps
├── common/                          # 公共模块
│   ├── constant/                    # 常量定义
│   │   ├── MachineStatus.java       # 机台状态枚举
│   │   ├── ProductionStatus.java    # 生产状态枚举
│   │   ├── ShiftCode.java           # 班次编码枚举
│   │   └── DataSource.java          # 数据来源枚举
│   ├── enums/                       # 枚举类
│   ├── exception/                   # 异常类
│   └── Result.java                  # 统一返回结果
│
├── entity/                          # 实体类模块
│   ├── base/                        # 基础实体
│   │   ├── BaseEntity.java          # 基础实体类
│   │   └── ApsBaseEntity.java       # APS扩展实体
│   │
│   ├── mdm/                         # 主数据实体
│   │   ├── MdmMoldingMachine.java
│   │   ├── MdmMaterialInfo.java
│   │   ├── MdmConstructionInfo.java
│   │   ├── MdmBomInfo.java
│   │   ├── MdmSkuConstructionRef.java
│   │   ├── MdmSkuStructureRef.java
│   │   ├── MdmCxMachineFixed.java
│   │   ├── MdmStructureLhRatio.java
│   │   ├── MdmStructureName.java
│   │   ├── MdmWorkCalendar.java
│   │   ├── MdmWorkWearInfo.java
│   │   └── MdmMaterialConsumeDetail.java
│   │
│   ├── schedule/                    # 排程实体
│   │   ├── CxScheduleResult.java
│   │   ├── CxScheduleDetail.java
│   │   ├── CxScheduleVersion.java
│   │   ├── CxPrecisionPlan.java
│   │   └── LhScheduleResult.java
│   │
│   └── config/                      # 配置实体
│       ├── CxShiftConfig.java
│       ├── CxTreadConfig.java
│       ├── CxParams.java
│       └── CxStock.java
│
├── mapper/                          # Mapper接口
│   ├── mdm/
│   │   ├── MdmMoldingMachineMapper.java
│   │   ├── MdmMaterialInfoMapper.java
│   │   └── ...
│   ├── schedule/
│   │   ├── CxScheduleResultMapper.java
│   │   └── ...
│   └── config/
│       ├── CxParamsMapper.java
│       └── CxStockMapper.java
│
├── service/                         # 服务层
│   ├── mdm/                         # 主数据服务
│   ├── schedule/                    # 排程服务
│   └── config/                      # 配置服务
│
├── controller/                      # 控制器层
│   ├── mdm/
│   ├── schedule/
│   └── config/
│
├── dto/                             # 数据传输对象
│   ├── request/                     # 请求DTO
│   └── response/                    # 响应DTO
│
├── vo/                              # 视图对象
│
└── util/                            # 工具类
```

### 2.2 数据库表命名规范

| 前缀 | 含义 | 示例 |
|------|------|------|
| `T_MDM_` | 主数据表 | T_MDM_MATERIAL_INFO |
| `T_CX_` | 成型排程表 | T_CX_SCHEDULE_RESULT |
| `T_LH_` | 硫化排程表 | T_LH_SCHEDULE_RESULT |
| `T_FACTORY_` | 工厂级数据表 | T_FACTORY_MONTH_PLAN... |

### 2.3 字段命名规范

```sql
-- 统一使用大写下划线命名
MACHINE_CODE          -- 机台编码
CX_MACHINE_NAME       -- 成型机台名称
SCHEDULE_DATE         -- 排程日期
IS_ACTIVE             -- 是否启用（0/1）
CREATE_TIME           -- 创建时间
UPDATE_TIME           -- 更新时间
```

---

## 🔄 三、新旧实体映射关系

### 3.1 机台实体映射

```
旧: t_cx_machine (Machine.java)
新: T_MDM_MOLDING_MACHINE (MdmMoldingMachine.java)

字段映射：
machine_code          → CX_MACHINE_CODE
machine_name          → CX_MACHINE_NAME
machine_type          → CX_MACHINE_TYPE_CODE
wrapping_type         → WRAPPING_TYPE
has_zero_degree_feeder→ HAS_ZERO_DEGREE_FEEDER
structure             → CURRENT_STRUCTURE
max_capacity_per_hour → PRODUCTION_CAPACITY
max_daily_capacity    → MAX_DAILY_CAPACITY
max_curing_machines   → MAX_CURING_MACHINES
fixed_structure1/2/3  → 移至 MdmCxMachineFixed
restricted_structures → 移至 MdmCxMachineFixed
status                → MAINTAIN_STATUS
is_active             → IS_ACTIVE
```

### 3.2 物料实体映射

```
旧: t_cx_material (Material.java)
新: T_MDM_MATERIAL_INFO (MdmMaterialInfo.java)

字段映射：
material_code         → MATERIAL_CODE
material_name         → MATERIAL_DESC
specification         → SPECIFICATIONS
product_structure     → STRUCTURE_NAME
main_pattern          → MAIN_PATTERN
pattern               → PATTERN
category              → MATERIAL_CATEGORY
vulcanize_time_minutes→ 移至 MdmSkuConstructionRef.CURING_TIME
is_main_product       → 新增字段，需确认
is_active             → FORBID_TAG（逻辑相反）
```

### 3.3 库存实体映射

```
旧: t_cx_stock (Stock.java)
新: T_CX_STOCK (CxStock.java)

字段映射：
stock_date            → STOCK_DATE
material_code         → EMBRYO_CODE（胎胚代码）
available_stock       → STOCK_NUM
unavailable_stock     → UNAVAILABLE_STOCK
modify_num            → MODIFY_NUM
bad_num               → BAD_NUM
```

### 3.4 排程结果实体映射

```
旧: t_cx_schedule_main (ScheduleMain.java)
新: T_CX_SCHEDULE_RESULT (CxScheduleResult.java)

核心变化：
1. 字段名统一大写
2. 新增 CX_BATCH_NO（成型批次号）
3. 新增 LH_SCHEDULE_IDS（硫化排程任务关联）
4. 班次字段保留（CLASS1~CLASS8）
5. 新增 LH_MACHINE_* 系列字段（硫化关联）
```

---

## 📊 四、核心业务流程

### 4.1 成型排程流程

```
┌─────────────────────────────────────────────────────────────────┐
│                      成型排程完整流程                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 数据准备阶段                                                 │
│     ├── 加载月计划数据 (FactoryMonthPlanProductionFinalResult)   │
│     ├── 加载机台信息 (MdmMoldingMachine)                         │
│     ├── 加载物料信息 (MdmMaterialInfo)                           │
│     ├── 加载施工信息 (MdmConstructionInfo)                       │
│     ├── 加载固定机台配置 (MdmCxMachineFixed)                     │
│     └── 加载库存数据 (CxStock)                                   │
│                                                                 │
│  2. 规则计算阶段                                                 │
│     ├── 计算机台可用性                                           │
│     │   ├── 检查机台状态                                         │
│     │   ├── 检查固定结构限制                                      │
│     │   └── 检查不可作业结构                                      │
│     ├── 计算物料适配性                                           │
│     │   ├── 检查SKU与施工关系                                    │
│     │   ├── 检查结构硫化配比                                      │
│     │   └── 检查零度材料需求                                      │
│     └── 计算产能约束                                             │
│         ├── 班次时间约束 (CxShiftConfig)                         │
│         ├── 工作日历约束 (MdmWorkCalendar)                       │
│         └── 库存可供时长约束                                      │
│                                                                 │
│  3. 排程算法阶段                                                 │
│     ├── P0: 续作排程（前日未完成）                                │
│     ├── P1: 精度计划优先排程                                     │
│     ├── P2: 常规计划排程                                         │
│     │   ├── 结构分组                                             │
│     │   ├── 顺位计算                                             │
│     │   └── 车次分配                                             │
│     └── 生成排程结果                                             │
│         ├── CxScheduleResult（主表）                             │
│         └── CxScheduleDetail（明细表）                           │
│                                                                 │
│  4. 硫化联动阶段                                                 │
│     ├── 生成硫化排程需求                                          │
│     ├── 计算硫化机台匹配                                          │
│     ├── 应用结构硫化配比 (MdmStructureLhRatio)                   │
│     └── 生成硫化排程结果 (LhScheduleResult)                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 实体间关联关系

```
┌─────────────────────────────────────────────────────────────────┐
│                      实体关联关系图                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  MdmMoldingMachine ─────┐                                       │
│       (成型机台)         │                                       │
│           │             │                                       │
│           ▼             │                                       │
│  MdmCxMachineFixed ◄────┤ 固定配置关系                           │
│       (固定机台)         │                                       │
│           │             │                                       │
│           ▼             │                                       │
│  MdmMaterialInfo ◄──────┤ 物料信息                               │
│       (物料/SKU)         │                                       │
│           │             │                                       │
│           ├─────────────┼─── MdmSkuConstructionRef               │
│           │             │    (SKU-施工关系)                      │
│           │             │                                       │
│           └─────────────┼─── MdmSkuStructureRef                  │
│                         │    (SKU-结构关系)                      │
│                         │                                       │
│                         ▼                                       │
│              MdmConstructionInfo                                 │
│                  (施工信息)                                       │
│                         │                                       │
│                         ▼                                       │
│                  MdmBomInfo                                      │
│                  (BOM示方书)                                      │
│                                                                 │
│  ════════════════════════════════════════════════════════════   │
│                                                                 │
│  CxScheduleResult ────────┐                                      │
│     (成型排程结果)         │                                      │
│           │               │                                      │
│           ▼               │                                      │
│  CxScheduleDetail ◄───────┤ 明细关联                              │
│     (排程明细/车次)        │                                      │
│           │               │                                      │
│           ▼               │                                      │
│  LhScheduleResult ◄───────┘ 硫化联动                              │
│     (硫化排程结果)                                                 │
│                                                                 │
│  ════════════════════════════════════════════════════════════   │
│                                                                 │
│  CxStock ◄───────────── CxScheduleResult                         │
│   (库存)                 (排程消耗库存)                           │
│                                                                 │
│  MdmWorkCalendar ◄───── CxScheduleResult                         │
│    (工作日历)            (排程日期约束)                           │
│                                                                 │
│  CxShiftConfig ◄──────── CxScheduleDetail                        │
│    (班次配置)            (班次时间约束)                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🚀 五、实施计划

### 5.1 阶段划分

| 阶段 | 内容 | 文件数 | 优先级 |
|------|------|--------|--------|
| **Phase 1** | 基础实体类 + 枚举 | 10+ | P0 |
| **Phase 2** | 主数据实体类（MDM） | 12 | P0 |
| **Phase 3** | 排程核心实体类 | 6 | P0 |
| **Phase 4** | 配置与库存实体类 | 4 | P0 |
| **Phase 5** | Mapper接口层 | 30+ | P1 |
| **Phase 6** | Service服务层 | 30+ | P1 |
| **Phase 7** | Controller控制层 | 15+ | P2 |
| **Phase 8** | 数据库DDL脚本 | 1 | P1 |

### 5.2 技术栈确认

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.x | 主框架 |
| MyBatis-Plus | 3.5.x | ORM框架 |
| Lombok | 最新 | 简化代码 |
| Swagger/OpenAPI | 3.x | API文档 |
| MySQL/Oracle | - | 数据库 |
| Maven | 3.x | 构建工具 |

---

## 📝 六、关键设计决策

### 6.1 字段类型选择

```java
// 时间类型统一使用 Java 8 时间API
LocalDate          // 日期（如排程日期）
LocalDateTime      // 日期时间（如开始结束时间）
LocalTime          // 时间（如班次开始结束）

// 数值类型
Integer            // 数量（如计划量、完成量）
Long               // 大数值（如库存）
BigDecimal         // 精确计算（如产能、时长）

// 状态标识
String             // 状态码（"0"/"1" 或 状态编码）
Integer            // 布尔标识（0/1）
```

### 6.2 枚举设计原则

```java
// 状态枚举应包含：code + desc
public enum ProductionStatus {
    NOT_STARTED("0", "未生产"),
    IN_PROGRESS("1", "生产中"),
    COMPLETED("2", "已完成");
    
    private final String code;
    private final String desc;
}
```

### 6.3 公共字段规范

```java
// BaseEntity 公共字段
CREATE_TIME        // 创建时间（自动填充）
UPDATE_TIME        // 更新时间（自动填充）
CREATE_BY          // 创建人
UPDATE_BY          // 更新人
REMARK             // 备注

// ApsBaseEntity 扩展字段
FACTORY_CODE       // 工厂编号（多工厂场景）
```

---

## ✅ 七、验收标准

### 7.1 代码规范

- [ ] 所有实体类使用 `@Data` 注解
- [ ] 所有字段添加 `@ApiModelProperty` 注解
- [ ] 表名使用 `@TableName` 明确指定
- [ ] 主键使用 `@TableId` 明确类型
- [ ] 枚举值使用常量类或枚举类

### 7.2 数据库规范

- [ ] 表名使用大写下划线命名
- [ ] 字段名使用大写下划线命名
- [ ] 所有表包含公共字段
- [ ] 主键统一为 `ID`，类型为 `BIGINT`
- [ ] 外键字段统一命名为 `XXX_ID`

### 7.3 功能验收

- [ ] 所有实体类可正常编译
- [ ] Mapper接口可正常CRUD
- [ ] Service层单元测试通过
- [ ] API接口可正常访问
- [ ] Swagger文档正确显示

---

## 📌 八、附录

### 8.1 新增实体类完整列表

| 序号 | 实体类 | 包路径 | 状态 |
|------|--------|--------|------|
| 1 | BaseEntity | entity.base | 待创建 |
| 2 | ApsBaseEntity | entity.base | 待创建 |
| 3 | MdmMoldingMachine | entity.mdm | 待创建 |
| 4 | MdmMaterialInfo | entity.mdm | 已提供 |
| 5 | MdmConstructionInfo | entity.mdm | 已提供 |
| 6 | MdmBomInfo | entity.mdm | 已提供 |
| 7 | MdmSkuConstructionRef | entity.mdm | 已提供 |
| 8 | MdmSkuStructureRef | entity.mdm | 已提供 |
| 9 | MdmCxMachineFixed | entity.mdm | 已提供 |
| 10 | MdmStructureLhRatio | entity.mdm | 已提供 |
| 11 | MdmStructureName | entity.mdm | 已提供 |
| 12 | MdmWorkCalendar | entity.mdm | 已提供 |
| 13 | MdmWorkWearInfo | entity.mdm | 已提供 |
| 14 | MdmMaterialConsumeDetail | entity.mdm | 已提供 |
| 15 | CxScheduleResult | entity.schedule | 已提供 |
| 16 | CxScheduleDetail | entity.schedule | 已提供 |
| 17 | CxScheduleVersion | entity.schedule | 已提供 |
| 18 | CxPrecisionPlan | entity.schedule | 已提供 |
| 19 | LhScheduleResult | entity.schedule | 已提供 |
| 20 | FactoryMonthPlanProductionFinalResult | entity.schedule | 已提供 |
| 21 | CxShiftConfig | entity.config | 已提供 |
| 22 | CxTreadConfig | entity.config | 已提供 |
| 23 | CxParams | entity.config | 已提供 |
| 24 | CxStock | entity.config | 已提供 |

---

**文档维护人：** APS Team
**最后更新：** 2024年3月
