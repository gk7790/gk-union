package com.gk.payment.callback;

import com.gk.common.enums.BizTypeEnum;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.ledger.posting.PaySuccessPostingRequest;
import com.gk.ledger.posting.PayoutPostingRequest;
import com.gk.ledger.service.LedgerPostingService;
import com.gk.payment.service.PayinOrderService;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.callback.support.PspCallbackUtils;
import com.gk.psp.callback.support.PspCallbackValidator;
import com.gk.psp.query.PspOrderQueryResult;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PSP 主动查单结果处理器。
 *
 * <p>这个类不接收 PSP HTTP 回调，而是给主动查单、补偿查询等流程使用：
 * 把 PSP 查询结果 {@link PspOrderQueryResult} 转成统一的 {@link PspCallbackResult}，
 * 再复用标准订单状态推进、账务入账/释放和商户通知逻辑。</p>
 *
 * <p>注意：沙箱 mock 如果只想推进订单状态，不应该走这个处理器；
 * 这里的终态成功/失败会真实触发账务处理。</p>
 */
@Service
@RequiredArgsConstructor
public class PspOrderResultHandler {
    private final PspCallbackOrderProcessor orderProcessor;
    private final PspCallbackValidator callbackValidator;
    private final LedgerPostingService ledgerPostingService;
    private final PspCallbackNotifyCreator notifyCreator;
    private final PayinOrderService payinOrderService;

    /**
     * 处理 PSP 查单结果。
     *
     * <p>流程顺序必须保持：标准化结果 -> 终态校验 -> 更新订单 -> 账务处理 -> 回写流水号 -> 商户通知。
     * 只有订单状态发生变化且结果是终态时，才会触发账务和通知，保证重复查单/重复结果的幂等性。</p>
     *
     * @param bizType 业务类型，代收 {@code PAYIN_ORDER} 或代付 {@code PAYOUT_ORDER}
     * @param order 当前平台订单快照
     * @param queryResult PSP 查单返回的标准结果
     * @return true 表示订单状态发生了推进；false 表示重复结果或订单已终态
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean handle(String bizType, PspCallbackOrder order, PspOrderQueryResult queryResult) {
        // 主动查单结果先转换成标准回调结果，后续和 PSP 回调共用同一套状态语义。
        PspCallbackResult result = queryResult.toCallbackResult(bizType);
        boolean terminal = PspCallbackUtils.isTerminal(result.getOrderStatus());
        if (terminal) {
            // 终态结果必须校验金额、币种、PSP 编码等关键字段，避免错误结果落账。
            callbackValidator.validateTerminalCallback(result, order);
        }
        // 先推进订单状态。这里暂不传账务流水，等账务成功后再 attach，避免状态和流水不一致。
        boolean orderChanged = orderProcessor.process(bizType, result, order, null);
        if (orderChanged && terminal) {
            // 只有第一次推进到终态时才做账务处理；重复终态结果不会再次入账或释放。
            LedgerPostingResult postingResult = postLedger(bizType, result, order);
            orderProcessor.attachPostingResult(bizType, order.id(), result.getOrderStatus(), postingResult);
            if (BizTypeEnum.PAYIN_ORDER.matches(bizType)
                    && PspCallbackUtils.STATUS_SUCCESS.equals(PspCallbackUtils.normalizeStatus(result.getOrderStatus()))) {
                // 代收成功入账后，继续计算待结算释放时间，必要时触发自动释放。
                payinOrderService.onPaySuccessPosted(order.id());
            }
            // 账务和订单状态都处理完成后，再创建商户通知，避免商户先收到未落账状态。
            notifyCreator.create(bizType, result, order, null);
        }
        return orderChanged;
    }

    /**
     * 根据终态结果执行对应账务动作。
     *
     * <p>代收只在成功时入待结算；代付成功时扣减冻结余额，失败时释放冻结余额。</p>
     */
    private LedgerPostingResult postLedger(String bizType, PspCallbackResult result, PspCallbackOrder order) {
        String status = PspCallbackUtils.normalizeStatus(result.getOrderStatus());
        if (BizTypeEnum.PAYIN_ORDER.matches(bizType) && PspCallbackUtils.STATUS_SUCCESS.equals(status)) {
            // 代收成功：资金进入商户待结算账户，商户手续费按订单快照入账。
            return ledgerPostingService.postPaySuccess(paySuccessRequest(result, order));
        }
        if (BizTypeEnum.PAYOUT_ORDER.matches(bizType) && PspCallbackUtils.STATUS_SUCCESS.equals(status)) {
            // 代付成功：把下单时冻结的金额正式扣减。
            return ledgerPostingService.postPayoutSuccess(payoutRequest(order));
        }
        if (BizTypeEnum.PAYOUT_ORDER.matches(bizType) && PspCallbackUtils.STATUS_FAILED.equals(status)) {
            // 代付失败：释放下单时冻结的金额，恢复商户可用余额。
            return ledgerPostingService.releasePayout(payoutRequest(order));
        }
        return null;
    }

    /**
     * 构建代收成功入账请求。
     */
    private PaySuccessPostingRequest paySuccessRequest(PspCallbackResult result, PspCallbackOrder order) {
        PaySuccessPostingRequest request = new PaySuccessPostingRequest();
        request.setTenantId(order.tenantId());
        request.setMerchantId(order.merchantId());
        request.setMerchantNo(order.merchantNo());
        request.setMerchantAppId(order.merchantAppId());
        request.setMerchantOrderNo(order.merchantOrderNo());
        request.setPspAccountId(order.pspAccountId());
        request.setBizId(order.id());
        request.setPayinOrderNo(order.orderNo());
        request.setCurrency(order.currency());
        // PSP 查单可能不返回金额，兜底使用平台订单金额，避免空金额影响账务入账。
        request.setAmount(PspCallbackUtils.defaultAmount(result.getAmount(), order.amount()));
        request.setMerchantFeeAmount(order.merchantFeeAmount());
        request.setSettleAmount(order.settleAmount());
        return request;
    }

    /**
     * 构建代付扣减/释放账务请求。
     */
    private PayoutPostingRequest payoutRequest(PspCallbackOrder order) {
        PayoutPostingRequest request = new PayoutPostingRequest();
        request.setTenantId(order.tenantId());
        request.setMerchantId(order.merchantId());
        request.setMerchantNo(order.merchantNo());
        request.setMerchantAppId(order.merchantAppId());
        request.setMerchantOrderNo(order.merchantOrderNo());
        request.setPspAccountId(order.pspAccountId());
        request.setBizId(order.id());
        request.setPayoutOrderNo(order.orderNo());
        request.setCurrency(order.currency());
        request.setAmount(order.amount());
        request.setMerchantFeeAmount(order.merchantFeeAmount());
        // totalDebitAmount 是代付实际冻结/扣减口径：代付金额 + 商户手续费。
        request.setTotalDebitAmount(order.totalDebitAmount());
        return request;
    }
}
