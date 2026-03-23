package com.zlt.aps.cx.dto;

import com.zlt.aps.cx.entity.*;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxStructurePriority;
import com.zlt.aps.cx.entity.mdm.*;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 排程上下文DTO
 * 用于承载排程过程中需要的所有数据
 *
 * @author APS Team
 */
@Data
public class ScheduleContextDTO {

    /**
     * 排程日期
     */
    private LocalDate scheduleDate;

    /**
     * 可用成型机台列表
     */
    private List<MdmMoldingMachine> availableMachines;

    /**
     * 固定机台配置列表
     */
    private List<MdmCxMachineFixed> machineFixedConfigs;

    /**
     * 物料主数据列表
     */
    private List<CxMaterial> materials;

    /**
     * 库存列表
     */
    private List<CxStock> stocks;

    /**
     * 结构硫化配比列表
     */
    private List<MdmStructureLhRatio> structureLhRatios;

    /**
     * 结构优先级配置列表
     */
    private List<CxStructurePriority> structurePriorities;

    /**
     * 排程参数配置
     */
    private Map<String, CxParamConfig> paramConfigMap;

    /**
     * 预警配置
     */
    private Map<String, CxAlertConfig> alertConfigMap;

    /**
     * 试制任务列表
     */
    private List<CxTrialTask> trialTasks;

    /**
     * 精度计划列表
     */
    private List<CxPrecisionPlan> precisionPlans;

    /**
     * 操作工请假列表
     */
    private List<CxOperatorLeave> operatorLeaves;

    /**
     * 材料异常列表
     */
    private List<CxMaterialException> materialExceptions;

    /**
     * 胎面停放配置列表
     */
    private List<CxTreadParkingConfig> treadParkingConfigs;

    /**
     * 结构收尾管理列表
     */
    private List<CxStructureEnding> structureEndings;

    /**
     * 工作日历
     */
    private MdmWorkCalendar workCalendar;

    /**
     * 昨日排程结果（用于续作判断）
     */
    private List<CxScheduleResult> yesterdayResults;

    /**
     * 班次配置
     */
    private Map<String, ShiftInfo> shiftConfigs;

    /**
     * 是否开产日
     */
    private Boolean isOpeningDay;

    /**
     * 是否停产日
     */
    private Boolean isClosingDay;

    /**
     * 停产剩余天数
     */
    private Integer daysToClose;

    /**
     * 班次信息
     */
    @Data
    public static class ShiftInfo {
        private String shiftCode;
        private String shiftName;
        private Integer startHour;
        private Integer endHour;
        private Integer standardHours;
        private Boolean isActive;
    }
}
