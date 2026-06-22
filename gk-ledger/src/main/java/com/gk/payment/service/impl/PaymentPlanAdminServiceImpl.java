package com.gk.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.payment.dao.PaymentPlanBucketDao;
import com.gk.payment.dao.PaymentPlanCatalogDao;
import com.gk.payment.dao.PaymentPlanRouteOptionDao;
import com.gk.payment.dto.PaymentPlanPreviewRequest;
import com.gk.payment.dto.PaymentPlanPreviewResponse;
import com.gk.payment.dto.PaymentPlanPublishRequest;
import com.gk.payment.dto.PaymentPlanPublishResponse;
import com.gk.payment.entity.PaymentPlanBucketEntity;
import com.gk.payment.entity.PaymentPlanCatalogEntity;
import com.gk.payment.entity.PaymentPlanRouteOptionEntity;
import com.gk.payment.plan.PaymentPlanCacheService;
import com.gk.payment.plan.PaymentPlanCompileRequest;
import com.gk.payment.plan.PaymentPlanCompileResult;
import com.gk.payment.plan.PaymentPlanCompiler;
import com.gk.payment.plan.PaymentPlanKey;
import com.gk.payment.plan.PaymentPlanStatus;
import com.gk.payment.service.PaymentPlanAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentPlanAdminServiceImpl implements PaymentPlanAdminService {
    private static final BigDecimal MONEY_UNIT = new BigDecimal("0.00000001");

    private final PaymentPlanCompiler paymentPlanCompiler;
    private final PaymentPlanCatalogDao paymentPlanCatalogDao;
    private final PaymentPlanBucketDao paymentPlanBucketDao;
    private final PaymentPlanRouteOptionDao paymentPlanRouteOptionDao;
    private final PaymentPlanCacheService paymentPlanCacheService;

    @Override
    public PaymentPlanPreviewResponse preview(PaymentPlanPreviewRequest request) {
        PaymentPlanCompileResult compileResult = paymentPlanCompiler.compile(toCompileRequest(request));
        return toPreviewResponse(compileResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentPlanPublishResponse publish(PaymentPlanPublishRequest request) {
        try {
            PaymentPlanCompileRequest compileRequest = toCompileRequest(request);
            PaymentPlanCompileResult compileResult = paymentPlanCompiler.compile(compileRequest);
            if (!compileResult.isValid()) {
                throw new GkException(firstError(compileResult));
            }

            PaymentPlanCatalogEntity catalog = compileResult.getCatalog();
            catalog.setVersion(nextVersion(catalog));
            catalog.setStatus(PaymentPlanStatus.STAGING);
            paymentPlanCatalogDao.insert(catalog);

            for (PaymentPlanCompileResult.CompiledBucket compiledBucket : compileResult.getBuckets()) {
                PaymentPlanBucketEntity bucket = compiledBucket.getBucket();
                bucket.setTenantId(catalog.getTenantId());
                bucket.setCatalogId(catalog.getId());
                paymentPlanBucketDao.insert(bucket);
                for (PaymentPlanRouteOptionEntity routeOption : compiledBucket.getRouteOptions()) {
                    routeOption.setTenantId(catalog.getTenantId());
                    routeOption.setCatalogId(catalog.getId());
                    routeOption.setBucketId(bucket.getId());
                    paymentPlanRouteOptionDao.insert(routeOption);
                }
            }

            // 同一商户应用维度只允许一个 ACTIVE 决策表，先退旧版，再激活当前发布版本。
            PaymentPlanCatalogEntity retired = new PaymentPlanCatalogEntity();
            retired.setStatus(PaymentPlanStatus.RETIRED);
            paymentPlanCatalogDao.update(retired, sameCatalogWrapper(catalog)
                    .eq("status", PaymentPlanStatus.ACTIVE)
                    .ne("id", catalog.getId()));

            PaymentPlanCatalogEntity active = new PaymentPlanCatalogEntity();
            active.setId(catalog.getId());
            active.setStatus(PaymentPlanStatus.ACTIVE);
            active.setActivatedAt(Instant.now());
            paymentPlanCatalogDao.updateById(active);

            if (catalog.getMerchantAppId() == null || catalog.getMerchantAppId() <= 0) {
                paymentPlanCacheService.evictAll();
            } else {
                paymentPlanCacheService.evict(PaymentPlanKey.of(
                        catalog.getTenantId(),
                        catalog.getMerchantId(),
                        catalog.getMerchantAppId(),
                        catalog.getDirection(),
                        catalog.getCountryCode(),
                        catalog.getCurrency(),
                        catalog.getMethodCode()
                ));
            }

            PaymentPlanPublishResponse response = new PaymentPlanPublishResponse();
            response.setPublished(true);
            response.setCatalogId(catalog.getId());
            response.setVersionNo(catalog.getVersion());
            response.setStatus(PaymentPlanStatus.ACTIVE);
            response.setBucketCount(catalog.getBucketCount());
            response.setRouteOptionCount(catalog.getRouteOptionCount());
            response.setRedisEvicted(true);
            return response;
        } catch (DuplicateKeyException ex) {
            throw new GkException(ErrorCode.DB_RECORD_EXISTS, "支付计划版本已存在，请刷新预览后重新发布", ex);
        }
    }

    private Long nextVersion(PaymentPlanCatalogEntity catalog) {
        PaymentPlanCatalogEntity latest = paymentPlanCatalogDao.selectOne(sameCatalogWrapper(catalog)
                .orderByDesc("version")
                .last("limit 1"));
        return latest == null || latest.getVersion() == null ? 1L : latest.getVersion() + 1;
    }

    private QueryWrapper<PaymentPlanCatalogEntity> sameCatalogWrapper(PaymentPlanCatalogEntity catalog) {
        QueryWrapper<PaymentPlanCatalogEntity> wrapper = new QueryWrapper<PaymentPlanCatalogEntity>()
                .eq("tenant_id", catalog.getTenantId())
                .eq("merchant_id", catalog.getMerchantId())
                .eq("direction", catalog.getDirection())
                .eq("country_code", catalog.getCountryCode())
                .eq("currency", catalog.getCurrency())
                .eq("method_code", catalog.getMethodCode());
        wrapper.eq("merchant_app_id", catalogMerchantAppId(catalog.getMerchantAppId()));
        return wrapper;
    }

    private Long catalogMerchantAppId(Long merchantAppId) {
        return merchantAppId == null || merchantAppId <= 0 ? 0L : merchantAppId;
    }

    private PaymentPlanCompileRequest toCompileRequest(PaymentPlanPreviewRequest request) {
        PaymentPlanCompileRequest compileRequest = new PaymentPlanCompileRequest();
        if (request == null) {
            return compileRequest;
        }
        compileRequest.setTenantId(request.getTenantId());
        compileRequest.setMerchantId(request.getMerchantId());
        compileRequest.setMerchantAppId(request.getMerchantAppId());
        compileRequest.setMerchantFeeRuleId(request.getMerchantFeeRuleId());
        compileRequest.setDirection(request.getDirection());
        compileRequest.setCurrency(request.getCurrency());
        compileRequest.setCountryCode(request.getCountryCode());
        compileRequest.setMethodCode(request.getMethodCode());
        compileRequest.setMinAmount(request.getMinAmount());
        compileRequest.setMaxAmount(request.getMaxAmount());
        compileRequest.setPspFeeRequired(request.getPspFeeRequired());
        if (request.getTestCases() != null) {
            compileRequest.setTestCases(request.getTestCases().stream()
                    .map(item -> {
                        PaymentPlanCompileRequest.TestCase testCase = new PaymentPlanCompileRequest.TestCase();
                        testCase.setMerchantOrderId(item.getMerchantOrderId());
                        testCase.setAmount(item.getAmount());
                        testCase.setBankCode(item.getPayee() == null ? null : item.getPayee().getBankCode());
                        return testCase;
                    })
                    .toList());
        }
        return compileRequest;
    }

    private PaymentPlanCompileRequest toCompileRequest(PaymentPlanPublishRequest request) {
        PaymentPlanCompileRequest compileRequest = new PaymentPlanCompileRequest();
        if (request == null) {
            return compileRequest;
        }
        compileRequest.setTenantId(request.getTenantId());
        compileRequest.setMerchantId(request.getMerchantId());
        compileRequest.setMerchantAppId(request.getMerchantAppId());
        compileRequest.setMerchantFeeRuleId(request.getMerchantFeeRuleId());
        compileRequest.setDirection(request.getDirection());
        compileRequest.setCurrency(request.getCurrency());
        compileRequest.setCountryCode(request.getCountryCode());
        compileRequest.setMethodCode(request.getMethodCode());
        compileRequest.setMinAmount(request.getMinAmount());
        compileRequest.setMaxAmount(request.getMaxAmount());
        compileRequest.setPspFeeRequired(request.getPspFeeRequired());
        compileRequest.setRemark(request.getRemark());
        return compileRequest;
    }

    private PaymentPlanPreviewResponse toPreviewResponse(PaymentPlanCompileResult compileResult) {
        PaymentPlanPreviewResponse response = new PaymentPlanPreviewResponse();
        response.setValid(compileResult.isValid());
        if (compileResult.getCatalog() != null) {
            response.setDirection(compileResult.getCatalog().getDirection());
            response.setCurrency(compileResult.getCatalog().getCurrency());
            response.setCountryCode(compileResult.getCatalog().getCountryCode());
            response.setMethodCode(compileResult.getCatalog().getMethodCode());
            response.setBucketCount(compileResult.getCatalog().getBucketCount());
            response.setRouteOptionCount(compileResult.getCatalog().getRouteOptionCount());
        }
        compileResult.getWarnings().forEach(item -> response.addWarning(item.code(), item.message()));
        compileResult.getErrors().forEach(item -> response.addError(item.code(), item.message()));
        response.setBuckets(compileResult.getBuckets().stream().map(this::toBucketResponse).toList());
        response.setTestResults(compileResult.getTestResults().stream().map(this::toTestResultResponse).toList());
        return response;
    }

    private PaymentPlanPreviewResponse.Bucket toBucketResponse(PaymentPlanCompileResult.CompiledBucket compiledBucket) {
        PaymentPlanPreviewResponse.Bucket bucket = new PaymentPlanPreviewResponse.Bucket();
        bucket.setStartAmount(compiledBucket.getBucket().getBucketStartAmount());
        bucket.setEndAmount(displayEndAmount(compiledBucket.getBucket().getBucketEndAmount()));
        bucket.setAmountRangeText(amountRangeText(bucket.getStartAmount(), bucket.getEndAmount()));
        bucket.setMerchantFeeRuleId(compiledBucket.getBucket().getMerchantFeeRuleId());
        bucket.setRouteOptions(compiledBucket.getRouteOptions().stream().map(this::toRouteOptionResponse).toList());
        return bucket;
    }

    private PaymentPlanPreviewResponse.RouteOption toRouteOptionResponse(PaymentPlanRouteOptionEntity option) {
        PaymentPlanPreviewResponse.RouteOption response = new PaymentPlanPreviewResponse.RouteOption();
        response.setRouteRuleId(option.getRouteRuleId());
        response.setPspId(option.getPspId());
        response.setPspCode(option.getPspCode());
        response.setPspMethodId(option.getPspMethodId());
        response.setPspMethodCode(option.getPspMethodCode());
        response.setPspAccountId(option.getPspAccountId());
        response.setPspAccountNo(option.getPspAccountNo());
        response.setPspFeeRuleId(option.getPspFeeRuleId());
        response.setPriority(option.getPriority());
        response.setWeight(option.getWeight());
        response.setFallbackOrder(option.getFallbackOrder());
        response.setStatus(option.getStatus());
        return response;
    }

    private PaymentPlanPreviewResponse.TestResult toTestResultResponse(PaymentPlanCompileResult.TestResult item) {
        PaymentPlanPreviewResponse.TestResult response = new PaymentPlanPreviewResponse.TestResult();
        response.setMerchantOrderId(item.getMerchantOrderId());
        response.setAmount(item.getAmount());
        response.setMatched(item.isMatched());
        response.setMessage(item.getMessage());
        response.setMerchantFeeRuleId(item.getMerchantFeeRuleId());
        response.setMerchantFeeAmount(item.getMerchantFeeAmount());
        response.setSettleAmount(item.getSettleAmount());
        response.setTotalDebitAmount(item.getTotalDebitAmount());
        response.setPspId(item.getPspId());
        response.setPspCode(item.getPspCode());
        response.setPspAccountId(item.getPspAccountId());
        response.setPspFeeRuleId(item.getPspFeeRuleId());
        response.setPspFeeAmount(item.getPspFeeAmount());
        response.setBankCode(item.getBankCode());
        response.setPspBankCode(item.getPspBankCode());
        return response;
    }

    private String firstError(PaymentPlanCompileResult compileResult) {
        if (compileResult.getErrors().isEmpty()) {
            return "Payment plan compile failed";
        }
        PaymentPlanCompileResult.Message message = compileResult.getErrors().get(0);
        return message.code() + ": " + message.message();
    }

    private BigDecimal displayEndAmount(BigDecimal endAmount) {
        return endAmount == null ? null : endAmount.subtract(MONEY_UNIT);
    }

    private String amountRangeText(BigDecimal startAmount, BigDecimal endAmount) {
        return amountText(startAmount) + " - " + amountText(endAmount);
    }

    private String amountText(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }
}
