package com.gk.openapi.service.impl;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.JSON;
import com.gk.common.constant.Constant;
import com.gk.common.enums.OrderSourceEnum;
import com.gk.common.enums.PayDirectionEnum;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.utils.BizKeyUtils;
import com.gk.infra.config.service.GkSysParamsConfigService;
import com.gk.infra.utils.AsynUtils;
import com.gk.ledger.dao.LedgerBalanceDao;
import com.gk.ledger.enums.LedgerAccountTypeEnum;
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
import com.gk.payment.constant.PaymentMethodCodes;
import com.gk.payment.dao.PayoutOrderDao;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.payment.enums.PayoutOrderStatusEnum;
import com.gk.payment.notify.MerchantOrderNotifyStatusService;
import com.gk.payment.outbox.PayoutSubmitOutboxProducer;
import com.gk.payment.plan.PayoutPlan;
import com.gk.payment.plan.PayoutPlanService;
import com.gk.payment.psp.PayoutPspSubmitService;
import com.gk.payment.service.OrderStatusLogService;
import com.gk.psp.route.PspRouteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 商户 OpenAPI 代付订单服务实现
 * <p>
 * 本类负责商户代付下单和查单的应用层编排：校验请求、处理商户订单号幂等、
 * 保存收款人信息、计算商户手续费、冻结商户余额、选择 PSP 路由、提交 PSP 代付，
 * 并在提交失败时尽量释放冻结资金。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OpenPayoutOrderServiceImpl implements OpenPayoutOrderService {
    private final PayoutOrderDao payoutOrderDao;
    private final PayoutPlanService payoutPlanService;
    private final MerchantOrderNotifyStatusService merchantOrderNotifyStatusService;
    private final OrderStatusLogService orderStatusLogService;
    private final PayoutSubmitOutboxProducer payoutSubmitOutboxProducer;
    private final PayoutPspSubmitService payoutPspSubmitService;
    private final LedgerBalanceDao ledgerBalanceDao;
    private final TransactionTemplate transactionTemplate;
    private final GkSysParamsConfigService configService;

    /**
     * 创建代付订单
     * <p>
     * 主流程：检查幂-> 校验金额和商户应用权-> 组装订单和收款人信息 ->
     * 计算商户手续-> 落库 -> 冻结余额 -> 提交 PSP -> 返回订单状态
     */
    @Override
    public PayoutOrderResponse create(PayoutOrderCreateRequest request) {
        if (request == null) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST);
        }
        PayoutCreateStepTimer timer = PayoutCreateStepTimer.start(request.getMerchantOrderId());
        // Check merchant order id first so repeated requests return the same platform order.
        PayoutOrderEntity existed = findByMerchantOrderNo(StringUtils.trim(request.getMerchantOrderId()));
        timer.mark("idempotency_check");
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ApiErrorCode.INVALID_AMOUNT);
        }
        validateAmountScale(request.getAmount());

        // OpenApiAuthFilter has already resolved tenant, merchant, and merchant app from app_id.
        ApiReqContext context = ApiReqContextHolder.get();
        MerchantEntity merchant = context.getMerchant();
        // Currency can fall back to merchant default; country only uses explicit API input.
        String currency = StringUtils.defaultIfBlank(request.getCurrency(), merchant.getDefaultCurrency());
        String countryCode = StringUtils.trimToNull(request.getCountryCode());
        if (StringUtils.isBlank(currency)) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "currency is required");
        }
        String normalizedCurrency = currency.toUpperCase(Locale.ROOT);
        String normalizedCountryCode = StringUtils.defaultString(countryCode).toUpperCase(Locale.ROOT);
        String normalizedMethod = request.getMethodCode().toUpperCase(Locale.ROOT);

        validatePayoutPayee(request, normalizedMethod);

        timer.mark("validate_request");
        if (existed != null) {
            // The idempotent request must keep all business-sensitive fields unchanged.
            validateIdempotentRequest(existed, request, normalizedCurrency, normalizedMethod);
            timer.mark("return_idempotent_order");
            PayoutOrderResponse response = toResponse(existed);
            timer.log("SUCCESS", existed);
            return response;
        }

        // Create the platform payout order in CREATED; funds are frozen before PSP submit.
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
        entity.setCountryCode(normalizedCountryCode);
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
        timer.mark("build_order");

        // Store payee plaintext for PSP submission and operations troubleshooting.
        applyPayee(entity, request);

        timer.mark("apply_payee");
        PayoutPlan payoutPlan = null;
        if (isTestApp(context.getMerchantApp())) {
            // Test app skips payment plan; sandbox fees default to zero.
            applySandboxPayoutDefaults(entity);
            timer.mark("resolve_payment_plan");
            timer.mark("apply_payment_plan");
        } else {
            // Formal app must use the published payment plan as the single source of fees and route.
            payoutPlan = payoutPlanService.resolve(entity);
            timer.mark("resolve_payment_plan");
            applyPayoutPlan(entity, payoutPlan);
            timer.mark("apply_payment_plan");
            precheckAvailableBalance(entity.getTenantId(), entity.getMerchantId(), entity.getCurrency(), entity.getTotalDebitAmount());
            timer.mark("balance_precheck");
        }

        boolean asyncSubmitEnabled = configService.payoutSubmitConfig().isAsyncSubmit();

        boolean created = asyncSubmitEnabled && !isTestApp(context.getMerchantApp())
                ? insertOrderAndOutbox(entity)
                : insertOrder(entity);
        timer.mark("insert_order");
        if (!created) {
            PayoutOrderResponse response = toResponse(entity);
            timer.log("SUCCESS", entity);
            return response;
        }
        if (isTestApp(context.getMerchantApp())) {
            submitToSandbox(entity);
            timer.mark("submit_sandbox");
            PayoutOrderResponse response = toResponse(entity);
            timer.log("SUCCESS", entity);
            return response;
        }
        if (asyncSubmitEnabled) {
            timer.mark("create_outbox");
            PayoutOrderResponse response = toResponse(entity);
            timer.log("ACCEPTED", entity);
            return response;
        }
        try {
            PayoutOrderEntity submitted = payoutPspSubmitService.freezeAndSubmit(
                    entity, payoutPlan.getRoute(),
                    PayoutPspSubmitService.SubmitContext.system(ApiReqContextHolder.getAppId(), traceId())
                            .withFreezeFailureException()
            );
            if (submitted != null) {
                entity = submitted;
            }
            timer.mark("freeze_payout");
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

        PayoutOrderResponse response = toResponse(entity);
        timer.log("SUCCESS", entity);
        return response;
    }

    /**
     * 按平台代付订单号查询订单
     */
    @Override
    public PayoutOrderResponse getByPayoutOrderNo(String payoutOrderNo) {
        PayoutOrderEntity entity = payoutOrderDao.selectOpenApiByPayoutOrderNo(
                ApiReqContextHolder.getTenantId(),
                ApiReqContextHolder.getMerchantId(),
                StringUtils.trim(payoutOrderNo)
        );
        return toResponse(entity);
    }

    /**
     * 按商户订单号查询订单
     */
    @Override
    public PayoutOrderResponse getByMerchantOrderNo(String merchantOrderNo) {
        PayoutOrderEntity entity = findByMerchantOrderNo(StringUtils.trim(merchantOrderNo));
        return toResponse(entity);
    }

    /**
     * 插入代付订单并记录创建状态日志
     * <p>
     * 如果并发请求触发唯一键冲突，会重新查询已有订单并按幂等规则复用
     */
    private boolean insertOrder(PayoutOrderEntity entity) {
        try {
            payoutOrderDao.insert(entity);
            recordStatusChange(entity, null, entity.getStatus(), "ORDER_CREATED", null, "MERCHANT");
            return true;
        } catch (DuplicateKeyException ex) {
            PayoutOrderEntity existed = findByMerchantOrderNo(entity.getMerchantOrderNo());
            if (existed != null) {
                validateIdempotentEntity(existed, entity);
                copyOrder(existed, entity);
                return false;
            }
            throw ex;
        }
    }

    /**
     * 把“创建代付订单”和“创建待提交 PSP outbox 任务”放在同一个数据库事务里执行
     * @param order 订单
     * @return Boolean
     */
    private Boolean insertOrderAndOutbox(PayoutOrderEntity order) {
        // Keep order insert and outbox insert in one transaction.
        return transactionTemplate.execute(status -> {
            boolean created = insertOrder(order);
            if (created) {
                payoutSubmitOutboxProducer.create(order);
            }
            return created;
        });
    }

    /**
     * 提交代付订单PSP
     * <p>
     * 这里完成 PSP 路由、PSP 手续费计算、适配器调用和订单状态更新
     * 任何提交阶段异常都会先尝试释放冻结资金，再把订单标记为失败
     */
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

    private void applySandboxPayoutDefaults(PayoutOrderEntity entity) {
        entity.setMerchantFeeAmount(BigDecimal.ZERO);
        entity.setTotalDebitAmount(entity.getAmount());
        entity.setMerchantFeeRuleId(null);
        entity.setMerchantFeeSnapshotJson(null);
        entity.setPspFeeAmount(BigDecimal.ZERO);
        entity.setPspFeeRuleId(null);
        entity.setPspFeeSnapshotJson(null);
    }

    /**
     * 保存 PSP 路由结果到代付订单
     */
    private void applyRoute(PayoutOrderEntity entity, PspRouteResult route) {
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
     * 记录代付订单状态变更
     */
    private void recordStatusChange(PayoutOrderEntity entity,
                                    String fromStatus,
                                    String toStatus,
                                    String eventType,
                                    String reason,
                                    String operatorType) {
        Long tenantId = entity.getTenantId();
        Long merchantId = entity.getMerchantId();
        Long orderId = entity.getId();
        String payoutOrderNo = entity.getPayoutOrderNo();
        String appId = ApiReqContextHolder.getAppId();
        String merchantOrderNo = entity.getMerchantOrderNo();
        String traceId = traceId();
        AsynUtils.execute("Order status log", () -> orderStatusLogService.recordChange(
                PayDirectionEnum.PAYOUT.code(),
                tenantId,
                merchantId,
                orderId,
                payoutOrderNo,
                fromStatus,
                toStatus,
                eventType,
                reason,
                operatorType,
                appId,
                merchantOrderNo,
                traceId
        ));
    }

    /**
     * 获取当前 OpenAPI 请求 traceId
     */
    private String traceId() {
        return ApiReqContextHolder.get().getTraceId();
    }

    /**
     * 代付创建接口分段耗时计时器
     * <p>
     * 只用于定位慢接口，不参与任何业务判断；每次成功或已处理失败时输出一summary，方便按 traceId 排查
     */
    private static final class PayoutCreateStepTimer {
        private final String merchantOrderNo;
        private final long startNanos;
        private long lastNanos;
        private final StringBuilder steps = new StringBuilder();

        private PayoutCreateStepTimer(String merchantOrderNo) {
            this.merchantOrderNo = StringUtils.trimToNull(merchantOrderNo);
            this.startNanos = System.nanoTime();
            this.lastNanos = this.startNanos;
        }

        private static PayoutCreateStepTimer start(String merchantOrderNo) {
            return new PayoutCreateStepTimer(merchantOrderNo);
        }

        private void mark(String step) {
            long now = System.nanoTime();
            if (!steps.isEmpty()) {
                steps.append(", ");
            }
            steps.append(step).append('=').append(toMillis(now - lastNanos)).append("ms");
            lastNanos = now;
        }

        private void log(String result, PayoutOrderEntity order) {
            long totalMs = toMillis(System.nanoTime() - startNanos);
            ApiReqContext context = ApiReqContextHolder.get();
            log.info(
                    "OpenAPI payout create profile result={}, traceId={}, tenantId={}, merchantId={}, appId={}, merchantOrderNo={}, payoutOrderNo={}, status={}, total={}ms, steps=[{}]",
                    result,
                    context == null ? null : context.getTraceId(),
                    context == null ? null : context.getTenantId(),
                    context == null ? null : context.getMerchantId(),
                    context == null ? null : context.getAppId(),
                    order == null ? merchantOrderNo : StringUtils.defaultIfBlank(order.getMerchantOrderNo(), merchantOrderNo),
                    order == null ? null : order.getPayoutOrderNo(),
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
     * 应用已发布支付决策表命中的代付方案
     * <p>
     * 决策表同时给出商户费率、PSP 路由PSP 成本，订单只保存快照字段
     */
    private void applyPayoutPlan(PayoutOrderEntity entity, PayoutPlan plan) {
        entity.setPaymentPlanCatalogId(plan.getCatalogId());
        entity.setPaymentPlanVersion(plan.getCatalogVersion());
        entity.setPaymentPlanBucketId(plan.getBucketId());
        entity.setPaymentPlanRouteOptionId(plan.getRouteOptionId());

        entity.setMerchantFeeAmount(plan.getMerchantFeeAmount());
        entity.setTotalDebitAmount(plan.getTotalDebitAmount());
        entity.setMerchantFeeRuleId(plan.getMerchantFee().getRule().getId());
        entity.setMerchantFeeSnapshotJson(plan.getMerchantFee().getSnapshotJson());

        applyRoute(entity, plan.getRoute());

        entity.setPspFeeAmount(plan.getPspFeeAmount() == null ? BigDecimal.ZERO : plan.getPspFeeAmount());
        if (plan.getPspFee() != null && plan.getPspFee().getRule() != null) {
            entity.setPspFeeRuleId(plan.getPspFee().getRule().getId());
            entity.setPspFeeSnapshotJson(plan.getPspFee().getSnapshotJson());
        }
    }

    /**
     * 冻结商户可用余额
     * <p>
     * 冻结成功后保holdNo 和冻结账务流水号，后PSP 成功会扣冻结
     * PSP 失败或提交异常会holdNo 释放冻结
     */
    private void precheckAvailableBalance(Long tenantId, Long merchantId, String currency, BigDecimal totalDebitAmount) {
        BigDecimal available = defaultZero(ledgerBalanceDao.selectMerchantAvailableBalance(
                tenantId,
                merchantId,
                SubjectTypeEnum.MERCHANT.code(),
                LedgerAccountTypeEnum.AVAILABLE.code(),
                currency
        ));
        if (available.compareTo(totalDebitAmount) < 0) {
            throw new ApiException(ApiErrorCode.INSUFFICIENT_BALANCE);
        }
    }

    /**
     * 校验重复商户订单号对应的请求参数是否一致
     * <p>
     * 代付额外校验收款账号，避免同一商户订单号被用于不同收款人
     */
    private void validateIdempotentRequest(PayoutOrderEntity existed, PayoutOrderCreateRequest request, String currency, String methodCode) {
        if (existed.getAmount() == null || request.getAmount() == null
                || existed.getAmount().compareTo(request.getAmount()) != 0
                || differsIgnoreCase(existed.getCurrency(), currency)
                || differsIgnoreCase(existed.getMethodCode(), methodCode)
                || differsTrimmed(existed.getNotifyUrl(), request.getNotifyUrl())
                || differsTrimmed(existed.getPayeeAccountNo(), requestPayeeAccountNo(request))
                || differsIgnoreCase(StringUtils.trimToEmpty(existed.getPayeeBankCode()), StringUtils.trimToEmpty(requestPayeeBankCode(request)))) {
            throw new ApiException(ApiErrorCode.DUPLICATE_REQUEST, "merchant_order_id exists with different request parameters");
        }
    }

    /**
     * 校验并发插入冲突后查到的已有订单是否与当前订单一致
     */
    private void validateIdempotentEntity(PayoutOrderEntity existed, PayoutOrderEntity entity) {
        if (existed.getAmount() == null || entity.getAmount() == null
                || existed.getAmount().compareTo(entity.getAmount()) != 0
                || differsIgnoreCase(existed.getCurrency(), entity.getCurrency())
                || differsIgnoreCase(existed.getMethodCode(), entity.getMethodCode())
                || differsTrimmed(existed.getNotifyUrl(), entity.getNotifyUrl())
                || differsTrimmed(existed.getPayeeAccountNo(), entity.getPayeeAccountNo())
                || differsIgnoreCase(StringUtils.trimToEmpty(existed.getPayeeBankCode()), StringUtils.trimToEmpty(entity.getPayeeBankCode()))) {
            throw new ApiException(ApiErrorCode.DUPLICATE_REQUEST, "merchant_order_id exists with different request parameters");
        }
    }

    /**
     * 银行卡代付必须携带系统标准银行编码，后续运行时会用它匹配 psp_bank_mapping
     */
    private void validatePayoutPayee(PayoutOrderCreateRequest request, String methodCode) {
        if (!PaymentMethodCodes.isBankCard(methodCode)) {
            return;
        }
        PayoutOrderCreateRequest.Payee payee = request == null ? null : request.getPayee();
        if (payee == null || StringUtils.isBlank(payee.getBankCode())) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "payee.bank_code is required for BANK_CARD payout");
        }
    }

    /**
     * 校验金额小数位
     */
    private void validateAmountScale(BigDecimal amount) {
        if (amount.scale() > 8) {
            throw new ApiException(ApiErrorCode.INVALID_AMOUNT, "amount scale must be less than or equal to 8");
        }
    }

    /**
     * 保存收款人信息
     * <p>
     * 使用 payee 结构保存收款人信息
     * <p>
     * 独立列保存账号、手机号、邮箱明文；payee_json 保留提交 PSP 所需的标准字段快照
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
        entity.setPayeeAccountNo(accountNo);
        entity.setPayeeBankCode(bankCode);
        entity.setPayeeWalletType(walletType);
        entity.setPayeePhone(StringUtils.trimToNull(phone));
        entity.setPayeeEmail(StringUtils.trimToNull(email));
        entity.setPayeeJson(JSON.toJSONString(
                payeeSnapshot(payeeName, accountNo, bankCode, walletType, phone, email),
                JSONWriter.Feature.WriteMapNullValue
        ));
    }

    /**
     * 构建可落库的收款人明文快照
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
        snapshot.put("bank_code", bankCode);
        snapshot.put("wallet_type", walletType);
        snapshot.put("phone", phone);
        snapshot.put("email", email);
        return snapshot;
    }

    /**
     * 构建 PSP 路由快照 JSON
     */
    private String routeSnapshotJson(PayoutOrderEntity entity, PspRouteResult route) {
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
     * 构建商户 OpenAPI 查询基础条件
     * <p>
     * 所有商户查单只允许访问当前 app_id 所属租户和商户的数据
     */
    private PayoutOrderEntity findByMerchantOrderNo(String merchantOrderNo) {
        return payoutOrderDao.selectOpenApiByMerchantOrderNo(
                ApiReqContextHolder.getTenantId(),
                ApiReqContextHolder.getMerchantId(),
                merchantOrderNo
        );
    }

    /**
     * 把已有订单字段复制到当前对象
     * <p>
     * 用于并发幂等场景，调用方可继续用当前对象生成统一响应
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
        target.setPspOrderNo(source.getPspOrderNo());
    }

    /**
     * 转换代付订单OpenAPI 响应
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
     * 将对象转JSON
     */
    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map && map.isEmpty()) {
            return null;
        }
        try {
            return JSON.toJSONString(value);
        } catch (Exception ex) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Invalid JSON field");
        }
    }

    /**
     * 按币种格式化金额输出
     */
    private String formatMoney(BigDecimal value, String currency) {
        return value == null ? null : ApiAmountUtils.formatCurrencyAmount(value, currency);
    }

    /**
     * BigDecimal 空值转 0
     */
    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean differsIgnoreCase(String left, String right) {
        return left == null ? right != null : !left.equalsIgnoreCase(right);
    }

    private boolean differsTrimmed(String left, String right) {
        return !StringUtils.trimToEmpty(left).equals(StringUtils.trimToEmpty(right));
    }

    private String requestPayeeAccountNo(PayoutOrderCreateRequest request) {
        PayoutOrderCreateRequest.Payee payee = request == null ? null : request.getPayee();
        return payee == null ? null : StringUtils.trimToNull(payee.getAccountNo());
    }

    private String requestPayeeBankCode(PayoutOrderCreateRequest request) {
        PayoutOrderCreateRequest.Payee payee = request == null ? null : request.getPayee();
        return payee == null ? null : StringUtils.trimToNull(payee.getBankCode());
    }

}
