# APS 成型排程系统 - 项目规范

## 项目概览

金宇轮胎APS系统-成型排程模块，基于 Spring Boot 2.7.18 开发，实现成型车间的智能排程功能。

### 核心功能
- **排程执行**: 根据硫化需求和库存情况自动生成成型排程
- **重排程**: 支持手动触发重新排程
- **动态调整**: 根据实时情况调整排程结果
- **节假日排程**: 特殊处理节假日和节前排程
- **试制排程**: 支持试制任务的排程管理

### 技术栈
- Java 1.8 (OpenJDK 1.8.0_482)
- Spring Boot 2.7.18
- MyBatis Plus 3.5.3.1
- MySQL 8.0.33
- Maven 3.8.7

## 目录结构

```
src/main/java/com/zlt/aps/
├── cx/                           # 成型排程模块
│   ├── config/                   # 配置类
│   │   ├── DatabaseInitializer.java
│   │   ├── MybatisPlusConfig.java
│   │   └── OpenApiConfig.java
│   ├── controller/               # 控制器层
│   │   ├── AlertConfigController.java
│   │   ├── MachineController.java
│   │   ├── ScheduleMainController.java
│   │   ├── ScheduleDetailController.java
│   │   └── StockController.java
│   ├── entity/                   # 实体类
│   │   ├── base/                 # 基础实体类
│   │   ├── config/               # 配置相关实体
│   │   │   ├── CxHolidayConfig.java
│   │   │   ├── CxKeyProduct.java
│   │   │   ├── CxParamConfig.java
│   │   │   ├── CxShiftConfig.java
│   │   │   ├── CxStructurePriority.java
│   │   │   └── CxStructureShiftCapacity.java
│   │   └── schedule/             # 排程相关实体
│   │       ├── CxScheduleDetail.java
│   │       ├── CxScheduleResult.java
│   │       ├── CxTrialPlan.java
│   │       └── LhScheduleResult.java
│   ├── enums/                    # 枚举类
│   │   └── DayVulcanizationModeEnum.java
│   ├── mapper/                   # Mapper 接口
│   ├── service/                  # 服务层
│   │   ├── engine/               # 核心算法引擎
│   │   │   ├── BalancingService.java
│   │   │   ├── ContinueTaskProcessor.java
│   │   │   ├── CoreScheduleAlgorithmService.java
│   │   │   ├── NewTaskProcessor.java
│   │   │   ├── ProductionCalculator.java
│   │   │   ├── ShiftScheduleService.java
│   │   │   ├── TaskGroupService.java
│   │   │   └── TrialTaskProcessor.java
│   │   └── impl/                 # 服务实现
│   │       └── validation/       # 数据校验策略
│   └── vo/                       # 值对象
│       ├── MonthPlanProductLhCapacityVo.java
│       ├── ScheduleContextVo.java
│       ├── ScheduleGenerateVo.java
│       ├── ScheduleQueryVo.java
│       ├── ScheduleRequestVo.java
│       └── ScheduleResultVo.java
└── mp/api/domain/entity/         # 主数据实体类
    ├── FactoryMonthPlanProductionFinalResult.java
    ├── MdmCxMachineFixed.java
    ├── MdmCxMachineOnlineInfo.java
    ├── MdmMaterialInfo.java
    ├── MdmMoldingMachine.java
    ├── MdmMonthSurplus.java
    ├── MdmSkuScheduleCategory.java
    ├── MdmStructureLhRatio.java
    └── MdmWorkCalendar.java
```

## 构建和测试命令

### 编译
```bash
mvn compile
```

### 运行测试
```bash
mvn test
```

### 打包
```bash
mvn package -DskipTests
```

### 运行应用
```bash
java -jar target/aps-forming-schedule-1.0.0.jar
```

## 代码风格指南

### 包命名规范
- `com.zlt.aps.cx` - 成型排程模块核心包
- `com.zlt.aps.mp` - 主数据相关包

### 类命名规范
- 实体类: `Cx` 前缀表示成型模块，`Mdm` 前缀表示主数据
- Mapper: 实体类名 + `Mapper`
- Service: 实体类名 + `Service`
- Controller: 模块名 + `Controller`

### 注释规范
- 类和公共方法必须添加 Javadoc 注释
- 使用 `@author` 标注作者
- 使用 `@param` 和 `@return` 说明参数和返回值

## 核心算法说明

### 排程执行流程
1. **构建排程上下文** (`ScheduleServiceImpl.buildScheduleContext`)
   - 加载机台信息、物料信息、库存数据
   - 加载硫化需求任务、在机信息
   - 加载配置参数和约束条件

2. **执行核心算法** (`CoreScheduleAlgorithmService.executeSchedule`)
   - 任务分组 (`TaskGroupService`)
   - 续作任务处理 (`ContinueTaskProcessor`)
   - 新任务处理 (`NewTaskProcessor`)
   - 试制任务处理 (`TrialTaskProcessor`)
   - 班次均衡分配 (`ShiftScheduleService`)

3. **保存排程结果**
   - 保存到 `T_CX_SCHEDULE_RESULT` 和 `T_CX_SCHEDULE_DETAIL` 表

### 约束条件
- 机台种类上限: 每台机台最多同时生产4种规格
- 续作优先: 正在生产的胎胚必须继续生产
- 班次均衡: 按夜:早:中 = 1:2:1 的比例分配产量
- 关键产品优先: 关键产品排程优先级更高

## 配置说明

### 数据库配置
数据库连接信息通过 `application.yml` 配置，支持动态数据源切换。

### 关键配置表
- `T_CX_PARAM_CONFIG`: 系统参数配置
- `T_CX_SHIFT_CONFIG`: 班次配置
- `T_CX_HOLIDAY_CONFIG`: 节假日配置
- `T_CX_KEY_PRODUCT`: 关键产品配置

## 注意事项

1. **不要删除或修改以下文件**:
   - `ApsFormingScheduleApplication.java` - 应用启动类
   - `DatabaseInitializer.java` - 数据库初始化
   - `MybatisPlusConfig.java` - MyBatis Plus 配置

2. **实体类修改注意事项**:
   - 修改实体类时需同步更新对应的 Mapper XML 文件
   - 新增字段需添加到 `DatabaseInitializer` 的建表语句中

3. **算法修改注意事项**:
   - 核心算法位于 `service/engine` 目录
   - 修改前需充分理解现有逻辑
   - 建议添加单元测试验证修改

## 最近清理记录

### 2024年清理未使用文件
删除了以下未使用的文件:
- `com/zlt/aps/cx/common/enums/` 目录下所有枚举类 (7个)
- `com/zlt/aps/cx/model/entity/MaterialGroup.java`
- `com/zlt/aps/cx/entity/CxMaterial.java`
- `com/zlt/aps/cx/mapper/CxMaterialMapper.java`
- `com/zlt/aps/cx/controller/MaterialController.java`
- `com/zlt/aps/cx/vo/StockCalcVo.java`
