package com.gk.payment.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.payment.dao.PayoutOrderDao;
import com.gk.payment.dto.PayoutOrderDTO;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.payment.enums.PayoutOrderStatusEnum;
import com.gk.payment.service.PayoutOrderService;
import com.gk.payment.state.PayoutOrderStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayoutOrderServiceImpl extends CrudServiceImpl<PayoutOrderDao, PayoutOrderEntity, PayoutOrderDTO> implements PayoutOrderService {
    private static final int MANUAL_REVIEW_DRAIN_BATCH = 50;
    private static final int MAX_ACTIVE_QUERY_COUNT = 30;
    private static final long PROCESSING_SLA_SECONDS = 2 * 60 * 60;
    private static final String MANUAL_REVIEW_REASON = "Payout order exceeded active query limit or SLA";

    private final PayoutOrderStateService payoutOrderStateService;

    @Override
    public PageData<PayoutOrderDTO> page(DynMap params) {
        params.put("showTenantName", ReqContextHolder.isPlatform());
        params.put("showMerchantName", !SubjectTypeEnum.MERCHANT.matches(ReqContextHolder.getSubjectType()));
        long pageNo = Math.max(params.getLong(Constant.PAGE, 1L), 1L);
        long limit = Math.max(params.getLong(Constant.LIMIT, 10L), 1L);
        params.put("offset", (pageNo - 1) * limit);
        params.put("limitValue", limit);

        Long total = baseDao.countPageWithName(params);
        List<PayoutOrderDTO> list = total == null || total == 0L
                ? List.of()
                : baseDao.selectPageWithName(params);
        return new PageData<>(list, total == null ? 0L : total);
    }

    @Override
    public QueryWrapper<PayoutOrderEntity> getWrapper(DynMap params) {
        QueryWrapper<PayoutOrderEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Long merchantAppId = params.getLong("merchantAppId", null);
        Long pspId = params.getLong("pspId", null);
        Long merchantFeeRuleId = params.getLong("merchantFeeRuleId", null);
        Long pspFeeRuleId = params.getLong("pspFeeRuleId", null);
        String payoutOrderNo = params.getStr("payoutOrderNo");
        String merchantOrderNo = params.getStr("merchantOrderNo");
        String idempotencyKey = params.getStr("idempotencyKey");
        String requestId = params.getStr("requestId");
        String status = params.getStr("status");
        String merchantNotifyStatus = params.getStr("merchantNotifyStatus");
        String countryCode = params.getStr("countryCode");
        String currency = params.getStr("currency");
        String methodCode = params.getStr("methodCode");
        String pspCode = params.getStr("pspCode");
        String pspOrderNo = params.getStr("pspOrderNo");
        String pspRequestNo = params.getStr("pspRequestNo");
        String holdNo = params.getStr("holdNo");
        String payeeAccountNo = params.getStr("payeeAccountNo");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(merchantAppId != null, "merchant_app_id", merchantAppId);
        wrapper.eq(pspId != null, "psp_id", pspId);
        wrapper.eq(merchantFeeRuleId != null, "merchant_fee_rule_id", merchantFeeRuleId);
        wrapper.eq(pspFeeRuleId != null, "psp_fee_rule_id", pspFeeRuleId);
        wrapper.eq(StrUtil.isNotBlank(payoutOrderNo), "payout_order_no", payoutOrderNo);
        wrapper.eq(StrUtil.isNotBlank(merchantOrderNo), "merchant_order_no", merchantOrderNo);
        wrapper.eq(StrUtil.isNotBlank(idempotencyKey), "idempotency_key", idempotencyKey);
        wrapper.eq(StrUtil.isNotBlank(requestId), "request_id", requestId);
        wrapper.eq(StrUtil.isNotBlank(status), "status", status);
        wrapper.eq(StrUtil.isNotBlank(merchantNotifyStatus), "merchant_notify_status", merchantNotifyStatus);
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", countryCode);
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", currency);
        wrapper.eq(StrUtil.isNotBlank(methodCode), "method_code", methodCode);
        wrapper.eq(StrUtil.isNotBlank(pspCode), "psp_code", pspCode);
        wrapper.eq(StrUtil.isNotBlank(pspOrderNo), "psp_order_no", pspOrderNo);
        wrapper.eq(StrUtil.isNotBlank(pspRequestNo), "psp_request_no", pspRequestNo);
        wrapper.eq(StrUtil.isNotBlank(holdNo), "hold_no", holdNo);
        wrapper.eq(StrUtil.isNotBlank(payeeAccountNo), "payee_account_no", payeeAccountNo);
        return wrapper;
    }

    @Override
    public int drainLongProcessingOrders() {
        Instant now = Instant.now();
        Instant slaTime = now.minusSeconds(PROCESSING_SLA_SECONDS);
        List<PayoutOrderEntity> orders = baseDao.selectList(new QueryWrapper<PayoutOrderEntity>()
                .eq("status", PayoutOrderStatusEnum.PROCESSING.code())
                .and(wrapper -> wrapper.ge("query_count", MAX_ACTIVE_QUERY_COUNT)
                        .or()
                        .isNotNull("submitted_at").le("submitted_at", slaTime))
                .orderByAsc("submitted_at", "id")
                .last("limit " + MANUAL_REVIEW_DRAIN_BATCH));
        int marked = 0;
        for (PayoutOrderEntity order : orders) {
            if (markManualReview(order)) {
                marked++;
            }
        }
        return marked;
    }

    private boolean markManualReview(PayoutOrderEntity order) {
        return payoutOrderStateService.markManualReview(order, MANUAL_REVIEW_REASON);
    }
}
