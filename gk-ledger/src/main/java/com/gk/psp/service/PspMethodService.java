package com.gk.psp.service;

import com.gk.common.core.service.CrudService;
import com.gk.common.dto.LabelDTO;
import com.gk.common.model.DynMap;
import com.gk.psp.dto.PspMethodDTO;
import com.gk.psp.entity.PspMethodEntity;

import java.util.List;

public interface PspMethodService extends CrudService<PspMethodEntity, PspMethodDTO> {
    List<LabelDTO> getMethodCodeDict(DynMap params);
}
