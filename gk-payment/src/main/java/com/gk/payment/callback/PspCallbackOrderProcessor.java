package com.gk.payment.callback;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gk.common.enums.BizTypeEnum;
import com.gk.common.enums.PayDirectionEnum;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.payment.dao.PayinOrderDao;
import com.gk.payment.enums.PayinOrderStatusEnum;
import com.gk.payment.enums.PayoutOrderStatusEnum;
import com.gk.payment.enums.SettleStatusEnum;
import com.gk.payment.dao.PayoutOrderDao;
import com.gk.payment.service.OrderStatusLogService;
import com.gk.psp.callback.PspCallbackBizException;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.callback.support.PspCallbackAckMapper;
import com.gk.psp.callback.support.PspCallbackUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * PSP 回调订单状态处理器 * <p>
 * 负责把标准化后的 PSP 回调结果落到代收或代付订单表，并记录订单状态变更日志
 * 更新时通过状态条件控制幂等，避免重复回调覆盖已终态订单
 */
@Component
@RequiredArgsConstructor
public class PspCallbackOrderProcessor {
    private final PayinOrderDao payinOrderDao;
    private final PayoutOrderDao payoutOrderDao;
    private final OrderStatusLogService orderStatusLogService;

    /**
     * 根据 PSP 回调处理订单状态     *
     *
     * @param bizType       业务类型，代收或代付
     * @param result        PSP 标准回调结果
     * @param order         当前订单快照
     * @param postingResult 账务入账、扣冻结或解冻结     * @return true 表示订单状态实际发生更新，false 表示重复回调或已终态无需更新
     */
    public boolean process(String bizType, PspCallbackResult result, PspCallbackOrder order, LedgerPostingResult postingResult) {
        String fromStatus = order.status();
        String targetStatus = PspCallbackUtils.normalizeStatus(result.getOrderStatus());
        if (PayinOrderStatusEnum.PROCESSING.code().equals(targetStatus)) {
            if (PspCallbackUtils.isFinalTerminal(fromStatus)
                    || PayinOrderStatusEnum.MANUAL_REVIEW.code().equals(fromStatus)
                    || PayoutOrderStatusEnum.MANUAL_REVIEW.code().equals(fromStatus)) {
                return false;
            }
            // 中间态只允许 CREATED/PROCESSING 继续推进，不触发账务和商户通知终态逻辑
            boolean updated = update(bizType, order.id(), wrapper -> applyCommon(wrapper, PayinOrderStatusEnum.PROCESSING.code(), result, order));
            if (updated) {
                recordChange(bizType, order, fromStatus, PayinOrderStatusEnum.PROCESSING.code(), statusEventType(bizType, PayinOrderStatusEnum.PROCESSING.code()), result);
            }
            return updated;
        }
        if (!PspCallbackUtils.isTerminal(targetStatus)) {
            throw new PspCallbackBizException(PspCallbackAckMapper.UNSUPPORTED_STATUS, "Unsupported callback order status");
        }
        if (targetStatus.equals(fromStatus) || PspCallbackUtils.isFinalTerminal(fromStatus)) {
            // 同状态重复回调或已最终终态订单直接忽略，保证回调幂等
            return false;
        }
        boolean updated = update(bizType, order.id(), wrapper -> applyTerminal(bizType, wrapper, result, order, targetStatus, postingResult));
        if (updated) {
            recordChange(bizType, order, fromStatus, targetStatus, statusEventType(bizType, targetStatus), result);
        }
        return updated;
    }

    /**
     * 补写账务流水号     * <p>
     * 当订单状态已更新但账务结果需要在后续阶段补充时，通过本方法把流水号挂回订单
     */
    public void attachPostingResult(String bizType, Long orderId, String targetStatus, LedgerPostingResult postingResult) {
        String journalNo = postingResult == null ? null : postingResult.getJournalNo();
        if (StringUtils.isBlank(journalNo)) {
            return;
        }
        String normalizedStatus = PspCallbackUtils.normalizeStatus(targetStatus);
        boolean updated = updateJournalNo(bizType, orderId, wrapper -> {
            if (BizTypeEnum.PAYIN_ORDER.matches(bizType)) {
                wrapper.set("ledger_journal_no", journalNo);
            } else if (PayinOrderStatusEnum.SUCCESS.code().equals(normalizedStatus)) {
                wrapper.set("success_journal_no", journalNo);
            } else if (PayinOrderStatusEnum.FAILED.code().equals(normalizedStatus)) {
                wrapper.set("release_journal_no", journalNo);
            }
        });
        if (!updated) {
            throw new IllegalStateException("Attach ledger journal to callback order failed");
        }
    }

    /**
     * 填充终态订单字段     * <p>
     * 代收成功进入待结算；代收失败取消结算；代付成功记录完成时间和成功流水     * 代付失败记录失败原因，并挂载释放冻结流水
     */
    private void applyTerminal(String bizType, UpdateWrapper<?> wrapper, PspCallbackResult result,
                               PspCallbackOrder order, String targetStatus, LedgerPostingResult postingResult) {
        applyCommon(wrapper, targetStatus, result, order);
        String statusReason = result.getErrorMessage();
        wrapper.set(statusReason != null, "status_reason", statusReason);
        boolean success = PayinOrderStatusEnum.SUCCESS.code().equals(targetStatus);
        String journalNo = postingResult == null ? null : postingResult.getJournalNo();
        Instant now = Instant.now();
        if (BizTypeEnum.PAYIN_ORDER.matches(bizType)) {
            if (success) {
                // 代收成功后资金进入待结算账户，后续由结算释放任务转入可用余额
                wrapper.set("paid_amount", PspCallbackUtils.defaultAmount(result.getAmount(), order.amount()))
                        .set("paid_at", now)
                        .set("settle_status", SettleStatusEnum.PENDING.code())
                        .set(journalNo != null, "ledger_journal_no", journalNo);
            } else {
                wrapper.set("failed_at", now)
                        .set("settle_status", SettleStatusEnum.CANCELLED.code());
            }
            return;
        }
        if (success) {
            // 代付成功后冻结资金被正式扣减，记录扣冻结账务流水号
            wrapper.set("completed_at", now)
                    .set(journalNo != null, "success_journal_no", journalNo);
        } else {
            String failMsg = StringUtils.left(result.getErrorMessage(), 512);
            // 代付失败后冻结释放，保留 PSP 错误码和错误信息便于人工排查
            wrapper.set("failed_at", now)
                    .set(result.getErrorCode() != null, "fail_code", result.getErrorCode())
                    .set(failMsg != null, "fail_msg", failMsg)
                    .set(journalNo != null, "release_journal_no", journalNo);
        }
    }

    /**
     * 填充代收和代付订单通用回调字段
     */
    private void applyCommon(UpdateWrapper<?> wrapper, String status, PspCallbackResult result, PspCallbackOrder order) {
        String pspOrderNo = StringUtils.defaultIfBlank(result.getPspOrderNo(), order.pspOrderNo());
        wrapper.set("status", status)
                .set("psp_status", status)
                .set(result.getPspStatus() != null, "psp_raw_status", result.getPspStatus())
                .set(pspOrderNo != null, "psp_order_no", pspOrderNo);
    }

    /**
     * 根据业务类型选择对应订单表执行状态更新
     */
    private boolean update(String bizType, Long id, Consumer<UpdateWrapper<?>> setter) {
        return BizTypeEnum.PAYIN_ORDER.matches(bizType)
                ? update(payinOrderDao, id, setter)
                : update(payoutOrderDao, id, setter);
    }

    /**
     * 执行带状态条件的订单更新     * <p>
     * 只允CREATED、PROCESSING 被回调推进，防止SUCCESS/FAILED 等终态被重复覆盖
     */
    private <T> boolean update(BaseMapper<T> dao, Long id, Consumer<UpdateWrapper<?>> setter) {
        UpdateWrapper<T> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id)
                .in("status",
                        PayinOrderStatusEnum.CREATED.code(),
                        PayinOrderStatusEnum.PROCESSING.code(),
                        PayinOrderStatusEnum.MANUAL_REVIEW.code(),
                        PayoutOrderStatusEnum.MANUAL_REVIEW.code());
        setter.accept(wrapper);
        return dao.update(null, wrapper) > 0;
    }

    /**
     * 根据业务类型选择对应订单表补写账务流水号
     */
    private boolean updateJournalNo(String bizType, Long id, Consumer<UpdateWrapper<?>> setter) {
        return BizTypeEnum.PAYIN_ORDER.matches(bizType)
                ? updateJournalNo(payinOrderDao, id, setter)
                : updateJournalNo(payoutOrderDao, id, setter);
    }

    /**
     * 不改变订单状态，仅按订单 ID 更新账务流水字段
     */
    private <T> boolean updateJournalNo(BaseMapper<T> dao, Long id, Consumer<UpdateWrapper<?>> setter) {
        UpdateWrapper<T> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id);
        setter.accept(wrapper);
        return dao.update(null, wrapper) > 0;
    }

    /**
     * 记录订单状态变更日志
     */
    private void recordChange(String bizType, PspCallbackOrder order, String fromStatus, String toStatus,
                              String eventType, PspCallbackResult result) {
        orderStatusLogService.recordChange(
                direction(bizType),
                order.tenantId(),
                order.merchantId(),
                order.id(),
                order.orderNo(),
                fromStatus,
                toStatus,
                eventType,
                result == null ? null : result.getErrorMessage(),
                "PSP",
                order.pspCode(),
                null,
                null
        );
    }

    /**
     * 转换订单类型文本
     */
    private String direction(String bizType) {
        return BizTypeEnum.PAYIN_ORDER.matches(bizType) ? PayDirectionEnum.PAYIN.code() : PayDirectionEnum.PAYOUT.code();
    }

    /**
     * 生成状态变更事件类型
     */
    private String statusEventType(String bizType, String status) {
        String prefix = BizTypeEnum.PAYIN_ORDER.matches(bizType) ? PayDirectionEnum.PAYIN.code() : PayDirectionEnum.PAYOUT.code();
        return prefix + "_" + PspCallbackUtils.normalizeStatus(status);
    }
}
