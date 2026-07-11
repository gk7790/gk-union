package com.gk.payment.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.payment.domain.enums.PayDirectionEnum;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.utils.ConvertUtils;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.ledger.posting.PaySuccessPostingRequest;
import com.gk.ledger.service.LedgerPostingService;
import com.gk.merchant.dao.MerchantDao;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.payment.dao.PayinOrderDao;
import com.gk.payment.dto.MerchantPayinOrderDTO;
import com.gk.payment.dto.PayinOrderDTO;
import com.gk.payment.entity.PayinOrderEntity;
import com.gk.payment.enums.PayinOrderStatusEnum;
import com.gk.payment.enums.SettleStatusEnum;
import com.gk.payment.service.OrderStatusLogService;
import com.gk.payment.service.PayinOrderService;
import com.gk.payment.state.PayinOrderStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayinOrderServiceImpl extends CrudServiceImpl<PayinOrderDao, PayinOrderEntity, PayinOrderDTO> implements PayinOrderService {

    private static final int SETTLE_DRAIN_BATCH = 50;
    private static final int EXPIRE_DRAIN_BATCH = 50;
    private static final int MANUAL_REVIEW_DRAIN_BATCH = 50;
    private static final int MAX_ACTIVE_QUERY_COUNT = 30;
    private static final long PROCESSING_SLA_SECONDS = 2 * 60 * 60;
    private static final String PAYIN_ORDER_EXPIRED_REASON = "Pay order expired";
    private static final String MANUAL_REVIEW_REASON = "Pay order exceeded active query limit or SLA";

    private final MerchantDao merchantDao;
    private final LedgerPostingService ledgerPostingService;
    private final OrderStatusLogService orderStatusLogService;
    private final PayinOrderStateService payinOrderStateService;

    @Override
    public PageData<PayinOrderDTO> page(DynMap params) {
        params.put("showTenantName", ReqContextHolder.isPlatform());
        params.put("showMerchantName", !SubjectTypeEnum.MERCHANT.matches(ReqContextHolder.getSubjectType()));
        long pageNo = Math.max(params.getLong(Constant.PAGE, 1L), 1L);
        long limit = Math.max(params.getLong(Constant.LIMIT, 10L), 1L);
        params.put("offset", (pageNo - 1) * limit);
        params.put("limitValue", limit);

        Long total = baseDao.countPageWithName(params);
        List<PayinOrderDTO> list = total == null || total == 0L
                ? List.of()
                : baseDao.selectPageWithName(params);
        return new PageData<>(list, total == null ? 0L : total);
    }

    @Override
    public PageData<MerchantPayinOrderDTO> merchantPage(DynMap params) {
        applyMerchantScope(params);
        PageData<PayinOrderDTO> page = page(params);
        return new PageData<>(ConvertUtils.sourceToTarget(page.getItems(), MerchantPayinOrderDTO.class), page.getTotal());
    }

    @Override
    public MerchantPayinOrderDTO merchantGet(Long id) {
        if (id == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "id is required");
        }
        DynMap params = new DynMap();
        params.put(Constant.PAGE, 1L);
        params.put(Constant.LIMIT, 1L);
        params.put("id", id);
        PageData<MerchantPayinOrderDTO> page = merchantPage(params);
        if (page.getItems() == null || page.getItems().isEmpty()) {
            throw new GkException(ErrorCode.NOT_FOUND, "Pay order not found");
        }
        return page.getItems().get(0);
    }

    private void applyMerchantScope(DynMap params) {
        params.put("tenantId", currentTenantId());
        params.put("merchantId", currentMerchantId());
    }

    private Long currentTenantId() {
        Long tenantId = ReqContextHolder.getTenantId();
        if (tenantId == null) {
            throw new GkException(ErrorCode.DATA_SCOPE_PARAMS_ERROR);
        }
        return tenantId;
    }

    private Long currentMerchantId() {
        Long merchantId = ReqContextHolder.getMerchantId();
        if (merchantId == null) {
            throw new GkException(ErrorCode.DATA_SCOPE_PARAMS_ERROR);
        }
        return merchantId;
    }

    /**
     * 代收订单成功入账后的结算入口。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onPaySuccessPosted(Long orderId) {
        if (orderId == null) {
            return;
        }
        PayinOrderEntity order = baseDao.selectById(orderId);
        if (order == null || !PayinOrderStatusEnum.SUCCESS.code().equals(order.getStatus())) {
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
            PayinOrderEntity patch = new PayinOrderEntity();
            patch.setId(orderId);
            patch.setSettleReleaseAt(releaseAt);
            baseDao.updateById(patch);
            order.setSettleReleaseAt(releaseAt);
        }
        if (shouldAutoRelease(merchant, order.getSettleReleaseAt())) {
            releaseSettle(orderId);
        }
    }

    /**
     * 单笔释放待结算余额至商户可用余额。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseSettle(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("order id is required");
        }
        PayinOrderEntity order = baseDao.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Pay order not found: " + orderId);
        }
        if (!PayinOrderStatusEnum.SUCCESS.code().equals(order.getStatus())) {
            throw new IllegalStateException("Pay order is not success: " + order.getPayinOrderNo());
        }
        if (SettleStatusEnum.RELEASED.code().equals(order.getSettleStatus())) {
            return;
        }
        if (!SettleStatusEnum.PENDING.code().equals(order.getSettleStatus())) {
            throw new IllegalStateException("Pay order settle status is not pending: " + order.getPayinOrderNo());
        }

        PaySuccessPostingRequest request = settlePostingRequest(order);
        LedgerPostingResult postingResult = ledgerPostingService.releasePaySettle(request);

        Instant now = Instant.now();
        UpdateWrapper<PayinOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", orderId)
                .eq("settle_status", SettleStatusEnum.PENDING.code())
                .set("settle_status", SettleStatusEnum.RELEASED.code())
                .set("settle_at", now)
                .set("settle_journal_no", postingResult.getJournalNo());
        if (baseDao.update(null, wrapper) == 0) {
            return;
        }
        orderStatusLogService.recordChange(
                PayDirectionEnum.PAYIN.code(),
                order.getTenantId(),
                order.getMerchantId(),
                order.getId(),
                order.getPayinOrderNo(),
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

    /**
     * 扫描到期的待结算代收订单并自动释放。
     */
    @Override
    public int drainDueSettlements() {
        Instant now = Instant.now();
        List<PayinOrderEntity> orders = baseDao.selectList(new QueryWrapper<PayinOrderEntity>()
                .eq("status", PayinOrderStatusEnum.SUCCESS.code())
                .eq("settle_status", SettleStatusEnum.PENDING.code())
                .isNotNull("settle_release_at")
                .le("settle_release_at", now)
                .orderByAsc("settle_release_at", "id")
                .last("limit " + SETTLE_DRAIN_BATCH));
        int released = 0;
        for (PayinOrderEntity order : orders) {
            try {
                MerchantEntity merchant = merchantDao.selectById(order.getMerchantId());
                if (merchant == null || !shouldAutoRelease(merchant, order.getSettleReleaseAt())) {
                    continue;
                }
                releaseSettle(order.getId());
                released++;
            } catch (Exception ex) {
                log.warn("Pay settle release failed, orderNo={}, err={}", order.getPayinOrderNo(), ex.getMessage());
            }
        }
        return released;
    }

    /**
     * 扫描超时未支付的代收订单并关闭。
     */
    @Override
    public int drainExpiredPayinOrders() {
        Instant now = Instant.now();
        List<PayinOrderEntity> orders = baseDao.selectList(new QueryWrapper<PayinOrderEntity>()
                .in("status", PayinOrderStatusEnum.CREATED.code(), PayinOrderStatusEnum.PROCESSING.code())
                .isNotNull("expire_at")
                .le("expire_at", now)
                .isNull("paid_at")
                .and(wrapper -> wrapper.isNull("paid_amount").or().eq("paid_amount", BigDecimal.ZERO))
                .orderByAsc("expire_at", "id")
                .last("limit " + EXPIRE_DRAIN_BATCH));
        int closed = 0;
        for (PayinOrderEntity order : orders) {
            try {
                if (closeExpiredPayinOrder(order, now)) {
                    closed++;
                }
            } catch (Exception ex) {
                log.warn("Pay order close expired failed, orderNo={}, err={}", order.getPayinOrderNo(), ex.getMessage());
            }
        }
        return closed;
    }

    /**
     * 扫描长时间处理中的代收订单并转人工处理。
     */
    @Override
    public int drainLongProcessingOrders() {
        Instant now = Instant.now();
        Instant slaTime = now.minusSeconds(PROCESSING_SLA_SECONDS);
        List<PayinOrderEntity> orders = baseDao.selectList(new QueryWrapper<PayinOrderEntity>()
                .eq("status", PayinOrderStatusEnum.PROCESSING.code())
                .and(wrapper -> wrapper.ge("query_count", MAX_ACTIVE_QUERY_COUNT)
                        .or()
                        .isNotNull("submitted_at").le("submitted_at", slaTime))
                .orderByAsc("submitted_at", "id")
                .last("limit " + MANUAL_REVIEW_DRAIN_BATCH));
        int marked = 0;
        for (PayinOrderEntity order : orders) {
            if (markManualReview(order)) {
                marked++;
            }
        }
        return marked;
    }

    private boolean markManualReview(PayinOrderEntity order) {
        return payinOrderStateService.markManualReview(order, MANUAL_REVIEW_REASON);
    }

    /**
     * 关闭单笔超时代收订单。
     */
    private boolean closeExpiredPayinOrder(PayinOrderEntity order, Instant now) {
        return payinOrderStateService.closeExpired(order, now, PAYIN_ORDER_EXPIRED_REASON);
    }

    /**
     * 判断是否可以按商户配置自动释放待结算余额。
     */
    private boolean shouldAutoRelease(MerchantEntity merchant, Instant releaseAt) {
        if (!SettleStatusEnum.isAutoReleaseMode(merchant.getSettleMode())) {
            return false;
        }
        return releaseAt != null && !releaseAt.isAfter(Instant.now());
    }

    /**
     * 构建待结算释放的账务请求。
     */
    private PaySuccessPostingRequest settlePostingRequest(PayinOrderEntity order) {
        PaySuccessPostingRequest request = new PaySuccessPostingRequest();
        request.setTenantId(order.getTenantId());
        request.setMerchantId(order.getMerchantId());
        request.setMerchantNo(order.getMerchantNo());
        request.setMerchantAppId(order.getMerchantAppId());
        request.setMerchantOrderNo(order.getMerchantOrderNo());
        request.setPspAccountId(order.getPspAccountId());
        request.setBizId(order.getId());
        request.setPayinOrderNo(order.getPayinOrderNo());
        request.setCurrency(order.getCurrency());
        request.setAmount(defaultAmount(order.getPaidAmount(), order.getAmount()));
        request.setMerchantFeeAmount(order.getMerchantFeeAmount());
        request.setSettleAmount(order.getSettleAmount());
        return request;
    }

    /**
     * 选择有效金额。
     */
    private BigDecimal defaultAmount(BigDecimal primary, BigDecimal fallback) {
        if (primary != null && primary.compareTo(BigDecimal.ZERO) > 0) {
            return primary;
        }
        return fallback;
    }

    /**
     * 构建后台代收订单分页查询条件。
     */
    @Override
    public QueryWrapper<PayinOrderEntity> getWrapper(DynMap params) {
        QueryWrapper<PayinOrderEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Long merchantAppId = params.getLong("merchantAppId", null);
        Long pspId = params.getLong("pspId", null);
        Long merchantFeeRuleId = params.getLong("merchantFeeRuleId", null);
        Long pspFeeRuleId = params.getLong("pspFeeRuleId", null);
        String payinOrderNo = params.getStr("payinOrderNo");
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
        wrapper.eq(StrUtil.isNotBlank(payinOrderNo), "payin_order_no", payinOrderNo);
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
