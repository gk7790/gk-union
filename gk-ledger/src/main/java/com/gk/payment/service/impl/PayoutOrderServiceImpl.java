package com.gk.payment.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.payment.dao.PayoutOrderDao;
import com.gk.payment.dto.PayoutOrderDTO;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.payment.service.PayoutOrderService;
import org.springframework.stereotype.Service;

@Service
public class PayoutOrderServiceImpl extends CrudServiceImpl<PayoutOrderDao, PayoutOrderEntity, PayoutOrderDTO> implements PayoutOrderService {

    @Override
    public QueryWrapper<PayoutOrderEntity> getWrapper(DynMap params) {
        QueryWrapper<PayoutOrderEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Long merchantAppId = params.getLong("merchantAppId", null);
        Long pspId = params.getLong("pspId", null);
        Long pspFeeRuleId = params.getLong("pspFeeRuleId", null);
        String payoutOrderNo = params.getStr("payoutOrderNo");
        String merchantOrderNo = params.getStr("merchantOrderNo");
        String idempotencyKey = params.getStr("idempotencyKey");
        String requestId = params.getStr("requestId");
        String status = params.getStr("status");
        String countryCode = params.getStr("countryCode");
        String currency = params.getStr("currency");
        String methodCode = params.getStr("methodCode");
        String pspCode = params.getStr("pspCode");
        String pspOrderNo = params.getStr("pspOrderNo");
        String pspRequestNo = params.getStr("pspRequestNo");
        String holdNo = params.getStr("holdNo");
        String payeeAccountHash = params.getStr("payeeAccountHash");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(merchantAppId != null, "merchant_app_id", merchantAppId);
        wrapper.eq(pspId != null, "psp_id", pspId);
        wrapper.eq(pspFeeRuleId != null, "psp_fee_rule_id", pspFeeRuleId);
        wrapper.eq(StrUtil.isNotBlank(payoutOrderNo), "payout_order_no", payoutOrderNo);
        wrapper.eq(StrUtil.isNotBlank(merchantOrderNo), "merchant_order_no", merchantOrderNo);
        wrapper.eq(StrUtil.isNotBlank(idempotencyKey), "idempotency_key", idempotencyKey);
        wrapper.eq(StrUtil.isNotBlank(requestId), "request_id", requestId);
        wrapper.eq(StrUtil.isNotBlank(status), "status", status);
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", countryCode);
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", currency);
        wrapper.eq(StrUtil.isNotBlank(methodCode), "method_code", methodCode);
        wrapper.eq(StrUtil.isNotBlank(pspCode), "psp_code", pspCode);
        wrapper.eq(StrUtil.isNotBlank(pspOrderNo), "psp_order_no", pspOrderNo);
        wrapper.eq(StrUtil.isNotBlank(pspRequestNo), "psp_request_no", pspRequestNo);
        wrapper.eq(StrUtil.isNotBlank(holdNo), "hold_no", holdNo);
        wrapper.eq(StrUtil.isNotBlank(payeeAccountHash), "payee_account_hash", payeeAccountHash);
        return wrapper;
    }
}
