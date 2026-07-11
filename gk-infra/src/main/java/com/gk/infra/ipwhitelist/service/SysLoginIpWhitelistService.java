package com.gk.infra.ipwhitelist.service;

import com.gk.common.core.service.CrudService;
import com.gk.infra.ipwhitelist.dto.SysLoginIpWhitelistDTO;
import com.gk.infra.ipwhitelist.entity.SysLoginIpWhitelistEntity;

public interface SysLoginIpWhitelistService extends CrudService<SysLoginIpWhitelistEntity, SysLoginIpWhitelistDTO> {
    boolean isLoginAllowed(String subjectType, Long tenantId, Long merchantId, Long subjectId, String clientIp);
}
