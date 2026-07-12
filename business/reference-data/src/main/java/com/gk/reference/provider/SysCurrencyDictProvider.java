package com.gk.reference.provider;

import com.gk.common.dto.LabelDTO;
import com.gk.common.model.DynMap;
import com.gk.common.provider.DynamicDictProvider;
import com.gk.reference.service.SysCurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SysCurrencyDictProvider implements DynamicDictProvider {
    private final SysCurrencyService sysCurrencyService;

    @Override
    public String type() {
        return "sys_currency";
    }

    @Override
    public List<LabelDTO> list(DynMap data) {
        return sysCurrencyService.getDict(data).stream().toList();
    }
}
