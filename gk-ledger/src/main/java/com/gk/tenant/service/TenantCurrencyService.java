package com.gk.tenant.service;

import com.gk.common.core.service.CrudService;
import com.gk.common.model.DynMap;
import com.gk.meta.dto.SysCurrencyDTO;
import com.gk.tenant.dto.TenantCurrencyDTO;
import com.gk.tenant.entity.TenantCurrencyEntity;

import java.util.List;

public interface TenantCurrencyService extends CrudService<TenantCurrencyEntity, TenantCurrencyDTO> {

    List<SysCurrencyDTO> getDict(DynMap params);
}
