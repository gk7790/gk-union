package com.gk.meta.service;

import com.gk.common.core.service.CrudService;
import com.gk.common.dto.LabelDTO;
import com.gk.common.model.DynMap;
import com.gk.meta.dto.SysCurrencyDTO;
import com.gk.meta.entity.SysCurrencyEntity;

import java.util.List;

public interface SysCurrencyService extends CrudService<SysCurrencyEntity, SysCurrencyDTO> {

    List<SysCurrencyDTO> getOptions(DynMap params);

    List<LabelDTO> getDict(DynMap params);
}
