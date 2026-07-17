package com.gk.psp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.common.transaction.SavepointExecutor;
import com.gk.psp.dao.PspCallbackLogDao;
import com.gk.psp.dto.PspCallbackLogDTO;
import com.gk.psp.entity.PspCallbackLogEntity;
import com.gk.psp.service.PspCallbackLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PspCallbackLogServiceImpl extends CrudServiceImpl<PspCallbackLogDao, PspCallbackLogEntity, PspCallbackLogDTO> implements PspCallbackLogService {

    @Override
    public QueryWrapper<PspCallbackLogEntity> getWrapper(DynMap params) {
        QueryWrapper<PspCallbackLogEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Long pspId = params.getLong("pspId", null);
        Long bizId = params.getLong("bizId", null);
        String pspCode = params.getStr("pspCode");
        String bizType = params.getStr("bizType");
        String bizNo = params.getStr("bizNo");
        String pspOrderNo = params.getStr("pspOrderNo");
        String callbackType = params.getStr("callbackType");
        String callbackId = params.getStr("callbackId");
        String callbackKey = params.getStr("callbackKey");
        String verifyStatus = params.getStr("verifyStatus");
        String processStatus = params.getStr("processStatus");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(pspId != null, "psp_id", pspId);
        wrapper.eq(bizId != null, "biz_id", bizId);
        wrapper.eq(StrUtil.isNotBlank(pspCode), "psp_code", pspCode);
        wrapper.eq(StrUtil.isNotBlank(bizType), "biz_type", bizType);
        wrapper.eq(StrUtil.isNotBlank(bizNo), "biz_no", bizNo);
        wrapper.eq(StrUtil.isNotBlank(pspOrderNo), "psp_order_no", pspOrderNo);
        wrapper.eq(StrUtil.isNotBlank(callbackType), "callback_type", callbackType);
        wrapper.eq(StrUtil.isNotBlank(callbackId), "callback_id", callbackId);
        wrapper.eq(StrUtil.isNotBlank(callbackKey), "callback_key", callbackKey);
        wrapper.eq(StrUtil.isNotBlank(verifyStatus), "verify_status", verifyStatus);
        wrapper.eq(StrUtil.isNotBlank(processStatus), "process_status", processStatus);
        return wrapper;
    }

    @Override
    public void record(PspCallbackLogEntity entity) {
        if (entity == null) {
            return;
        }
        try {
            SavepointExecutor.run(() -> baseDao.insert(entity));
        } catch (DuplicateKeyException ignored) {
            // 重复回调保留首条日志，与同步 insert 行为一致
            } catch (Exception ex) {
            log.warn("Save PSP callback log failed: {}", ex.getMessage());
        }
    }
}
