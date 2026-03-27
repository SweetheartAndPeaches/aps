package com.zlt.aps.cx.service.impl;

import lombok.Data;

import java.io.Serializable;

/**
 * 硫化机台日产能信息
 * 用于计算成型机台的满算力
 *
 * @author APS Team
 */
@Data
public class LhMachineCapacityInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 硫化机台编码
     */
    private String lhMachineCode;

    /**
     * 物料编码（胎胚编码）
     */
    private String materialCode;

    /**
     * 日硫化产能（条/天）
     */
    private Integer dailyCapacity;

    /**
     * 是否配比塞满（0-未塞满，1-塞满）
     * 根据最大配比数量判断
     */
    private Integer isFullRatio;

    /**
     * 备注
     */
    private String remark;
}
