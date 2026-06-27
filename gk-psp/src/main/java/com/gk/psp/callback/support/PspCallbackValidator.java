package com.gk.psp.callback.support;

import com.gk.common.model.Result;
import com.gk.psp.callback.PspCallbackBizException;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackResult;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * PSP 回调业务校验器
 * <p>
 * 负责在回调适配器解析出标准结果后，校验 PSP 编码、金额、币种等关键字段
 * 避免非法回调或串单回调进入后续账务处理
 * 币种缺失时由 {@link PspCallbackCurrencyResolver} 按适配器策略回退订单币种后再校验
 */
@Component
@RequiredArgsConstructor
public class PspCallbackValidator {
    private final PspCallbackCurrencyResolver currencyResolver;

    public Result<Void> validateTerminalCallbackResult(PspCallbackResult result, PspCallbackOrder order) {
        try {
            validateTerminalCallback(result, order);
            return Result.success(null);
        } catch (PspCallbackBizException ex) {
            return Result.fail(ex.getAckCode());
        }
    }

    /**
     * 校验终态回调的关键业务字段
     * <p>
     * 成功回调必须带金额并与订单一致；币种优先PSP 回传，缺失时按策略回退订单币种
     * 失败、关闭等终态不强制 PSP 返回金额/币种，但只要返回了这些字段就必须与订单一致
     */
    public void validateTerminalCallback(PspCallbackResult result, PspCallbackOrder order) {
        if (result == null || order == null) {
            throw new PspCallbackBizException(PspCallbackAckMapper.SYSTEM_ERROR, "Invalid PSP callback context");
        }
        if (StringUtils.isNotBlank(result.getPspCode())
                && StringUtils.isNotBlank(order.pspCode())
                && !Strings.CI.equals(result.getPspCode(), order.pspCode())) {
            throw new PspCallbackBizException(PspCallbackAckMapper.PSP_CODE_MISMATCH, "PSP callback PSP code mismatch");
        }

        currencyResolver.resolve(result, order);

        String status = PspCallbackUtils.normalizeStatus(result.getOrderStatus());
        if (PspCallbackUtils.STATUS_SUCCESS.equals(status)) {
            validateRequiredAmount(result.getAmount(), order.amount());
            return;
        }
        validateOptionalAmount(result.getAmount(), order.amount());
    }

    private void validateRequiredAmount(BigDecimal callbackAmount, BigDecimal orderAmount) {
        if (callbackAmount == null || orderAmount == null || callbackAmount.compareTo(orderAmount) != 0) {
            throw new PspCallbackBizException(PspCallbackAckMapper.AMOUNT_MISMATCH, "PSP callback amount mismatch");
        }
    }

    private void validateOptionalAmount(BigDecimal callbackAmount, BigDecimal orderAmount) {
        if (callbackAmount != null && orderAmount != null && callbackAmount.compareTo(orderAmount) != 0) {
            throw new PspCallbackBizException(PspCallbackAckMapper.AMOUNT_MISMATCH, "PSP callback amount mismatch");
        }
    }
}
