package com.gk.openapi.service.impl;

import com.alibaba.fastjson2.JSONWriter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.common.constant.Constant;
import com.gk.common.utils.BizKeyUtils;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.ledger.posting.PayoutPostingRequest;
import com.gk.ledger.service.LedgerPostingService;
import com.gk.merchant.entity.MerchantAppEntity;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.merchant.enums.MerchantAppEnvEnum;
import com.gk.openapi.dto.PayoutOrderCreateRequest;
import com.gk.openapi.dto.PayoutOrderResponse;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.openapi.security.ApiReqContext;
import com.gk.openapi.security.ApiReqContextHolder;
import com.gk.openapi.service.OpenPayoutOrderService;
import com.gk.openapi.util.ApiAmountUtils;
import com.gk.common.enums.OrderSourceEnum;
import com.gk.payment.enums.PayoutOrderStatusEnum;
import com.gk.payment.dao.PayoutOrderDao;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.payment.fee.MerchantFeeResult;
import com.gk.payment.plan.PaymentPlan;
import com.gk.payment.plan.PaymentPlanResolver;
import com.gk.payment.notify.MerchantOrderNotifyStatusService;
import com.gk.payment.service.MerchantFeeRuleService;
import com.gk.payment.service.OrderStatusLogService;
import com.gk.psp.dispatch.PspPayoutDispatchResult;
import com.gk.psp.dispatch.PspPayoutDispatchService;
import com.gk.psp.fee.PspFeeResult;
import com.gk.psp.route.PspRouteResult;
import com.gk.psp.route.PspRouteSelector;
import com.gk.psp.service.PspFeeRuleService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 商户 OpenAPI 代付订单服务实现。
 * <p>
 * 本类负责商户代付下单和查单的应用层编排：校验请求、处理商户订单号幂等、
 * 保存收款人脱敏信息、计算商户手续费、冻结商户余额、选择 PSP 路由、提交 PSP 代付，
 * 并在提交失败时尽量释放冻结资金。
 */
@Service
@RequiredArgsConstructor
public class OpenPayoutOrderServiceImpl implements OpenPayoutOrderService {
    private final PayoutOrderDao payoutOrderDao;
    private final MerchantFeeRuleService merchantFeeRuleService;
    private final PspRouteSelector pspRouteSelector;
    private final PspFeeRuleService pspFeeRuleService;
    private final PaymentPlanResolver paymentPlanResolver;
    private final PspPayoutDispatchService pspPayoutDispatchService;
    private final LedgerPostingService ledgerPostingService;
    private final ObjectMapper objectMapper;
    private final MerchantOrderNotifyStatusService merchantOrderNotifyStatusService;
    private final OrderStatusLogService orderStatusLogService;

    /**
     * 创建代付订单。
     * <p>
     * 主流程：检查幂等 -> 校验金额和商户应用权限 -> 组装订单和收款人信息 ->
     * 计算商户手续费 -> 落库 -> 冻结余额 -> 提交 PSP -> 返回订单状态。
     */
    @Override
    public PayoutOrderResponse create(PayoutOrderCreateRequest request) {
        // 先按商户订单号查重，保证商户重复请求时按幂等规则返回同一笔平台订单。
        PayoutOrderEntity existed = payoutOrderDao.selectOne(
                baseWrapper()
                        .eq("merchant_order_no", StringUtils.trim(request.getMerchantOrderId()))
                        .last("limit 1")
        );
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ApiErrorCode.INVALID_AMOUNT);
        }
        validateAmountScale(request.getAmount());

        // OpenApiAuthFilter 已经根据 app_id 识别租户、商户和商户应用。
        ApiReqContext context = ApiReqContextHolder.get();
        MerchantEntity merchant = context.getMerchant();
        // 币种和国家允许不传，优先从商户默认配置兜底。
        String currency = StringUtils.defaultIfBlank(request.getCurrency(), merchant.getDefaultCurrency());
        String countryCode = StringUtils.defaultIfBlank(request.getCountryCode(), merchant.getCountryCode());
        if (StringUtils.isBlank(currency) || StringUtils.isBlank(countryCode)) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "currency and country_code is required");
        }
        String normalizedCurrency = currency.toUpperCase(Locale.ROOT);
        String normalizedMethod = request.getMethodCode().toUpperCase(Locale.ROOT);
        validateMerchantAppAccess(context.getMerchantApp(), normalizedCurrency, normalizedMethod);

        if (existed != null) {
            // 同一商户订单号再次请求时，金额、币种、方式、通知地址、收款账号必须一致。
            validateIdempotentRequest(existed, request, normalizedCurrency, normalizedMethod);
            return toResponse(existed);
        }

        // 创建平台代付订单。代付会先进入 CREATED，冻结成功后再提交 PSP。
        PayoutOrderEntity entity = new PayoutOrderEntity();
        entity.setTenantId(context.getTenantId());
        entity.setMerchantId(context.getMerchantId());
        entity.setMerchantNo(context.getMerchantNo());
        entity.setMerchantAppId(context.getMerchantAppId());
        entity.setAppId(context.getAppId());
        entity.setPayoutOrderNo(BizKeyUtils.genPayoutOrderNo());
        entity.setMerchantOrderNo(StringUtils.trim(request.getMerchantOrderId()));
        entity.setIdempotencyKey(StringUtils.trim(request.getMerchantOrderId()));
        entity.setOrderSource(OrderSourceEnum.API.code());
        entity.setCountryCode(countryCode.toUpperCase(Locale.ROOT));
        entity.setCurrency(normalizedCurrency);
        entity.setMethodCode(normalizedMethod);
        entity.setAmount(request.getAmount());
        entity.setMerchantFeeAmount(BigDecimal.ZERO);
        entity.setTotalDebitAmount(request.getAmount());
        entity.setPspFeeAmount(BigDecimal.ZERO);
        entity.setPurpose(StringUtils.trimToNull(request.getPurpose()));
        entity.setNotifyUrl(StringUtils.trimToNull(request.getNotifyUrl()));
        entity.setMerchantNotifyStatus(merchantOrderNotifyStatusService.initialStatus(entity.getNotifyUrl()));
        entity.setStatus(PayoutOrderStatusEnum.CREATED.code());
        entity.setQueryCount(0);
        entity.setExtraJson(toJson(request.getExtra()));
        entity.setVersion(0);

        // 收款人敏感信息只保存掩码和 hash；用于展示、幂等校验和问题排查。
        applyPayee(entity, request);
        PaymentPlan paymentPlan = null;
        if (!isTestApp(context.getMerchantApp())) {
            // 正式代付优先读取后台发布的支付决策表；未发布时继续走旧实时规则，确保迁移不影响现有业务。
            paymentPlan = paymentPlanResolver.resolvePayout(entity).orElse(null);
        }
        if (paymentPlan != null) {
            applyPaymentPlan(entity, paymentPlan);
        } else {
            // 商户手续费会计入 totalDebitAmount，冻结时按“代付金额 + 商户手续费”冻结。
            applyMerchantFee(entity);
        }

        // 先落平台订单再冻结资金，便于冻结失败时留下可追踪订单状态。
        boolean created = insertOrder(entity);
        if (!created) {
            return toResponse(entity);
        }
        if (isTestApp(context.getMerchantApp())) {
            submitToSandbox(entity);
            return toResponse(entity);
        }
        try {
            // 代付必须先冻结商户可用余额，避免 PSP 已受理后商户余额不足。
            freezePayout(entity);
        } catch (ApiException ex) {
            markFailed(entity, ex.getMessage(), ex.getErrorCode().name());
            throw ex;
        } catch (Exception ex) {
            ApiErrorCode errorCode = StringUtils.containsIgnoreCase(ex.getMessage(), "Insufficient ledger balance")
                    ? ApiErrorCode.INSUFFICIENT_BALANCE
                    : ApiErrorCode.SYSTEM_ERROR;
            markFailed(entity, errorCode.getMessage(), errorCode.name());
            throw new ApiException(errorCode);
        }

        // 冻结成功后再提交 PSP；如果提交失败，会尝试释放冻结。
        submitToPsp(entity, paymentPlan);

        return toResponse(entity);
    }

    /**
     * 按平台代付订单号查询订单。
     */
    @Override
    public PayoutOrderResponse getByPayoutOrderNo(String payoutOrderNo) {
        PayoutOrderEntity entity = payoutOrderDao.selectOne(
                baseWrapper()
                        .eq("payout_order_no", StringUtils.trim(payoutOrderNo))
                        .last("limit 1")
        );
        return toResponse(entity);
    }

    /**
     * 按商户订单号查询订单。
     */
    @Override
    public PayoutOrderResponse getByMerchantOrderNo(String merchantOrderNo) {
        PayoutOrderEntity entity = payoutOrderDao.selectOne(
                baseWrapper()
                        .eq("merchant_order_no", StringUtils.trim(merchantOrderNo))
                        .last("limit 1")
        );
        return toResponse(entity);
    }

    /**
     * 插入代付订单并记录创建状态日志。
     * <p>
     * 如果并发请求触发唯一键冲突，会重新查询已有订单并按幂等规则复用。
     */
    private boolean insertOrder(PayoutOrderEntity entity) {
        try {
            payoutOrderDao.insert(entity);
            recordStatusChange(entity, null, entity.getStatus(), "ORDER_CREATED", null, "MERCHANT");
            return true;
        } catch (DuplicateKeyException ex) {
            PayoutOrderEntity existed = payoutOrderDao.selectOne(
                    baseWrapper()
                            .eq("merchant_order_no", entity.getMerchantOrderNo())
                            .last("limit 1")
            );
            if (existed != null) {
                validateIdempotentEntity(existed, entity);
                copyOrder(existed, entity);
                return false;
            }
            throw ex;
        }
    }

    /**
     * 提交代付订单到 PSP。
     * <p>
     * 这里完成 PSP 路由、PSP 手续费计算、适配器调用和订单状态更新。
     * 任何提交阶段异常都会先尝试释放冻结资金，再把订单标记为失败。
     */
    private void submitToPsp(PayoutOrderEntity order, PaymentPlan paymentPlan) {
        try {
            PspRouteResult route;
            if (paymentPlan != null && paymentPlan.getRoute() != null) {
                // 使用下单前已经命中的决策表路由，避免冻结后再次实时选路由导致快照不一致。
                route = paymentPlan.getRoute();
            } else {
                // 未发布决策表时保留旧逻辑：冻结成功后实时选择 PSP 路由并计算 PSP 成本。
                route = pspRouteSelector.selectPayout(order);
                applyRoute(order, route);
                applyPspFee(order);
            }

            // 调用 PSP 分发服务，具体 PSP 协议由对应 adapter 处理。
            PspPayoutDispatchResult dispatchResult = pspPayoutDispatchService.dispatch(order, route);
            applyDispatchResult(order, dispatchResult);
            if (!dispatchResult.isSuccess()) {
                // PSP 明确拒绝代付提交时，释放前面已经冻结的商户资金。
                releasePayout(order);
            }
            payoutOrderDao.updateById(order);
        } catch (ApiException ex) {
            // 路由失败、适配器异常等场景也要尽量释放冻结，避免资金长期占用。
            releasePayout(order);
            markFailed(order, ex.getMessage(), ex.getErrorCode().name());
            throw ex;
        } catch (Exception ex) {
            releasePayout(order);
            markFailed(order, ApiErrorCode.SYSTEM_ERROR.getMessage(), ApiErrorCode.SYSTEM_ERROR.name());
            throw new ApiException(ApiErrorCode.SYSTEM_ERROR);
        }
    }

    private void submitToSandbox(PayoutOrderEntity order) {
        String fromStatus = order.getStatus();
        order.setPspRequestNo(BizKeyUtils.genPspRequestNo());
        order.setPspCode(Constant.SANDBOX);
        order.setPspOrderNo(Constant.SANDBOX + "_" + order.getPayoutOrderNo());
        order.setPspStatus(PayoutOrderStatusEnum.PROCESSING.code());
        order.setPspRawStatus(PayoutOrderStatusEnum.PROCESSING.code());
        order.setStatus(PayoutOrderStatusEnum.PROCESSING.code());
        order.setSubmittedAt(Instant.now());
        order.setNextQueryAt(null);
        payoutOrderDao.updateById(order);
        recordStatusChange(order, fromStatus, order.getStatus(), "SANDBOX_SUBMIT", null, "SYSTEM");
    }

    private boolean isTestApp(MerchantAppEntity app) {
        return app != null && MerchantAppEnvEnum.TEST.code().equals(app.getAppEnv());
    }

    /**
     * 保存 PSP 路由结果到代付订单。
     */
    private void applyRoute(PayoutOrderEntity entity, PspRouteResult route) {
        entity.setRouteRuleId(route.getRouteRuleId());
        entity.setRouteSnapshotJson(routeSnapshotJson(route));
        entity.setPspId(route.getPspId());
        entity.setPspCode(route.getPspCode());
        entity.setPspMethodId(route.getPspMethodId());
        entity.setPspMethodCode(route.getPspMethodCode());
        entity.setPspAccountId(route.getPspAccountId());
        entity.setPspAccountNo(route.getPspAccountNo());
    }

    /**
     * 应用 PSP 代付提交结果。
     * <p>
     * PSP 受理成功后订单进入 PROCESSING，等待 PSP 回调或主动查单推进终态；
     * PSP 明确拒绝时订单直接 FAILED。
     */
    private void applyDispatchResult(PayoutOrderEntity entity, PspPayoutDispatchResult result) {
        String fromStatus = entity.getStatus();
        entity.setPspRequestNo(result.getPspRequestNo());
        entity.setPspOrderNo(result.getPspOrderNo());
        entity.setPspRawStatus(result.getRawStatus());
        if (result.isSuccess()) {
            entity.setStatus(PayoutOrderStatusEnum.PROCESSING.code());
            entity.setPspStatus(PayoutOrderStatusEnum.PROCESSING.code());
            entity.setSubmittedAt(Instant.now());
            // 设置下一次主动查单时间，兜底处理 PSP 回调丢失或延迟。
            entity.setNextQueryAt(Instant.now().plusSeconds(60));
            recordStatusChange(entity, fromStatus, entity.getStatus(), "PSP_SUBMIT", null, "SYSTEM");
            return;
        }
        entity.setStatus(PayoutOrderStatusEnum.FAILED.code());
        entity.setPspStatus(PayoutOrderStatusEnum.FAILED.code());
        entity.setFailCode(result.getErrorCode());
        entity.setFailMsg(StringUtils.left(result.getErrorMessage(), 512));
        String reason = StringUtils.defaultIfBlank(
                result.getErrorMessage(),
                StringUtils.defaultIfBlank(result.getResponseMessage(), "PSP payout submit failed")
        );
        entity.setStatusReason(reason);
        entity.setFailedAt(Instant.now());
        recordStatusChange(entity, fromStatus, entity.getStatus(), "PSP_SUBMIT_FAILED", reason, "SYSTEM");
    }

    /**
     * 将代付订单标记为失败并记录状态日志。
     */
    private void markFailed(PayoutOrderEntity entity, String reason, String failCode) {
        String fromStatus = entity.getStatus();
        entity.setStatus(PayoutOrderStatusEnum.FAILED.code());
        String message = StringUtils.defaultIfBlank(reason, "Payout order failed");
        entity.setStatusReason(message);
        entity.setFailCode(failCode);
        entity.setFailMsg(StringUtils.left(reason, 512));
        entity.setFailedAt(Instant.now());
        payoutOrderDao.updateById(entity);
        recordStatusChange(entity, fromStatus, entity.getStatus(), "ORDER_FAILED", message, "SYSTEM");
    }

    /**
     * 记录代付订单状态变更。
     */
    private void recordStatusChange(PayoutOrderEntity entity,
                                    String fromStatus,
                                    String toStatus,
                                    String eventType,
                                    String reason,
                                    String operatorType) {
        orderStatusLogService.recordChange(
                "PAYOUT",
                entity.getTenantId(),
                entity.getMerchantId(),
                entity.getId(),
                entity.getPayoutOrderNo(),
                fromStatus,
                toStatus,
                eventType,
                reason,
                operatorType,
                ApiReqContextHolder.getAppId(),
                entity.getMerchantOrderNo(),
                traceId()
        );
    }

    /**
     * 获取当前 OpenAPI 请求 traceId。
     */
    private String traceId() {
        ApiReqContext context = ApiReqContextHolder.get();
        return context == null ? null : context.getTraceId();
    }

    /**
     * 计算商户手续费和总扣款金额。
     * <p>
     * 代付冻结金额为代付本金加商户手续费，即 totalDebitAmount。
     */
    private void applyMerchantFee(PayoutOrderEntity entity) {
        MerchantFeeResult feeResult = merchantFeeRuleService.calculatePayout(entity);
        BigDecimal feeAmount = defaultZero(feeResult.getMerchantFeeAmount());
        entity.setMerchantFeeAmount(feeAmount);
        entity.setTotalDebitAmount(entity.getAmount().add(feeAmount));
        entity.setMerchantFeeRuleId(feeResult.getRule().getId());
        entity.setMerchantFeeSnapshotJson(feeResult.getSnapshotJson());
    }

    /**
     * 应用已发布支付决策表命中的代付方案。
     * <p>
     * 决策表同时给出商户费率、PSP 路由和 PSP 成本，订单只保存快照字段。
     */
    private void applyPaymentPlan(PayoutOrderEntity entity, PaymentPlan plan) {
        BigDecimal feeAmount = defaultZero(plan.getMerchantFeeAmount());
        entity.setMerchantFeeAmount(feeAmount);
        entity.setTotalDebitAmount(entity.getAmount().add(feeAmount));
        entity.setMerchantFeeRuleId(plan.getMerchantFee().getRule().getId());
        entity.setMerchantFeeSnapshotJson(plan.getMerchantFee().getSnapshotJson());

        applyRoute(entity, plan.getRoute());

        entity.setPspFeeAmount(plan.getPspFeeAmount());
        if (plan.getPspFee() != null && plan.getPspFee().getRule() != null) {
            entity.setPspFeeRuleId(plan.getPspFee().getRule().getId());
            entity.setPspFeeSnapshotJson(plan.getPspFee().getSnapshotJson());
        }
    }

    /**
     * 冻结商户可用余额。
     * <p>
     * 冻结成功后保存 holdNo 和冻结账务流水号，后续 PSP 成功会扣冻结，
     * PSP 失败或提交异常会按 holdNo 释放冻结。
     *
     * @param order 订单信息
     */
    private void freezePayout(PayoutOrderEntity order) {
        LedgerPostingResult result = ledgerPostingService.freezePayout(payoutPostingRequest(order));
        order.setHoldNo(result.getHoldNo());
        order.setFreezeJournalNo(result.getJournalNo());
        payoutOrderDao.updateById(order);
    }

    /**
     * 释放代付冻结资金。
     * <p>
     * 仅在 PSP 提交失败或明确拒绝时调用；如果释放失败，保留原始 PSP 错误，
     * 后续可由运营或补偿任务根据 holdNo 处理。
     *
     * @param order 订单信息
     */
    private void releasePayout(PayoutOrderEntity order) {
        if (StringUtils.isBlank(order.getHoldNo())) {
            return;
        }
        try {
            LedgerPostingResult result = ledgerPostingService.releasePayout(payoutPostingRequest(order));
            order.setReleaseJournalNo(result.getJournalNo());
        } catch (Exception ignored) {
            // Keep the original PSP error visible; ledger retry/release can be handled by operations.
        }
    }

    /**
     * 构建账务代付请求对象。
     */
    private PayoutPostingRequest payoutPostingRequest(PayoutOrderEntity entity) {
        PayoutPostingRequest request = new PayoutPostingRequest();
        request.setTenantId(entity.getTenantId());
        request.setMerchantId(entity.getMerchantId());
        request.setMerchantNo(entity.getMerchantNo());
        request.setMerchantAppId(entity.getMerchantAppId());
        request.setMerchantOrderNo(entity.getMerchantOrderNo());
        request.setPspAccountId(entity.getPspAccountId());
        request.setBizId(entity.getId());
        request.setPayoutOrderNo(entity.getPayoutOrderNo());
        request.setCurrency(entity.getCurrency());
        request.setAmount(entity.getAmount());
        request.setMerchantFeeAmount(entity.getMerchantFeeAmount());
        request.setTotalDebitAmount(entity.getTotalDebitAmount());
        return request;
    }

    /**
     * 计算 PSP 手续费并保存规则快照。
     */
    private void applyPspFee(PayoutOrderEntity entity) {
        PspFeeResult feeResult = pspFeeRuleService.calculatePayout(entity);
        entity.setPspFeeAmount(feeResult.getPspFeeAmount());
        entity.setPspFeeRuleId(feeResult.getRule().getId());
        entity.setPspFeeSnapshotJson(feeResult.getSnapshotJson());
    }

    /**
     * 校验重复商户订单号对应的请求参数是否一致。
     * <p>
     * 代付额外校验收款账号 hash，避免同一商户订单号被用于不同收款人。
     */
    private void validateIdempotentRequest(PayoutOrderEntity existed, PayoutOrderCreateRequest request, String currency, String methodCode) {
        if (existed.getAmount() == null || request.getAmount() == null
                || existed.getAmount().compareTo(request.getAmount()) != 0
                || !StringUtils.equalsIgnoreCase(existed.getCurrency(), currency)
                || !StringUtils.equalsIgnoreCase(existed.getMethodCode(), methodCode)
                || !StringUtils.equals(StringUtils.trimToEmpty(existed.getNotifyUrl()), StringUtils.trimToEmpty(request.getNotifyUrl()))
                || !StringUtils.equals(StringUtils.trimToEmpty(existed.getPayeeAccountHash()), sha256Hex(requestPayeeAccountNo(request)))) {
            throw new ApiException(ApiErrorCode.DUPLICATE_REQUEST, "merchant_order_id exists with different request parameters");
        }
    }

    /**
     * 校验并发插入冲突后查到的已有订单是否与当前订单一致。
     */
    private void validateIdempotentEntity(PayoutOrderEntity existed, PayoutOrderEntity entity) {
        if (existed.getAmount() == null || entity.getAmount() == null
                || existed.getAmount().compareTo(entity.getAmount()) != 0
                || !StringUtils.equalsIgnoreCase(existed.getCurrency(), entity.getCurrency())
                || !StringUtils.equalsIgnoreCase(existed.getMethodCode(), entity.getMethodCode())
                || !StringUtils.equals(StringUtils.trimToEmpty(existed.getNotifyUrl()), StringUtils.trimToEmpty(entity.getNotifyUrl()))
                || !StringUtils.equals(StringUtils.trimToEmpty(existed.getPayeeAccountHash()), StringUtils.trimToEmpty(entity.getPayeeAccountHash()))) {
            throw new ApiException(ApiErrorCode.DUPLICATE_REQUEST, "merchant_order_id exists with different request parameters");
        }
    }

    /**
     * 校验当前商户应用是否允许使用指定币种和代付方式。
     */
    private void validateMerchantAppAccess(MerchantAppEntity app, String currency, String methodCode) {
        if (!allowed(app == null ? null : app.getAllowedCurrencyJson(), currency)) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "currency is not allowed for app");
        }
        if (!allowed(app == null ? null : app.getAllowedMethodJson(), methodCode)) {
            throw new ApiException(ApiErrorCode.UNSUPPORTED_METHOD, "method is not allowed for app");
        }
    }

    /**
     * 判断配置列表是否允许当前值。
     * <p>
     * 空配置表示不限制。
     */
    private boolean allowed(String jsonArray, String value) {
        if (StringUtils.isBlank(jsonArray)) {
            return true;
        }
        try {
            List<String> allowedValues = objectMapper.readValue(jsonArray, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            return allowedValues.stream().anyMatch(item -> StringUtils.equalsIgnoreCase(item, value));
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
     * 保存收款人信息。
     * <p>
     * 使用 payee 结构保存收款人信息。
     * <p>
     * 独立检索列只保存账号、手机号、邮箱的掩码和 hash；payee_json 保留提交 PSP 所需的标准字段快照。
     */
    private void applyPayee(PayoutOrderEntity entity, PayoutOrderCreateRequest request) {
        PayoutOrderCreateRequest.Payee payee = request.getPayee();
        String payeeName = payee == null ? null : StringUtils.trimToNull(payee.getName());
        String accountNo = payee == null ? null : StringUtils.trimToNull(payee.getAccountNo());
        String bankCode = payee == null ? null : StringUtils.trimToNull(payee.getBankCode());
        String walletType = payee == null ? null : StringUtils.trimToNull(payee.getWalletType());
        String phone = payee == null ? null : payee.getPhone();
        String email = payee == null ? null : payee.getEmail();

        entity.setPayeeName(payeeName);
        entity.setPayeeAccountMask(mask(accountNo, 4, 4));
        entity.setPayeeAccountHash(sha256Hex(accountNo));
        entity.setPayeeBankCode(bankCode);
        entity.setPayeeWalletType(walletType);
        entity.setPayeePhoneMask(mask(phone, 3, 4));
        entity.setPayeePhoneHash(sha256Hex(phone));
        entity.setPayeeEmailMask(maskEmail(email));
        entity.setPayeeEmailHash(sha256Hex(email));
        entity.setPayeeJson(JSON.toJSONString(
                payeeSnapshot(payeeName, accountNo, bankCode, walletType, phone, email),
                JSONWriter.Feature.WriteMapNullValue
        ));
    }

    /**
     * 构建可落库的收款人脱敏快照。
     */
    private Map<String, Object> payeeSnapshot(String name,
                                              String accountNo,
                                              String bankCode,
                                              String walletType,
                                              String phone,
                                              String email) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", name);
        snapshot.put("account_no", accountNo);
        snapshot.put("account_mask", mask(accountNo, 4, 4));
        snapshot.put("bank_code", bankCode);
        snapshot.put("wallet_type", walletType);
        snapshot.put("phone", phone);
        snapshot.put("phone_mask", mask(phone, 3, 4));
        snapshot.put("email", email);
        snapshot.put("email_mask", maskEmail(email));
        return snapshot;
    }

    /**
     * 构建 PSP 路由快照 JSON。
     */
    private String routeSnapshotJson(PspRouteResult route) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("routeRuleId", route.getRouteRuleId());
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
     * 构建商户 OpenAPI 查询基础条件。
     * <p>
     * 所有商户查单只允许访问当前 app_id 所属租户和商户的数据。
     */
    private QueryWrapper<PayoutOrderEntity> baseWrapper() {
        return new QueryWrapper<PayoutOrderEntity>()
                .eq("tenant_id", ApiReqContextHolder.getTenantId())
                .eq("merchant_id", ApiReqContextHolder.getMerchantId());
    }

    /**
     * 把已有订单字段复制到当前对象。
     * <p>
     * 用于并发幂等场景，调用方可继续用当前对象生成统一响应。
     */
    private void copyOrder(PayoutOrderEntity source, PayoutOrderEntity target) {
        target.setId(source.getId());
        target.setTenantId(source.getTenantId());
        target.setMerchantId(source.getMerchantId());
        target.setMerchantNo(source.getMerchantNo());
        target.setMerchantAppId(source.getMerchantAppId());
        target.setAppId(source.getAppId());
        target.setPayoutOrderNo(source.getPayoutOrderNo());
        target.setMerchantOrderNo(source.getMerchantOrderNo());
        target.setStatus(source.getStatus());
        target.setStatusReason(source.getStatusReason());
        target.setAmount(source.getAmount());
        target.setMerchantFeeAmount(source.getMerchantFeeAmount());
        target.setMerchantFeeRuleId(source.getMerchantFeeRuleId());
        target.setMerchantFeeSnapshotJson(source.getMerchantFeeSnapshotJson());
        target.setTotalDebitAmount(source.getTotalDebitAmount());
        target.setPspFeeAmount(source.getPspFeeAmount());
        target.setPspFeeRuleId(source.getPspFeeRuleId());
        target.setPspFeeSnapshotJson(source.getPspFeeSnapshotJson());
        target.setCurrency(source.getCurrency());
        target.setCountryCode(source.getCountryCode());
        target.setMethodCode(source.getMethodCode());
        target.setPspOrderNo(source.getPspOrderNo());
    }

    /**
     * 转换代付订单为 OpenAPI 响应。
     */
    private PayoutOrderResponse toResponse(PayoutOrderEntity entity) {
        if (entity == null) {
            throw new ApiException(ApiErrorCode.ORDER_NOT_FOUND);
        }
        PayoutOrderResponse response = new PayoutOrderResponse();
        response.setSystemOrderId(entity.getPayoutOrderNo());
        response.setMerchantOrderId(entity.getMerchantOrderNo());
        response.setStatus(entity.getStatus());
        response.setStatusReason(entity.getStatusReason());
        response.setAmount(formatMoney(entity.getAmount(), entity.getCurrency()));
        response.setCurrency(entity.getCurrency());
        response.setCountryCode(entity.getCountryCode());
        response.setMethodCode(entity.getMethodCode());
        return response;
    }

    /**
     * 将对象转成 JSON。
     */
    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map && map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Invalid JSON field");
        }
    }

    /**
     * 按币种格式化金额输出。
     */
    private String formatMoney(BigDecimal value, String currency) {
        return value == null ? null : ApiAmountUtils.formatCurrencyAmount(value, currency);
    }

    /**
     * BigDecimal 空值转 0。
     */
    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 返回第一个非空字符串。
     */
    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return StringUtils.trim(value);
            }
        }
        return null;
    }

    private String requestPayeeAccountNo(PayoutOrderCreateRequest request) {
        PayoutOrderCreateRequest.Payee payee = request == null ? null : request.getPayee();
        return payee == null ? null : StringUtils.trimToNull(payee.getAccountNo());
    }

    /**
     * 对账号、手机号等文本做前后保留的掩码。
     */
    private String mask(String value, int prefix, int suffix) {
        String text = StringUtils.trimToNull(value);
        if (text == null) {
            return null;
        }
        if (text.length() <= prefix + suffix) {
            return "*".repeat(Math.min(text.length(), 6));
        }
        return text.substring(0, prefix) + "****" + text.substring(text.length() - suffix);
    }

    /**
     * 对邮箱做脱敏。
     */
    private String maskEmail(String value) {
        String email = StringUtils.trimToNull(value);
        if (email == null) {
            return null;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return mask(email, 1, 0);
        }
        return email.charAt(0) + "****" + email.substring(atIndex);
    }

    /**
     * 计算文本 SHA-256 摘要。
     * <p>
     * 用于收款账号、手机号、邮箱等敏感字段的幂等校验和排查定位。
     */
    private String sha256Hex(String value) {
        String text = StringUtils.trimToNull(value);
        if (text == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ApiException(ApiErrorCode.SYSTEM_ERROR);
        }
    }
}
