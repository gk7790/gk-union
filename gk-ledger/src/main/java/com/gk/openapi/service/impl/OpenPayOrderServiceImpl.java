package com.gk.openapi.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.gk.common.constant.Constant;
import com.gk.common.utils.BizKeyUtils;
import com.gk.infra.utils.AsynUtils;
import com.gk.merchant.entity.MerchantAppEntity;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.merchant.enums.MerchantAppEnvEnum;
import com.gk.openapi.dto.PayOrderCreateRequest;
import com.gk.openapi.dto.PayOrderResponse;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.openapi.security.ApiReqContext;
import com.gk.openapi.security.ApiReqContextHolder;
import com.gk.openapi.service.OpenPayOrderService;
import com.gk.openapi.util.ApiAmountUtils;
import com.gk.common.enums.OrderSourceEnum;
import com.gk.payment.enums.PayOrderStatusEnum;
import com.gk.payment.dao.PayOrderDao;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.enums.SettleStatusEnum;
import com.gk.payment.fee.MerchantFeeResult;
import com.gk.payment.plan.PayinPlan;
import com.gk.payment.plan.PayinPlanService;
import com.gk.payment.notify.MerchantOrderNotifyStatusService;
import com.gk.payment.service.MerchantFeeRuleService;
import com.gk.payment.service.OrderStatusLogService;
import com.gk.psp.dispatch.PspPayDispatchResult;
import com.gk.psp.dispatch.PspPayDispatchService;
import com.gk.psp.route.PspRouteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 商户 OpenAPI 代收订单服务实现。
 * <p>
 * 本类负责商户代收下单和查单的应用层编排：校验请求、处理商户订单号幂等、
 * 计算商户手续费、选择 PSP 路由、提交 PSP 下单，并把订单状态变化写入状态日志。
 * 真正的 PSP 协议差异由 {@link PspPayDispatchService} 和 PSP adapter 承接。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenPayOrderServiceImpl implements OpenPayOrderService {
    private final PayOrderDao payOrderDao;
    private final MerchantFeeRuleService merchantFeeRuleService;
    private final PayinPlanService payinPlanService;
    private final PspPayDispatchService pspPayDispatchService;
    private final MerchantOrderNotifyStatusService merchantOrderNotifyStatusService;
    private final OrderStatusLogService orderStatusLogService;

    /**
     * 创建代收订单。
     * <p>
     * 主流程：检查幂等 -> 校验金额和商户应用权限 -> 组装平台订单 -> 计算商户手续费 ->
     * 落库 -> 提交 PSP -> 返回支付链接和订单状态。
     */
    @Override
    public PayOrderResponse create(PayOrderCreateRequest request) {
        if (request == null) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST);
        }
        PayCreateStepTimer timer = PayCreateStepTimer.start(request.getMerchantOrderId());
        // 先按商户订单号查重，保证商户重复请求时能按幂等规则返回同一笔平台订单。
        PayOrderEntity existed = findByMerchantOrderNo(StringUtils.trim(request.getMerchantOrderId()));
        timer.mark("idempotency_check");

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ApiErrorCode.INVALID_AMOUNT);
        }
        // 校验金额小数位。
        validateAmountScale(request.getAmount());

        // OpenApiAuthFilter 已经把 app_id 解析出的租户、商户、应用上下文放入 ThreadLocal。
        ApiReqContext context = ApiReqContextHolder.get();
        MerchantEntity merchant = context.getMerchant();

        // 代收不再按国家匹配；countryCode 只作为订单快照字段，有传则保存，没有则留空。
        String currency = StringUtils.defaultIfBlank(request.getCurrency(), merchant.getDefaultCurrency());
        String countryCode = StringUtils.defaultIfBlank(request.getCountryCode(), merchant.getCountryCode());
        if (StringUtils.isBlank(currency)) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "currency is required");
        }
        String normalizedCurrency = currency.toUpperCase(Locale.ROOT);
        String normalizedCountryCode = StringUtils.defaultString(countryCode).toUpperCase(Locale.ROOT);
        String normalizedMethod = request.getMethodCode().toUpperCase(Locale.ROOT);
        validateMerchantAppAccess(context.getMerchantApp(), normalizedCurrency, normalizedMethod);
        timer.mark("validate_request");

        if (existed != null) {
            // 同一商户订单号再次请求时，核心请求参数必须一致，否则按重复请求冲突处理。
            validateIdempotentRequest(existed, request, normalizedCurrency, normalizedMethod);
            timer.mark("return_idempotent_order");
            PayOrderResponse response = toResponse(existed);
            timer.log("SUCCESS", existed);
            return response;
        }

        // 生成平台代收订单，订单号、租户、商户、应用等信息均来自认证上下文和平台规则。
        PayOrderEntity entity = new PayOrderEntity();
        entity.setTenantId(context.getTenantId());
        entity.setMerchantId(context.getMerchantId());
        entity.setMerchantNo(context.getMerchantNo());
        entity.setMerchantAppId(context.getMerchantAppId());
        entity.setAppId(context.getAppId());
        entity.setPayOrderNo(BizKeyUtils.genPayOrderNo());
        entity.setMerchantOrderNo(StringUtils.trim(request.getMerchantOrderId()));
        entity.setIdempotencyKey(StringUtils.trim(request.getMerchantOrderId()));
        entity.setOrderSource(OrderSourceEnum.API.code());
        entity.setCountryCode(normalizedCountryCode);
        entity.setCurrency(normalizedCurrency);
        entity.setMethodCode(normalizedMethod);
        entity.setAmount(request.getAmount());
        entity.setPaidAmount(BigDecimal.ZERO);
        entity.setPspFeeAmount(BigDecimal.ZERO);
        entity.setSubject(request.getSubject());
        entity.setDescription(request.getDescription());
        entity.setClientIp(context.getClientIp());
        entity.setPayerJson(toJson(request.getPayer()));
        entity.setNotifyUrl(request.getNotifyUrl());
        entity.setReturnUrl(request.getReturnUrl());
        entity.setMerchantNotifyStatus(merchantOrderNotifyStatusService.initialStatus(entity.getNotifyUrl()));
        entity.setStatus(PayOrderStatusEnum.CREATED.code());
        entity.setSettleStatus(SettleStatusEnum.PENDING.code());
        entity.setQueryCount(0);
        entity.setExtraJson(toJson(request.getExtra()));
        entity.setVersion(0);
        timer.mark("build_order");

        PayinPlan payinPlan = null;
        if (isTestApp(context.getMerchantApp())) {
            // 测试 App 不请求真实 PSP，只计算商户侧费率并走沙箱成功流程。
            applyMerchantFee(entity);
            timer.mark("resolve_payment_plan");
            timer.mark("apply_payment_plan");
        } else {
            // 正式 App 先解析完整 PayinPlan，确保商户费率、PSP 路由、PSP 成本费率都已准备好。
            payinPlan = payinPlanService.resolve(entity);
            timer.mark("resolve_payment_plan");
            // 解析结果立即写入订单快照，后续即使配置变化，也不影响这笔订单的审计口径。
            applyPayinPlan(entity, payinPlan);
            timer.mark("apply_payment_plan");
        }

        // 先落平台订单再请求 PSP，避免 PSP 已受理但平台没有订单记录。
        boolean created = insertOrder(entity);
        timer.mark("insert_order");
        if (!created) {
            PayOrderResponse response = toResponse(entity);
            timer.log("SUCCESS", entity);
            return response;
        }
        if (isTestApp(context.getMerchantApp())) {
            submitToSandbox(entity);
            timer.mark("submit_sandbox");
            PayOrderResponse response = toResponse(entity);
            timer.log("SUCCESS", entity);
            return response;
        }
        // 提交上游 PSP 成功后，订单进入 PROCESSING，等待 PSP 回调或查单补偿推进终态。
        try {
            submitToPsp(entity, payinPlan);
            timer.mark("submit_psp");
        } catch (ApiException ex) {
            timer.mark("submit_psp_failed");
            timer.log("FAILED:" + ex.getErrorCode().name(), entity);
            throw ex;
        } catch (RuntimeException ex) {
            timer.mark("submit_psp_failed");
            timer.log("FAILED:" + ex.getClass().getSimpleName(), entity);
            throw ex;
        }
        PayOrderResponse response = toResponse(entity);
        timer.log("SUCCESS", entity);
        return response;
    }

    /**
     * 插入代收订单并记录创建状态日志。
     * <p>
     * 如果并发请求触发唯一键冲突，会重新查询已有订单并按幂等规则复用。
     */
    private boolean insertOrder(PayOrderEntity entity) {
        try {
            payOrderDao.insert(entity);
            recordStatusChange(entity, null, entity.getStatus(), "ORDER_CREATED", null, "MERCHANT");
            return true;
        } catch (DuplicateKeyException ex) {
            PayOrderEntity existed = findByMerchantOrderNo(entity.getMerchantOrderNo());
            if (existed != null) {
                validateIdempotentEntity(existed, entity);
                copyOrder(existed, entity);
                return false;
            }
            throw ex;
        }
    }

    /**
     * 提交代收订单到 PSP。
     * <p>
     * 这里完成 PSP 路由、PSP 手续费计算、适配器调用和订单状态更新。
     * 出现异常时会把订单标记为 FAILED，并继续向上抛出 OpenAPI 错误。
     *
     * @param order 订单
     */
    private void submitToPsp(PayOrderEntity order, PayinPlan payinPlan) {
        try {
            if (payinPlan == null || payinPlan.getRoute() == null) {
                throw new ApiException(ApiErrorCode.SERVICE_NOT_READY, "Payin plan is not resolved");
            }
            // PSP 提交必须使用下单前已经解析并落库的路由，避免落单后再次选路由导致订单快照不一致。
            PspRouteResult route = payinPlan.getRoute();

            // 调用 PSP 分发服务，真正的 HTTP 协议由具体 PSP adapter 处理。
            PspPayDispatchResult dispatchResult = pspPayDispatchService.dispatch(order, route);

            applyDispatchResult(order, dispatchResult);

            payOrderDao.updateById(order);
        } catch (ApiException ex) {
            if (order != null) {
                markFailed(order, ex.getMessage());
            }
            throw ex;
        } catch (Exception ex) {
            log.error("OpenAPI pay submit failed, payOrderNo={}, merchantOrderNo={}",
                    order == null ? null : order.getPayOrderNo(),
                    order == null ? null : order.getMerchantOrderNo(),
                    ex);
            if (order != null) {
                markFailed(order, ApiErrorCode.SYSTEM_ERROR.getMessage());
            }
            throw new ApiException(ApiErrorCode.SYSTEM_ERROR, ex);
        }
    }

    private void submitToSandbox(PayOrderEntity order) {
        String fromStatus = order.getStatus();
        order.setPspRequestNo(BizKeyUtils.genPspRequestNo());
        order.setPspCode(Constant.SANDBOX);
        order.setPspOrderNo(Constant.SANDBOX + "_" + order.getPayOrderNo());
        order.setPspPayUrl("/sandbox/pay/" + order.getPayOrderNo());
        order.setPspStatus(PayOrderStatusEnum.PROCESSING.code());
        order.setPspRawStatus(PayOrderStatusEnum.PROCESSING.code());
        order.setStatus(PayOrderStatusEnum.PROCESSING.code());
        order.setNextQueryAt(null);
        payOrderDao.updateById(order);
        recordStatusChange(order, fromStatus, order.getStatus(), "SANDBOX_SUBMIT", null, "SYSTEM");
    }

    private boolean isTestApp(MerchantAppEntity app) {
        return app != null && MerchantAppEnvEnum.TEST.code().equals(app.getAppEnv());
    }

    /**
     * 保存 PSP 路由结果到订单。
     * <p>
     * 后续 PSP 回调、主动查单和问题排查都依赖这些 PSP 标识和账户信息。
     *
     * @param entity 订单
     * @param route 路由
     */
    private void applyRoute(PayOrderEntity entity, PspRouteResult route) {
        entity.setRouteRuleId(route.getRouteRuleId());
        entity.setRouteGroupId(route.getRouteGroupId());
        entity.setRouteChannelId(route.getRouteChannelId());
        entity.setRouteSnapshotJson(routeSnapshotJson(entity, route));
        entity.setPspId(route.getPspId());
        entity.setPspCode(route.getPspCode());
        entity.setPspMethodId(route.getPspMethodId());
        entity.setPspMethodCode(route.getPspMethodCode());
        entity.setPspAccountId(route.getPspAccountId());
        entity.setPspAccountNo(route.getPspAccountNo());
    }

    /**
     * 应用 PSP 下单返回结果。
     * <p>
     * 适配器返回成功表示 PSP 已受理，订单进入 PROCESSING；
     * 适配器返回失败表示 PSP 明确拒绝，订单直接置为 FAILED。
     *
     * @param entity 订单
     * @param result 请求结果
     */
    private void applyDispatchResult(PayOrderEntity entity, PspPayDispatchResult result) {
        String fromStatus = entity.getStatus();
        entity.setPspRequestNo(result.getPspRequestNo());
        entity.setPspOrderNo(result.getPspOrderNo());
        entity.setPspPayUrl(result.getPayUrl());
        entity.setPspPayParamsJson(result.getPayParamsJson());
        entity.setPspRawStatus(result.getRawStatus());
        if (result.isSuccess()) {
            entity.setStatus(PayOrderStatusEnum.PROCESSING.code());
            entity.setPspStatus(PayOrderStatusEnum.PROCESSING.code());
            entity.setSubmittedAt(Instant.now());
            // 设置下一次主动查单时间，兜底处理 PSP 回调丢失或延迟。
            entity.setNextQueryAt(Instant.now().plusSeconds(60));
            recordStatusChange(entity, fromStatus, entity.getStatus(), "PSP_SUBMIT", null, "SYSTEM");
            return;
        }
        entity.setStatus(PayOrderStatusEnum.FAILED.code());
        entity.setPspStatus(PayOrderStatusEnum.FAILED.code());
        String reason = StringUtils.defaultIfBlank(
                result.getErrorMessage(),
                StringUtils.defaultIfBlank(result.getResponseMessage(), "PSP submit failed")
        );
        entity.setStatusReason(reason);
        recordStatusChange(entity, fromStatus, entity.getStatus(), "PSP_SUBMIT_FAILED", reason, "SYSTEM");
    }

    /**
     * 将代收订单标记为失败并记录状态日志。
     *
     * @param entity 订单
     * @param reason 失败原因
     */
    private void markFailed(PayOrderEntity entity, String reason) {
        String fromStatus = entity.getStatus();
        entity.setStatus(PayOrderStatusEnum.FAILED.code());
        String message = StringUtils.defaultIfBlank(reason, "Pay order failed");
        entity.setStatusReason(message);
        payOrderDao.updateById(entity);
        recordStatusChange(entity, fromStatus, entity.getStatus(), "ORDER_FAILED", message, "SYSTEM");
    }

    /**
     * 记录代收订单状态变更。
     */
    private void recordStatusChange(PayOrderEntity entity,
                                    String fromStatus,
                                    String toStatus,
                                    String eventType,
                                    String reason,
                                    String operatorType) {
        AsynUtils.execute("Order status log", () -> orderStatusLogService.recordChange(
                    "PAY",
                    entity.getTenantId(),
                    entity.getMerchantId(),
                    entity.getId(),
                    entity.getPayOrderNo(),
                    fromStatus,
                    toStatus,
                    eventType,
                    reason,
                    operatorType,
                    ApiReqContextHolder.getAppId(),
                    entity.getMerchantOrderNo(),
                    traceId()
            ));
    }

    /**
     * 获取当前 OpenAPI 请求 traceId。
     */
    private String traceId() {
        ApiReqContext context = ApiReqContextHolder.get();
        return context == null ? null : context.getTraceId();
    }

    private static final class PayCreateStepTimer {
        private final String merchantOrderNo;
        private final long startNanos;
        private long lastNanos;
        private final StringBuilder steps = new StringBuilder();

        private PayCreateStepTimer(String merchantOrderNo) {
            this.merchantOrderNo = StringUtils.trimToNull(merchantOrderNo);
            this.startNanos = System.nanoTime();
            this.lastNanos = this.startNanos;
        }

        private static PayCreateStepTimer start(String merchantOrderNo) {
            return new PayCreateStepTimer(merchantOrderNo);
        }

        private void mark(String step) {
            long now = System.nanoTime();
            if (!steps.isEmpty()) {
                steps.append(", ");
            }
            steps.append(step).append('=').append(toMillis(now - lastNanos)).append("ms");
            lastNanos = now;
        }

        private void log(String result, PayOrderEntity order) {
            long totalMs = toMillis(System.nanoTime() - startNanos);
            ApiReqContext context = ApiReqContextHolder.get();
            log.info(
                    "OpenAPI pay create profile result={}, traceId={}, tenantId={}, merchantId={}, appId={}, merchantOrderNo={}, payOrderNo={}, status={}, total={}ms, steps=[{}]",
                    result,
                    context == null ? null : context.getTraceId(),
                    context == null ? null : context.getTenantId(),
                    context == null ? null : context.getMerchantId(),
                    context == null ? null : context.getAppId(),
                    order == null ? merchantOrderNo : StringUtils.defaultIfBlank(order.getMerchantOrderNo(), merchantOrderNo),
                    order == null ? null : order.getPayOrderNo(),
                    order == null ? null : order.getStatus(),
                    totalMs,
                    steps
            );
        }

        private static long toMillis(long nanos) {
            return Math.max(0L, nanos / 1_000_000L);
        }
    }

    /**
     * 将扩展 Map 转成 JSON。
     */
    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return JSON.toJSONString(value);
        } catch (Exception e) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Invalid JSON field");
        }
    }

    /**
     * 把已有订单字段复制到当前对象。
     * <p>
     * 用于并发幂等场景，调用方可继续用当前对象生成统一响应。
     */
    private void copyOrder(PayOrderEntity source, PayOrderEntity target) {
        target.setId(source.getId());
        target.setTenantId(source.getTenantId());
        target.setMerchantId(source.getMerchantId());
        target.setMerchantNo(source.getMerchantNo());
        target.setMerchantAppId(source.getMerchantAppId());
        target.setAppId(source.getAppId());
        target.setPayOrderNo(source.getPayOrderNo());
        target.setMerchantOrderNo(source.getMerchantOrderNo());
        target.setStatus(source.getStatus());
        target.setStatusReason(source.getStatusReason());
        target.setAmount(source.getAmount());
        target.setPaidAmount(source.getPaidAmount());
        target.setMerchantFeeAmount(source.getMerchantFeeAmount());
        target.setMerchantFeeRuleId(source.getMerchantFeeRuleId());
        target.setMerchantFeeSnapshotJson(source.getMerchantFeeSnapshotJson());
        target.setPspFeeAmount(source.getPspFeeAmount());
        target.setPspFeeRuleId(source.getPspFeeRuleId());
        target.setPspFeeSnapshotJson(source.getPspFeeSnapshotJson());
        target.setSettleAmount(source.getSettleAmount());
        target.setPaymentPlanCatalogId(source.getPaymentPlanCatalogId());
        target.setPaymentPlanVersion(source.getPaymentPlanVersion());
        target.setPaymentPlanBucketId(source.getPaymentPlanBucketId());
        target.setPaymentPlanRouteOptionId(source.getPaymentPlanRouteOptionId());
        target.setRouteRuleId(source.getRouteRuleId());
        target.setRouteGroupId(source.getRouteGroupId());
        target.setRouteChannelId(source.getRouteChannelId());
        target.setRouteSnapshotJson(source.getRouteSnapshotJson());
        target.setCurrency(source.getCurrency());
        target.setCountryCode(source.getCountryCode());
        target.setMethodCode(source.getMethodCode());
        target.setPspPayUrl(source.getPspPayUrl());
        target.setPspOrderNo(source.getPspOrderNo());
    }

    /**
     * 按平台代收订单号查询订单。
     */
    @Override
    public PayOrderResponse getByPayOrderNo(String payOrderNo) {
        PayOrderEntity entity = payOrderDao.selectOpenApiByPayOrderNo(
                ApiReqContextHolder.getTenantId(),
                ApiReqContextHolder.getMerchantId(),
                StringUtils.trim(payOrderNo)
        );
        return toResponse(entity);
    }

    /**
     * 按商户订单号查询订单。
     */
    @Override
    public PayOrderResponse getByMerchantOrderNo(String merchantOrderNo) {
        PayOrderEntity entity = findByMerchantOrderNo(StringUtils.trim(merchantOrderNo));
        return toResponse(entity);
    }

    /**
     * 构建商户 OpenAPI 查询基础条件。
     * <p>
     * 所有商户查单只允许访问当前 app_id 所属租户和商户的数据。
     *
     * @return 返回筛选条件
     */
    private PayOrderEntity findByMerchantOrderNo(String merchantOrderNo) {
        return payOrderDao.selectOpenApiByMerchantOrderNo(
                ApiReqContextHolder.getTenantId(),
                ApiReqContextHolder.getMerchantId(),
                merchantOrderNo
        );
    }

    /**
     * 计算商户手续费和结算金额。
     * <p>
     * 商户手续费影响入账拆分：订单成功后，结算金额进入商户待结算账户，
     * 手续费进入平台收入或成本相关账户。
     *
     * @param entity 订单
     */
    private void applyMerchantFee(PayOrderEntity entity) {
        MerchantFeeResult feeResult = merchantFeeRuleService.calculatePayin(entity);
        entity.setMerchantFeeAmount(feeResult.getMerchantFeeAmount());
        entity.setSettleAmount(feeResult.getSettleAmount());
        entity.setMerchantFeeRuleId(feeResult.getRule().getId());
        entity.setMerchantFeeSnapshotJson(feeResult.getSnapshotJson());
    }

    /**
     * 计算 PSP 手续费并保存规则快照。
     * <p>
     * PSP 手续费是平台对上游的成本，用于后续利润、对账和报表。
     *
     * @param entity 订单
     */
    private void applyPayinPlan(PayOrderEntity entity, PayinPlan plan) {
        entity.setPaymentPlanCatalogId(plan.getCatalogId());
        entity.setPaymentPlanVersion(plan.getCatalogVersion());
        entity.setPaymentPlanBucketId(plan.getBucketId());
        entity.setPaymentPlanRouteOptionId(plan.getRouteOptionId());

        entity.setMerchantFeeAmount(plan.getMerchantFeeAmount());
        entity.setSettleAmount(plan.getSettleAmount());
        entity.setMerchantFeeRuleId(plan.getMerchantFee().getRule().getId());
        entity.setMerchantFeeSnapshotJson(plan.getMerchantFee().getSnapshotJson());

        applyRoute(entity, plan.getRoute());

        entity.setPspFeeAmount(plan.getPspFeeAmount() == null ? BigDecimal.ZERO : plan.getPspFeeAmount());
        if (plan.getPspFee() != null && plan.getPspFee().getRule() != null) {
            entity.setPspFeeRuleId(plan.getPspFee().getRule().getId());
            entity.setPspFeeSnapshotJson(plan.getPspFee().getSnapshotJson());
        }
    }

    private String routeSnapshotJson(PayOrderEntity entity, PspRouteResult route) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("catalogId", entity.getPaymentPlanCatalogId());
        snapshot.put("catalogVersion", entity.getPaymentPlanVersion());
        snapshot.put("bucketId", entity.getPaymentPlanBucketId());
        snapshot.put("routeOptionId", entity.getPaymentPlanRouteOptionId());
        snapshot.put("routeRuleId", route.getRouteRuleId());
        snapshot.put("routeGroupId", route.getRouteGroupId());
        snapshot.put("routeChannelId", route.getRouteChannelId());
        snapshot.put("pspId", route.getPspId());
        snapshot.put("pspCode", route.getPspCode());
        snapshot.put("pspMethodId", route.getPspMethodId());
        snapshot.put("pspMethodCode", route.getPspMethodCode());
        snapshot.put("pspAccountId", route.getPspAccountId());
        snapshot.put("pspAccountNo", route.getPspAccountNo());
        snapshot.put("pspBankCode", route.getPspBankCode());
        return JSON.toJSONString(snapshot, JSONWriter.Feature.WriteMapNullValue);
    }

    /**
     * 校验重复商户订单号对应的请求参数是否一致。
     */
    private void validateIdempotentRequest(PayOrderEntity existed, PayOrderCreateRequest request, String currency, String methodCode) {
        if (existed.getAmount() == null || request.getAmount() == null
                || existed.getAmount().compareTo(request.getAmount()) != 0
                || !StringUtils.equalsIgnoreCase(existed.getCurrency(), currency)
                || !StringUtils.equalsIgnoreCase(existed.getMethodCode(), methodCode)
                || !StringUtils.equals(StringUtils.trimToEmpty(existed.getNotifyUrl()), StringUtils.trimToEmpty(request.getNotifyUrl()))) {
            throw new ApiException(ApiErrorCode.DUPLICATE_REQUEST, "merchant_order_id exists with different request parameters");
        }
    }

    /**
     * 校验并发插入冲突后查到的已有订单是否与当前订单一致。
     */
    private void validateIdempotentEntity(PayOrderEntity existed, PayOrderEntity entity) {
        if (existed.getAmount() == null || entity.getAmount() == null
                || existed.getAmount().compareTo(entity.getAmount()) != 0
                || !StringUtils.equalsIgnoreCase(existed.getCurrency(), entity.getCurrency())
                || !StringUtils.equalsIgnoreCase(existed.getMethodCode(), entity.getMethodCode())
                || !StringUtils.equals(StringUtils.trimToEmpty(existed.getNotifyUrl()), StringUtils.trimToEmpty(entity.getNotifyUrl()))) {
            throw new ApiException(ApiErrorCode.DUPLICATE_REQUEST, "merchant_order_id exists with different request parameters");
        }
    }

    /**
     * 校验当前商户应用是否允许使用指定币种和支付方式。
     */
    private void validateMerchantAppAccess(MerchantAppEntity app, String currency, String methodCode) {
        if (isNotAllowed(app == null ? null : app.getAllowedCurrencyJson(), currency)) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "currency is not allowed for app");
        }
        if (isNotAllowed(app == null ? null : app.getAllowedMethodJson(), methodCode)) {
            throw new ApiException(ApiErrorCode.UNSUPPORTED_METHOD, "method is not allowed for app");
        }
    }

    /**
     * 判断配置列表是否允许当前值。
     * <p>
     * 空配置表示不限制。
     */
    private boolean isNotAllowed(String jsonArray, String value) {
        if (StringUtils.isBlank(jsonArray)) {
            return false;
        }
        try {
            List<String> allowedValues = JSON.parseArray(jsonArray, String.class);
            return allowedValues.stream().noneMatch(item -> StringUtils.equalsIgnoreCase(item, value));
        } catch (Exception ex) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Invalid app allowed config");
        }
    }

    /**
     * 校验金额小数位。
     */
    private void validateAmountScale(BigDecimal amount) {
        if (amount.scale() > 8) {
            throw new ApiException(ApiErrorCode.INVALID_AMOUNT, "amount scale must be less than or equal to 8");
        }
    }

    /**
     * 转换代收订单为 OpenAPI 响应。
     */
    private PayOrderResponse toResponse(PayOrderEntity entity) {
        if (entity == null) {
            throw new ApiException(ApiErrorCode.ORDER_NOT_FOUND);
        }
        PayOrderResponse response = new PayOrderResponse();
        response.setSystemOrderId(entity.getPayOrderNo());
        response.setMerchantOrderId(entity.getMerchantOrderNo());
        response.setStatus(entity.getStatus());
        response.setStatusReason(entity.getStatusReason());
        response.setAmount(formatMoney(entity.getAmount(), entity.getCurrency()));
        response.setCurrency(entity.getCurrency());
        response.setCountryCode(entity.getCountryCode());
        response.setMethodCode(entity.getMethodCode());
        response.setPayUrl(entity.getPspPayUrl());
        return response;
    }

    /**
     * 按币种格式化金额输出。
     */
    private String formatMoney(BigDecimal value, String currency) {
        return value == null ? null : ApiAmountUtils.formatCurrencyAmount(value, currency);
    }
}
