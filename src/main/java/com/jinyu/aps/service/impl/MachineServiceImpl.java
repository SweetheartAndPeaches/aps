package com.jinyu.aps.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinyu.aps.entity.Machine;
import com.jinyu.aps.mapper.MachineMapper;
import com.jinyu.aps.service.MachineService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 成型机台Service实现类
 *
 * @author APS Team
 */
@Service
public class MachineServiceImpl extends ServiceImpl<MachineMapper, Machine> implements MachineService {

    @Override
    public List<Machine> listAvailableMachines() {
        LambdaQueryWrapper<Machine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Machine::getIsActive, 1)
                .eq(Machine::getStatus, "RUNNING")
                .orderByAsc(Machine::getLineNumber)
                .orderByAsc(Machine::getMachineCode);
        return list(wrapper);
    }

    @Override
    public List<Machine> listByLineNumber(Integer lineNumber) {
        LambdaQueryWrapper<Machine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Machine::getIsActive, 1)
                .eq(Machine::getLineNumber, lineNumber)
                .orderByAsc(Machine::getMachineCode);
        return list(wrapper);
    }
}
