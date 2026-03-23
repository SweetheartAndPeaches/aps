# 金宇轮胎APS系统-成型排程完整技术文档

**文档版本**：V6.0.0  
**文档日期**：2026年3月23日  
**项目名称**：金宇轮胎生产排程系统（APS）-成型排程模块  
**版本说明**：Nick重新校验版本

---

## 文档变更记录

| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|----------|--------|
| V6.0.0 | 2026-03-23 | 重新审查 | 许世超 |
| V5.0.0 | 2026-03-23 | 整合数据库设计V5.1.0（修正版）；新增约束规则配置表、试错分配日志表、班次均衡调整记录表等5张算法支持表 | 系统生成 |
| V4.1.0 | 2026-03-22 | 新增第十二部分"测试设计"；补充接口容错机制、性能分析、异常处理分支 | 系统生成 |
| V4.0.0 | 2026-03-21 | 整合蓝图文档业务需求、优化现状与优化项、完善接口设计 | 系统生成 |
| V3.0.0-B | 2026-03-21 | 整合B版本试错分配算法、波浪交替策略、顺位标识更新、班次均衡调整 | 系统生成 |
| V2.0.0 | 2026-03-21 | 整合架构设计优化方案和补充流程图 | 系统生成 |
| V1.0.0 | 2026-03-21 | 初始版本 | 系统生成 |

---


---


# 第四部分：数据库表设计





## 一、成型排产完整主流程

```mermaid
flowchart TD
    Start([开始]) --> Input[输入: 计划日期、计划天数]
    Input --> Phase1Start[S5.1 前置校验与初始化]
    
    subgraph Phase1Sub [阶段一: 前置校验与初始化]
        Phase1Start --> GetMesData[获取MES数据<br/>胎胚库存/在产规格]
        GetMesData --> GetCuringPlan[获取硫化工序日计划]
        GetCuringPlan --> GetMachines[获取可用成型机台]
        GetMachines --> CheckData[数据完整性校验]
        CheckData --> DataValid{数据有效?}
        DataValid -->|否| ErrorEnd([结束: 数据异常])
        DataValid -->|是| InitContext[初始化排程上下文]
        InitContext --> CalcRemainder
    end
    
    CalcRemainder[S5.2 计算成型余量]
    CalcRemainder --> LoopCuring[遍历硫化日计划]
    LoopCuring --> CalcCuringRem[计算硫化余量<br/>= 计划量 - 完成量]
    CalcCuringRem --> CalcMoldRem[计算成型余量<br/>= 硫化余量 - 胎胚库存]
    CalcMoldRem --> MarkEnding[标注收尾SKU<br/>3天内收尾或<400条]
    MarkEnding --> NextCuring{还有计划?}
    NextCuring -->|是| LoopCuring
    NextCuring -->|否| SortTasks
    
    SortTasks[S5.3 任务排序]
    SortTasks --> PriorityStart[应用优先级规则]
    
    subgraph PriorityRulesSub [优先级排序规则]
        PriorityStart --> Rule1[规则1: 续作优先]
        Rule1 --> Rule2[规则2: 收尾SKU优先]
        Rule2 --> Rule3[规则3: 硫化日计划顺序]
        Rule3 --> Rule4[规则4: 优先级字段排序]
    end
    
    PriorityStart --> GetPending[生成待排产任务列表]
    GetPending --> Phase3
    
    Phase3[S5.3 续作与新增任务排产]
    Phase3 --> DayLoop[遍历日期]
    DayLoop --> ShiftLoop[遍历班次<br/>早班/中班/夜班]
    
    ShiftLoop --> HandleStartStop[处理开停产计划]
    HandleStartStop --> IsStop{是否停产?}
    IsStop -->|是| SkipShift[跳过该班次]
    IsStop -->|否| HandleAccuracy
    
    HandleAccuracy[处理精度计划]
    HandleAccuracy --> FilterMachines[过滤可用机台]
    FilterMachines --> GetAvailableMachines[获取该班次可用机台]
    GetAvailableMachines --> Continue
    
    Continue[S5.3.1 续作排产]
    Continue --> LoopContinueMachines[遍历机台]
    LoopContinueMachines --> HasInProduction{机台有在产?}
    HasInProduction -->|否| CheckContinueNext
    HasInProduction -->|是| GetInProSpec[获取在产规格]
    GetInProSpec --> HasRemainder{余量>0?}
    HasRemainder -->|否| CheckContinueNext
    HasRemainder -->|是| CalcContinueQty[计算续作计划量]
    CalcContinueQty --> BuildContinueLine[构建续作排程行]
    BuildContinueLine --> MarkContinue[标记: 续作=true]
    MarkContinue --> UpdateContinueRem[更新余量]
    UpdateContinueRem --> CheckContinueNext{还有机台?}
    CheckContinueNext -->|是| LoopContinueMachines
    CheckContinueNext -->|否| NewSpec
    
    SkipShift --> ShiftNext
    NewSpec[S5.3.2 新增规格排产]
    NewSpec --> S5_3_10[新增规格处理]
    
    ShiftLoop --> AddToAllLines[添加到总排程列表]
    AddToAllLines --> ShiftNext{还有班次?}
    ShiftNext -->|是| ShiftLoop
    ShiftNext -->|否| DayNext
    DayLoop --> DayNext{还有日期?}
    DayNext -->|是| DayLoop
    DayNext -->|否| Phase4
    
    Phase4[S5.4 各个班次量均衡调整]
    Phase4 --> BalanceStart[收集班次数据]
    
    subgraph BalancePhaseSub [均衡调整]
        BalanceStart --> CollectData[收集班次数据]
        CollectData --> CompareStandard[对比班产标准]
        CompareStandard --> CheckDeviation{偏差范围?}
        CheckDeviation -->|正常| MarkNormal[标记: 正常]
        CheckDeviation -->|超下限| IncreaseQty[增加产量<br/>+12条/车]
        CheckDeviation -->|超上限| DecreaseQty[减少产量<br/>-12条/车]
        
        IncreaseQty --> CheckInventory{库存充足?}
        CheckInventory -->|否| LogInventoryWarn[记录库存警告]
        CheckInventory -->|是| ExecuteIncrease
        DecreaseQty --> CheckMinQty{>=最低产量?}
        CheckMinQty -->|否| KeepMin[保持最低]
        CheckMinQty -->|是| ExecuteDecrease
        ExecuteIncrease[执行增加]
        ExecuteDecrease[执行减少]
        
        ExecuteIncrease --> ValidateBalance
        ExecuteDecrease --> ValidateBalance
        MarkNormal --> ValidateBalance
        LogInventoryWarn --> ValidateBalance
        
        ValidateBalance[验证均衡结果]
    end
    
    ValidateBalance --> Phase5
    
    Phase5[S5.5 顺位标识与发布]
    Phase5 --> SetSequence[设置顺位标识]
    
    subgraph SequenceRulesSub [顺位标识规则]
        SetSequence --> SeqRule1[规则1: 续作优先<br/>sequence = 1]
        SeqRule1 --> SeqRule2[规则2: 收尾SKU<br/>sequence = 2]
        SeqRule2 --> SeqRule3[规则3: 固定规格<br/>sequence = 3]
        SeqRule3 --> SeqRule4[规则4: 新增规格<br/>sequence = 4]
    end
    
    SetSequence --> ValidateFinal[最终验证]
    ValidateFinal --> CheckConstraints{约束检查}
    
    subgraph ConstraintCheckSub [约束检查]
        CheckConstraints --> CheckTypeCount[胎胚种类数 <= 4]
        CheckTypeCount --> CheckRatio[成型硫化配比 <= 1.15]
        CheckRatio --> CheckSwitch[结构切换 <= 2次/天]
        CheckSwitch --> CheckInventoryFinal[库存时长检查]
    end
    
    CheckConstraints --> ConstraintsOK{全部通过?}
    ConstraintsOK -->|否| AdjustPlan[调整计划] --> ValidateFinal
    ConstraintsOK -->|是| SaveResult
    
    SaveResult[保存排程结果]
    SaveResult --> BatchInsert[批量插入数据库]
    BatchInsert --> PushMES[推送到MES系统]
    PushMES --> GenerateReport[生成排程报表]
    GenerateReport --> SendNotification[发送通知]
    SendNotification --> End([结束])
```

---

## 二、S5.3.10 试错分配算法详细流程

```mermaid
flowchart TD
    Start([开始]) --> Input[输入: 待排产任务列表、可用机台列表]
    Input --> PrepStart[S5.3.10.1 准备工作]
    
    subgraph PrepWork [准备工作]
        PrepStart --> GroupTasks[按规格分组待排产任务]
        GroupTasks --> SortByPriority[按优先级排序任务组]
        SortByPriority --> InitMachines[初始化机台状态]
        InitMachines --> InitCurrentLoad[设置当前负载 = 在产余量]
        InitCurrentLoad --> InitSkuCount[设置当前种类数 = 0]
        InitSkuCount --> SetGlobalBest[初始化全局最优方案]
    end
    
    SetGlobalBest --> TaskProcStart[S5.3.10.7 从第一个种类开始递归分配]
    TaskProcStart --> TaskLoop[遍历任务组]
    
    subgraph TaskProc [处理单个任务组]
        TaskLoop --> GetTaskInfo[获取任务信息<br/>规格/余量/优先级]
        GetTaskInfo --> FindMachineStart[S5.3.10.7.6 找能接活的机子]
        
        FindMachineStart --> MachineIter[遍历可用机台]
        
        subgraph MachineFilter [机台筛选]
            MachineIter --> CheckAvail{机台可用?}
            CheckAvail -->|否| NextMach[下一台]
            CheckAvail -->|是| CheckFixedSpec{是否固定规格?}
            
            CheckFixedSpec -->|是| MatchSpec{规格匹配?}
            MatchSpec -->|是| AddCand[加入候选列表]
            MatchSpec -->|否| CheckSwitchCond
            
            CheckFixedSpec -->|否| CheckSwitchCond[检查切换条件]
            CheckSwitchCond --> CheckSwitchCnt{今日切换次数<2?}
            CheckSwitchCnt -->|否| NextMach
            CheckSwitchCnt -->|是| CheckInch{英寸切换?}
            
            CheckInch -->|是| CheckShift{班次=早班?}
            CheckShift -->|否| NextMach
            CheckShift -->|是| CheckCap{产能充足?}
            
            CheckInch -->|否| CheckCap
            CheckCap -->|否| NextMach
            CheckCap -->|是| CheckInv{库存充足?}
            CheckInv -->|否| NextMach
            CheckInv -->|是| AddCand
            
            AddCand --> SortCand[按负载排序候选机台]
        end
        
        SortCand --> AllocEntry[进入分配计算]
        AllocEntry --> CalcAllocQty[计算可分配数量]
        
        subgraph AllocCalc [分配数量计算]
            CalcAllocQty --> GetTaskRem[获取任务余量]
            GetTaskRem --> GetMachCap[获取机台剩余产能]
            GetMachCap --> CheckInvQty[检查胎胚库存]
            CheckInvQty --> CalcMinQty[计算最小分配单位]
            CalcMinQty --> CalcResult["AllocQty = min(余量, 产能, 库存, 整车)"]
        end
        
        CalcResult --> CheckAlloc{可分配数量>0?}
        CheckAlloc -->|否| CheckMachLeft
        CheckAlloc -->|是| TryAssignEntry[开始尝试分配]
        
        subgraph TryAssign [尝试分配]
            TryAssignEntry --> SaveState[保存当前状态]
            SaveState --> UpdateMachLoad[更新机台负载]
            UpdateMachLoad --> UpdateSkuCnt[更新机台种类数]
            UpdateSkuCnt --> UpdateTaskRem[更新任务余量]
            UpdateTaskRem --> RecordSwitch[记录结构切换]
            
            RecordSwitch --> CheckTaskComplete{任务完成?}
            CheckTaskComplete -->|是| TaskDone[任务完成]
            CheckTaskComplete -->|否| RecurseAlloc[递归分配下一组]
            
            RecurseAlloc --> CompareBest[与全局最优比较]
            
            subgraph BestComp [最优方案比较]
                CompareBest --> CalcLoadDiff[计算负载差]
                CalcLoadDiff --> CalcTypeDiff[计算种类差]
                CalcTypeDiff --> GetBestDiff[获取最优方案的差]
                GetBestDiff --> IsBetter{新方案更优?}
                
                IsBetter -->|是| UpdateBest[更新全局最优方案]
                IsBetter -->|否| KeepBest[保持原最优方案]
                
                UpdateBest --> RestoreState
                KeepBest --> RestoreState
            end
            
            RestoreState --> NextCand{还有候选机台?}
            NextCand -->|是| ContinueTry[继续尝试]
            NextCand -->|否| NoMoreCand[无候选机台]
        end
        
        ContinueTry --> AllocEntry
        
        NoMoreCand --> CheckMachLeft{还有机台?}
        NextMach --> CheckMachLeft
        CheckMachLeft -->|是| MachineIter
        CheckMachLeft -->|否| RecordNoSol[记录无解状态]
        
        RecordNoSol --> TaskDone
        TaskDone --> NextTask{还有任务?}
        NextTask -->|是| TaskLoop
        NextTask -->|否| ReturnBestSol[返回最优方案]
    end
    
    ReturnBestSol --> CheckSuccess{找到可行方案?}
    CheckSuccess -->|否| NoSolEnd([结束: 无可行方案])
    CheckSuccess -->|是| OutputRes
    
    subgraph Output [输出结果]
        OutputRes --> GenLines[生成排程明细行]
        GenLines --> SetProdMode[设置生产模式<br/>投产/收尾]
        SetProdMode --> SetSeq[设置顺位标识]
        SetSeq --> ReturnList[返回排程明细列表]
    end
    
    ReturnList --> End([结束])
```

---

## 三、顺位标识定时更新流程

```mermaid
flowchart TD
    Start([定时任务触发]) --> CheckTime{是否在排班时间?}
    CheckTime -->|否| Wait[等待下次触发]
    CheckTime -->|是| FetchCurrentLines
    
    FetchCurrentLines[获取当前排程明细]
    FetchCurrentLines --> FilterToday[筛选今日排程]
    FilterToday --> GroupByMachine[按机台分组]
    
    GroupByMachine --> LoopMachine[遍历每个机台]
    LoopMachine --> GetMachineLines[获取机台排程列表]
    GetMachineLines --> IdentifyLines
    
    IdentifyLines[识别各类型排程]
    
    subgraph Identify [识别逻辑]
        IdentifyLines --> Check1{是续作规格?}
        Check1 -->|是| MarkSeq1[标记 sequence = 1<br/>续作优先]
        Check1 -->|否| Check2
        
        Check2{是收尾SKU?}
        Check2 -->|是| MarkSeq2[标记 sequence = 2<br/>收尾优先]
        Check2 -->|否| Check3
        
        Check3{是固定规格?}
        Check3 -->|是| MarkSeq3[标记 sequence = 3<br/>固定规格优先]
        Check3 -->|否| Check4
        
        Check4{是新增规格?}
        Check4 -->|是| MarkSeq4[标记 sequence = 4<br/>新增规格]
    end
    
    MarkSeq1 --> CollectLines
    MarkSeq2 --> CollectLines
    MarkSeq3 --> CollectLines
    MarkSeq4 --> CollectLines
    
    CollectLines[收集更新的排程行]
    CollectLines --> SortBySequence[按sequence升序排序]
    SortBySequence --> CheckNextMachine{还有机台?}
    CheckNextMachine -->|是| LoopMachine
    CheckNextMachine -->|否| UpdateDB
    
    UpdateDB[批量更新数据库]
    UpdateDB --> CheckSuccess{更新成功?}
    CheckSuccess -->|否| LogError[记录错误日志]
    CheckSuccess -->|是| NotifyMES
    
    NotifyMES[通知MES系统更新]
    NotifyMES --> GenerateReport[生成更新报告]
    GenerateReport --> SendAlert{有重要变更?}
    
    SendAlert -->|是| SendNotification[发送通知]
    SendAlert -->|否| End
    
    SendNotification[发送短信/邮件通知]
    SendNotification --> End
    
    End([结束])
    Wait --> Start
```

---

## 四、试错分配算法最优解判断逻辑

```mermaid
flowchart TD
    Start([开始]) --> Input[输入: 当前方案、历史最优方案]
    Input --> CalcCurrent[计算当前方案指标]
    
    subgraph CalcCurrentMetrics [当前方案指标]
        CalcCurrent --> GetLoadList[获取各机台负载列表]
        GetLoadList --> CalcLoadDiff 
        CalcLoadDiff --> LoadDiffValue[负载差值]
        
        CalcCurrent --> GetTypeList[获取各机台种类列表]
        GetTypeList --> CalcTypeDiff
        CalcTypeDif --> TypeDiffValue[种类差值]
        
        CalcCurrent --> CalcTotalLoad
        CalcTotalLoad --> TotalLoadValue[总负载量]
    end
    
    LoadDiffValue --> CompareBest
    TypeDiffValue --> CompareBest
    TotalLoadValue --> CompareBest
    
    CompareBest[与历史最优方案比较]
    CompareBest --> GetBestMetrics[获取最优方案指标]
    GetBestMetrics --> BestLoadDiff[最优负载差]
    GetBestMetrics --> BestTypeDiff[最优种类差]
    GetBestMetrics --> BestTotalLoad[最优总负载]
    
    CompareMetrics[比较指标]
    CompareMetrics --> LoadCompare
    
    subgraph CompareLogic [比较逻辑]
        LoadCompare[负载比较]
        LoadCompare --> LoadBetter{新负载差 < 最优负载差?}
        
        LoadBetter -->|是| CheckType
        LoadBetter -->|否| LoadEqual{新负载差 = 最优负载差?}
        
        LoadEqual -->|是| CheckType
        LoadEqual -->|否| CompareResult[新方案较差]
        
        CheckType[种类比较]
        CheckType --> TypeBetter{新种类差 < 最优种类差?}
        
        TypeBetter -->|是| UpdateBest[更新最优方案]
        TypeBetter -->|否| TypeEqual{新种类差 = 最优种类差?}
        
        TypeEqual -->|是| CompareTotal
        TypeEqual -->|否| CompareResult
        
        CompareTotal[总负载比较]
        CompareTotal --> TotalCompare{总负载相同?}
        TotalCompare -->|是| KeepBest[保持原方案]
        TotalCompare -->|否| UpdateBest
    end
    
    UpdateBest[更新全局最优方案]
    UpdateBest --> RecordBest[记录方案详情]
    RecordBest --> SaveMetrics[保存指标数据]
    SaveMetrics --> End
    
    CompareResult[新方案较差，不更新]
    CompareResult --> KeepBest
    
    KeepBest[保持原最优方案]
    KeepBest --> End
    
    End([结束，返回是否更新])
```

---

## 五、胎面整车波浪交替分配流程（补充）

```mermaid
flowchart TD
    Start([开始]) --> Input[输入: 总整车数、班次类型]
    Input --> CheckTotalQty{总整车数 <= 11?}
    
    CheckTotalQty -->|是| EvenMode[均匀分配模式]
    CheckTotalQty -->|否| WaveMode[波浪交替模式]
    
    subgraph EvenAllocation [均匀分配]
        EvenMode --> CalcPerShift[计算每班分配数]
        CalcPerShift --> Formula1
        Formula1 --> AssignAllShifts[所有班次分配相同数量]
        AssignAllShifts --> CalculateQty[计算产量 = 分配数 × 12]
    end
    
    subgraph WaveAllocation [波浪交替]
        WaveMode --> GetShiftType[获取班次类型]
        GetShiftType --> EarlyShift{早班?}
        EarlyShift -->|是| EarlyPlan
        EarlyShift -->|否| MidShift
        
        MidShift{中班?}
        MidShift -->|是| MidPlan
        MidShift -->|否| NightPlan
        
        NightShift{夜班?}
        NightShift -->|是| NightPlan
        NightShift -->|否| EarlyShift
        
        EarlyPlan[早班分配]
        EarlyPlan --> AssignEarly[分配6车]
        AssignEarly --> CalcEarlyQty
        
        MidPlan[中班分配]
        MidPlan --> CalcRemainder
        CalcRemainder --> AssignMid[分配剩余车数]
        AssignMid --> CalcMidQty
        
        NightPlan[夜班分配]
        NightPlan --> AssignNight[分配6车]
        AssignNight --> CalcNightQty
    end
    
    CalcQty --> ValidateTotal
    CalcEarlyQty --> ValidateTotal
    CalcMidQty --> ValidateTotal
    CalcNightQty --> ValidateTotal
    
    ValidateTotal[验证总分配量]
    ValidateTotal --> CheckSum{分配总和 = 原总数?}
    CheckSum -->|否| Adjust分配[调整分配]
    Adjust分配 --> ValidateTotal
    CheckSum -->|是| End
    
    End([结束，返回各班次分配])
```

---

## 六、数据校验与初始化详细流程

```mermaid
flowchart TD
    Start([开始]) --> Trigger[触发: 一键生成成型计划]
    Trigger --> Phase1[S5.1 前置校验与初始化]
    
    Phase1 --> CollectData[收集基础数据]
    
    subgraph DataCollection [数据收集]
        CollectData --> Step1[Step 1: 获取胎胚实时库存]
        Step1 --> CallMES1[调用MES接口]
        CallMES1 --> GetInventory[获取胎胚库存数据]
        GetInventory --> ValidateInventory{库存数据有效?}
        ValidateInventory -->|否| ErrorInventory[记录错误: 库存数据异常]
        ValidateInventory -->|是| Step2
        
        Step2[Step 2: 获取成型机在产规格]
        Step2 --> CallMES2[调用MES接口]
        CallMES2 --> GetCurrentSpec[获取当前在产规格]
        GetCurrentSpec --> ValidateSpec{规格数据有效?}
        ValidateSpec -->|否| ErrorSpec[记录错误: 规格数据异常]
        ValidateSpec -->|是| Step3
        
        Step3[Step 3: 获取硫化工序日计划]
        Step3 --> CallMES3[调用MES接口]
        CallMES3 --> GetCuringPlan[获取硫化日计划]
        GetCuringPlan --> ValidateCuring{计划数据有效?}
        ValidateCuring -->|否| ErrorCuring[记录错误: 计划数据异常]
        ValidateCuring -->|是| Step4
        
        Step4[Step 4: 获取可用成型机台]
        Step4 --> QueryDB[查询数据库]
        QueryDB --> GetMachines[获取机台列表]
        GetMachines --> ValidateMachines{机台数据有效?}
        ValidateMachines -->|否| ErrorMachine[记录错误: 机台数据异常]
        ValidateMachines -->|是| Step5
        
        Step5[Step 5: 获取班产标准]
        Step5 --> QueryDB2[查询数据库]
        QueryDB2 --> GetStandards[获取班产标准]
        GetStandards --> ValidateStandards{标准数据有效?}
        ValidateStandards -->|否| ErrorStandard[记录错误: 标准数据异常]
        ValidateStandards -->|是| Step6
        
        Step6[Step 6: 获取配置参数]
        Step6 --> QueryConfig[查询配置中心]
        QueryConfig --> GetConfig[获取配置参数]
        GetConfig --> ValidateConfig{配置数据有效?}
        ValidateConfig -->|否| ErrorConfig[记录错误: 配置数据异常]
        ValidateConfig -->|是| Step7
        
        Step7[Step 7: 获取精度计划]
        Step7 --> QueryDB3[查询数据库]
        QueryDB3 --> GetAccuracy[获取精度计划]
        GetAccuracy --> ValidateAccuracy{精度数据有效?}
        ValidateAccuracy -->|否| ErrorAccuracy[记录错误: 精度数据异常]
        ValidateAccuracy -->|是| Step8
        
        Step8[Step 8: 获取开停产计划]
        Step8 --> QueryDB4[查询数据库]
        QueryDB4 --> GetStopStart[获取开停产计划]
        GetStopStart --> ValidateStopStart{计划数据有效?}
        ValidateStopStart -->|否| ErrorStopStart[记录错误: 开停产计划异常]
        ValidateStopStart -->|是| CheckErrors
    end
    
    CheckErrors{有错误记录?}
    CheckErrors -->|是| BuildErrorMessage[构建错误消息]
    BuildErrorMessage --> ThrowException[抛出数据异常]
    ThrowException --> EndError([结束: 初始化失败])
    CheckErrors -->|否| InitContext
    
    InitContext[初始化排程上下文]
    InitContext --> CreateContext[new MoldingScheduleContext]
    CreateContext --> SetStartDate[设置开始日期]
    SetStartDate --> SetDays[设置计划天数]
    SetDays --> SetMachines[设置机台列表]
    SetMachines --> SetInventories[设置库存数据]
    SetInventories --> SetCuringPlans[设置硫化计划]
    SetCuringPlans --> SetStandards[设置班产标准]
    SetStandards --> SetConfig[设置配置参数]
    SetConfig --> SetStopStart[设置开停产计划]
    SetStopStart --> ValidateContext
    
    ValidateContext[验证上下文完整性]
    ValidateContext --> CheckMachinesAvailable{有机台可用?}
    CheckMachinesAvailable -->|否| ErrorNoMachine[错误: 无可用机台]
    CheckMachinesAvailable -->|是| CheckPlansExist
    
    CheckPlansExist{有硫化计划?}
    CheckPlansExist -->|否| ErrorNoPlan[错误: 无硫化计划]
    CheckPlansExist -->|是| InitSuccess
    
    InitSuccess[初始化成功]
    InitSuccess --> CalcInventoryHours[计算库存时长]
    CalcInventoryHours --> SetContextReady[标记上下文就绪]
    SetContextReady --> ReturnContext
    
    ReturnContext[返回排程上下文]
    ReturnContext --> EndSuccess([结束: 初始化完成])
    
    ErrorInventory --> CheckErrors
    ErrorSpec --> CheckErrors
    ErrorCuring --> CheckErrors
    ErrorMachine --> CheckErrors
    ErrorStandard --> CheckErrors
    ErrorConfig --> CheckErrors
    ErrorAccuracy --> CheckErrors
    ErrorStopStart --> CheckErrors
    
    ErrorNoMachine --> EndError
    ErrorNoPlan --> EndError
```

---

## 七、班次量均衡调整详细流程

```mermaid
flowchart TD
    Start([开始]) --> Input[输入: 排程列表、班产标准]
    Input --> Phase4[S5.4 各个班次量均衡调整]
    
    Phase4 --> GroupByShift[按日期和班次分组]
    GroupByShift --> ShiftLoop[遍历每个班次]
    
    ShiftLoop --> GetShiftLines[获取班次排程明细]
    GetShiftLines --> CalcShiftTotal[计算班次总产量]
    CalcShiftTotal --> GetStandard[获取该班次班产标准]
    
    GetStandard --> CompareStandard[对比班产标准]
    
    subgraph DeviationCheck [偏差检查]
        CompareStandard --> CalcDeviation
        CalcDeviation --> CheckDeviation
        
        CheckDeviation{偏差范围?}
        CheckDeviation -->|偏差<5%| NormalRange[正常范围]
        CheckDeviation -->|偏差>20%| HighDeviation[严重偏差]
        CheckDeviation -->|5%-20%| MediumDeviation[中度偏差]
    end
    
    NormalRange --> MarkStatus[标记状态: 正常]
    MarkStatus --> LogNormal[记录正常日志]
    LogNormal --> NextShift
    
    HighDeviation --> NeedAdjust[需要调整]
    NeedAdjust --> DetermineDirection[确定调整方向]
    
    subgraph AdjustLogic [调整逻辑]
        DetermineDirection --> IsLow{实际产量 < 标准?}
        IsLow -->|是| IncreaseMode[增加模式]
        IsLow -->|否| DecreaseMode[减少模式]
        
        IncreaseMode[增加模式]
        IncreaseMode --> CheckInventory{库存充足?}
        CheckInventory -->|否| LogInventoryWarn[记录库存警告] --> NextShift
        CheckInventory -->|是| CalcIncreaseQty
        CalcIncreaseQty --> CheckCapacity{产能足够?}
        CheckCapacity -->|否| AdjustIncrease[调整增加量至产能上限]
        AdjustIncrease --> ExecuteIncrease
        CheckCapacity -->|是| ExecuteIncrease
        
        ExecuteIncrease[执行增加]
        ExecuteIncrease --> SelectSKU[选择待增加SKU]
        SelectSKU --> UpdatePlanQty[更新计划产量]
        UpdatePlanQty --> LogIncrease[记录增加日志]
        LogIncrease --> ValidateNewQty
        
        DecreaseMode[减少模式]
        DecreaseMode --> CalcDecreaseQty
        CalcDecreaseQty --> CheckMinQty{减后>=最低产量?}
        CheckMinQty -->|否| AdjustDecrease[调整减少量至最低产量]
        AdjustDecrease --> ExecuteDecrease
        CheckMinQty -->|是| ExecuteDecrease
        
        ExecuteDecrease[执行减少]
        ExecuteDecrease --> SelectReduceSKU[选择待减少SKU]
        SelectReduceSKU --> ReducePlanQty[减少计划产量]
        ReducePlanQty --> LogDecrease[记录减少日志]
        LogDecrease --> ValidateNewQty
    end
    
    ValidateNewQty[验证新产量]
    ValidateNewQty --> RecalcTotal[重新计算班次总量]
    RecalcTotal --> RecheckDeviation[重新检查偏差]
    RecheckDeviation --> CheckNewDeviation{新偏差<10%?}
    CheckNewDeviation -->|否| IterativeAdjust[迭代调整] --> DetermineDirection
    CheckNewDeviation -->|是| MarkAdjusted
    
    MarkAdjusted[标记状态: 已调整]
    MarkAdjusted --> LogAdjusted[记录调整日志]
    LogAdjusted --> NextShift
    
    MediumDeviation --> OptionalAdjust[可选调整]
    OptionalAdjust --> CheckInventory2{库存不足?}
    CheckInventory2 -->|是| IncreaseMode
    CheckInventory2 -->|否| CheckOverload{产能过载?}
    CheckOverload -->|是| DecreaseMode
    CheckOverload -->|否| NextShift
    
    NextShift{还有班次?}
    NextShift -->|是| ShiftLoop
    NextShift -->|否| ValidateOverall
    
    ValidateOverall[整体验证]
    ValidateOverall --> CheckBalance{整体均衡?}
    CheckBalance -->|否| GlobalAdjust[全局调整]
    GlobalAdjust --> RebalanceAll[重新平衡所有班次]
    RebalanceAll --> ValidateOverall
    CheckBalance -->|是| Finalize
    
    Finalize[完成均衡调整]
    Finalize --> GenerateReport[生成调整报告]
    GenerateReport --> End([结束])
```

---

## 八、完整数据流向图（补充）

```mermaid
flowchart LR
  subgraph ExternalSystems [外部系统]
      MES[MES系统<br/>胎胚库存/在产规格/日计划]
      MESOut[MES系统<br/>排程下发/执行反馈]
      Config[配置中心<br/>动态参数]
  end
  
  subgraph Database [数据库]
      DB_Machines[(成型机台表)]
      DB_Standards[(班产标准表)]
      DB_StopStart[(开停产计划表)]
      DB_Accuracy[(精度计划表)]
      DB_Schedule[(排程明细表)]
      DB_Inventory[(库存记录表)]
  end
  
  subgraph APS [成型排程系统]
      subgraph ControllerLayer [控制层]
          Controller[MoldingScheduleController]
      end
      
      subgraph ServiceLayer [服务层]
          Service[MoldingScheduleService]
      end
      
      subgraph Domain [领域层]
          Context[MoldingScheduleContext]
          Calculator[余量计算器]
          Generator[排程生成器]
          TrialAlloc[试错分配器]
          Balancer[均衡调整器]
          Validator[约束验证器]
          Sequencer[顺位标识器]
      end
      
      subgraph Algorithm [算法层]
          Algorithm1[递归回溯算法]
          Algorithm2[波浪交替分配]
          Algorithm3[最优解比较]
          Algorithm4[负载均衡算法]
      end
      
      subgraph Event [事件层]
          Publisher[事件发布器]
          Subscribers[订阅者<br/>MES推送/预警/报表]
      end
  end
  
  subgraph Monitor [监控与反馈]
      Monitor1[排程执行监控]
      Monitor2[库存预警]
      Monitor3[异常处理]
  end
  
  ExternalSystems --> Controller
  Database --> Controller
  Controller --> Service
  
  Service --> Context
  Service --> Calculator
  Service --> Generator
  Service --> Balancer
  Service --> Validator
  Service --> Sequencer
  
  MES --> Context
  Database --> Context
  Config --> Context
  
  Calculator --> Context
  Context --> Generator
  
  Generator --> TrialAlloc
  TrialAlloc --> Algorithm1
  Algorithm1 --> Algorithm3
  Algorithm3 --> Algorithm2
  Algorithm2 --> TrialAlloc
  TrialAlloc --> Generator
  
  Generator --> Balancer
  Balancer --> Algorithm4
  Algorithm4 --> Balancer
  Balancer --> Service
  
  Service --> Validator
  Validator --> Service
  
  Service --> Sequencer
  Sequencer --> Service
  
  Service --> Publisher
  Publisher --> Subscribers
  Subscribers --> MESOut
  Subscribers --> Monitor2
  
  MESOut --> Monitor1
  Monitor1 --> Monitor3
  Monitor3 --> Context
  
  Service --> DB_Schedule
  Service --> DB_Inventory
  
  style ExternalSystems fill:#e1f5ff
  style Database fill:#fff4e1
  style ControllerLayer fill:#ffe1f5
  style ServiceLayer fill:#e1ffe1
  style Domain fill:#f5ffe1
  style Algorithm fill:#e1e1ff
  style Event fill:#ffe1e1
  style Monitor fill:#f0f0f0
```

---

## 九、试错分配算法核心逻辑流程图

```mermaid
flowchart TD
    Start([开始: 递归分配]) --> Input[输入: 当前任务索引、机台状态]
    Input --> BaseCase
    
    BaseCase[基准情况检查]
    BaseCase --> AllTasksDone{所有任务已分配?}
    AllTasksDone -->|是| EvaluateSolution[评估当前方案]
    EvaluateSolution --> CompareWithBest[与全局最优比较]
    CompareWithBest --> IsBetter{更优?}
    IsBetter -->|是| UpdateBest[更新全局最优]
    IsBetter -->|否| ReturnFalse
    UpdateBest --> ReturnTrue
    ReturnTrue([返回: 成功])
    ReturnFalse([返回: 失败])
    
    AllTasksDone -->|否| GetTask[获取当前任务]
    GetTask --> TaskComplete{任务余量<=0?}
    TaskComplete -->|是| NextTask[递归处理下一任务]
    NextTask --> GetNextTask
    GetNextTask --> Input
    
    TaskComplete -->|否| FindMachines[找可用机台]
    FindMachines --> MachineLoop[遍历机台]
    
    MachineLoop --> CheckMachine{机台可用?}
    CheckMachine -->|否| NextMachine
    CheckMachine -->|是| CheckCapacity{产能足够?}
    
    CheckCapacity -->|否| NextMachine
    CheckCapacity -->|是| CalcMaxQty[计算最大可分配量]
    
    CalcMaxQty --> MaxQty{可分配>0?}
    MaxQty --> 0 -->|否| NextMachine
    MaxQty --> 0 -->|是| TryQuantities
    
    TryQuantities[尝试分配数量]
    TryQuantities --> QtyLoop[从最大量递减尝试]
    
    QtyLoop --> SaveCurrentState[保存当前状态]
    SaveCurrentState --> AssignQty[分配数量]
    AssignQty --> UpdateMachine[更新机台状态]
    UpdateMachine --> UpdateTask[更新任务余量]
    UpdateTask --> RecursiveCall[递归处理当前任务]
    
    RecursiveCall --> RecursiveSuccess{递归成功?}
    RecursiveSuccess -->|是| ReturnSuccess([返回: 成功])
    RecursiveSuccess -->|否| RestoreState[恢复状态]
    
    RestoreState --> NextQty{还有数量可尝试?}
    NextQty -->|是| QtyLoop
    NextQty -->|否| NextMachine
    
    NextMachine{还有机台?}
    NextMachine -->|是| MachineLoop
    NextMachine -->|否| Backtrack([回溯: 无解])
    
    Backtrack --> ReturnFailure([返回: 失败])
    ReturnSuccess --> ReturnSuccess
    ReturnFailure --> ReturnFailure
```

---

## 十、开产首班处理流程

```mermaid
flowchart TD
    Start([开始]) --> CheckStart{是否为开产日?}
    CheckStart -->|否| EndNormal([正常结束])
    CheckStart -->|是| GetFirstShift[获取首班排程]

    GetFirstShift --> GetKeyProducts[获取结构关键产品列表]
    GetKeyProducts --> LoopLines[遍历首班排程行]

    LoopLines --> CalcSixHour[计算6小时计划量<br/>= 原量 × 0.5]
    CalcSixHour --> RoundVehicle[按整车取整]

    RoundVehicle --> CheckKeyProduct{是否关键产品?}
    CheckKeyProduct -->|是| CheckOnlyKey{结构只有该产品?}
    CheckKeyProduct -->|否| SetNormal[设置计划量]

    CheckOnlyKey -->|是| SetNormal
    CheckOnlyKey -->|否| SetZero[计划量设为0]
    SetZero --> MoveNextShift[移到下一班]

    SetNormal --> MarkStarting[标记为首班]
    MoveNextShift --> MarkStarting

    MarkStarting --> MoreLines{还有排程行?}
    MoreLines -->|是| LoopLines
    MoreLines -->|否| Validate[验证总计划量]

    Validate --> End([结束])
```

---

## 十一、停产最后一班处理流程

```mermaid
flowchart TD
    Start([开始]) --> CheckStop{是否为停产前最后一班?}
    CheckStop -->|否| EndNormal([正常结束])
    CheckStop -->|是| CalcStopTime[计算硫化停火时间]

    CalcStopTime --> CalcMoldingStop[计算成型停机时间<br/>= 停火时间 - 8小时]
    CalcMoldingStop --> GetLastShift[获取最后一班排程]

    GetLastShift --> LoopLines[遍历排程行]

    LoopLines --> GetInventory[获取当前库存]
    GetInventory --> CalcCuringDemand[计算硫化需求<br/>= 硫化机数 × 模数 × 硫化次数]

    CalcCuringDemand --> CalcPlanQty[计算计划量<br/>= 硫化需求 - 库存]
    CalcPlanQty --> MaxZero{计划量 > 0?}

    MaxZero -->|是| RoundVehicle[按整车取整]
    MaxZero -->|否| SetZero[计划量设为0]

    RoundVehicle --> UpdateLine[更新排程行]
    SetZero --> UpdateLine

    UpdateLine --> MarkEnding[标记为收尾]
    MarkEnding --> CheckInventory{验证库存是否为0?}

    CheckInventory -->|否| LogWarning[记录警告日志]
    CheckInventory -->|是| MoreLines{还有排程行?}
    LogWarning --> MoreLines

    MoreLines -->|是| LoopLines
    MoreLines -->|否| End([结束])
```

---

## 十二、产能不足处理流程

```mermaid
flowchart TD
    Start([开始]) --> CalcTotal[计算总需求和总产能]
    CalcTotal --> CheckCapacity{总需求 > 总产能?}

    CheckCapacity -->|否| EndNormal([产能充足，结束])
    CheckCapacity -->|是| CalcGap[计算产能缺口]

    CalcGap --> SortByPriority[按优先级排序任务<br/>低优先级在前]
    SortByPriority --> LoopLines[遍历排程行]

    LoopLines --> CheckGap{产能缺口 > 0?}
    CheckGap -->|否| End
    CheckGap -->|是| CalcReduce[计算扣减量<br/>= min当前量, 缺口]

    CalcReduce --> ReduceQty[扣减计划量]
    ReduceQty --> RecordReason[记录扣减原因]
    RecordReason --> UpdateGap[更新产能缺口]

    UpdateGap --> MoreLines{还有排程行?}
    MoreLines -->|是| LoopLines
    MoreLines -->|否| CheckFinalGap{仍有缺口?}

    CheckFinalGap -->|是| SendAlert[发送产能严重不足预警]
    CheckFinalGap -->|否| End([结束])
    SendAlert --> End
```

---

## 十三、库存爆满处理流程

```mermaid
flowchart TD
    Start([开始]) --> CheckAllOverstock{所有SKU库存 > 18小时?}

    CheckAllOverstock -->|否| EndNormal([结束])
    CheckAllOverstock -->|是| SendAlert[发送库存过高预警]

    SendAlert --> LoopInventory[遍历库存列表]
    LoopInventory --> CalcOverstock[计算超额时长<br/>= 当前时长 - 18]

    CalcOverstock --> CalcSuggestReduce[计算建议减量<br/>= 超额时长 × 每小时消耗]
    CalcSuggestReduce --> RoundVehicle[按整车取整]

    RoundVehicle --> AddSuggestion[添加减量建议]
    AddSuggestion --> MoreInventory{还有库存?}

    MoreInventory -->|是| LoopInventory
    MoreInventory -->|否| SaveSuggestions[保存建议供计划员参考]
    SaveSuggestions --> End([结束])
```

---

## 十四、试制校验流程

```mermaid
flowchart TD
    Start([开始]) --> CheckSunday{是否为周日?}
    CheckSunday -->|是| ErrorSunday[错误：周日不做试制]
    CheckSunday -->|否| CheckQuantityEven{数量是否为双数?}

    ErrorSunday --> EndError([结束，校验失败])

    CheckQuantityEven -->|否| ErrorEven[错误：数量须为双数]
    CheckQuantityEven -->|是| CheckShift{班次是否为早/中班?}

    ErrorEven --> EndError

    CheckShift -->|否| ErrorShift[错误：只能安排早/中班]
    CheckShift -->|是| CheckDailyCount{当天试制数量 < 2?}

    ErrorShift --> EndError

    CheckDailyCount -->|否| ErrorCount[错误：试制数量超限]
    CheckDailyCount -->|是| CheckFirstProduction{是否为结构首次起产?}

    ErrorCount --> EndError

    CheckFirstProduction -->|是| ErrorFirst[错误：首次起产日不安排试制]
    CheckFirstProduction -->|否| CheckConditions[检查配方/模具/在机]

    ErrorFirst --> EndError

    CheckConditions --> ConditionsOK{条件满足?}
    ConditionsOK -->|否| ErrorConditions[错误：条件不满足]
    ConditionsOK -->|是| EndSuccess([校验通过])

    ErrorConditions --> EndError
```

---

## 十五、胎面卷曲异常处理流程

```mermaid
flowchart TD
    Start([扫码上报]) --> GetActualMeters[获取实际米数]
    GetActualMeters --> GetPlannedMeters[获取计划米数]

    GetPlannedMeters --> CalcCompleteRate[计算完成率<br/>= 实际 / 计划]

    CalcCompleteRate --> CheckThreshold{完成率 < 80%?}

    CheckThreshold -->|是| AdjustTo100[调整完成率为100%]
    AdjustTo100 --> AddRemark[添加备注：胎面卷曲不够]

    CheckThreshold -->|否| CalcActualQty[计算实际产量<br/>= 计划 × 完成率]
    CalcActualQty --> UpdateLine[更新排程行]

    AddRemark --> UpdateLine
    UpdateLine --> LogRecord[记录处理日志]
    LogRecord --> End([结束])
```

---

## 十六、大卷帘布用完处理流程

```mermaid
flowchart TD
    Start([库存=0]) --> GetStructureSkus[获取结构下所有SKU]
    GetStructureSkus --> LoopSkus[遍历SKU]

    LoopSkus --> CheckMainProduct{是否主销产品?}

    CheckMainProduct -->|是| SuggestIncrease[建议加量生产]
    SuggestIncrease --> LogIncrease[记录：主销产品可加量]

    CheckMainProduct -->|否| CannotIncrease[不能加量]
    CannotIncrease --> LogCannot[记录：按单产品不能加量]

    LogIncrease --> MoreSkus{还有SKU?}
    LogCannot --> MoreSkus

    MoreSkus -->|是| LoopSkus
    MoreSkus -->|否| NotifyPlanner[通知计划员决策]
    NotifyPlanner --> End([结束])
```

---

## 十七、精度计划冲突处理流程

```mermaid
flowchart TD
    Start([冲突检测]) --> CheckUrgent{是否为紧急任务?}

    CheckUrgent -->|否| PostponeTask[任务顺延]
    PostponeTask --> EndNormal([结束])

    CheckUrgent -->|是| FindAlternative[查找可替代机台]

    FindAlternative --> HasAlternative{找到可替代机台?}

    HasAlternative -->|是| TransferTask[任务转移至其他机台]
    TransferTask --> EndTransfer([结束])

    HasAlternative -->|否| SendAlert[发送冲突预警]
    SendAlert --> WaitDecision[等待计划员决策]
    WaitDecision --> EndManual([结束])
```

---

## 十八、事务恢复流程

```mermaid
flowchart TD
    Start([系统启动]) --> QueryIncomplete[查询未完成的排程任务]
    QueryIncomplete --> HasIncomplete{有未完成任务?}

    HasIncomplete -->|否| EndNormal([正常启动])
    HasIncomplete -->|是| LoopTransactions[遍历未完成任务]

    LoopTransactions --> LogWarning[记录警告日志]
    LogWarning --> RollbackData[回滚未完成数据]
    RollbackData --> ReleaseLocks[释放资源锁]
    ReleaseLocks --> MarkRollbacked[标记为可重新触发]

    MarkRollbacked --> MoreTransactions{还有未完成任务?}
    MoreTransactions -->|是| LoopTransactions
    MoreTransactions -->|否| LogRecovery[记录恢复完成]
    LogRecovery --> End([结束])
```

---

## 十九、动态调整并发控制流程

```mermaid
flowchart TD
    Start([动态调整请求]) --> TryLock[尝试获取分布式锁]

    TryLock --> LockAcquired{获取成功?}

    LockAcquired -->|否| ReturnBusy[返回：操作繁忙，请稍后重试]
    ReturnBusy --> EndBusy([结束])

    LockAcquired -->|是| ExecuteAdjust[执行调整操作]
    ExecuteAdjust --> ReleaseLock[释放分布式锁]
    ReleaseLock --> ReturnResult[返回调整结果]
    ReturnResult --> End([结束])
```

---

## 二十、其他流程图

### 10.1 胎胚库存时长计算流程

```mermaid
flowchart TD
    Start([开始]) --> Input[输入: SKU编码、计划日期]
    Input --> GetInventory[获取胎胚实时库存]
    GetInventory --> GetCuringInfo[获取硫化机台数、单台模数]
    GetCuringInfo --> GetCuringTime[获取硫化时间]
    GetCuringTime --> CalcHours
    
    CalcHours[计算可供硫化时长]
    CalcHours --> Formula[公式: 库存 / 机台数 / 模数 / 硫化时间]
    Formula --> SaveHours[保存计算结果]
    SaveHours --> CalcShiftEnd
    
    CalcShiftEnd[计算交班库存时长]
    CalcShiftEnd --> GetShiftEndTime[获取交班时间]
    GetShiftEndTime --> Subtract[总时长 - 截止交班时长]
    Subtract --> SaveShiftEnd[保存交班时长]
    SaveShiftEnd --> CheckWarning
    
    CheckWarning[检查预警]
    CheckWarning --> CompareShiftEnd{交班时长 < 8小时?}
    CompareShiftEnd -->|是| MarkUnderstock[标记欠产预警]
    CompareShiftEnd -->|否| CompareOverstock
    
    MarkUnderstock --> End
    CompareOverstock{总时长 > 18小时?}
    CompareOverstock -->|是| MarkOverstock[标记超期预警]
    CompareOverstock -->|否| End
    
    MarkOverstock --> End
    End([结束])
```

### 10.2 开停产处理流程

```mermaid
flowchart TD
    Start([开始]) --> Input[输入: 计划日期、班次]
    Input --> GetPlans[获取开停产计划]
    GetPlans --> CheckType{计划类型?}
    
    CheckType -->|开产| StartPlan
    CheckType -->|停产| StopPlan
    
    subgraph StartPlanFlow [开产处理]
        StartPlan[开产处理]
        StartPlan --> GetMachineCodes[获取开产机台列表]
        GetMachineCodes --> CheckFirstShift{是否首班开产?}
        CheckFirstShift -->|是| CalcFirstQty[计算首班产量 = 标准×0.5]
        CheckFirstShift -->|否| CalcNormalQty[使用标准产量]
        CalcFirstQty --> CreateScheduleLines
        CalcNormalQty --> CreateScheduleLines
        CreateScheduleLines[创建排程明细]
        CreateScheduleLines --> MarkStarting[标记投产=true]
        MarkStarting --> UpdateStatus[更新计划状态=执行中]
    end
    
    subgraph StopPlanFlow [停产处理]
        StopPlan[停产处理]
        StopPlan --> GetStopMachines[获取停产机台列表]
        GetStopMachines --> CheckStopMode{停产方式?}
        
        CheckStopMode -->|全部收尾| AllEnding
        CheckStopMode -->|分阶段| PhaseEnding
        
        AllEnding[全部收尾]
        AllEnding --> CheckInventory{有在制品库存?}
        CheckInventory -->|是| CalcEndingQty[计算收尾产量 = 库存量]
        CheckInventory -->|否| SetZero[计划量=0]
        CalcEndingQty --> CreateStopLines
        SetZero --> CreateStopLines
        
        PhaseEnding[分阶段收尾]
        PhaseEnding --> CalcPhaseQty[计算分阶段产量]
        CalcPhaseQty --> CreateStopLines
        
        CreateStopLines[创建停产排程明细]
        CreateStopLines --> MarkEnding[标记收尾=true]
        MarkEnding --> RecordReason[记录停产原因]
    end
    
    UpdateStatus --> End
    RecordReason --> End
    End([结束])
```

---


## D. 试错分配算法特点

**核心思想**：通过递归回溯的方式，尝试所有可能的分配方案，找出满足所有约束条件且使负载和种类数最均衡的最优方案。

**优点**：
- 保证找到全局最优解（如果存在）
- 可以处理复杂的约束条件
- 均衡性好

**缺点**：
- 计算复杂度高（指数级）
- 对于大规模问题需要剪枝优化

**剪枝策略**：
1. 提前终止：当某个任务无法分配时，立即回溯
2. 有序尝试：按优先级顺序尝试分配，更快找到可行解
3. 记忆化：缓存中间结果，避免重复计算
4. 下界估计：当当前方案已不可能优于最优方案时，提前终止



## E. 术语表

| 术语 | 英文 | 说明 |
|------|------|------|
| APS | Advanced Planning and Scheduling | 高级计划与排程系统 |
| SKU | Stock Keeping Unit | 库存保有单位 |
| MES | Manufacturing Execution System | 制造执行系统 |
| WMS | Warehouse Management System | 仓库管理系统 |
| TBR | Truck and Bus Radial | 全钢子午线轮胎 |
| PCR | Passenger Car Radial | 半钢子午线轮胎 |
| 胎胚 | Tire Embryo | 成型后的未硫化轮胎 |
| 硫化 | Curing | 轮胎生产的最后工序 |
| 成型 | Molding | 将各部件组合成胎胚的工序 |
| 结构 | Structure | 轮胎规格，如12R22.5 |
| 续作 | Continuation | 延续上一班次的生产 |
| 收尾 | Ending | 某SKU最后一批生产 |
| 投产 | Starting | 新SKU开始生产 |

## F. 文档变更记录

| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|----------|--------|
| V4.1.0 | 2026-03-22 | 新增第十二部分"测试设计"；补充接口容错机制、性能分析、异常处理分支 | 系统生成 |
| V4.0.0 | 2026-03-21 | 整合蓝图文档业务需求、优化现状与优化项、完善接口设计 | 系统生成 |
| V3.0.0-B | 2026-03-21 | 整合B版本试错分配算法、波浪交替策略、顺位标识更新、班次均衡调整 | 系统生成 |
| V2.0.0 | 2026-03-21 | 整合架构设计优化方案和补充流程图 | 系统生成 |
| V1.0.0 | 2026-03-21 | 初始版本 | 系统生成 |

## G. 试制与量试规则

| 规则项 | 规则内容 |
|--------|----------|
| 提前申请 | 提前7天提交试制需求 |
| 条件检查 | 配方（制造示方、文字示方、硫化示方）、结构在机、模具 |
| 每日数量 | 一天最多做2个新胎胚 |
| 周日安排 | 周日不做试制 |
| 班次限制 | 只能安排在早班或中班（7:30-15:00） |
| 数量要求 | 必须是双数 |
| 紧急插单 | 紧急试制可在锁定期内插单，普通试制排到锁定期后1天 |
| 同一机台 | 试制和量试要在同一台成型机做 |
| 优先级 | 新胎胚优先级高于普通新增胎胚，但不能挤掉已排好的实单 |

## H. 精度计划规则

| 规则项 | 规则内容 |
|--------|----------|
| 校验周期 | 每个机台每两个月做一次 |
| 校验时长 | 每次4小时 |
| 每日数量 | 一天最多做2台 |
| 提前安排 | 正常提前3天安排（X号到期，安排在X-2号） |
| 班次安排 | 胎胚库存够吃超过一个班，安排在早班（7:30-11:30）；特殊情况可安排中班（13:00-17:00） |
| 硫化处理 | 精度期间成型机停机，胎胚库存够4小时以上硫化机继续生产，不够则减产一半 |

## I. 停产与开产规则

### 停产规则

| 时间节点 | 减量比例 |
|----------|----------|
| 倒数第3天 | 90% |
| 倒数第2天 | 80% |
| 倒数第1天 | 70% |

- 减量优先级：先减本来就没活的机台 → 当天刚好收尾的机台 → 客人少的结构 → 大订单
- 成型机停机时间：比硫化机停火提前1个班次
- 最后一班计划量：保证做完后胎胚库存刚好为0，正好够硫化机吃到停火

### 开产规则

| 规则项 | 规则内容 |
|--------|----------|
| 开机时间 | 成型机比硫化机提前1个班次开机 |
| 首班时长 | 只排6小时（不是正常8小时） |
| 首班产量 | 计划量减半 |
| 关键产品 | 开产第一个班不排关键产品，从第二个班才开始做（除非结构只有该产品） |

## J. 材料异常处理规则

### 胎面卷曲米数不够

- 操作工扫码上报实际米数
- 完成率低于80%（可配置）时，把完成率调成100%
- 在原因里备注"胎面卷曲不够"

### 大卷帘布用完

- 主销产品（月均销量≥500条）：可以加量生产（哪怕不在计划里），尽可能利用剩下的材料
- 按单生产的产品：不能加量

## K. 收尾管理规则

| 场景 | 处理方式 |
|------|----------|
| 10天内能做完 | 正常安排 |
| 10天内做不完 | 计算延误量，平摊到未来3天补回来 |
| 3天内补不完 | 通知月计划调整（调用接口） |
| 3天内要收尾 | 打上"紧急收尾"标签，优先安排 |
| 主销产品收尾 | 月均销量≥500条，收尾余量不够一整车时，按整车下 |
| 非主销产品收尾 | 收尾余量≤2条时舍弃，>2条时按实际量下 |

---

# 第十一部分：成型排程系统整体说明

## 一、系统是什么？

成型排程系统就是帮助计划员安排每天生产任务的工具。它负责把硫化车间要生产的胎胚需求，转化成每台成型机每个班次具体做什么、做多少、按什么顺序做。

系统要考虑很多因素：

- 成型机有多少台、哪些能用、哪些在保养
- 胎胚库里还有多少库存
- 今天硫化要消耗多少
- 每台成型机最多能做几种不同的胎胚（不能超过4种）
- 胎面是按"整车"来的，一车就是12条
- 胎面做好后要停放4小时才能用
- 节假日要提前减产、节后要恢复
- 研发要试制新胎胚
- 成型机要定期做精度校验
- 操作工请假要调整计划
- 有些结构（菜系）快要收尾了，要优先安排

系统要把这些复杂的情况都处理好，最后输出一张清晰的排产表，发给车间执行。

---

## 二、系统要处理的特殊场景

### 2.1 开产与停产

#### 停产（比如放长假）

- 停产前三天，每天的计划量要按比例减少：倒数第3天90%、倒数第2天80%、倒数第1天70%
- 减量的优先级：先减本来就没活的机台，再减当天刚好收尾的机台，然后减那些客人少的结构，最后才减大订单
- 成型机停机时间：比硫化机停火提前1个班次
- 最后一班的计划量要算好，保证做完后胎胚库存刚好为0，正好够硫化机吃到停火
- 如果某个结构在停产期间有换模能力，可以临时新增胎胚，这种不受"增模要在机3天"的限制

#### 开产（节假日结束）

- 成型机比硫化机提前1个班次开机
- 开产后第一个班只排6小时（不是正常8小时），计划量减半
- 如果这个结构里有"关键产品"（质量要求特别高的），开产第一个班不排这些产品，从第二个班才开始做。但如果这个结构只有这一个关键产品，那第一个班也只能排它

---

### 2.2 试制与量试（研发新胎胚）

研发部要试做新胎胚，提前7天把需求提交给系统。系统会检查三个条件：

- 配方有没有（制造示方、文字示方、硫化示方）
- 这个结构目前有没有成型机在生产（结构在机）
- 模具有没有

满足条件后，系统按以下规则排产：

- 一天最多做2个新胎胚，周日不做
- 如果某天是这个结构第一次起产，不安排新胎胚
- 紧急的可以在锁定期内插单，普通的排到锁定期后1天
- 同一个胎胚的试制和量试要在同一台成型机做（系统会记住试制用的机台，量试时优先选它）
- 新胎胚的优先级高于普通的新增胎胚，但不能挤掉已经排好的实单
- 只能安排在早班或中班（7:30-15:00），数量必须是双数

---

### 2.3 成型精度计划（设备校准）

品质部每周会下发精度计划，告诉系统哪些机台什么时候要做精度校验。每个机台每两个月做一次，每次4小时。

- 正常提前3天安排（比如X号到期，就安排在X-2号）
- 一天最多做2台
- 如果胎胚库存够吃超过一个班，就安排在早班（7:30-11:30）；特殊情况可以安排中班（13:00-17:00）

精度期间，成型机停机。系统会判断：如果胎胚库存够硫化机吃4小时以上，硫化机继续生产；如果不够，硫化机要减产一半，慢慢消化库存，等成型精度做完再恢复。

---

### 2.4 操作工请假

计划员可以在系统里登记哪个机台、哪个班次、哪个厨师请假。登记后，计划员人工把那个机台的计划往后顺延或转给其他机台。系统不自动处理，只记录和提醒。

---

### 2.5 收尾管理（月度计划层面的收尾）

系统会每天检查每个结构（菜系）的收尾情况。

1. 先算还要做多少才能收尾：硫化余量 - 胎胚库存
2. 看10天内能不能做完：
   - 如果做不完，就计算延误了多少，把这个延误量平摊到未来3天里，让这3天多做一点补回来
   - 如果未来3天把所有机台开足马力也补不完，系统就通知月计划调整（调用接口）
3. 如果3天内就要收尾，就打上"紧急收尾"标签，优先安排
4. 10天以外的，正常安排

这个检查每天都会做，因为库存和计划都在变。

---

## 三、日常排程怎么做（每天早上的流程）

### 第一步：看全局

计划员一上班，系统先帮他算好今天要做什么。

#### 算需求量

系统用公式计算每个胎胚今天要做多少条：

日胎胚计划量 = (硫化今天要吃掉的量 - 从库存里分给这个胎胚的量) × (1 + 损耗率)

其中，库存是按比例分给不同胎胚的（如果多个胎胚共用同一种胎胚的话）。

#### 检查收尾

系统把胎胚按结构分组，对每个结构算一下"还要做多少才能收尾"。按前面说的逻辑，给每个结构打上标签（紧急收尾、计划收尾、正常）。

#### 查看机台

系统列出所有成型机，哪些能用、哪些在保养、哪个昨天做了什么（历史任务）。如果有精度计划，今天要做精度的机台就扣掉4小时产能。如果有人请假，那个机台那个班次就标记为不可用。

#### 如果有节假日

- 停产：系统按比例减量，按优先级扣减
- 开产：系统提前1个班次开机，首班只排6小时，计划量减半

#### 如果有试制

系统把符合条件的试制胎胚加入待排菜单

#### 如果有关键产品

如果今天是开产日，系统会把关键产品的第一个班计划量设为0（除非这个结构只有它）

---

### 第二步：分配任务到机台

系统用"试错法"把胎胚分给各台成型机。

1. 先把胎胚按需求量从大到小排队，但紧急收尾的插到最前面
2. 对每个胎胚，系统尝试分给不同机台：
   - 如果昨天这个机台做过这个胎胚（老熟人），而且"强制保留"开关打开，这个机台可以优先接，而且不算新种类
   - 如果是新胎胚，只能找还没达到种类上限（最多4种）的机台
   - 在能接的机台里，优先选当前干活最少的、种类最少的
3. 尝试不同的分法：比如胎胚A需要80条，给机台1最多能接50条，就先试50条，不行再减到40条……直到所有胎胚分完，或者退回重试
4. 系统会记住所有可行的分法，最后选出"干活最平均"且"种类数最平均"的那个方案

---

### 第三步：把一天的任务拆成三个班

每个机台一天的总任务量定了，现在要分到早、中、夜三个班。

#### 基础规则

- 夜班:早班:中班 = 1:2:1（波浪形）
- 但生产要按"整车"来，一车是12条。所以先按比例算理论班产量（比如9-18-9），然后向上取整到12的倍数（12-24-12），再微调让总和等于日计划
- 如果某个班微调后变成0，允许，但尽量让三个班都有活

#### 特殊处理

- **开产首班**：只排6小时，计划量减半
- **停产最后一班**：精确计算，保证做完后库存为0，且正好够硫化吃到停火
- **关键产品**：开产日第一个班不排
- **收尾处理**：
  - 主销产品（月均销量≥500条）：收尾余量不够一整车时，按整车下
  - 非主销产品：收尾余量≤2条时舍弃，>2条时按实际量下
- **精度计划**：有精度的机台，要避开那4小时

---

### 第四步：排生产顺序

每个机台每个班要做哪些胎胚、各做多少整车都定了。现在要排谁先做、谁后做。

**核心原则**：谁最急（库存快没了）谁先做。紧急收尾的再优先。

#### 怎么算急不急？

预计库存可供硫化时长 = (胎胚实时库存 + 计划) / (硫化机数 × 单台模数)

这个时长越短，越急。

系统把每个"胎胚+整车"当成一个任务，先按"是否紧急收尾"分组，再按库存时长从小到大排顺序。

**预警**：如果某个胎胚的库存时长 > 18小时，说明库存太高了，系统会预警。

---

### 第五步：执行过程中动态调整

计划排好了，车间开始生产。但实际生产可能有快有慢，库存会有波动。系统每班结束前1小时会检查一次。

1. 算预计交班库存 = 当前库存 + 本班已做 + 本班剩余计划 – 本班剩余消耗
2. 算交班可供时长 = 预计交班库存 / (硫化机数×单台模数) – 剩余班次时间
3. 如果交班可供时长 < 6小时，说明到交班时库存只够吃6小时了，有断料风险。系统给下个班这个胎胚加1整车，同时从库存最长的胎胚下个班计划里减1整车，平衡总库存
4. 如果交班可供时长 > 18小时，系统预警

调整后还要重新算顺位，并且检查胎面能不能跟上：

- 胎面停放时间4小时，系统会算每个任务开始时胎面有没有到位
- 如果胎面还没到，顺位后移
- 如果胎面刚好卡着点（差10分钟以内），预警但不后移

这个调整会滚动影响未来8个班次，每班结束前都来一遍，保证计划一直"新鲜"。

---

### 第六步：处理材料异常

#### 胎面卷曲米数不够

如果胎面送到时，首卷或末卷长度不够，实际做不了那么多。操作工会扫码，系统拿到实际米数后，如果完成率低于80%（可配置），就把完成率调成100%，并在原因里备注"胎面卷曲不够"。这样就不会因为材料问题冤枉机台。

#### 大卷帘布用完

当大卷帘布（特殊材料）库存为0时，系统会触发计划修正。对于主销产品，可以加量生产（哪怕不在计划里），尽可能利用剩下的材料；对于按单生产的产品，不能加量。

---

### 第七步：发布计划

所有调整确认后，系统把未来8个班的详细计划发布到MES，车间各机台按单生产。系统会持续接收完成量回报，用于下一轮的动态调整。

---

## 四、输出什么

最终排程表包含：

- 哪个成型机台、供哪个硫化机台
- 做什么胎胚（物料编码、描述）
- 月计划多少、已经做了多少、还剩多少
- 当前胎胚库存
- 未来8个班（T日早/中、T+1日夜/早/中、T+2日夜/早/中）每个班的计划量、顺位、完成量、原因分析

**颜色标识**：

- 快收尾的（余量小于阈值）：橙色
- 新开规格：黄色
- 试制量试：蓝色

---

这样，整个成型排程系统就能在复杂的约束下，平稳高效地运转，既不让硫化机断料，也不让胎胚库存爆满，同时让每台成型机的工作量和种类数都尽量均衡。无论是日常、节假日、研发新胎胚、设备校准、材料异常、人员请假，还是月度收尾管理，都能从容应对。

---


**文档结束**
