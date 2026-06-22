package com.gk.psp.log;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.gk.common.enums.BizTypeEnum;
import com.gk.common.utils.BizKeyUtils;
import com.gk.openapi.security.ApiReqContext;
import com.gk.openapi.security.ApiReqContextHolder;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.psp.dispatch.PspPayDispatchResult;
import com.gk.psp.dispatch.PspPayoutDispatchResult;
import com.gk.psp.entity.PspRequestLogEntity;
import com.gk.psp.query.PspOrderQueryResult;
import com.gk.psp.route.PspRouteResult;
import com.gk.psp.service.PspRequestLogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * PSP 出站请求日志记录器。
 * <p>
 * 平台主动请求 PSP 的场景包括：代收下单、代付提交、代收查单、代付查单。
 * 本类负责把这些请求的 URL、方法、请求体、响应体、耗时、成功状态和错误信息写入
 * {@code psp_request_log}，便于排查上游调用、对账和异常补偿。
 * <p>
 * 日志写入通过异步线程池执行，避免 PSP 调用主链路被日志落库阻塞。
 */
@Slf4j
@Component
public class PspRequestLogger {
    private static final String DEFAULT_HTTP_METHOD = "POST";

    private final PspRequestLogService pspRequestLogService;
    private final Executor pspRequestLogExecutor;

    public PspRequestLogger(
            PspRequestLogService pspRequestLogService,
            @Qualifier("pspRequestLogExecutor") Executor pspRequestLogExecutor
    ) {
        this.pspRequestLogService = pspRequestLogService;
        this.pspRequestLogExecutor = pspRequestLogExecutor;
    }

    /**
     * 记录代收下单适配器正常返回的请求日志。
     * <p>
     * 注意：适配器正常返回不等于 PSP 业务成功，result.success 会被写入 success 字段。
     */
    public void paySubmitSuccess(PayOrderEntity order, PspRouteResult route, PspPayDispatchResult result, long costMs) {
        PspRequestLogEntity entity = baseEntity(order, route, result);
        entity.setRequestUrl(StringUtils.defaultIfBlank(result.getRequestUrl(), defaultPayRequestUrl(route)));
        entity.setHttpMethod(StringUtils.defaultIfBlank(result.getHttpMethod(), DEFAULT_HTTP_METHOD));
        entity.setRequestHeadersJson(sanitizeText(result.getRequestHeadersJson()));
        entity.setRequestBody(sanitizeText(StringUtils.defaultIfBlank(result.getRequestBody(), toJson(payRequestSnapshot(order, route)))));
        entity.setResponseStatus(result.getResponseStatus());
        entity.setResponseBody(sanitizeText(result.getRawResponseJson()));
        entity.setSuccess(result.isSuccess() ? 1 : 0);
        entity.setErrorCode(result.getErrorCode());
        entity.setErrorMsg(StringUtils.left(result.getErrorMessage(), 1024));
        entity.setCostMs(costMs);
        submit(entity);
    }

    /**
     * 记录代收下单过程中抛出异常的请求日志。
     * <p>
     * 例如适配器缺失、网络异常、签名构造异常等，这类场景没有完整 PSP 响应。
     */
    public void paySubmitFailed(PayOrderEntity order, PspRouteResult route, Throwable throwable, long costMs) {
        PspRequestLogEntity entity = baseEntity(order, route, null);
        entity.setRequestUrl(defaultPayRequestUrl(route));
        entity.setHttpMethod(DEFAULT_HTTP_METHOD);
        entity.setRequestBody(sanitizeText(toJson(payRequestSnapshot(order, route))));
        entity.setSuccess(0);
        entity.setErrorCode("PSP_SUBMIT_FAILED");
        entity.setErrorMsg(StringUtils.left(throwable == null ? null : throwable.getMessage(), 1024));
        entity.setCostMs(costMs);
        submit(entity);
    }

    /**
     * 记录代付提交适配器正常返回的请求日志。
     * <p>
     * 代付通常先冻结商户余额，再提交 PSP；该日志可用于追踪冻结后是否成功请求上游。
     */
    public void payoutSubmitSuccess(PayoutOrderEntity order, PspRouteResult route, PspPayoutDispatchResult result, long costMs) {
        PspRequestLogEntity entity = baseEntity(order, route, result);
        entity.setRequestUrl(StringUtils.defaultIfBlank(result.getRequestUrl(), defaultPayoutRequestUrl(route)));
        entity.setHttpMethod(StringUtils.defaultIfBlank(result.getHttpMethod(), DEFAULT_HTTP_METHOD));
        entity.setRequestHeadersJson(sanitizeText(result.getRequestHeadersJson()));
        entity.setRequestBody(sanitizeText(StringUtils.defaultIfBlank(result.getRequestBody(), toJson(payoutRequestSnapshot(order, route)))));
        entity.setResponseStatus(result.getResponseStatus());
        entity.setResponseBody(sanitizeText(result.getRawResponseJson()));
        entity.setSuccess(result.isSuccess() ? 1 : 0);
        entity.setErrorCode(result.getErrorCode());
        entity.setErrorMsg(StringUtils.left(result.getErrorMessage(), 1024));
        entity.setCostMs(costMs);
        submit(entity);
    }

    /**
     * 记录代付提交过程中抛出异常的请求日志。
     * <p>
     * 上层会根据异常决定订单失败和冻结释放，本方法只负责保存上游调用失败证据。
     */
    public void payoutSubmitFailed(PayoutOrderEntity order, PspRouteResult route, Throwable throwable, long costMs) {
        PspRequestLogEntity entity = baseEntity(order, route, null);
        entity.setRequestUrl(defaultPayoutRequestUrl(route));
        entity.setHttpMethod(DEFAULT_HTTP_METHOD);
        entity.setRequestBody(sanitizeText(toJson(payoutRequestSnapshot(order, route))));
        entity.setSuccess(0);
        entity.setErrorCode("PSP_PAYOUT_SUBMIT_FAILED");
        entity.setErrorMsg(StringUtils.left(throwable == null ? null : throwable.getMessage(), 1024));
        entity.setCostMs(costMs);
        submit(entity);
    }

    /**
     * 记录代收主动查单正常返回的请求日志。
     * <p>
     * 查单任务用于补偿 PSP 回调丢失或延迟的场景。
     */
    public void payQuerySuccess(PayOrderEntity order, PspRouteResult route, PspOrderQueryResult result, long costMs) {
        PspRequestLogEntity entity = baseEntity(order, route, null);
        applyQueryResult(entity, result, defaultPayQueryUrl(route), costMs);
        submit(entity);
    }

    /**
     * 记录代收主动查单异常日志。
     */
    public void payQueryFailed(PayOrderEntity order, PspRouteResult route, Throwable throwable, long costMs) {
        PspRequestLogEntity entity = baseEntity(order, route, null);
        entity.setRequestUrl(defaultPayQueryUrl(route));
        entity.setHttpMethod(DEFAULT_HTTP_METHOD);
        entity.setRequestBody(sanitizeText(toJson(payRequestSnapshot(order, route))));
        entity.setSuccess(0);
        entity.setErrorCode("PSP_PAY_QUERY_FAILED");
        entity.setErrorMsg(StringUtils.left(throwable == null ? null : throwable.getMessage(), 1024));
        entity.setCostMs(costMs);
        submit(entity);
    }

    /**
     * 记录代付主动查单正常返回的请求日志。
     * <p>
     * 查单结果后续会走和 PSP 回调类似的状态更新、账务扣冻结或解冻逻辑。
     */
    public void payoutQuerySuccess(PayoutOrderEntity order, PspRouteResult route, PspOrderQueryResult result, long costMs) {
        PspRequestLogEntity entity = baseEntity(order, route, null);
        applyQueryResult(entity, result, defaultPayoutQueryUrl(route), costMs);
        submit(entity);
    }

    /**
     * 记录代付主动查单异常日志。
     */
    public void payoutQueryFailed(PayoutOrderEntity order, PspRouteResult route, Throwable throwable, long costMs) {
        PspRequestLogEntity entity = baseEntity(order, route, null);
        entity.setRequestUrl(defaultPayoutQueryUrl(route));
        entity.setHttpMethod(DEFAULT_HTTP_METHOD);
        entity.setRequestBody(sanitizeText(toJson(payoutRequestSnapshot(order, route))));
        entity.setSuccess(0);
        entity.setErrorCode("PSP_PAYOUT_QUERY_FAILED");
        entity.setErrorMsg(StringUtils.left(throwable == null ? null : throwable.getMessage(), 1024));
        entity.setCostMs(costMs);
        submit(entity);
    }

    /**
     * 构建代收 PSP 请求日志基础字段。
     * <p>
     * 包括租户、商户、PSP、业务单号、平台请求号、PSP 请求号、PSP 订单号和 traceId。
     */
    private PspRequestLogEntity baseEntity(PayOrderEntity order, PspRouteResult route, PspPayDispatchResult result) {
        PspRequestLogEntity entity = new PspRequestLogEntity();
        entity.setTenantId(order == null ? null : order.getTenantId());
        entity.setMerchantId(order == null ? null : order.getMerchantId());
        entity.setPspId(route == null ? null : route.getPspId());
        entity.setPspCode(route == null ? null : route.getPspCode());
        entity.setBizType(BizTypeEnum.PAY_ORDER.code());
        entity.setBizId(order == null ? null : order.getId());
        entity.setBizNo(order == null ? null : order.getPayOrderNo());
        String pspRequestNo = result == null ? null : result.getPspRequestNo();
        entity.setRequestNo(StringUtils.defaultIfBlank(pspRequestNo, BizKeyUtils.genPspRequestNo()));
        entity.setPspRequestNo(pspRequestNo);
        entity.setPspOrderNo(result == null ? null : result.getPspOrderNo());
        ApiReqContext context = ApiReqContextHolder.get();
        entity.setTraceId(context == null ? null : context.getTraceId());
        return entity;
    }

    /**
     * 构建代付 PSP 请求日志基础字段。
     */
    private PspRequestLogEntity baseEntity(PayoutOrderEntity order, PspRouteResult route, PspPayoutDispatchResult result) {
        PspRequestLogEntity entity = new PspRequestLogEntity();
        entity.setTenantId(order == null ? null : order.getTenantId());
        entity.setMerchantId(order == null ? null : order.getMerchantId());
        entity.setPspId(route == null ? null : route.getPspId());
        entity.setPspCode(route == null ? null : route.getPspCode());
        entity.setBizType(BizTypeEnum.PAYOUT_ORDER.code());
        entity.setBizId(order == null ? null : order.getId());
        entity.setBizNo(order == null ? null : order.getPayoutOrderNo());
        String pspRequestNo = result == null ? null : result.getPspRequestNo();
        entity.setRequestNo(StringUtils.defaultIfBlank(pspRequestNo, BizKeyUtils.genPspRequestNo()));
        entity.setPspRequestNo(pspRequestNo);
        entity.setPspOrderNo(result == null ? null : result.getPspOrderNo());
        ApiReqContext context = ApiReqContextHolder.get();
        entity.setTraceId(context == null ? null : context.getTraceId());
        return entity;
    }

    /**
     * 构建代收请求快照。
     * <p>
     * 当适配器没有提供原始请求体时，使用该快照作为日志请求体，至少保留订单、金额、
     * 币种、支付方式和 PSP 账户等关键排查信息。
     */
    private Map<String, Object> payRequestSnapshot(PayOrderEntity order, PspRouteResult route) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("pay_order_no", order == null ? null : order.getPayOrderNo());
        snapshot.put("merchant_order_no", order == null ? null : order.getMerchantOrderNo());
        snapshot.put("amount", order == null ? null : order.getAmount());
        snapshot.put("currency", order == null ? null : order.getCurrency());
        snapshot.put("method_code", order == null ? null : order.getMethodCode());
        snapshot.put("psp_method_code", route == null ? null : route.getPspMethodCode());
        snapshot.put("psp_account_no", route == null ? null : route.getPspAccountNo());
        return snapshot;
    }

    /**
     * 构建代付请求快照。
     * <p>
     * 只记录收款账号掩码，不记录完整收款账号，避免敏感信息进入日志。
     */
    private Map<String, Object> payoutRequestSnapshot(PayoutOrderEntity order, PspRouteResult route) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("payout_order_no", order == null ? null : order.getPayoutOrderNo());
        snapshot.put("merchant_order_no", order == null ? null : order.getMerchantOrderNo());
        snapshot.put("amount", order == null ? null : order.getAmount());
        snapshot.put("currency", order == null ? null : order.getCurrency());
        snapshot.put("method_code", order == null ? null : order.getMethodCode());
        snapshot.put("psp_method_code", route == null ? null : route.getPspMethodCode());
        snapshot.put("psp_account_no", route == null ? null : route.getPspAccountNo());
        snapshot.put("payee_name", order == null ? null : order.getPayeeName());
        snapshot.put("payee_account_no", order == null ? null : order.getPayeeAccountNo());
        snapshot.put("payee_wallet_type", order == null ? null : order.getPayeeWalletType());
        return snapshot;
    }

    /**
     * 生成默认代收下单 URL。
     * <p>
     * 适配器没有明确返回 requestUrl 时，用 PSP baseUrl 拼出一个默认地址便于排查。
     */
    private String defaultPayRequestUrl(PspRouteResult route) {
        if (route == null || StringUtils.isBlank(route.getPspBaseUrl())) {
            return null;
        }
        return StringUtils.removeEnd(route.getPspBaseUrl(), "/") + "/open-api/create-pay-order";
    }

    /**
     * 生成默认代付提交 URL。
     */
    private String defaultPayoutRequestUrl(PspRouteResult route) {
        if (route == null || StringUtils.isBlank(route.getPspBaseUrl())) {
            return null;
        }
        return StringUtils.removeEnd(route.getPspBaseUrl(), "/") + "/open-api/create-payout-order";
    }

    /**
     * 将查单结果填充到 PSP 请求日志实体。
     */
    private void applyQueryResult(PspRequestLogEntity entity, PspOrderQueryResult result, String defaultUrl, long costMs) {
        entity.setRequestNo(StringUtils.defaultIfBlank(result.getPspRequestNo(), entity.getRequestNo()));
        entity.setPspRequestNo(result.getPspRequestNo());
        entity.setPspOrderNo(result.getPspOrderNo());
        entity.setRequestUrl(StringUtils.defaultIfBlank(result.getRequestUrl(), defaultUrl));
        entity.setHttpMethod(StringUtils.defaultIfBlank(result.getHttpMethod(), DEFAULT_HTTP_METHOD));
        entity.setRequestHeadersJson(sanitizeText(result.getRequestHeadersJson()));
        entity.setRequestBody(sanitizeText(result.getRequestBody()));
        entity.setResponseStatus(result.getResponseStatus());
        entity.setResponseBody(sanitizeText(result.getRawResponseJson()));
        entity.setSuccess(result.isSuccess() ? 1 : 0);
        entity.setErrorCode(result.getErrorCode());
        entity.setErrorMsg(StringUtils.left(result.getErrorMessage(), 1024));
        entity.setCostMs(costMs);
    }

    /**
     * 生成默认代收查单 URL。
     */
    private String defaultPayQueryUrl(PspRouteResult route) {
        if (route == null || StringUtils.isBlank(route.getPspBaseUrl())) {
            return null;
        }
        return StringUtils.removeEnd(route.getPspBaseUrl(), "/") + "/open-api/query-pay-order";
    }

    /**
     * 生成默认代付查单 URL。
     */
    private String defaultPayoutQueryUrl(PspRouteResult route) {
        if (route == null || StringUtils.isBlank(route.getPspBaseUrl())) {
            return null;
        }
        return StringUtils.removeEnd(route.getPspBaseUrl(), "/") + "/open-api/query-payout-order";
    }

    /**
     * 异步提交 PSP 请求日志。
     * <p>
     * 日志失败只记录 warn，不影响订单主流程。
     */
    private void submit(PspRequestLogEntity entity) {
        try {
            pspRequestLogExecutor.execute(() -> pspRequestLogService.record(entity));
        } catch (Exception ex) {
            log.warn("Submit PSP request log failed: {}", ex.getMessage());
        }
    }

    /**
     * 对象转 JSON。
     * <p>
     * 转换失败时返回 null，避免日志辅助字段影响主流程。
     */
    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return JSON.toJSONString(value, JSONWriter.Feature.WriteMapNullValue);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 脱敏请求/响应文本。
     * <p>
     * 对常见密钥、token、签名字段做掩码，降低 PSP 请求日志泄露敏感信息的风险。
     */
    private String sanitizeText(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        return value
                .replaceAll("(?i)(\"(?:api_secret|secret|password|token|access_token|sign|signature|api_key)\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")
                .replaceAll("(?i)((?:api_secret|secret|password|token|access_token|sign|signature|api_key)=)[^&\\s]*", "$1***");
    }
}
