package com.gk.tenant.provider;

import com.gk.common.context.ReqContextHolder;
import com.gk.common.dto.LabelDTO;
import com.gk.common.model.DynMap;
import com.gk.common.provider.DynamicDictProvider;
import com.gk.tenant.service.TenantCurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TenantCurrencyDictProvider implements DynamicDictProvider {
    private final TenantCurrencyService tenantCurrencyService;

    @Override
    public String type() {
        return "tenant_currency";
    }

    @Override
    public List<LabelDTO> list(DynMap data) {
        Long tenantId = ReqContextHolder.getTenantId();

        if (tenantId == null || tenantId <= 0) {
            return List.of();
        }

        return tenantCurrencyService.getProviderDict(tenantId)
                .stream()
                .map(item -> new LabelDTO(item.getCurrency(), item.getCurrency()))
                .toList();
    }
}
