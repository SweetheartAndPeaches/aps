package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DFS 均衡分配算法测试
 *
 * 验证场景：
 * 1. 11R22.5-JD571四层案例：2台机台、6种胎胚、16个需求
 * 2. 简单案例：验证基本分配正确性
 * 3. 边界案例：种类数达到上限
 */
@DisplayName("DFS均衡分配算法测试")
class BalancingServiceTest {

    private BalancingService balancingService;

    @BeforeEach
    void setUp() {
        balancingService = new BalancingService();
    }

    // ==================== 11R22.5 案例测试 ====================

    @Nested
    @DisplayName("11R22.5-JD571四层案例（用户日志场景复现）")
    class I11R22_5Test {

        /**
         * 复现用户日志场景：
         * 2台机台(H1502, H1503), 每台 maxCapacity=8, maxTypes=4
         * 6种胎胚: 215101729(4), 215104553(4), 215101828(3), 215102348(2), 215102719(2), 215102643(1)
         * 总需求=16, 总产能=16
         *
         * 理论分析：2×4=8 种类槽 ≥ 6 种，2×8=16 产能 = 16 需求 → 完整解存在
         */
        @Test
        @DisplayName("11R22.5案例 - DFS应找到16/16完整解")
        void testI11R22_5_FullAllocation() {
            // 构建任务列表（模拟用户日志中的任务需求明细）
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks = new ArrayList<>();

            // 215101729(4): 4个硫化机台需求 → 4个任务，每个vulcanizeMachineCount=1
            for (int i = 0; i < 4; i++) {
                tasks.add(createTask("215101729", "11R22.5-JD571四层", 1));
            }
            // 215104553(4)
            for (int i = 0; i < 4; i++) {
                tasks.add(createTask("215104553", "11R22.5-JD571四层", 1));
            }
            // 215101828(3)
            for (int i = 0; i < 3; i++) {
                tasks.add(createTask("215101828", "11R22.5-JD571四层", 1));
            }
            // 215102348(2)
            for (int i = 0; i < 2; i++) {
                tasks.add(createTask("215102348", "11R22.5-JD571四层", 1));
            }
            // 215102719(2)
            for (int i = 0; i < 2; i++) {
                tasks.add(createTask("215102719", "11R22.5-JD571四层", 1));
            }
            // 215102643(1)
            tasks.add(createTask("215102643", "11R22.5-JD571四层", 1));

            // 构建机台配置
            List<MpCxCapacityConfiguration> machines = new ArrayList<>();
            machines.add(createMachine("H1502", "11R22.5-JD571四层"));
            machines.add(createMachine("H1503", "11R22.5-JD571四层"));

            // 机台产能和种类数配置
            Map<String, Integer> machineMaxLhMap = new HashMap<>();
            machineMaxLhMap.put("H1502", 8);
            machineMaxLhMap.put("H1503", 8);

            Map<String, Integer> machineMaxEmbryoTypesMap = new HashMap<>();
            machineMaxEmbryoTypesMap.put("H1502", 4);
            machineMaxEmbryoTypesMap.put("H1503", 4);

            // 无历史胎胚
            Map<String, Set<String>> machineHistoryMap = new HashMap<>();
            machineHistoryMap.put("H1502", new HashSet<>());
            machineHistoryMap.put("H1503", new HashSet<>());

            // 构建上下文（设置均衡阈值）
            ScheduleContextVo context = createContext();

            // 执行 DFS 均衡分配
            BalancingService.BalancingResult result = balancingService.balanceEmbryosToMachinesWithMachineCapacity(
                    tasks, machines, machineHistoryMap, machineMaxLhMap, machineMaxEmbryoTypesMap, false, context);

            // 验证结果
            assertNotNull(result, "分配结果不应为null");
            assertNotNull(result.getAssignments(), "分配列表不应为null");
            assertFalse(result.getAssignments().isEmpty(), "分配列表不应为空");

            // 统计总分配量
            int totalAssigned = 0;
            Map<String, Integer> machineLoadMap = new HashMap<>();
            Map<String, Set<String>> machineTypeMap = new HashMap<>();

            for (BalancingService.MachineAssignment assignment : result.getAssignments()) {
                String machineCode = assignment.getMachineCode();
                int machineLoad = 0;
                Set<String> types = new HashSet<>();

                for (BalancingService.EmbryoAssignment embryo : assignment.getEmbryoAssignments()) {
                    totalAssigned += embryo.getAssignedQty();
                    machineLoad += embryo.getAssignedQty();
                    types.add(embryo.getEmbryoCode());
                }

                machineLoadMap.put(machineCode, machineLoad);
                machineTypeMap.put(machineCode, types);

                System.out.println("机台 " + machineCode + ": 种类数=" + types.size()
                        + ", 负荷=" + machineLoad + ", 胎胚=" + formatAssignments(assignment));
            }

            System.out.println("总分配: " + totalAssigned + "/16");

            // 关键验证：总分配量应为16（完整解）
            assertEquals(16, totalAssigned, "DFS应找到16/16完整解，2台机台各8产能8种类上限完全可以容纳6种16个需求");

            // 验证每台机台种类数不超过4
            for (Map.Entry<String, Set<String>> entry : machineTypeMap.entrySet()) {
                assertTrue(entry.getValue().size() <= 4,
                        "机台 " + entry.getKey() + " 种类数 " + entry.getValue().size() + " 不应超过4");
            }

            // 验证每台机台负荷不超过8
            for (Map.Entry<String, Integer> entry : machineLoadMap.entrySet()) {
                assertTrue(entry.getValue() <= 8,
                        "机台 " + entry.getKey() + " 负荷 " + entry.getValue() + " 不应超过8");
            }
        }

        /**
         * 验证均衡性：两台机台的负荷差距和种类差距在阈值内
         */
        @Test
        @DisplayName("11R22.5案例 - 分配结果应均衡")
        void testI11R22_5_BalancedResult() {
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks = createI11R22_5Tasks();
            List<MpCxCapacityConfiguration> machines = createI11R22_5Machines();
            Map<String, Integer> machineMaxLhMap = createI11R22_5MaxLhMap();
            Map<String, Integer> machineMaxEmbryoTypesMap = createI11R22_5MaxTypesMap();
            Map<String, Set<String>> machineHistoryMap = createI11R22_5HistoryMap();
            ScheduleContextVo context = createContext();

            BalancingService.BalancingResult result = balancingService.balanceEmbryosToMachinesWithMachineCapacity(
                    tasks, machines, machineHistoryMap, machineMaxLhMap, machineMaxEmbryoTypesMap, false, context);

            Map<String, Integer> machineLoadMap = new HashMap<>();
            Map<String, Integer> machineTypeCountMap = new HashMap<>();

            for (BalancingService.MachineAssignment assignment : result.getAssignments()) {
                int load = assignment.getEmbryoAssignments().stream()
                        .mapToInt(BalancingService.EmbryoAssignment::getAssignedQty).sum();
                machineLoadMap.put(assignment.getMachineCode(), load);

                Map<String, Integer> embryoQtyMap = new LinkedHashMap<>();
                for (BalancingService.EmbryoAssignment e : assignment.getEmbryoAssignments()) {
                    embryoQtyMap.merge(e.getEmbryoCode(), e.getAssignedQty(), Integer::sum);
                }
                machineTypeCountMap.put(assignment.getMachineCode(), embryoQtyMap.size());
            }

            // 负荷差距应不超过3
            int loadDiff = Math.abs(machineLoadMap.getOrDefault("H1502", 0) - machineLoadMap.getOrDefault("H1503", 0));
            assertTrue(loadDiff <= 3, "负荷差距 " + loadDiff + " 应不超过阈值3");

            // 种类差距应不超过1
            int typeDiff = Math.abs(machineTypeCountMap.getOrDefault("H1502", 0) - machineTypeCountMap.getOrDefault("H1503", 0));
            assertTrue(typeDiff <= 1, "种类差距 " + typeDiff + " 应不超过阈值1");
        }
    }

    // ==================== 简单案例测试 ====================

    @Nested
    @DisplayName("简单分配测试")
    class SimpleAllocationTest {

        @Test
        @DisplayName("2台机台4种胎胚 - 完整分配")
        void testSimpleAllocation() {
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks = new ArrayList<>();
            // 2种胎胚各2个需求
            for (int i = 0; i < 2; i++) {
                tasks.add(createTask("EMB001", "STRUCT_A", 1));
            }
            for (int i = 0; i < 2; i++) {
                tasks.add(createTask("EMB002", "STRUCT_A", 1));
            }

            List<MpCxCapacityConfiguration> machines = new ArrayList<>();
            machines.add(createMachine("M001", "STRUCT_A"));
            machines.add(createMachine("M002", "STRUCT_A"));

            Map<String, Integer> machineMaxLhMap = new HashMap<>();
            machineMaxLhMap.put("M001", 4);
            machineMaxLhMap.put("M002", 4);

            Map<String, Integer> machineMaxEmbryoTypesMap = new HashMap<>();
            machineMaxEmbryoTypesMap.put("M001", 4);
            machineMaxEmbryoTypesMap.put("M002", 4);

            Map<String, Set<String>> machineHistoryMap = new HashMap<>();
            machineHistoryMap.put("M001", new HashSet<>());
            machineHistoryMap.put("M002", new HashSet<>());

            ScheduleContextVo context = createContext();

            BalancingService.BalancingResult result = balancingService.balanceEmbryosToMachinesWithMachineCapacity(
                    tasks, machines, machineHistoryMap, machineMaxLhMap, machineMaxEmbryoTypesMap, false, context);

            int totalAssigned = result.getAssignments().stream()
                    .flatMap(a -> a.getEmbryoAssignments().stream())
                    .mapToInt(BalancingService.EmbryoAssignment::getAssignedQty)
                    .sum();

            assertEquals(4, totalAssigned, "简单案例应全部分配: 4/4");
        }

        @Test
        @DisplayName("1台机台5种胎胚 - 种类数超限应部分分配")
        void testTypeLimitExceeded() {
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks = new ArrayList<>();
            // 5种胎胚，但机台最多4种
            for (int i = 1; i <= 5; i++) {
                tasks.add(createTask("EMB00" + i, "STRUCT_A", 1));
            }

            List<MpCxCapacityConfiguration> machines = new ArrayList<>();
            machines.add(createMachine("M001", "STRUCT_A"));

            Map<String, Integer> machineMaxLhMap = new HashMap<>();
            machineMaxLhMap.put("M001", 8);

            Map<String, Integer> machineMaxEmbryoTypesMap = new HashMap<>();
            machineMaxEmbryoTypesMap.put("M001", 4);

            Map<String, Set<String>> machineHistoryMap = new HashMap<>();
            machineHistoryMap.put("M001", new HashSet<>());

            ScheduleContextVo context = createContext();

            BalancingService.BalancingResult result = balancingService.balanceEmbryosToMachinesWithMachineCapacity(
                    tasks, machines, machineHistoryMap, machineMaxLhMap, machineMaxEmbryoTypesMap, false, context);

            int totalAssigned = result.getAssignments().stream()
                    .flatMap(a -> a.getEmbryoAssignments().stream())
                    .mapToInt(BalancingService.EmbryoAssignment::getAssignedQty)
                    .sum();

            // 单机台最多4种，5种只能分配4个
            assertEquals(4, totalAssigned, "种类数超限时应部分分配: 4/5");

            Set<String> assignedTypes = new HashSet<>();
            result.getAssignments().stream()
                    .flatMap(a -> a.getEmbryoAssignments().stream())
                    .forEach(e -> assignedTypes.add(e.getEmbryoCode()));
            assertEquals(4, assignedTypes.size(), "分配的胎胚种类数不应超过4");
        }
    }

    // ==================== 辅助方法 ====================

    private CoreScheduleAlgorithmService.DailyEmbryoTask createTask(String embryoCode, String structureName, int vulcanizeMachineCount) {
        CoreScheduleAlgorithmService.DailyEmbryoTask task = new CoreScheduleAlgorithmService.DailyEmbryoTask();
        task.setEmbryoCode(embryoCode);
        task.setStructureName(structureName);
        task.setVulcanizeMachineCount(vulcanizeMachineCount);
        task.setIsTrialTask(false);
        task.setIsEndingTask(false);
        task.setIsContinueTask(false);
        task.setPlannedProduction(vulcanizeMachineCount);
        return task;
    }

    private MpCxCapacityConfiguration createMachine(String machineCode, String structureName) {
        MpCxCapacityConfiguration config = new MpCxCapacityConfiguration();
        config.setCxMachineCode(machineCode);
        config.setStructureName(structureName);
        return config;
    }

    private Map<String, String> createParam(String code, String value) {
        Map<String, String> param = new HashMap<>();
        param.put("paramCode", code);
        param.put("paramValue", value);
        return param;
    }

    private ScheduleContextVo createContext() {
        ScheduleContextVo context = new ScheduleContextVo();
        Map<String, CxParamConfig> paramConfigMap = new HashMap<>();
        paramConfigMap.put("BALANCE_TYPE_DIFF_THRESHOLD", createParamConfig("BALANCE_TYPE_DIFF_THRESHOLD", "1"));
        paramConfigMap.put("BALANCE_LOAD_DIFF_THRESHOLD", createParamConfig("BALANCE_LOAD_DIFF_THRESHOLD", "3"));
        context.setParamConfigMap(paramConfigMap);
        return context;
    }

    private CxParamConfig createParamConfig(String code, String value) {
        CxParamConfig config = new CxParamConfig();
        config.setParamCode(code);
        config.setParamValue(value);
        return config;
    }

    // 11R22.5 辅助方法
    private List<CoreScheduleAlgorithmService.DailyEmbryoTask> createI11R22_5Tasks() {
        List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks = new ArrayList<>();
        String[][] taskDefs = {
                {"4", "215101729"}, {"4", "215104553"}, {"3", "215101828"},
                {"2", "215102348"}, {"2", "215102719"}, {"1", "215102643"}
        };
        for (String[] def : taskDefs) {
            int count = Integer.parseInt(def[0]);
            for (int j = 0; j < count; j++) {
                tasks.add(createTask(def[1], "11R22.5-JD571四层", 1));
            }
        }
        return tasks;
    }

    private List<MpCxCapacityConfiguration> createI11R22_5Machines() {
        List<MpCxCapacityConfiguration> machines = new ArrayList<>();
        machines.add(createMachine("H1502", "11R22.5-JD571四层"));
        machines.add(createMachine("H1503", "11R22.5-JD571四层"));
        return machines;
    }

    private Map<String, Integer> createI11R22_5MaxLhMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("H1502", 8);
        map.put("H1503", 8);
        return map;
    }

    private Map<String, Integer> createI11R22_5MaxTypesMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("H1502", 4);
        map.put("H1503", 4);
        return map;
    }

    private Map<String, Set<String>> createI11R22_5HistoryMap() {
        Map<String, Set<String>> map = new HashMap<>();
        map.put("H1502", new HashSet<>());
        map.put("H1503", new HashSet<>());
        return map;
    }

    private String formatAssignments(BalancingService.MachineAssignment assignment) {
        Map<String, Integer> embryoQtyMap = new LinkedHashMap<>();
        for (BalancingService.EmbryoAssignment e : assignment.getEmbryoAssignments()) {
            embryoQtyMap.merge(e.getEmbryoCode(), e.getAssignedQty(), Integer::sum);
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : embryoQtyMap.entrySet()) {
            parts.add(entry.getKey() + "(" + entry.getValue() + ")");
        }
        return parts.toString();
    }
}
