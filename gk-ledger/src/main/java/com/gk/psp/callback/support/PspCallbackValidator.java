package com.gk.psp.callback.support;

import com.gk.payment.enums.PayOrderStatusEnum;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * PSP 回调业务校验器。
 * <p>
 * 负责在回调适配器解析出标准结果后，校验 PSP 编码、金额、币种等关键字段，
 * 避免非法回调或串单回调进入后续账务处理。
 */
@Component
public class PspCallbackValidator {
    /**
     * 校验终态回调的关键业务字段。
     * <p>
     * 成功回调必须带金额和币种，并且与平台订单一致；失败、关闭等回调允许 PSP 不返回金额或币种，
     * 但只要返回了这些字段，就必须与平台订单一致。
     *
     * @param bizType 业务类型，代收或代付
     * @param result PSP 适配器解析后的标准回调结果
     * @param order 平台订单快照
     */
    public void validateTerminalCallback(String bizType, PspCallbackResult result, PspCallbackOrder order) {
        if (result == null || order == null) {
            throw new IllegalStateException("Invalid PSP callback context");
        }
        // PSP 编码同时存在时必须一致，防止其他通道的回调误处理当前订单。
        if (StringUtils.isNotBlank(result.getPspCode())
                && StringUtils.isNotBlank(order.pspCode())
                && !StringUtils.equalsIgnoreCase(result.getPspCode(), order.pspCode())) {
            throw new IllegalStateException("PSP callback PSP code mismatch");
        }
        String status = PspCallbackUtils.normalizeStatus(result.getOrderStatus());
        if (PayOrderStatusEnum.SUCCESS.code().equals(status)) {
            // 成功终态会触发入账或扣冻结，金额和币种必须严格校验。
            validateRequiredAmount(result.getAmount(), order.amount());
            validateRequiredCurrency(result.getCurrency(), order.currency());
            return;
        }
        // 非成功终态不强制 PSP 返回金额/币种，但返回后必须匹配平台订单。
        validateOptionalAmount(result.getAmount(), order.amount());
        validateOptionalCurrency(result.getCurrency(), order.currency());
    }

    /**
     * 校验必填金额。
     */
    private void validateRequiredAmount(BigDecimal callbackAmount, BigDecimal orderAmount) {
        if (callbackAmount == null || orderAmount == null || callbackAmount.compareTo(orderAmount) != 0) {
            throw new IllegalStateException("PSP callback amount mismatch");
        }
    }

    /**
     * 校验可选金额。
     */
    private void validateOptionalAmount(BigDecimal callbackAmount, BigDecimal orderAmount) {
        if (callbackAmount != null && orderAmount != null && callbackAmount.compareTo(orderAmount) != 0) {
            throw new IllegalStateException("PSP callback amount mismatch");
        }
    }

    /**
     * 校验必填币种。
     */
    private void validateRequiredCurrency(String callbackCurrency, String orderCurrency) {
        if (StringUtils.isBlank(callbackCurrency) || !StringUtils.equalsIgnoreCase(callbackCurrency, orderCurrency)) {
            throw new IllegalStateException("PSP callback currency mismatch");
        }
    }

    /**
     * 校验可选币种。
     */
    private void validateOptionalCurrency(String callbackCurrency, String orderCurrency) {
        if (StringUtils.isNotBlank(callbackCurrency) && !StringUtils.equalsIgnoreCase(callbackCurrency, orderCurrency)) {
            throw new IllegalStateException("PSP callback currency mismatch");
        }
    }
}
