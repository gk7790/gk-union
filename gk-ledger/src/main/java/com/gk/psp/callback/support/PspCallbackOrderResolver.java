package com.gk.psp.callback.support;

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
 * 根据业务类型和 PSP 回调中的订单号，定位平台代收或代付订单，并补齐验签需要的 PSP 账户密钥。
 */
@Component
@RequiredArgsConstructor
public class PspCallbackOrderResolver {
    private final PayOrderDao payOrderDao;
    private final PayoutOrderDao payoutOrderDao;
    private final PspAccountDao pspAccountDao;

    /**
     * 解析 PSP 回调对应的平台订单。
     *
     * @param bizType 业务类型，代收或代付
     * @param result PSP 适配器解析后的标准回调结果
     * @return 订单快照和对应 PSP 账户密钥
     */
    public PspCallbackOrder resolve(String bizType, PspCallbackResult result) {
        if (BizTypeEnum.PAY_ORDER.matches(bizType)) {
            // 代收优先用平台代收单号查找，缺失时回退到 PSP 单号。
            PayOrderEntity order = findOne(payOrderDao, "pay_order_no", result);
            if (order == null) {
                throw new IllegalStateException("Pay order not found");
            }
            return PspCallbackOrder.of(order, apiSecret(order.getPspAccountId()));
        }
        // 非代收按代付处理，保持代收和代付回调入口统一。
        PayoutOrderEntity order = findOne(payoutOrderDao, "payout_order_no", result);
        if (order == null) {
            throw new IllegalStateException("Payout order not found");
        }
        return PspCallbackOrder.of(order, apiSecret(order.getPspAccountId()));
    }

    /**
     * 按平台订单号或 PSP 订单号查询单条订单。
     */
    private <T> T findOne(BaseMapper<T> dao, String orderNoColumn, PspCallbackResult result) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(result.getSystemOrderNo())) {
            wrapper.eq(orderNoColumn, result.getSystemOrderNo());
        } else if (StringUtils.isNotBlank(result.getPspOrderNo())) {
            wrapper.eq("psp_order_no", result.getPspOrderNo());
        } else {
            // 回调里没有任何可定位订单的编号，交由上层记录失败日志。
            return null;
        }
        return dao.selectOne(wrapper.last("limit 1"));
    }

    /**
     * 获取 PSP 账户密钥。
     * <p>
     * 该密钥用于适配器执行回调验签。
     */
    private String apiSecret(Long pspAccountId) {
        if (pspAccountId == null) {
            return null;
        }
        PspAccountEntity account = pspAccountDao.selectById(pspAccountId);
        return account == null ? null : account.getApiSecret();
    }
}
