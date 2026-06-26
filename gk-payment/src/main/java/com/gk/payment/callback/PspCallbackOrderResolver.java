package com.gk.payment.callback;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gk.common.enums.BizTypeEnum;
import com.gk.payment.dao.PayOrderDao;
import com.gk.payment.dao.PayoutOrderDao;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.entity.PspAccountEntity;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * PSP ص
 * <p>
 *  PSP صƽ̨Ż PSP Ŷλƽ̨ǩҪ PSP ˻Կ
 */
@Component
@RequiredArgsConstructor
public class PspCallbackOrderResolver {
    private final PayOrderDao payOrderDao;
    private final PayoutOrderDao payoutOrderDao;
    private final PspAccountDao pspAccountDao;

    public PspCallbackOrder resolve(String bizType, PspCallbackResult result) {
        if (BizTypeEnum.PAY_ORDER.matches(bizType)) {
            PayOrderEntity order = findOne(payOrderDao, "pay_order_no", result);
            if (order == null) {
                throw new IllegalStateException("Pay order not found");
            }
            return fromPayOrder(order, apiSecret(order.getPspAccountId()));
        }
        PayoutOrderEntity order = findOne(payoutOrderDao, "payout_order_no", result);
        if (order == null) {
            throw new IllegalStateException("Payout order not found");
        }
        return fromPayoutOrder(order, apiSecret(order.getPspAccountId()));
    }

    public PspCallbackOrder fromPayOrder(PayOrderEntity order, String apiSecret) {
        return new PspCallbackOrder(
                order.getId(), order.getTenantId(), order.getMerchantId(), order.getMerchantNo(),
                order.getMerchantAppId(), order.getAppId(), order.getPspId(), order.getPspCode(),
                order.getPspAccountId(), apiSecret, order.getPayOrderNo(), order.getMerchantOrderNo(),
                order.getPspOrderNo(), order.getStatus(), order.getAmount(), order.getMerchantFeeAmount(),
                order.getSettleAmount(), null, order.getCurrency(), order.getNotifyUrl()
        );
    }

    public PspCallbackOrder fromPayoutOrder(PayoutOrderEntity order, String apiSecret) {
        return new PspCallbackOrder(
                order.getId(), order.getTenantId(), order.getMerchantId(), order.getMerchantNo(),
                order.getMerchantAppId(), order.getAppId(), order.getPspId(), order.getPspCode(),
                order.getPspAccountId(), apiSecret, order.getPayoutOrderNo(), order.getMerchantOrderNo(),
                order.getPspOrderNo(), order.getStatus(), order.getAmount(), order.getMerchantFeeAmount(),
                null, order.getTotalDebitAmount(), order.getCurrency(), order.getNotifyUrl()
        );
    }

    private <T> T findOne(BaseMapper<T> dao, String orderNoColumn, PspCallbackResult result) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(result.getSystemOrderNo())) {
            wrapper.eq(orderNoColumn, result.getSystemOrderNo());
        } else if (StringUtils.isNotBlank(result.getPspOrderNo())) {
            wrapper.eq("psp_order_no", result.getPspOrderNo());
        } else {
            return null;
        }
        return dao.selectOne(wrapper.last("limit 1"));
    }

    private String apiSecret(Long pspAccountId) {
        if (pspAccountId == null) {
            return null;
        }
        PspAccountEntity account = pspAccountDao.selectById(pspAccountId);
        return account == null ? null : account.getApiSecret();
    }
}
