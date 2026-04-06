//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zlt.bill.common.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import java.util.List;

public interface IDocService<T extends BaseEntity> {
    int save(T var1);

    int save(List<T> var1);

    int removeByIds(List<Long> var1);

    String checkUnique(T var1);

    AjaxResult importData(List<T> var1, boolean var2, Long var3);
}
