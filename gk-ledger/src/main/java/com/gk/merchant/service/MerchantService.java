package com.gk.merchant.service;

import com.gk.common.core.service.CrudService;
import com.gk.common.model.DynMap;
import com.gk.merchant.dto.MerchantDTO;
import com.gk.merchant.entity.MerchantEntity;

import java.util.List;

public interface MerchantService extends CrudService<MerchantEntity, MerchantDTO> {
    List<MerchantDTO> getDict(DynMap params);
}
