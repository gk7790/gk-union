package com.gk.payment.outbox;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.payment.domain.error.PaymentErrorCode;
import com.gk.payment.domain.error.PaymentException;
import com.gk.payment.dao.PayoutOrderDao;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.payment.enums.PayoutOrderStatusEnum;
import com.gk.payment.psp.PayoutPspSubmitService;
import com.gk.psp.callback.support.PspCallbackUrlBuilder;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.dao.PspProviderDao;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.entity.PspMethodEntity;
import com.gk.psp.entity.PspProviderEntity;
import com.gk.psp.route.PspRouteResult;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayoutSubmitOutboxConsumer {
    private final PayoutOrderDao payoutOrderDao;
    private final PspProviderDao pspProviderDao;
    private final PspAccountDao pspAccountDao;
    private final PspMethodDao pspMethodDao;
    private final PspCallbackUrlBuilder callbackUrlBuilder;
    private final PayoutPspSubmitService payoutPspSubmitService;

    public void consume(String payloadJson) {
        PayoutSubmitOutboxPayload payload = JSON.parseObject(payloadJson, PayoutSubmitOutboxPayload.class);
        if (payload == null || StringUtils.isBlank(payload.payoutOrderNo())) {
            throw new PaymentException(PaymentErrorCode.INVALID_REQUEST, "Invalid payout submit outbox payload");
        }
        PayoutOrderEntity order = payoutOrderDao.selectOne(new QueryWrapper<PayoutOrderEntity>()
                .eq("tenant_id", payload.tenantId())
                .eq("payout_order_no", payload.payoutOrderNo())
                .last("limit 1"));
        if (order == null) {
            throw new PaymentException(PaymentErrorCode.INVALID_REQUEST, "Payout order not found: " + payload.payoutOrderNo());
        }
        if (shouldSkipPspSubmit(order)) {
            return;
        }

        PspRouteResult route = buildPspSubmitRoute(order);
        payoutPspSubmitService.freezeAndSubmit(order, route, PayoutPspSubmitService.SubmitContext.system(order.getAppId(), null));
    }

    private boolean shouldSkipPspSubmit(PayoutOrderEntity order) {
        return PayoutOrderStatusEnum.PROCESSING.code().equals(order.getStatus())
                || PayoutOrderStatusEnum.MANUAL_REVIEW.code().equals(order.getStatus())
                || PayoutOrderStatusEnum.SUCCESS.code().equals(order.getStatus())
                || PayoutOrderStatusEnum.FAILED.code().equals(order.getStatus())
                || PayoutOrderStatusEnum.CANCELLED.code().equals(order.getStatus());
    }

    private PspRouteResult buildPspSubmitRoute(PayoutOrderEntity order) {
        if (order.getPspId() == null || order.getPspAccountId() == null || StringUtils.isBlank(order.getPspCode())) {
            throw new PaymentException(PaymentErrorCode.SERVICE_NOT_READY, "PSP route snapshot is incomplete");
        }
        PspProviderEntity provider = pspProviderDao.selectById(order.getPspId());
        PspAccountEntity account = pspAccountDao.selectById(order.getPspAccountId());
        PspMethodEntity method = order.getPspMethodId() == null ? null : pspMethodDao.selectById(order.getPspMethodId());
        if (provider == null || account == null) {
            throw new PaymentException(PaymentErrorCode.SERVICE_NOT_READY, "PSP route snapshot is unavailable");
        }

        PspRouteResult route = new PspRouteResult();
        route.setRouteRuleId(order.getRouteRuleId());
        route.setRouteGroupId(order.getRouteGroupId());
        route.setRouteChannelId(order.getRouteChannelId());
        route.setPspId(order.getPspId());
        route.setPspCode(order.getPspCode());
        route.setPspBaseUrl(provider.getBaseUrl());
        route.setProviderConfigJson(provider.getConfigJson());
        route.setPspCallbackUrl(callbackUrlBuilder.payoutCallbackUrl(order.getPspAccountNo()));
        route.setPspMethodId(order.getPspMethodId());
        route.setPspMethodCode(order.getPspMethodCode());
        route.setMethodConfigJson(method == null ? null : method.getConfigJson());
        route.setPspAccountId(order.getPspAccountId());
        route.setPspAccountNo(order.getPspAccountNo());
        route.setPspAccountApiKey(account.getApiKey());
        route.setPspAccountApiSecret(account.getApiSecret());
        route.setAccountConfigJson(account.getConfigJson());
        route.setPspBankCode(pspBankCode(order.getRouteSnapshotJson()));
        return route;
    }

    private String pspBankCode(String routeSnapshotJson) {
        if (StringUtils.isBlank(routeSnapshotJson)) {
            return null;
        }
        try {
            JSONObject snapshot = JSON.parseObject(routeSnapshotJson);
            return snapshot == null ? null : StringUtils.trimToNull(snapshot.getString("pspBankCode"));
        } catch (Exception ex) {
            return null;
        }
    }

}
