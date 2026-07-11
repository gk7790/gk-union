package com.gk.psp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.psp.dao.PspRequestLogDao;
import com.gk.psp.dto.PspRequestLogDTO;
import com.gk.psp.entity.PspRequestLogEntity;
import com.gk.psp.service.PspRequestLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PspRequestLogServiceImpl extends CrudServiceImpl<PspRequestLogDao, PspRequestLogEntity, PspRequestLogDTO> implements PspRequestLogService {

    @Override
    public QueryWrapper<PspRequestLogEntity> getWrapper(DynMap params) {
        QueryWrapper<PspRequestLogEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Long pspId = params.getLong("pspId", null);
        Long bizId = params.getLong("bizId", null);
        Integer success = params.containsKey("success") ? params.getInt("success") : null;
        Integer responseStatus = params.containsKey("responseStatus") ? params.getInt("responseStatus") : null;
        String pspCode = params.getStr("pspCode");
        String bizType = params.getStr("bizType");
        String bizNo = params.getStr("bizNo");
        String requestNo = params.getStr("requestNo");
        String pspRequestNo = params.getStr("pspRequestNo");
        String pspOrderNo = params.getStr("pspOrderNo");
        String traceId = params.getStr("traceId");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(pspId != null, "psp_id", pspId);
        wrapper.eq(bizId != null, "biz_id", bizId);
        wrapper.eq(success != null, "success", success);
        wrapper.eq(responseStatus != null, "response_status", responseStatus);
        wrapper.eq(StrUtil.isNotBlank(pspCode), "psp_code", pspCode);
        wrapper.eq(StrUtil.isNotBlank(bizType), "biz_type", bizType);
        wrapper.eq(StrUtil.isNotBlank(bizNo), "biz_no", bizNo);
        wrapper.eq(StrUtil.isNotBlank(requestNo), "request_no", requestNo);
        wrapper.eq(StrUtil.isNotBlank(pspRequestNo), "psp_request_no", pspRequestNo);
        wrapper.eq(StrUtil.isNotBlank(pspOrderNo), "psp_order_no", pspOrderNo);
        wrapper.eq(StrUtil.isNotBlank(traceId), "trace_id", traceId);
        return wrapper;
    }

    @Override
    public void record(PspRequestLogEntity entity) {
        try {
            baseDao.insert(entity);
        } catch (Exception ex) {
            log.warn("Save PSP request log failed: {}", ex.getMessage());
        }
    }
}
