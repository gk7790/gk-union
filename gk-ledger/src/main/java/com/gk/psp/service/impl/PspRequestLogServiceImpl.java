package com.gk.psp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.psp.dao.PspRequestLogDao;
import com.gk.psp.dto.PspRequestLogDTO;
import com.gk.psp.entity.PspRequestLogEntity;
import com.gk.psp.service.PspRequestLogService;
import org.springframework.stereotype.Service;

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
        String pspCode = params.getStr("pspCode");
        String bizType = params.getStr("bizType");
        String bizNo = params.getStr("bizNo");
        String requestNo = params.getStr("requestNo");
        String pspOrderNo = params.getStr("pspOrderNo");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(pspId != null, "psp_id", pspId);
        wrapper.eq(bizId != null, "biz_id", bizId);
        wrapper.eq(success != null, "success", success);
        wrapper.eq(StrUtil.isNotBlank(pspCode), "psp_code", pspCode);
        wrapper.eq(StrUtil.isNotBlank(bizType), "biz_type", bizType);
        wrapper.eq(StrUtil.isNotBlank(bizNo), "biz_no", bizNo);
        wrapper.eq(StrUtil.isNotBlank(requestNo), "request_no", requestNo);
        wrapper.eq(StrUtil.isNotBlank(pspOrderNo), "psp_order_no", pspOrderNo);
        return wrapper;
    }
}
