package com.gk.tenant.port;

import java.util.List;
import java.util.Map;

public interface TenantCurrencyPort {
    void sync(Long tenantId, List<String> currencies);
    Map<Long, List<String>> findCurrencies(List<Long> tenantIds);
}
