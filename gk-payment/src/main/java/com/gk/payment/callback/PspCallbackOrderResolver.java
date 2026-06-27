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
 * PSP 回调订单解析器。
 * <p>
 * 回调报文只携带 PSP 返回的平台单号或 PSP 单号，后续验签又需要订单绑定的 PSP 账户密钥。
 * 因此本类负责在验签前先定位平台订单，并把订单快照和账户密钥封装成
 * {@link PspCallbackOrder}，供验签、金额校验、状态推进、账务入账和商户通知使用。
 */
@Component
@RequiredArgsConstructor
public class PspCallbackOrderResolver {
    private final PayOrderDao payOrderDao;
    private final PayoutOrderDao payoutOrderDao;
    private final PspAccountDao pspAccountDao;

    /**
     * 根据业务类型和标准化后的回调结果定位平台订单。
     * <p>
     * 代收查 {@code pay_order}，代付查 {@code payout_order}；找到订单后会同步加载
     * PSP 账户密钥，供回调适配器执行签名校验。找不到订单时抛出明确异常，由上层映射为
     * {@code ORDER_NOT_FOUND} 回调处理结果。
     */
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

    /**
     * 将代收订单实体转换为回调处理所需的统一订单快照。
     * <p>
     * 该快照只保留回调链路需要的字段，避免后续处理直接依赖订单表实体结构。
     */
    public PspCallbackOrder fromPayOrder(PayOrderEntity order, String apiSecret) {
        return new PspCallbackOrder(
                order.getId(), order.getTenantId(), order.getMerchantId(), order.getMerchantNo(),
                order.getMerchantAppId(), order.getAppId(), order.getPspId(), order.getPspCode(),
                order.getPspAccountId(), apiSecret, order.getPayOrderNo(), order.getMerchantOrderNo(),
                order.getPspOrderNo(), order.getStatus(), order.getAmount(), order.getMerchantFeeAmount(),
                order.getSettleAmount(), null, order.getCurrency(), order.getNotifyUrl()
        );
    }

    /**
     * 将代付订单实体转换为回调处理所需的统一订单快照。
     * <p>
     * 代付额外携带 {@code totalDebitAmount}，用于成功扣减冻结或失败释放冻结时保持账务金额一致。
     */
    public PspCallbackOrder fromPayoutOrder(PayoutOrderEntity order, String apiSecret) {
        return new PspCallbackOrder(
                order.getId(), order.getTenantId(), order.getMerchantId(), order.getMerchantNo(),
                order.getMerchantAppId(), order.getAppId(), order.getPspId(), order.getPspCode(),
                order.getPspAccountId(), apiSecret, order.getPayoutOrderNo(), order.getMerchantOrderNo(),
                order.getPspOrderNo(), order.getStatus(), order.getAmount(), order.getMerchantFeeAmount(),
                null, order.getTotalDebitAmount(), order.getCurrency(), order.getNotifyUrl()
        );
    }

    /**
     * 按回调单号查找订单。
     * <p>
     * 优先使用平台订单号，因为它由本系统生成且唯一性更强；缺失时才回退到 PSP 单号。
     * 查询同时带上 {@code psp_code} 条件，避免不同 PSP 通道返回相同 PSP 单号时串单。
     */
    private <T> T findOne(BaseMapper<T> dao, String orderNoColumn, PspCallbackResult result) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(result.getPspCode())) {
            wrapper.eq("psp_code", result.getPspCode());
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

    /**
     * 读取订单绑定 PSP 账户的 API Secret。
     * <p>
     * 回调验签必须使用订单下单时绑定的 PSP 账户密钥，不能只依赖路径里的 PSP 编码。
     */
    private String apiSecret(Long pspAccountId) {
        if (pspAccountId == null) {
            return null;
        }
        PspAccountEntity account = pspAccountDao.selectById(pspAccountId);
        return account == null ? null : account.getApiSecret();
    }
}
