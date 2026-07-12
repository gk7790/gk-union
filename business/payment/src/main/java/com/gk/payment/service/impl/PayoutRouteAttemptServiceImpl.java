package com.gk.payment.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.payment.dao.PayoutRouteAttemptDao;
import com.gk.payment.dto.PayoutRouteAttemptDTO;
import com.gk.payment.entity.PayoutRouteAttemptEntity;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.payment.service.PayoutRouteAttemptService;
import com.gk.psp.dispatch.PspPayoutDispatchResult;
import com.gk.psp.route.PspRouteResult;
import org.springframework.stereotype.Service;

@Service
public class PayoutRouteAttemptServiceImpl extends CrudServiceImpl<PayoutRouteAttemptDao, PayoutRouteAttemptEntity, PayoutRouteAttemptDTO> implements PayoutRouteAttemptService {

    @Override
    public void recordAttempt(PayoutRouteAttemptEntity attempt) {
        if (attempt == null) {
            return;
        }
        insert(attempt);
    }

    @Override
    public PayoutRouteAttemptEntity recordAttempt(PayoutOrderEntity order, PspRouteResult route, PspPayoutDispatchResult result) {
        if (order == null || order.getId() == null) {
            return null;
        }
        PayoutRouteAttemptEntity attempt = new PayoutRouteAttemptEntity();
        attempt.setTenantId(order.getTenantId());
        attempt.setPayoutOrderId(order.getId());
        attempt.setPayoutOrderNo(order.getPayoutOrderNo());
        attempt.setAttemptNo(nextAttemptNo(order));
        attempt.setRouteOptionId(order.getPaymentPlanRouteOptionId());
        attempt.setRouteRuleId(route == null ? order.getRouteRuleId() : route.getRouteRuleId());
        attempt.setRouteGroupId(route == null ? order.getRouteGroupId() : route.getRouteGroupId());
        attempt.setRouteChannelId(route == null ? order.getRouteChannelId() : route.getRouteChannelId());
        attempt.setPspId(route == null ? order.getPspId() : route.getPspId());
        attempt.setPspCode(route == null ? order.getPspCode() : route.getPspCode());
        attempt.setPspMethodId(route == null ? order.getPspMethodId() : route.getPspMethodId());
        attempt.setPspMethodCode(route == null ? order.getPspMethodCode() : route.getPspMethodCode());
        attempt.setPspAccountId(route == null ? order.getPspAccountId() : route.getPspAccountId());
        attempt.setPspAccountNo(route == null ? order.getPspAccountNo() : route.getPspAccountNo());
        attempt.setPspBankCode(route == null ? null : route.getPspBankCode());
        if (result != null) {
            attempt.setSubmitResultStatus(result.getSubmitResultStatus() == null ? null : result.getSubmitResultStatus().name());
            attempt.setErrorCode(result.getErrorCode());
            attempt.setErrorMessage(result.getErrorMessage());
            attempt.setPspRequestNo(result.getPspRequestNo());
            attempt.setPspOrderNo(result.getPspOrderNo());
            attempt.setRawStatus(result.getRawStatus());
            attempt.setResponseStatus(result.getResponseStatus());
            attempt.setResponseCode(result.getResponseCode());
            attempt.setResponseMessage(result.getResponseMessage());
            attempt.setRawResponseJson(result.getRawResponseJson());
        }
        insert(attempt);
        return attempt;
    }

    @Override
    public QueryWrapper<PayoutRouteAttemptEntity> getWrapper(DynMap params) {
        QueryWrapper<PayoutRouteAttemptEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long payoutOrderId = params.getLong("payoutOrderId", null);
        Long routeOptionId = params.getLong("routeOptionId", null);
        Long routeChannelId = params.getLong("routeChannelId", null);
        Long pspId = params.getLong("pspId", null);
        Long pspAccountId = params.getLong("pspAccountId", null);
        Integer attemptNo = params.containsKey("attemptNo") ? params.getInt("attemptNo") : null;
        Integer responseStatus = params.containsKey("responseStatus") ? params.getInt("responseStatus") : null;
        String payoutOrderNo = params.getStr("payoutOrderNo");
        String pspCode = params.getStr("pspCode");
        String pspBankCode = params.getStr("pspBankCode");
        String submitResultStatus = params.getStr("submitResultStatus");
        String errorCode = params.getStr("errorCode");
        String pspRequestNo = params.getStr("pspRequestNo");
        String pspOrderNo = params.getStr("pspOrderNo");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(payoutOrderId != null, "payout_order_id", payoutOrderId);
        wrapper.eq(routeOptionId != null, "route_option_id", routeOptionId);
        wrapper.eq(routeChannelId != null, "route_channel_id", routeChannelId);
        wrapper.eq(pspId != null, "psp_id", pspId);
        wrapper.eq(pspAccountId != null, "psp_account_id", pspAccountId);
        wrapper.eq(attemptNo != null, "attempt_no", attemptNo);
        wrapper.eq(responseStatus != null, "response_status", responseStatus);
        wrapper.eq(StrUtil.isNotBlank(payoutOrderNo), "payout_order_no", payoutOrderNo);
        wrapper.eq(StrUtil.isNotBlank(pspCode), "psp_code", pspCode);
        wrapper.eq(StrUtil.isNotBlank(pspBankCode), "psp_bank_code", pspBankCode);
        wrapper.eq(StrUtil.isNotBlank(submitResultStatus), "submit_result_status", submitResultStatus);
        wrapper.eq(StrUtil.isNotBlank(errorCode), "error_code", errorCode);
        wrapper.eq(StrUtil.isNotBlank(pspRequestNo), "psp_request_no", pspRequestNo);
        wrapper.eq(StrUtil.isNotBlank(pspOrderNo), "psp_order_no", pspOrderNo);
        return wrapper;
    }

    private int nextAttemptNo(PayoutOrderEntity order) {
        Integer maxAttemptNo = baseDao.selectMaxAttemptNo(order.getTenantId(), order.getId());
        return maxAttemptNo == null ? 1 : maxAttemptNo + 1;
    }
}
