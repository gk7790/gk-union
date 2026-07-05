package com.gk.payment.outbox;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.payment.dao.PayinOrderDao;
import com.gk.payment.entity.PayinOrderEntity;
import com.gk.payment.enums.PayinOrderStatusEnum;
import com.gk.payment.psp.PayinPspSubmitService;
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
public class PayinSubmitOutboxConsumer {
    private final PayinOrderDao payinOrderDao;
    private final PspProviderDao pspProviderDao;
    private final PspAccountDao pspAccountDao;
    private final PspMethodDao pspMethodDao;
    private final PspCallbackUrlBuilder callbackUrlBuilder;
    private final PayinPspSubmitService payinPspSubmitService;

    public void consume(String payloadJson) {
        PayinSubmitOutboxPayload payload = JSON.parseObject(payloadJson, PayinSubmitOutboxPayload.class);
        if (payload == null || StringUtils.isBlank(payload.payinOrderNo())) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Invalid payin submit outbox payload");
        }
        PayinOrderEntity order = payinOrderDao.selectOne(new QueryWrapper<PayinOrderEntity>()
                .eq("tenant_id", payload.tenantId())
                .eq("payin_order_no", payload.payinOrderNo())
                .last("limit 1"));
        if (order == null) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Payin order not found: " + payload.payinOrderNo());
        }
        if (shouldSkipPspSubmit(order)) {
            return;
        }

        PspRouteResult route = buildPspSubmitRoute(order);
        payinPspSubmitService.submit(order, route, PayinPspSubmitService.SubmitContext.system(order.getAppId(), null));
    }

    private boolean shouldSkipPspSubmit(PayinOrderEntity order) {
        return PayinOrderStatusEnum.PROCESSING.code().equals(order.getStatus())
                || PayinOrderStatusEnum.MANUAL_REVIEW.code().equals(order.getStatus())
                || PayinOrderStatusEnum.SUCCESS.code().equals(order.getStatus())
                || PayinOrderStatusEnum.FAILED.code().equals(order.getStatus())
                || PayinOrderStatusEnum.CLOSED.code().equals(order.getStatus());
    }

    private PspRouteResult buildPspSubmitRoute(PayinOrderEntity order) {
        if (order.getPspId() == null || order.getPspAccountId() == null || StringUtils.isBlank(order.getPspCode())) {
            throw new ApiException(ApiErrorCode.SERVICE_NOT_READY, "PSP route snapshot is incomplete");
        }
        PspProviderEntity provider = pspProviderDao.selectById(order.getPspId());
        PspAccountEntity account = pspAccountDao.selectById(order.getPspAccountId());
        PspMethodEntity method = order.getPspMethodId() == null ? null : pspMethodDao.selectById(order.getPspMethodId());
        if (provider == null || account == null) {
            throw new ApiException(ApiErrorCode.SERVICE_NOT_READY, "PSP route snapshot is unavailable");
        }

        PspRouteResult route = new PspRouteResult();
        route.setRouteRuleId(order.getRouteRuleId());
        route.setRouteGroupId(order.getRouteGroupId());
        route.setRouteChannelId(order.getRouteChannelId());
        route.setPspId(order.getPspId());
        route.setPspCode(order.getPspCode());
        route.setPspBaseUrl(provider.getBaseUrl());
        route.setProviderConfigJson(provider.getConfigJson());
        route.setPspCallbackUrl(callbackUrlBuilder.payinCallbackUrl(order.getPspAccountNo()));
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
