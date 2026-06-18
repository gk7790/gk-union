package com.gk.tenant.service;

import com.gk.common.core.service.CrudService;
import com.gk.common.dto.LabelDTO;
import com.gk.common.model.DynMap;
import com.gk.tenant.dto.TenantDTO;
import com.gk.tenant.dto.TenantOnboardRequest;
import com.gk.tenant.dto.TenantOnboardResult;
import com.gk.tenant.entity.TenantEntity;

import java.util.List;

public interface TenantService extends CrudService<TenantEntity, TenantDTO> {

    List<LabelDTO> getDict(DynMap params);

    TenantOnboardResult onboard(TenantOnboardRequest request);
}
