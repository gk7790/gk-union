package com.gk.psp.service;

import com.gk.common.core.service.CrudService;
import com.gk.psp.dto.PspRequestLogDTO;
import com.gk.psp.entity.PspRequestLogEntity;

public interface PspRequestLogService extends CrudService<PspRequestLogEntity, PspRequestLogDTO> {
    void record(PspRequestLogEntity entity);
}
