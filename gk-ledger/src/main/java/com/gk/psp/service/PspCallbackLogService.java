package com.gk.psp.service;

import com.gk.common.core.service.CrudService;
import com.gk.psp.dto.PspCallbackLogDTO;
import com.gk.psp.entity.PspCallbackLogEntity;

public interface PspCallbackLogService extends CrudService<PspCallbackLogEntity, PspCallbackLogDTO> {

    /**
     * 异步落库入口：插入回调日志；重复回调幂等键冲突时忽略，不覆盖已有记录�?     */
    void record(PspCallbackLogEntity entity);
}
