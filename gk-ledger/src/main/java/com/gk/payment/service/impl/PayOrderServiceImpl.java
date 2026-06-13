package com.gk.payment.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.ledger.posting.PaySuccessPostingRequest;
import com.gk.ledger.service.LedgerPostingService;
import com.gk.merchant.dao.MerchantDao;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.payment.dao.PayOrderDao;
import com.gk.payment.dto.PayOrderDTO;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.enums.PayOrderStatusEnum;
import com.gk.payment.enums.SettleStatusEnum;
import com.gk.payment.service.OrderStatusLogService;
import com.gk.payment.service.PayOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayOrderServiceImpl extends CrudServiceImpl<PayOrderDao, PayOrderEntity, PayOrderDTO> implements PayOrderService {

    private static final int SETTLE_DRAIN_BATCH = 50;

    private final MerchantDao merchantDao;
    private final LedgerPostingService ledgerPostingService;
    private final OrderStatusLogService orderStatusLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onPaySuccessPosted(Long orderId) {
        if (orderId == null) {
            return;
        }
        PayOrderEntity order = baseDao.selectById(orderId);
        if (order == null || !PayOrderStatusEnum.SUCCESS.code().equals(order.getStatus())) {
            return;
        }
        if (!SettleStatusEnum.PENDING.code().equals(order.getSettleStatus())) {
            return;
        }
        MerchantEntity merchant = merchantDao.selectById(order.getMerchantId());
        if (merchant == null) {
            return;
        }
        Instant paidAt = order.getPaidAt() != null ? order.getPaidAt() : Instant.now();
        Instant releaseAt = SettleStatusEnum.computeReleaseAt(merchant.getSettleCycle(), paidAt, merchant.getTimezone());
        if (order.getSettleReleaseAt() == null) {
            PayOrderEntity patch = new PayOrderEntity();
            patch.setId(orderId);
            patch.setSettleReleaseAt(releaseAt);
            baseDao.updateById(patch);
            order.setSettleReleaseAt(releaseAt);
        }
        if (shouldAutoRelease(merchant, order.getSettleReleaseAt())) {
            releaseSettle(orderId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseSettle(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("order id is required");
        }
        PayOrderEntity order = baseDao.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Pay order not found: " + orderId);
        }
        if (!PayOrderStatusEnum.SUCCESS.code().equals(order.getStatus())) {
            throw new IllegalStateException("Pay order is not success: " + order.getPayOrderNo());
        }
        if (SettleStatusEnum.RELEASED.code().equals(order.getSettleStatus())) {
            return;
        }
        if (!SettleStatusEnum.PENDING.code().equals(order.getSettleStatus())) {
            throw new IllegalStateException("Pay order settle status is not pending: " + order.getPayOrderNo());
        }

        PaySuccessPostingRequest request = settlePostingRequest(order);
        LedgerPostingResult postingResult = ledgerPostingService.releasePaySettle(request);

        Instant now = Instant.now();
        UpdateWrapper<PayOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", orderId)
                .eq("settle_status", SettleStatusEnum.PENDING.code())
                .set("settle_status", SettleStatusEnum.RELEASED.code())
                .set("settle_at", now)
                .set("settle_journal_no", postingResult.getJournalNo());
        if (baseDao.update(null, wrapper) == 0) {
            return;
        }
        orderStatusLogService.recordChange(
                "PAY",
                order.getTenantId(),
                order.getMerchantId(),
                order.getId(),
                order.getPayOrderNo(),
                SettleStatusEnum.PENDING.code(),
                SettleStatusEnum.RELEASED.code(),
                "SETTLE_RELEASE",
                null,
                "SYSTEM",
                null,
                order.getMerchantOrderNo(),
                null
        );
    }

    @Override
    public int drainDueSettlements() {
        Instant now = Instant.now();
        List<PayOrderEntity> orders = baseDao.selectList(new QueryWrapper<PayOrderEntity>()
                .eq("status", PayOrderStatusEnum.SUCCESS.code())
                .eq("settle_status", SettleStatusEnum.PENDING.code())
                .isNotNull("settle_release_at")
                .le("settle_release_at", now)
                .orderByAsc("settle_release_at", "id")
                .last("limit " + SETTLE_DRAIN_BATCH));
        int released = 0;
        for (PayOrderEntity order : orders) {
            try {
                MerchantEntity merchant = merchantDao.selectById(order.getMerchantId());
                if (merchant == null || !shouldAutoRelease(merchant, order.getSettleReleaseAt())) {
                    continue;
                }
                releaseSettle(order.getId());
                released++;
            } catch (Exception ex) {
                log.warn("Pay settle release failed, orderNo={}, err={}", order.getPayOrderNo(), ex.getMessage());
            }
        }
        return released;
    }

    private boolean shouldAutoRelease(MerchantEntity merchant, Instant releaseAt) {
        if (!SettleStatusEnum.isAutoReleaseMode(merchant.getSettleMode())) {
            return false;
        }
        return releaseAt != null && !releaseAt.isAfter(Instant.now());
    }

    private PaySuccessPostingRequest settlePostingRequest(PayOrderEntity order) {
        PaySuccessPostingRequest request = new PaySuccessPostingRequest();
        request.setTenantId(order.getTenantId());
        request.setMerchantId(order.getMerchantId());
        request.setBizId(order.getId());
        request.setPayOrderNo(order.getPayOrderNo());
        request.setCurrency(order.getCurrency());
        request.setAmount(defaultAmount(order.getPaidAmount(), order.getAmount()));
        request.setMerchantFeeAmount(order.getMerchantFeeAmount());
        request.setSettleAmount(order.getSettleAmount());
        return request;
    }

    private BigDecimal defaultAmount(BigDecimal primary, BigDecimal fallback) {
        if (primary != null && primary.compareTo(BigDecimal.ZERO) > 0) {
            return primary;
        }
        return fallback;
    }

    @Override
    public QueryWrapper<PayOrderEntity> getWrapper(DynMap params) {
        QueryWrapper<PayOrderEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Long merchantAppId = params.getLong("merchantAppId", null);
        Long pspId = params.getLong("pspId", null);
        Long merchantFeeRuleId = params.getLong("merchantFeeRuleId", null);
        Long pspFeeRuleId = params.getLong("pspFeeRuleId", null);
        String payOrderNo = params.getStr("payOrderNo");
        String merchantOrderNo = params.getStr("merchantOrderNo");
        String idempotencyKey = params.getStr("idempotencyKey");
        String requestId = params.getStr("requestId");
        String status = params.getStr("status");
        String settleStatus = params.getStr("settleStatus");
        String merchantNotifyStatus = params.getStr("merchantNotifyStatus");
        String countryCode = params.getStr("countryCode");
        String currency = params.getStr("currency");
        String methodCode = params.getStr("methodCode");
        String pspCode = params.getStr("pspCode");
        String pspOrderNo = params.getStr("pspOrderNo");
        String pspRequestNo = params.getStr("pspRequestNo");
        String ledgerJournalNo = params.getStr("ledgerJournalNo");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(merchantAppId != null, "merchant_app_id", merchantAppId);
        wrapper.eq(pspId != null, "psp_id", pspId);
        wrapper.eq(merchantFeeRuleId != null, "merchant_fee_rule_id", merchantFeeRuleId);
        wrapper.eq(pspFeeRuleId != null, "psp_fee_rule_id", pspFeeRuleId);
        wrapper.eq(StrUtil.isNotBlank(payOrderNo), "pay_order_no", payOrderNo);
        wrapper.eq(StrUtil.isNotBlank(merchantOrderNo), "merchant_order_no", merchantOrderNo);
        wrapper.eq(StrUtil.isNotBlank(idempotencyKey), "idempotency_key", idempotencyKey);
        wrapper.eq(StrUtil.isNotBlank(requestId), "request_id", requestId);
        wrapper.eq(StrUtil.isNotBlank(status), "status", status);
        wrapper.eq(StrUtil.isNotBlank(settleStatus), "settle_status", settleStatus);
        wrapper.eq(StrUtil.isNotBlank(merchantNotifyStatus), "merchant_notify_status", merchantNotifyStatus);
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", countryCode);
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", currency);
        wrapper.eq(StrUtil.isNotBlank(methodCode), "method_code", methodCode);
        wrapper.eq(StrUtil.isNotBlank(pspCode), "psp_code", pspCode);
        wrapper.eq(StrUtil.isNotBlank(pspOrderNo), "psp_order_no", pspOrderNo);
        wrapper.eq(StrUtil.isNotBlank(pspRequestNo), "psp_request_no", pspRequestNo);
        wrapper.eq(StrUtil.isNotBlank(ledgerJournalNo), "ledger_journal_no", ledgerJournalNo);
        return wrapper;
    }
}
