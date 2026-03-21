# Java编程规范

> **文档版本**：v1.0
> **适用范围**：智立通（厦门）科技有限公司IS软件开发组所有项目中的JAVA代码编写部分
> **生效日期**：2026年3月22日

---

## 文档信息

| 项目 | 内容 |
|------|------|
| 项目名称 | XX项目 |
| 文档名称 | Java编程规范 |
| 拟制人 | - |
| 拟制日期 | - |
| 审核人 | - |
| 审核日期 | - |
| 批准人 | - |
| 批准日期 | - |
| 文件编号 | - |
| 机密等级 | - |

---

## 修改记录

| 修订号 | 作者 | 日期 | 简要说明 |
|--------|------|------|----------|
| 1.0 | - | - | 初始版本 |

---

## 目录

- [1. 引言](#1-引言)
  - [1.1. 编写目的](#11-编写目的)
  - [1.2. 适用范围](#12-适用范围)
  - [1.3. 读者对象](#13-读者对象)
  - [1.4. 缩略词](#14-缩略词)
  - [1.5. 参考资料](#15-参考资料)
- [2. 排版、格式](#2-排版格式)
- [3. 命名](#3-命名)
- [4. 对象、变量](#4-对象变量)
- [5. 注释](#5-注释)
- [6. 方法](#6-方法)
- [7. 其它](#7-其它)
- [8. 逻辑走查](#8-逻辑走查)

---

## 1. 引言

### 1.1. 编写目的

提供一整套编写高效可靠的 Java 代码的标准、约定和指南。它们以安全可靠的软件工程原则为基础，使代码易于理解、维护和增强。

### 1.2. 适用范围

本文档适用于智立通（厦门）科技有限公司IS软件开发组所有项目中的JAVA代码编写部分。所有新编写的JAVA代码必须严格遵循本文档。对于已有代码维护，也尽量采用本文档的相关约定。

### 1.3. 读者对象

本文档供以下相关人员阅览：

- 编程规范编写人员
- JAVA/JSP程序开发人员
- 代码走查人员

### 1.4. 缩略词

（待补充）

### 1.5. 参考资料

（待补充）

---

## 2. 排版、格式

### 2.1 缩进规则

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-2-1 | 所有的缩进定义为4个空格 | ✅ 强制 |
| 规则3-2-2 | 在代码中不要使用TAB键，应该使用4个空格键代替，因为在不同的编辑器下因为TAB设置的空格数目不一致会引起格式混乱 | ✅ 强制 |
| 规则3-2-3 | 页宽设置为120，通常一行代码不应该超过这个宽度，如超过，应该在一个逗号或者一个操作符后折行，语句折行后, 应该比原来的语句再缩进4个字符 | ✅ 强制 |
| 规则3-2-4 | 长表达式要在低优先级操作符处划分新行，操作符放在新行之首 | ✅ 强制 |

**示例：**

```java
if (((nUserType == 0) && (getUserState == 1)) || 
    ((nUserType == 1) && (getUserState == 0))) {
    // ...
}
```

### 2.2 大括号规则

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-2-5 | "{"与"}" 语句应该位于同一列的位置，与引用它们的语句左对齐。在函数体的开始、类的定义、结构的定义、枚举的定义以及if、for、do、while、switch、case语句中的程序都要采用如上的缩进方式 | ✅ 强制 |

**示例：**

```java
if (i > 0) {
    i++;
}

for (i = 0; i < MAX_NUMBER; i++) {
    // ...
}
```

### 2.3 语句规则

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-2-6 | 不允许把多条语句写在同一行 | ✅ 强制 |
| 可选3-2-7 | 相对独立的程序块之间、变量说明之后必须加空行 | 💡 建议 |

**错误示例：**

```java
// ❌ 错误
int i = 1; float f = 0;
```

**正确示例：**

```java
// ✅ 正确
int i = 0;
int nAge = 0;

if (checkUserValid(strUserId)) {
    // ...
}
```

### 2.4 空格使用规则

| 规则编号 | 规则内容 |
|----------|----------|
| 规则3-2-8 | 在代码中操作符、关键字和空格的关系应遵守以下规范 |

**详细规范：**

1. **if、for、while、switch等与后面的括号间应加空格**，使if等关键字更为突出、明显

```java
if (a >= b && c > d)
```

2. **函数名之后不要留空格**，紧跟左括号'('，以与关键字区别

3. **逗号、分号只在后面加空格**

```java
int i, j = 0;
```

4. **比较操作符, 赋值操作符"="、"+="，算术操作符"+"、"%"，逻辑操作符"&&"、"&"，位域操作符"<<"、"^"等二元操作符的前后加空格**

```java
a += 1;
a = b + c;
```

5. **"!"、"~"、"++"、"--"、"&"（地址运算符）等一元操作符前后不加空格**

```java
i++;
```

---

## 3. 命名

### 3.1 通用命名规则

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-3-1 | 代码中的命名均不能下划线或美元符号开始，也不能以下划线或美元符合结束 | ✅ 强制 |
| 规则3-3-2 | 所有编程相关的命名严禁使用拼音与英文混合的方式，更不允许直接使用中文的方式 | ✅ 强制 |
| 规则3-3-3 | 使用可以准确说明变量/字段/类的完整的英文描述符 | ✅ 强制 |
| 可选3-3-4 | 应该尽量采用软件产品相应领域的术语 | 💡 建议 |
| 规则3-3-5 | 除了部分被大家公认的缩写，尽量少用缩写，如果有使用了缩写则一定要注释注明 | ✅ 强制 |
| 规则3-3-6 | 杜绝不规范的缩写，避免望文不知义 | ✅ 强制 |
| 可选3-3-7 | 避免使用长名字（最好不超过 15 个字母） | 💡 建议 |
| 规则3-3-8 | 避免使用相似或者仅在大小写上有区别的名字 | ✅ 强制 |

**错误示例：**

```java
// ❌ 错误示例
_name / __name / $name / name_ / name$ / name__
RIBENGUIZI / Asan / blackList / whiteList / slave
```

**正确示例：**

```java
// ✅ 正确示例
firstName, grandTotal, CorporateCustomer
AtomicReferenceFieldUpdater
```

### 3.2 包名命名规则

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 可选3-3-9 | package的名字原则应该都是由一个小写单词组成；如果package名字由多个单词组成，所有的单词都应是小写 | 💡 建议 |

**公司包名格式：**

```
com.zlt.xxxx.yyyy
```

**说明：**
- `xxxx`：项目简称
- `yyyy`：package名
- 如果一个服务里包含多个项目，可以 `com.xxxx.yyyy` 结尾，`yyyy`：指模块编码

**常用包名规范：**

| 包类型 | 命名格式 | 示例 |
|--------|----------|------|
| 配置类 | `com.zlt.xxxx.yyyy.autoconfig` | - |
| 实体类 | `com.zlt.xxxx.yyyy.api.domain` | - |
| Mapper | `com.zlt.xxxx.yyyy.mapper` | 代码生成/开发生成 |
| VO | `com.zlt.xxxx.api.vo` | - |
| Service | `com.zlt.xxxx.yyyy.service` | - |
| Controller | `com.zlt.xxxx.yyyy.controller` | - |
| Util | `com.zlt.xxxx.utils` | - |
| 常量 | `com.zlt.xxxx.yyyy.api.constant` | - |
| API | `com.zlt.xxxx.yyyy.api` | - |
| 枚举 | `com.zlt.xxxx.api.enum` | - |

### 3.3 类名命名规则

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-3-10 | 类名使用UpperCamelCase 风格，但以下情形例外：DO / BO / DTO / VO / AO / PO / UID 等 | ✅ 强制 |

**命名风格说明：**
- **UpperCamelCase**：首字母大写，如 `StudentInfo`、`UserInfo`、`ProductInfo`
- **lowerCamelCase**：首字母小写，如 `studentInfo`、`userInfo`、`productInfo`

**常用类名规范：**

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 实体类 | `T` + 功能名称 | `TBuildingMachine` |
| VO | 功能名称 + `Vo` | `BuildingMachineVo` |
| 枚举 | 功能名称 + `Enum` | `MachineStatusEnum` |
| 常量 | `Constant` 或 功能名称 + `Constant` | `SystemConstant` |
| 工具类 | 功能名称 + `Util` | `StringUtil` |
| Mapper | 功能名称 + `Mapper` | `BuildingMachineMapper` |
| Controller | 功能名称 + `Controller` | `BuildingMachineController` |
| Service | `I` + 功能名称 + `Service` | `IBuildingMachineService` |
| 配置类 | 功能名称 + `Config` | `DatabaseConfig` |
| API | `I` + 功能名称 + `RemoteService` | `IUserRemoteService` |
| 抽象类 | `Abs` + 功能名称 或 `Base` + 功能名称 | `AbsUserService` / `BaseService` |
| 异常类 | 功能名称 + `Exception` | `BusinessException` |
| 测试类 | 测试类名称 + `Test` | `UserServiceTest` |

**示例：**

```java
// ✅ 正确示例
ForceCode
UserDO
HtmlDTO
XmlService
TcpUdpDeal
TaPromotion
```

### 3.4 方法名、变量名命名规则

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-3-11 | 方法名、参数名、成员变量、局部变量都统一使用lowerCamelCase 风格 | ✅ 强制 |
| 规则3-3-12 | 变量名或类的属性名的前缀应是名词或形容词，尽量在变量名或类的属性名名中体现所属类型 | ✅ 强制 |
| 规则3-3-13 | 类方法的命名应采用完整的英文描述符，类方法名称的第一个单词常常采用一个有强烈动作色彩的动词 | ✅ 强制 |

**示例：**

```java
// ✅ 正确示例
localValue
getHttpMessage()
inputUserId

Integer maxCount = 10;
String tableTitle = 'LoginTable';
moldingMachineVOS
moldingMachineVOList
```

**建议常用变量名或属性名后缀：**

| 后缀名 | 含义 | 示例 |
|--------|------|------|
| ID | ID | `moldingMachineId` |
| Code | 编号 | `moldingMachineCode`、`deparmentCode` |
| Name | 名称 | `departmentName` |
| Qty/num | 数量 | `purchaseQty`、`purchaseNum` |
| Price | 单价 | `purchasePrice` |
| Amt/Amount/Money | 金额 | `purchaseAmount`、`purchaseMoney` |
| Date | 日期 | `settleDate` |
| Time | 时间 | `createTime` |
| Vo | VO对象 | `departmentVo` |
| List/set | 列表 | `deparmentList`、`departmentSet`、`departmentVos` |

### 3.5 方法名前缀约定

| 动词前缀 | 含义 | 返回值 |
|----------|------|--------|
| can | 判断某个动作是否有权限 | 返回一个布尔值。True：可执行，false：不可执行 |
| has | 判断是否含有某个值 | 返回一个布尔值。true：含有此值；false：不含有此值 |
| is | 判断是否为某个值 | 返回一个布尔值。true：为某个值；false：不为某个值 |
| get | 获取某个值 | 函数返回一个非布尔值 |
| set | 设置某个值 | 无返回值、返回是否设置成功或者返回链式对象 |
| load | 加载某些数据到内存或缓存 | 无返回值或者返回是否加载完成的结果 |
| select | 加载某些数据并返回 | 返回查询数据或集合 |
| count | 获取统计值 | 返回整型 |
| save/insert | 新增保存数据 | 返回保存后的实体对像或列表 |
| remove/delete | 删除数据 | 返回布尔值，true：删除成功，false：删除失败 |
| update | 数据更新 | 无返回值或更新后的实体类对像、列表 |

**示例：**

```java
// ✅ 正确示例
openAccount()
printMailingLabel()
```

### 3.6 特殊局部变量命名

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 建议3-3-13 | 为方便起见，对于如下几个特殊的局部变量类型，命名约定可以放宽 | 💡 建议 |

**特殊类型命名规范：**

1. **流（Stream）**
   - 单输入流：`in`
   - 单输出流：`out`
   - 既输入又输出：`inOut`

2. **循环计数器**
   - 使用：`i`、`j` 或 `k`
   - 始终保持一致使用

3. **异常（Exception）**
   - 使用：`e`

### 3.7 常量命名规则

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 可选3-3-14 | 静态常量字段（static final）全部采用大写字母，单词之间用下划线分隔 | 💡 建议 |

**示例：**

```java
// ✅ 正确示例
MIN_BALANCE
DEFAULT_DATE
MAX_COUNT
```

---

## 4. 对象、变量

### 4.1 字符串处理

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 可选3-4-1 | 在进行字符串操作时尽量使用 StringBuffer 对象，而不是直接使用String | 💡 建议 |

**说明：**
StringBuffer 类是构成 String 类的基础。当我们在构造字符串的时候，我们应该用 StringBuffer 来实现大部分的工作，当工作完成后将 StringBuffer 对象再转换为需要的 String 对象。

**示例：**

```java
// ✅ 推荐做法
StringBuffer sb = new StringBuffer();
sb.append("Hello");
sb.append("World");
String result = sb.toString();

// ❌ 不推荐做法
String result = "";
result += "Hello";
result += "World";
```

### 4.2 线程安全

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 建议3-4-2 | 避免不必要的使用关键字 synchronized，应该在必要的时候再使用她，这是一个避免死锁的好方法 | 💡 建议 |

### 4.3 集合类使用

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 建议3-4-3 | 谨慎使用Vector和ArrayList类 | 💡 建议 |

**对比说明：**

| 特性 | Vector | ArrayList |
|------|--------|-----------|
| 同步性 | 同步（线程安全） | 异步（非线程安全） |
| 性能 | 较低（同步开销） | 较高 |
| 扩容策略 | 原来的一倍 | 原来的50% |

**选择建议：**
- 如果不需要线程安全的集合，使用ArrayList
- 如果要在集合中保存大量数据，使用Vector（可通过设置初始化大小避免资源开销）

### 4.4 对象释放

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 可选3-4-4 | 对于不使用的对象显示的设置为null，以提高java虚拟机的垃圾回收效率 | 💡 建议 |

**示例：**

```java
Vector vt = new Vector();
// ...
vt.Clear();
vt = null; // 主动释放内存，尽量减少对内存的占用时间
```

---

## 5. 注释

### 5.1 文档注释规范

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-5-1 | 必须使用框架gen表生成器，生成代码产生基本的文档注释。必须用 Javadoc / swagger 来为类生成文档规范 | ✅ 强制 |

### 5.2 文件头注释

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-5-2 | 在文件的开始部分应该有文件的说明信息 | ✅ 强制 |

**必须包含的信息：**
1. 版权信息
2. 文件名及其注释
3. 作者，完成日期
4. 版本信息
5. 修改记录

**示例：**

```java
/**
 * Copyright (c) 2008, 智立通（厦门）科技有限公司 All rights reserved。
 * 文件名称：StringUtil.java
 * 描 述：字符串处理类
 *
 * @author xxx
 * @date 2007年2月27日
 * @version 1.0
 * 
 * 修改记录：
 * 修改时间：2008年2月27日
 * 修 改 人：xxx
 * 修改内容：增加字符串分割方法
 */
```

### 5.3 方法注释

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-5-3 | 应按照javadoc的规范自动生成类方法中的注释信息 | ✅ 强制 |

**示例：**

```java
/**
 * 获取三个整数的最小值(功能描述)
 *
 * @param num1 [in] , int. 整数1
 * @param num2 [in] , int. 整数2
 * @param num3 [in] , int. 整数3
 * @return int. 三个整数中的最小值
 */
private int minOfThree(int num1, int num2, int num3) {
    // ...
}
```

### 5.4 变量注释

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 建议3-5-4 | 变量名字要求含义明确，可以没有注释。如果需要添加注释，请按照如下格式 | 💡 建议 |

**格式：**

```java
// 队列长度
protected int iSize;
```

### 5.5 注释基本原则

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-5-5 | 注释应该增加代码的清晰度，内容要清楚、明了，含义准确，防止注释二义性 | ✅ 强制 |
| 规则3-5-6 | 保持注释的简洁。最好的注释应该是简单明了的注释 | ✅ 强制 |
| 规则3-5-7 | 注释与代码应保持一致。修改代码同时修改相应的注释；不再有用的注释要删除 | ✅ 强制 |
| 规则3-5-8 | 注释应与其描述的代码相近，对代码的注释应放在其上方（对单条语句的注释）相邻位置，不可放在下面 | ✅ 强制 |
| 规则3-5-10 | 注释与所描述内容进行同样的缩排 | ✅ 强制 |

**错误示例：**

```java
// ❌ 错误的注释格式
public boolean checkUserValid(int nUserState){ //如果用户状态正常，则返回正确；否则返回错误
    return (nUserState == 1);
}
```

**正确示例：**

```java
// ✅ 正确的注释格式
// 如果用户状态正常，则返回正确；否则返回错误
public boolean checkUserValid(int nUserState) {
    return (nUserState == 1);
}
```

### 5.6 待办事项注释

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-5-9 | 如果代码中有需要日后补充的内容，必须添加注释说明 | ✅ 强制 |

**格式：**

```java
/** 
 * @todo 非正式定义子系统编号，需要删除
 */
```

### 5.7 变量和分支注释

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-5-11 | 对变量的定义和分支语句（条件分支、循环语句等）必须编写注释 | ✅ 强制 |

**示例：**

```java
for (nUserState != 1) { // 如果用户状态不正常
    // ...
}
```

### 5.8 代码块结束标记

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 建议3-5-12 | 在程序块的结束行右方加注释标记，以表明某程序块的结束 | 💡 建议 |

**示例：**

```java
if (nUserState != 1) { // 如果用户状态不正常
    for (......) {
        // .......
    }
} // end of if (nUserState != 1)
```

### 5.9 API接口注释

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-5-12 | 控制层代码action入口，必须编制API说明。声明@Api | ✅ 强制 |
| 规则3-5-13 | 控制层说明Api的用途和名称 | ✅ 强制 |

**示例：**

```java
@Api("Kettle数据接口")
@RestController
@RequestMapping("/kettle")
public class KettleBizController extends BaseController {

    @GetMapping("/start/{taskType}/{id}")
    @ApiOperation("执行一个kettle任务")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "taskType", value = "任务类型", required = true, dataType = "String"),
        @ApiImplicitParam(name = "id", value = "任务ID", required = true, dataType = "Long")
    })
    public AjaxResult startTrans(
        @PathVariable("taskType") String taskType,
        @PathVariable("id") Integer id
    ) {
        // ...
    }
}
```

---

## 6. 方法

### 6.1 方法规模控制

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-6-1 | 函数的规模尽量限制在200行以内，超过时拆分多个方法，增加可读性 | ✅ 强制 |

### 6.2 方法单一职责

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 可选3-6-2 | 一个方法仅完成一件功能。如果一个方法实现多个功能，可以考虑分拆成多个方法 | 💡 建议 |
| 可选3-6-3 | 如果多段代码重复做同一件事情，那么可考虑提供一个公用的方法实现这个功能 | 💡 建议 |

### 6.3 方法调用规则

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 建议3-6-4 | 非必要逻辑，减少函数本身或函数间的递归调用。算法除外 | 💡 建议 |

### 6.4 参数验证

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 可选3-6-5 | 检查函数所有参数输入的有效性 | 💡 建议 |

**示例：**

```java
// ✅ 正确做法
public void processList(List<String> list) {
    if (list == null || list.isEmpty()) {
        throw new IllegalArgumentException("List不能为空");
    }
    // ...
}
```

### 6.5 异常处理

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 可选3-6-6 | 对方法的异常中应有全面的处理 | 💡 建议 |

**示例：**

```java
try {
    // ...
} catch (SQLException sqle) {
    // 处理SQL异常
} catch (Exception e) {
    // 捕捉其它所有的Exception
    // 处理异常
}
```

### 6.6 代码清理

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-6-8 | 去掉没有用、没必要的变量、代码 | ✅ 强制 |
| 规则3-6-9 | 某些语句经编译后产生告警，但如果你认为它是正确的，那么应通过某种手段去掉告警信息 | ✅ 强制 |

### 6.7 系统退出

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 建议3-6-10 | exit 除了在 main 中可以被调用外，其他的地方不应该调用 | 💡 建议 |

### 6.8 循环常量

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 可选3-6-11 | 位于for循环中作为计数器值的数字常量，除了-1,0和1之外，不应被直接写入代码 | 💡 建议 |

**示例：**

```java
// ❌ 错误做法
for (int i = 0; i < 7; i++) {
    // ...
}

// ✅ 正确做法
final int DAYS_OF_WEEK = 7;
for (int i = 0; i < DAYS_OF_WEEK; i++) {
    // ...
}
```

---

## 7. 其它

### 7.1 日志输出

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-7-1 | 项目组提前设定调试信息的打印方式和打印级别，并设定统一的调试标识。严禁使用System.out.println("……")输出调试信息 | ✅ 强制 |
| 规则3-7-9 | 系统输出必须使用@Slf4j 注解，输出日志打印文件信息。禁止使用system的日志输出方法 | ✅ 强制 |

**示例：**

```java
@Slf4j
@Service
public class UserServiceImpl {
    
    public void process() {
        if (log.isDebugEnabled()) {
            log.debug("用户状态获取成功");
        }
    }
}
```

### 7.2 Import规范

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 建议3-7-2 | 要将系统包，插件包，自定义包区分开来；尽量明确指明需要包括的package中的类，不要使用"import java.io.*" | 💡 建议 |

**示例：**

```java
// ✅ 正确做法
import java.io.InputStream;
import java.util.Observable;
import com.nl.fjvip.tool.*;

// ❌ 不推荐做法
import java.io.*;
```

### 7.3 数据库操作

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-7-3 | 数据库操作业务逻辑，必须使用事务注解 | ✅ 强制 |
| 规则3-7-4 | 数据库操作使用框架Mapper,不允许使用JDBC等直接实例化连接的方法 | ✅ 强制 |
| 规则3-7-5 | 为提高系统性能，尽量避免在循环执行SQL；查询取数按业务分类一次性获取，数据插入、更新、删除应循环中分类收集并在循环后批量执行 | ✅ 强制 |
| 规则3-7-6 | 批量插入、更新、删除操作，在框架生成的服务serviceImpl代码上实现的接口 | ✅ 强制 |

**批量操作接口：**

```java
public interface IBaseService {
    public void insertBatchData(Collection dataList);
    public void updateBatchData(Collection dataList);
    public void mergerIntoBatchData(List dataList);
}
```

### 7.4 IN条件查询

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-7-7 | 避免使用IN条件的SQL查询方式。在必须使用的情况下，使用框架BaseService的executeSelectIn方法取数 | ✅ 强制 |

**示例：**

```java
@Override
public List selectAllConstructVersionByProductList(List productCodes) {
    return super.executeSelectIn(
        productConstructionRelaMapper::selectAllConstructVersionByProductList,
        productCodes
    );
}
```

### 7.5 导入导出功能

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-7-8 | 导入导出功能，不允许在项目中重复实现，使用框架controller的接口,重写接口来实现特殊逻辑 | ✅ 强制 |

**示例：**

```java
@Override
public String getProcedureCode() {
    return "0";
}

@Override
public String getFunctionName() {
    return getExportTemplateFileName();
}

@Override
public AjaxResult importDataByFeign(List list, boolean updateSupport, Long importLogId) {
    return iDocProductLimitEntityService.importData(list, false, importLogId);
}

@Override
public String getExportTemplateFileName() {
    return "品种限制成型机";
}

@Override
public List exportDataByFeign(DocProductLimitVo entity) {
    List docProductLimitVos = iDocProductLimitEntityService.getExcelList(entity);
    if (!docProductLimitVos.isEmpty()) {
        docProductLimitVos.forEach(vo -> {
            AjaxResult ajaxResult = iProductInfoService.getProductInfo(vo.getProductCode());
            if (ajaxResult.get("data") != null) {
                ProductInfo info = JSON.parseObject(JSON.toJSONString(ajaxResult.get("data")), ProductInfo.class);
                vo.setProductDescription(info.getSpecificationsDescribe());
            }
        });
    }
    return docProductLimitVos;
}

@Override
public ExcelUtil getCustomExcelUtil() {
    return new ExcelUtil<>(DocProductLimitVo.class, Arrays.asList("remark"));
}
```

### 7.6 国际化支持

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-7-10 | 项目中输出前后端接口消息，在前端中显示的，必须使用国际化转换，不允许在代码中硬编码输出内容 | ✅ 强制 |

**示例：**

```java
// ✅ 正确做法
String failMsg = StringUtils.format(
    I18nUtil.getMessage("api.factory.error.get.user.fail"),
    throwable.getMessage()
);

// 字典使用的国际化方式
DictUtils.getLabel(key)

// ❌ 错误做法
return "获取用户失败:" + throwable.getMessage();
```

### 7.7 Cloud接口规范

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-7-11 | Cloud接口提供给前端框架的API接口，value 必须是 gateway | ✅ 强制 |

**示例：**

```java
@FeignClient(
    contextId = "iSysLoginService",
    value = ServiceNameConstants.GATEWAY_SERVICE,
    path = "${api.path.auth:auth}"
)
public interface ISysLoginService {
    @PostMapping("/login")
    R login(@RequestBody LoginBody form);
}
```

### 7.8 Stream操作规范

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-7-12 | 使用Stream操作集合容器，不能使用可能出现空的类属性作为处理条件。如果必须使用，那么先进行filter把空属性的对象过滤掉 | ✅ 强制 |

### 7.9 工具类规范

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-7-13 | Utils工具类，不允许重复实现框架中已实现的类。优先使用zlt-common包，或ruoyi-common包中的utils | ✅ 强制 |

### 7.10 框架版本管理

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-7-14 | 新建项目使用框架主干版本，主干版本通过私服获取，不允许使用本机编译版本 | ✅ 强制 |
| 规则3-7-15 | 项目需要增加框架特殊功能或解决BUG，优先选择在框架主干上修正后，发布私服到项目中使用 | ✅ 强制 |
| 规则3-7-16 | 项目发布包从jenkins打包，不允许本地打包，使用测试通过的版本包 | ✅ 强制 |

**框架扩展方式：**

1. 在框架类上进行"模板方法"处理，扩展出方法给项目实现
2. 在项目中继承框架类，对使用的方法进行重写、重载扩展方法实现
3. 在项目中使用继承的子类

**注意事项：**
- 增加功能为项目单独使用，没有通用性，不得提交到框架主干版本
- 项目的技术负责人1名，有发布框架代码权限，专人处理框架代码

### 7.11 项目发布规范

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-7-16 | 使用生产发布环境的Jenkins发布到生产服务器。修改服务器配置采用相同方法把YML配置及启动文件发布到svn。Jenkin任务配置保留3个构建版本 | ✅ 强制 |
| 规则3-7-17 | 项目发布的日志配置信息，非必要不允许修改。生产环境日志必须控制文件滚动，禁止不限制文件大小的日志输出 | ✅ 强制 |
| 规则3-7-18 | 项目发布客户的版本，需建立SVN分支，bug在分支上解决后，合并到主干 | ✅ 强制 |
| 规则3-7-19 | 项目构建拆分api/engine/UI/biz/bizbase/common包。跨项目工程引用包，使用私服中转，不允许本机编译 | ✅ 强制 |

---

## 8. 逻辑走查

包括纯代码方面的逻辑和业务逻辑。

### 8.1 代码重复处理

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-8-1 | 项目中不允许存在相同重复代码，必须进行重构 | ✅ 强制 |

### 8.2 代码抽象化

| 规则编号 | 规则内容 | 强制程度 |
|----------|----------|----------|
| 规则3-8-2 | 代码实体、服务、Utils，代码生成后，有公共相同代码的情况下，必须进行抽象化处理 | ✅ 强制 |

**处理范围：**
- 实体有相同的字段
- 冗余字段
- 方法重叠等

---

## 附录

### A. 术语表

| 术语 | 说明 |
|------|------|
| UpperCamelCase | 首字母大写的驼峰命名法 |
| lowerCamelCase | 首字母小写的驼峰命名法 |
| DO | Data Object，数据对象 |
| BO | Business Object，业务对象 |
| DTO | Data Transfer Object，数据传输对象 |
| VO | View Object，视图对象 |
| AO | Application Object，应用对象 |
| PO | Persistent Object，持久化对象 |

### B. 强制程度说明

| 图标 | 含义 |
|------|------|
| ✅ 强制 | 必须严格遵守的规则，违反将导致代码审查不通过 |
| 💡 建议 | 推荐遵循的最佳实践，违反需要给出合理解释 |

### C. 常见错误示例

#### 命名错误

```java
// ❌ 错误
int a;  // 含义不明确
String name_;  // 包含下划线
List userList;  // 应使用List后缀

// ✅ 正确
int userAge;  // 含义明确
String userName;  // 无下划线
List<String> userList;  // 使用List后缀
```

#### 注释错误

```java
// ❌ 错误
// 方法注释格式不正确
public void process() {
    // 单行注释未缩进
    int i = 0;  // i++
}

// ✅ 正确
/**
 * 处理业务逻辑
 * 
 * @throws BusinessException 业务异常
 */
public void process() throws BusinessException {
    int index = 0;  // 初始化索引
    index++;  // 索引递增
}
```

#### 代码结构错误

```java
// ❌ 错误
if (condition) {
    // 代码块
    } else {  // 大括号不对齐
    // 代码块
}

// ✅ 正确
if (condition) {
    // 代码块
} else {  // 大括号对齐
    // 代码块
}
```

---

## 文档说明

**文档版本**：v1.0  
**文档类型**：编程规范文档  
**适用对象**：JAVA/JSP程序开发人员、代码走查人员  
**更新日期**：2026年3月22日

---

**备注**：本文档适用于智立通（厦门）科技有限公司IS软件开发组所有项目中的JAVA代码编写部分。所有新编写的JAVA代码必须严格遵循本文档。对于已有代码维护，也尽量采用本文档的相关约定。
