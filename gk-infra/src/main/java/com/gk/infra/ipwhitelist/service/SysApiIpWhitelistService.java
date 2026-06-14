package com.gk.infra.ipwhitelist.service;

import com.gk.common.core.service.CrudService;
import com.gk.infra.ipwhitelist.dto.SysApiIpWhitelistDTO;
import com.gk.infra.ipwhitelist.entity.SysApiIpWhitelistEntity;

public interface SysApiIpWhitelistService extends CrudService<SysApiIpWhitelistEntity, SysApiIpWhitelistDTO> {
    boolean isMerchantApiAllowed(Long tenantId, Long merchantId, String clientIp);
}
