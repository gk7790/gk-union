package com.gk.psp.service;

import com.gk.common.core.service.CrudService;
import com.gk.common.dto.LabelDTO;
import com.gk.common.model.DynMap;
import com.gk.psp.balance.PspBalanceSnap;
import com.gk.psp.dto.PspAccountDTO;
import com.gk.psp.entity.PspAccountEntity;

import java.util.List;

public interface PspAccountService extends CrudService<PspAccountEntity, PspAccountDTO> {
    List<LabelDTO> getDict(DynMap params);

    PspBalanceSnap getBalance(Long id);

    PspBalanceSnap refreshBalance(Long id);
}
