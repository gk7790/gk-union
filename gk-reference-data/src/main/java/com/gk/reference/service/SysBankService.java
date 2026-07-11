package com.gk.reference.service;

import com.gk.common.core.service.CrudService;
import com.gk.common.model.DynMap;
import com.gk.reference.dto.SysBankDTO;
import com.gk.reference.entity.SysBankEntity;

import java.util.List;

public interface SysBankService extends CrudService<SysBankEntity, SysBankDTO> {

    List<SysBankDTO> getDict(DynMap params);
}
