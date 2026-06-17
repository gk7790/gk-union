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
    private static final int EXPIRE_DRAIN_BATCH = 50;
    private static final int MANUAL_REVIEW_DRAIN_BATCH = 50;
    private static final int MAX_ACTIVE_QUERY_COUNT = 30;
    private static final long PROCESSING_SLA_SECONDS = 2 * 60 * 60;
    private static final String PAY_ORDER_EXPIRED_REASON = "Pay order expired";
    private static final String MANUAL_REVIEW_REASON = "Pay order exceeded active query limit or SLA";

    private final MerchantDao merchantDao;
    private final LedgerPostingService ledgerPostingService;
    private final OrderStatusLogService orderStatusLogService;

    /**
     * 代收订单成功入账后的结算入口。
     * <p>
     * PSP 回调或主动查单确认代收成功后，账务先把商户净额记入待结算账户。
     * 本方法负责根据商户结算周期计算计划释放时间；若商户是 AUTO 模式且已到释放时间，
     * 则继续调用 {@link #releaseSettle(Long)} 把待结算余额释放到商户可用余额。
     *
     * @param orderId 代收订单ID
     */
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

    /**
     * 单笔释放待结算余额至商户可用余额。
     * <p>
     * 适用于运营手动释放，或自动结算任务内部调用。方法会校验订单必须是代收成功且
     * settle_status=PENDING，然后调用账务服务生成 SETTLE_RELEASE 凭证，最后回写订单
     * 的释放状态、释放时间和账务凭证号。
     *
     * @param orderId 代收订单ID
     */
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

    /**
     * 扫描到期的待结算代收订单并自动释放。
     * <p>
     * 供 {@code paySettleReleaseTask} 定时任务调用。只处理订单状态为 SUCCESS、
     * settle_status=PENDING 且 settle_release_at 已到期的订单；再次读取商户配置，
     * 确认商户仍为 AUTO 结算模式后才执行释放，避免商户结算模式变更后继续自动释放。
     *
     * @return 本轮成功释放的订单数量
     */
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

    /**
     * 扫描超时未支付的代收订单并关闭。
     * <p>
     * 供 {@code payOrderCloseTask} 定时任务调用。只扫描 expire_at 已到期、未支付、
     * 未入账的 CREATED/PROCESSING 订单，并通过条件更新改为 CLOSED，避免与支付成功
     * 回调或主动查单并发时误关闭已支付订单。
     *
     * @return 本轮成功关闭的订单数量
     */
    @Override
    public int drainExpiredPayOrders() {
        Instant now = Instant.now();
        List<PayOrderEntity> orders = baseDao.selectList(new QueryWrapper<PayOrderEntity>()
                .in("status", PayOrderStatusEnum.CREATED.code(), PayOrderStatusEnum.PROCESSING.code())
                .isNotNull("expire_at")
                .le("expire_at", now)
                .isNull("paid_at")
                .and(wrapper -> wrapper.isNull("paid_amount").or().eq("paid_amount", BigDecimal.ZERO))
                .orderByAsc("expire_at", "id")
                .last("limit " + EXPIRE_DRAIN_BATCH));
        int closed = 0;
        for (PayOrderEntity order : orders) {
            try {
                if (closeExpiredPayOrder(order, now)) {
                    closed++;
                }
            } catch (Exception ex) {
                log.warn("Pay order close expired failed, orderNo={}, err={}", order.getPayOrderNo(), ex.getMessage());
            }
        }
        return closed;
    }

    /**
     * 扫描长时间处理中的代收订单并转人工处理。
     * <p>
     * 触发条件与代付一致：主动查单次数达到上限，或提交 PSP 后超过 SLA 仍未终态。
     */
    @Override
    public int drainLongProcessingOrders() {
        Instant now = Instant.now();
        Instant slaTime = now.minusSeconds(PROCESSING_SLA_SECONDS);
        List<PayOrderEntity> orders = baseDao.selectList(new QueryWrapper<PayOrderEntity>()
                .eq("status", PayOrderStatusEnum.PROCESSING.code())
                .and(wrapper -> wrapper.ge("query_count", MAX_ACTIVE_QUERY_COUNT)
                        .or()
                        .isNotNull("submitted_at").le("submitted_at", slaTime))
                .orderByAsc("submitted_at", "id")
                .last("limit " + MANUAL_REVIEW_DRAIN_BATCH));
        int marked = 0;
        for (PayOrderEntity order : orders) {
            if (markManualReview(order)) {
                marked++;
            }
        }
        return marked;
    }

    private boolean markManualReview(PayOrderEntity order) {
        UpdateWrapper<PayOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", order.getId())
                .eq("status", PayOrderStatusEnum.PROCESSING.code())
                .set("status", PayOrderStatusEnum.MANUAL_REVIEW.code())
                .set("status_reason", MANUAL_REVIEW_REASON)
                .set("next_query_at", null);
        if (baseDao.update(null, wrapper) == 0) {
            return false;
        }
        orderStatusLogService.recordChange(
                "PAY",
                order.getTenantId(),
                order.getMerchantId(),
                order.getId(),
                order.getPayOrderNo(),
                order.getStatus(),
                PayOrderStatusEnum.MANUAL_REVIEW.code(),
                "PAY_MANUAL_REVIEW",
                MANUAL_REVIEW_REASON,
                "SYSTEM",
                null,
                order.getMerchantOrderNo(),
                null
        );
        return true;
    }

    /**
     * 关闭单笔超时代收订单。
     * <p>
     * 使用条件更新保证幂等和并发安全：只有订单仍处于 CREATED/PROCESSING 且未支付时
     * 才会转为 CLOSED。关闭成功后记录订单状态轨迹，方便运营追踪超时关单原因。
     *
     * @param order 待关闭订单
     * @param now 本轮扫描时间
     * @return 是否实际完成关闭
     */
    private boolean closeExpiredPayOrder(PayOrderEntity order, Instant now) {
        UpdateWrapper<PayOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", order.getId())
                .in("status", PayOrderStatusEnum.CREATED.code(), PayOrderStatusEnum.PROCESSING.code())
                .isNull("paid_at")
                .and(item -> item.isNull("paid_amount").or().eq("paid_amount", BigDecimal.ZERO))
                .set("status", PayOrderStatusEnum.CLOSED.code())
                .set("status_reason", PAY_ORDER_EXPIRED_REASON)
                .set("closed_at", now)
                .set("next_query_at", null);
        if (baseDao.update(null, wrapper) == 0) {
            return false;
        }
        orderStatusLogService.recordChange(
                "PAY",
                order.getTenantId(),
                order.getMerchantId(),
                order.getId(),
                order.getPayOrderNo(),
                order.getStatus(),
                PayOrderStatusEnum.CLOSED.code(),
                "ORDER_EXPIRED",
                PAY_ORDER_EXPIRED_REASON,
                "SYSTEM",
                null,
                order.getMerchantOrderNo(),
                null
        );
        return true;
    }

    /**
     * 判断订单是否可以按商户配置自动释放待结算余额。
     * <p>
     * 只有商户结算模式为 AUTO，且计划释放时间不晚于当前时间时，才允许定时任务自动释放。
     *
     * @param merchant 商户
     * @param releaseAt 计划释放时间
     * @return 是否允许自动释放
     */
    private boolean shouldAutoRelease(MerchantEntity merchant, Instant releaseAt) {
        if (!SettleStatusEnum.isAutoReleaseMode(merchant.getSettleMode())) {
            return false;
        }
        return releaseAt != null && !releaseAt.isAfter(Instant.now());
    }

    /**
     * 构建待结算释放的账务请求。
     * <p>
     * releasePaySettle 需要知道商户、订单、币种、实收金额、商户手续费和待释放净额。
     * paidAmount 为空或为 0 时回退到订单原始金额，兼容部分 PSP 未回传实付金额的场景。
     *
     * @param order 代收订单
     * @return 账务释放请求
     */
    private PaySuccessPostingRequest settlePostingRequest(PayOrderEntity order) {
        PaySuccessPostingRequest request = new PaySuccessPostingRequest();
        request.setTenantId(order.getTenantId());
        request.setMerchantId(order.getMerchantId());
        request.setPspAccountId(order.getPspAccountId());
        request.setBizId(order.getId());
        request.setPayOrderNo(order.getPayOrderNo());
        request.setCurrency(order.getCurrency());
        request.setAmount(defaultAmount(order.getPaidAmount(), order.getAmount()));
        request.setMerchantFeeAmount(order.getMerchantFeeAmount());
        request.setSettleAmount(order.getSettleAmount());
        return request;
    }

    /**
     * 选择有效金额。
     * <p>
     * 优先使用大于 0 的主金额；主金额为空或非正数时，使用兜底金额。
     *
     * @param primary 优先金额
     * @param fallback 兜底金额
     * @return 最终金额
     */
    private BigDecimal defaultAmount(BigDecimal primary, BigDecimal fallback) {
        if (primary != null && primary.compareTo(BigDecimal.ZERO) > 0) {
            return primary;
        }
        return fallback;
    }

    /**
     * 构建后台代收订单分页查询条件。
     * <p>
     * 支持按租户、商户、商户应用、PSP、订单号、商户订单号、状态、结算状态、
     * 国家、币种、支付方式和账务凭证号等维度过滤。
     *
     * @param params 查询参数
     * @return MyBatis-Plus 查询条件
     */
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
