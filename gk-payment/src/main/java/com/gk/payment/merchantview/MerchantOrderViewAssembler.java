package com.gk.payment.merchantview;

import com.gk.payment.domain.amount.MoneyFormat;
import com.gk.payment.entity.PayinOrderEntity;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.payment.enums.MerchantOrderStatusEnum;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.callback.support.PspCallbackUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MerchantOrderViewAssembler {

    public PayinOrderView fromPayinOrder(PayinOrderEntity order) {
        PayinOrderView view = new PayinOrderView();
        applyCommon(view,
                order.getPayinOrderNo(),
                order.getMerchantOrderNo(),
                order.getStatus(),
                order.getMerchantStatusCode(),
                order.getMerchantStatusReason(),
                order.getAmount(),
                order.getCurrency(),
                order.getCountryCode(),
                order.getMethodCode(),
                order.getMerchantFeeAmount());
        view.setPayUrl(StringUtils.trimToNull(order.getPspPayUrl()));
        view.setPaidAmount(moneyIfPositive(order.getPaidAmount(), order.getCurrency()));
        view.setSettleAmount(moneyIfPositive(order.getSettleAmount(), order.getCurrency()));
        return view;
    }

    public PayoutOrderView fromPayoutOrder(PayoutOrderEntity order) {
        PayoutOrderView view = new PayoutOrderView();
        applyCommon(view,
                order.getPayoutOrderNo(),
                order.getMerchantOrderNo(),
                order.getStatus(),
                order.getMerchantStatusCode(),
                order.getMerchantStatusReason(),
                order.getAmount(),
                order.getCurrency(),
                order.getCountryCode(),
                order.getMethodCode(),
                order.getMerchantFeeAmount());
        view.setDebitAmount(money(defaultDebitAmount(order.getTotalDebitAmount(), order.getAmount()), order.getCurrency()));
        return view;
    }

    public PayinOrderView fromPayinCallback(PspCallbackOrder order, PspCallbackResult result) {
        PayinOrderView view = new PayinOrderView();
        String status = PspCallbackUtils.normalizeStatus(result.getOrderStatus());
        applyCommon(view,
                order.orderNo(),
                order.merchantOrderNo(),
                status,
                notifyMerchantStatusCode(order, result),
                notifyMerchantStatusReason(order, result),
                order.amount(),
                order.currency(),
                order.countryCode(),
                order.methodCode(),
                order.merchantFeeAmount());
        BigDecimal paidAmount = PspCallbackUtils.defaultAmount(result.getAmount(), order.amount());
        view.setPaidAmount(money(paidAmount, order.currency()));
        view.setSettleAmount(moneyIfPositive(order.settleAmount(), order.currency()));
        return view;
    }

    public PayoutOrderView fromPayoutCallback(PspCallbackOrder order, PspCallbackResult result) {
        PayoutOrderView view = new PayoutOrderView();
        String status = PspCallbackUtils.normalizeStatus(result.getOrderStatus());
        applyCommon(view,
                order.orderNo(),
                order.merchantOrderNo(),
                status,
                notifyMerchantStatusCode(order, result),
                notifyMerchantStatusReason(order, result),
                order.amount(),
                order.currency(),
                order.countryCode(),
                order.methodCode(),
                order.merchantFeeAmount());
        view.setDebitAmount(money(defaultDebitAmount(order.totalDebitAmount(), order.amount()), order.currency()));
        return view;
    }

    private void applyCommon(MerchantOrderView view,
                             String systemOrderId,
                             String merchantOrderId,
                             String orderStatus,
                             String merchantStatusCode,
                             String merchantStatusReason,
                             BigDecimal amount,
                             String currency,
                             String countryCode,
                             String methodCode,
                             BigDecimal feeAmount) {
        view.setSystemOrderId(systemOrderId);
        view.setMerchantOrderId(merchantOrderId);
        view.setStatus(MerchantOrderStatusEnum.merchantCode(StringUtils.trimToNull(merchantStatusCode), orderStatus));
        view.setStatusReason(MerchantOrderStatusEnum.merchantReason(StringUtils.trimToNull(merchantStatusReason), orderStatus));
        view.setAmount(money(amount, currency));
        view.setCurrency(currency);
        view.setCountryCode(countryCode);
        view.setMethodCode(methodCode);
        view.setFeeAmount(moneyIfPositive(feeAmount, currency));
    }

    private String notifyMerchantStatusCode(PspCallbackOrder order, PspCallbackResult result) {
        return manualNotify(result) ? order.merchantStatusCode() : null;
    }

    private String notifyMerchantStatusReason(PspCallbackOrder order, PspCallbackResult result) {
        return manualNotify(result) ? order.merchantStatusReason() : null;
    }

    private boolean manualNotify(PspCallbackResult result) {
        return result != null && "MANUAL_NOTIFY".equals(result.getCallbackType());
    }

    private BigDecimal defaultDebitAmount(BigDecimal totalDebitAmount, BigDecimal amount) {
        return positive(totalDebitAmount) ? totalDebitAmount : amount;
    }

    private String money(BigDecimal value, String currency) {
        return value == null ? null : MoneyFormat.format(value);
    }

    private String moneyIfPositive(BigDecimal value, String currency) {
        return positive(value) ? money(value, currency) : null;
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
