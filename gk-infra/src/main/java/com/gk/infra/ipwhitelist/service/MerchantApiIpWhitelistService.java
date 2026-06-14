package com.gk.infra.ipwhitelist.service;

import com.gk.common.core.service.CrudService;
import com.gk.infra.ipwhitelist.dto.MerchantApiIpWhitelistDTO;
import com.gk.infra.ipwhitelist.entity.MerchantApiIpWhitelistEntity;

public interface MerchantApiIpWhitelistService extends CrudService<MerchantApiIpWhitelistEntity, MerchantApiIpWhitelistDTO> {
    boolean isMerchantApiAllowed(Long tenantId, Long merchantId, String clientIp);
}
