package com.gk.psp.service;

import com.gk.common.core.service.CrudService;
import com.gk.psp.dto.PspBankMappingDTO;
import com.gk.psp.entity.PspBankMappingEntity;

import java.util.List;

public interface PspBankMappingService extends CrudService<PspBankMappingEntity, PspBankMappingDTO> {

    List<PspBankMappingDTO> getMatrix(Long pspId, String countryCode, String currency);

    void saveMatrix(Long pspId, String countryCode, String currency, List<PspBankMappingDTO> items);
}
