package com.gk.tenant.service;


import com.gk.common.core.service.CrudService;
import com.gk.common.dto.LabelDTO;
import com.gk.common.model.DynMap;
import com.gk.tenant.dto.SysTenantDTO;
import com.gk.tenant.dto.TenantOnboardRequest;
import com.gk.tenant.dto.TenantOnboardResult;
import com.gk.tenant.entity.SysTenantEntity;

import java.util.List;

/**
 * 参数管理
 *
 * @author Lowen
 * @since 1.0.0
 */
public interface SysTenantService extends CrudService<SysTenantEntity, SysTenantDTO> {

    List<LabelDTO> getDict(DynMap params);

    TenantOnboardResult onboard(TenantOnboardRequest request);
}