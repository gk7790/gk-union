package com.gk.tenant.service;

import com.gk.common.core.service.CrudService;
import com.gk.common.model.DynMap;
import com.gk.reference.dto.SysCurrencyDTO;
import com.gk.tenant.dto.TenantCurrencyDTO;
import com.gk.tenant.entity.TenantCurrencyEntity;
import com.gk.tenant.port.TenantCurrencyPort;

import java.util.Arrays;
import java.util.List;

public interface TenantCurrencyService extends CrudService<TenantCurrencyEntity, TenantCurrencyDTO>, TenantCurrencyPort {

    List<SysCurrencyDTO> getDict(DynMap params);

    List<TenantCurrencyEntity> getProviderDict(Long tenantId);
}
