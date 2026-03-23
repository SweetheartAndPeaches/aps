package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.cx.entity.Machine;

import java.util.List;

/**
 * 成型机台Service接口
 *
 * @author APS Team
 */
public interface MachineService extends IService<Machine> {

    /**
     * 获取所有可用机台
     */
    List<Machine> listAvailableMachines();

    /**
     * 根据产线获取机台
     */
    List<Machine> listByLineNumber(Integer lineNumber);
}
