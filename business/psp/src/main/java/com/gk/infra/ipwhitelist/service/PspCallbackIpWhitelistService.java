package com.gk.infra.ipwhitelist.service;

import com.gk.common.core.service.CrudService;
import com.gk.infra.ipwhitelist.dto.PspCallbackIpWhitelistDTO;
import com.gk.infra.ipwhitelist.entity.PspCallbackIpWhitelistEntity;

public interface PspCallbackIpWhitelistService extends CrudService<PspCallbackIpWhitelistEntity, PspCallbackIpWhitelistDTO> {
    boolean isPspCallbackAllowed(String pspCode, String clientIp);
}
