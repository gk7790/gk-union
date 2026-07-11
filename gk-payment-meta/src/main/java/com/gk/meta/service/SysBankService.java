package com.gk.meta.service;

import com.gk.common.core.service.CrudService;
import com.gk.common.model.DynMap;
import com.gk.meta.dto.SysBankDTO;
import com.gk.meta.entity.SysBankEntity;

import java.util.List;

public interface SysBankService extends CrudService<SysBankEntity, SysBankDTO> {

    List<SysBankDTO> getDict(DynMap params);
}
