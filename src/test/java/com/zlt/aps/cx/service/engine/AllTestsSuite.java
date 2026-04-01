package com.zlt.aps.cx.service.engine;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * APS成型排程系统 - 测试套件
 * 
 * 覆盖需求文档的所有核心业务场景：
 * 1. 核心计算逻辑测试
 * 2. 节假日开产与停产测试
 * 3. 试制量试测试
 * 4. 精度计划测试
 * 5. 收尾管理测试
 * 6. 动态调整测试
 */
@Suite
@SuiteDisplayName("APS成型排程系统 - 全量测试套件")
@SelectClasses({
    CoreCalculationTest.class,
    HolidayScheduleTest.class,
    TrialScheduleTest.class,
    PrecisionScheduleTest.class,
    FinishingScheduleTest.class,
    DynamicAdjustmentTest.class
})
class AllTestsSuite {
    // 测试套件入口
}
