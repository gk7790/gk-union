package com.gk.payment.callback;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gk.common.enums.BizTypeEnum;
import com.gk.payment.dao.PayinOrderDao;
import com.gk.payment.dao.PayoutOrderDao;
import com.gk.payment.entity.PayinOrderEntity;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.psp.callback.PspCallbackBizException;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.callback.support.PspCallbackAckMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * PSP 回调订单解析器。
 *
 * <p>签名校验通过后，根据标准化回调结果和回调路径解析出的 PSP 账号上下文定位平台订单，
 * 并封装成后续校验、入账、通知商户使用的订单快照。</p>
 */
@Component
@RequiredArgsConstructor
public class PspCallbackOrderResolver {
    private final PayinOrderDao payinOrderDao;
    private final PayoutOrderDao payoutOrderDao;

    /**
     * 根据业务类型和回调结果定位平台订单。
     *
     * <p>查询条件会同时带上 {@code psp_account_id} 和 {@code psp_code}，避免不同 PSP 账号或通道
     * 返回相同 PSP 单号时串单。</p>
     */
    public PspCallbackOrder resolve(String bizType, PspCallbackResult result,
                                    Long pspAccountId, String pspCode, String apiSecret) {
        if (BizTypeEnum.PAYIN_ORDER.matches(bizType)) {
            PayinOrderEntity order = findOne(payinOrderDao, "payin_order_no", result, pspAccountId, pspCode);
            if (order == null) {
                throw new PspCallbackBizException(PspCallbackAckMapper.ORDER_NOT_FOUND, "Pay order not found");
            }
            return fromPayinOrder(order, apiSecret);
        }

        PayoutOrderEntity order = findOne(payoutOrderDao, "payout_order_no", result, pspAccountId, pspCode);
        if (order == null) {
            throw new PspCallbackBizException(PspCallbackAckMapper.ORDER_NOT_FOUND, "Payout order not found");
        }
        return fromPayoutOrder(order, apiSecret);
    }

    /**
     * 将代收订单实体转换为回调处理需要的统一订单快照。
     */
    public PspCallbackOrder fromPayinOrder(PayinOrderEntity order, String apiSecret) {
        return new PspCallbackOrder(
                order.getId(), order.getTenantId(), order.getMerchantId(), order.getMerchantNo(),
                order.getMerchantAppId(), order.getAppId(), order.getPspId(), order.getPspCode(),
                order.getPspAccountId(), apiSecret, order.getPayinOrderNo(), order.getMerchantOrderNo(),
                order.getPspOrderNo(), order.getStatus(), order.getStatusReason(), order.getCountryCode(),
                order.getMethodCode(), order.getAmount(), order.getMerchantFeeAmount(),
                order.getSettleAmount(), null, order.getCurrency(), order.getNotifyUrl()
        );
    }

    /**
     * 将代付订单实体转换为回调处理需要的统一订单快照。
     */
    public PspCallbackOrder fromPayoutOrder(PayoutOrderEntity order, String apiSecret) {
        return new PspCallbackOrder(
                order.getId(), order.getTenantId(), order.getMerchantId(), order.getMerchantNo(),
                order.getMerchantAppId(), order.getAppId(), order.getPspId(), order.getPspCode(),
                order.getPspAccountId(), apiSecret, order.getPayoutOrderNo(), order.getMerchantOrderNo(),
                order.getPspOrderNo(), order.getStatus(), order.getStatusReason(), order.getCountryCode(),
                order.getMethodCode(), order.getAmount(), order.getMerchantFeeAmount(),
                null, order.getTotalDebitAmount(), order.getCurrency(), order.getNotifyUrl()
        );
    }

    /**
     * 按回调单号查询订单。
     *
     * <p>优先使用系统订单号，缺失时回退到 PSP 单号。</p>
     */
    private <T> T findOne(BaseMapper<T> dao, String orderNoColumn, PspCallbackResult result,
                          Long pspAccountId, String pspCode) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        if (pspAccountId != null) {
            wrapper.eq("psp_account_id", pspAccountId);
        }
        if (StringUtils.isNotBlank(pspCode)) {
            wrapper.eq("psp_code", pspCode);
        }
        if (StringUtils.isNotBlank(result.getSystemOrderNo())) {
            wrapper.eq(orderNoColumn, result.getSystemOrderNo());
        } else if (StringUtils.isNotBlank(result.getPspOrderNo())) {
            wrapper.eq("psp_order_no", result.getPspOrderNo());
        } else {
            return null;
        }
        return dao.selectOne(wrapper.last("limit 1"));
    }
}
