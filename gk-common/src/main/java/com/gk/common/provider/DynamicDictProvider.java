package com.gk.common.provider;

import com.gk.common.dto.LabelDTO;
import com.gk.common.model.DynMap;

import java.util.List;

/**
 * 动态字典提供者。
 * 每一个动态字典实现一个 Provider。
 * 例如：
 * tenant_currency -> TenantCurrencyDictProvider
 * psp_account     -> PspAccountDictProvider
 */
public interface DynamicDictProvider {

    String type();

    default String name() {
        return type();
    }

    default int order() {
        return 0;
    }

    /**
     * 获取动态字典数据。
     * 这里可以读取数据库，也可以根据当前登录上下文过滤。
     */
    List<LabelDTO> list(DynMap data);
}