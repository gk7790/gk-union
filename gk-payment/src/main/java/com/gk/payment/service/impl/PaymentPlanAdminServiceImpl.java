package com.gk.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.enums.FeeBearerEnum;
import com.gk.common.enums.FeeModeEnum;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.infra.enums.StatusEnum;
import com.gk.payment.dao.PaymentPlanBucketDao;
import com.gk.payment.dao.PaymentPlanCatalogDao;
import com.gk.payment.dao.PaymentPlanRouteOptionDao;
import com.gk.payment.dao.PaymentRouteGroupDao;
import com.gk.payment.dao.PaymentRouteRuleDao;
import com.gk.payment.dto.PaymentPlanBatchPreviewRequest;
import com.gk.payment.dto.PaymentPlanBatchPreviewResponse;
import com.gk.payment.dto.PaymentPlanDetailResponse;
import com.gk.payment.dto.PaymentPlanPreviewRequest;
import com.gk.payment.dto.PaymentPlanPreviewResponse;
import com.gk.payment.dto.PaymentPlanPublishRequest;
import com.gk.payment.dto.PaymentPlanPublishResponse;
import com.gk.payment.dto.PaymentPlanVersionDTO;
import com.gk.payment.entity.PaymentPlanBucketEntity;
import com.gk.payment.entity.PaymentPlanCatalogEntity;
import com.gk.payment.entity.PaymentPlanRouteOptionEntity;
import com.gk.payment.entity.PaymentRouteChannelEntity;
import com.gk.payment.entity.PaymentRouteGroupEntity;
import com.gk.payment.entity.PaymentRouteRuleEntity;
import com.gk.payment.plan.PaymentPlanCacheService;
import com.gk.payment.plan.PaymentPlanCompileRequest;
import com.gk.payment.plan.PaymentPlanCompileResult;
import com.gk.payment.plan.PaymentPlanCompiler;
import com.gk.payment.plan.PaymentPlanKey;
import com.gk.payment.plan.PaymentPlanStatus;
import com.gk.payment.service.PaymentPlanAdminService;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.entity.PspFeeRuleEntity;
import com.gk.psp.entity.PspMethodEntity;
import com.gk.psp.entity.PspProviderEntity;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentPlanAdminServiceImpl implements PaymentPlanAdminService {
    private static final BigDecimal MONEY_UNIT = new BigDecimal("0.00000001");
    private static final BigDecimal DEFAULT_BATCH_MIN_AMOUNT = BigDecimal.ZERO;
    private static final BigDecimal DEFAULT_BATCH_MAX_AMOUNT = new BigDecimal("100000");

    private final PaymentPlanCompiler paymentPlanCompiler;
    private final PaymentPlanCatalogDao paymentPlanCatalogDao;
    private final PaymentPlanBucketDao paymentPlanBucketDao;
    private final PaymentPlanRouteOptionDao paymentPlanRouteOptionDao;
    private final PaymentRouteGroupDao paymentRouteGroupDao;
    private final PaymentRouteRuleDao paymentRouteRuleDao;
    private final PaymentPlanCacheService paymentPlanCacheService;

    @Override
    public PageData<PaymentPlanVersionDTO> page(DynMap params) {
        IPage<PaymentPlanCatalogEntity> page = paymentPlanCatalogDao.selectPage(
                catalogPage(params),
                catalogPageWrapper(params)
        );
        List<PaymentPlanVersionDTO> items = page.getRecords().stream()
                .map(this::toVersionDTO)
                .toList();
        return new PageData<>(items, page.getTotal());
    }

    @Override
    public PaymentPlanDetailResponse detail(Long catalogId) {
        PaymentPlanCatalogEntity catalog = requireCatalog(catalogId);
        List<PaymentPlanBucketEntity> buckets = paymentPlanBucketDao.selectList(new QueryWrapper<PaymentPlanBucketEntity>()
                .eq("tenant_id", catalog.getTenantId())
                .eq("catalog_id", catalog.getId())
                .orderByAsc("sort")
                .orderByAsc("id"));
        List<PaymentPlanRouteOptionEntity> options = paymentPlanRouteOptionDao.selectList(new QueryWrapper<PaymentPlanRouteOptionEntity>()
                .eq("tenant_id", catalog.getTenantId())
                .eq("catalog_id", catalog.getId())
                .orderByAsc("bucket_id")
                .orderByAsc("sort")
                .orderByAsc("priority")
                .orderByAsc("id"));
        Map<Long, List<PaymentPlanRouteOptionEntity>> optionsByBucket = options.stream()
                .collect(Collectors.groupingBy(PaymentPlanRouteOptionEntity::getBucketId));

        PaymentPlanDetailResponse response = new PaymentPlanDetailResponse();
        response.setCatalog(toDetailCatalog(catalog));
        response.setBuckets(buckets.stream()
                .map(bucket -> toDetailBucket(bucket, optionsByBucket.getOrDefault(bucket.getId(), List.of())))
                .toList());
        return response;
    }

    @Override
    public PaymentPlanPreviewResponse preview(PaymentPlanPreviewRequest request) {
        PaymentPlanCompileResult compileResult = paymentPlanCompiler.compile(toCompileRequest(request));
        return toPreviewResponse(compileResult);
    }

    @Override
    public PaymentPlanBatchPreviewResponse batchPreviewByRouteGroup(PaymentPlanBatchPreviewRequest request) {
        if (request == null || request.getRouteGroupId() == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "路由组不能为");
        }
        PaymentRouteGroupEntity group = paymentRouteGroupDao.selectById(request.getRouteGroupId());
        if (group == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "路由组不存在");
        }
        List<PaymentRouteRuleEntity> routeRules = paymentRouteRuleDao.selectList(new QueryWrapper<PaymentRouteRuleEntity>()
                .eq("tenant_id", tenantId(request, group))
                .eq("group_id", group.getId())
                .eq("status", StatusEnum.NORMAL.code())
                .orderByAsc("priority")
                .orderByAsc("id"));

        PaymentPlanBatchPreviewResponse response = new PaymentPlanBatchPreviewResponse();
        response.setTenantId(group.getTenantId());
        response.setRouteGroupId(group.getId());
        response.setGroupCode(group.getGroupCode());
        response.setGroupName(group.getGroupName());

        for (PaymentRouteRuleEntity routeRule : routeRules) {
            PaymentPlanBatchPreviewResponse.Item item = batchPreviewItem(request, routeRule);
            response.getItems().add(item);
            if (Boolean.TRUE.equals(item.getSkipped())) {
                response.setSkippedCount(response.getSkippedCount() + 1);
            } else if (Boolean.TRUE.equals(item.getValid())) {
                response.setValidCount(response.getValidCount() + 1);
            } else {
                response.setInvalidCount(response.getInvalidCount() + 1);
            }
        }
        response.setTotal(response.getItems().size());
        return response;
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

            // 同一商户应用维度只允许一ACTIVE 决策表，先退旧版，再激活当前发布版本
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

            evictCatalogCache(catalog);

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentPlanPublishResponse activate(Long catalogId) {
        PaymentPlanCatalogEntity catalog = requireCatalog(catalogId);
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

        catalog.setStatus(PaymentPlanStatus.ACTIVE);
        catalog.setActivatedAt(active.getActivatedAt());
        evictCatalogCache(catalog);

        PaymentPlanPublishResponse response = new PaymentPlanPublishResponse();
        response.setPublished(true);
        response.setCatalogId(catalog.getId());
        response.setVersionNo(catalog.getVersion());
        response.setStatus(PaymentPlanStatus.ACTIVE);
        response.setBucketCount(catalog.getBucketCount());
        response.setRouteOptionCount(catalog.getRouteOptionCount());
        response.setRedisEvicted(true);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentPlanPublishResponse retire(Long catalogId) {
        PaymentPlanCatalogEntity catalog = requireCatalog(catalogId);
        PaymentPlanCatalogEntity retired = new PaymentPlanCatalogEntity();
        retired.setId(catalog.getId());
        retired.setStatus(PaymentPlanStatus.RETIRED);
        paymentPlanCatalogDao.updateById(retired);

        catalog.setStatus(PaymentPlanStatus.RETIRED);
        evictCatalogCache(catalog);

        PaymentPlanPublishResponse response = new PaymentPlanPublishResponse();
        response.setPublished(false);
        response.setCatalogId(catalog.getId());
        response.setVersionNo(catalog.getVersion());
        response.setStatus(PaymentPlanStatus.RETIRED);
        response.setBucketCount(catalog.getBucketCount());
        response.setRouteOptionCount(catalog.getRouteOptionCount());
        response.setRedisEvicted(true);
        return response;
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

    private QueryWrapper<PaymentPlanCatalogEntity> catalogPageWrapper(DynMap params) {
        QueryWrapper<PaymentPlanCatalogEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Long merchantAppId = params.getLong("merchantAppId", null);
        String direction = params.getStr("direction");
        String countryCode = params.getStr("countryCode");
        String currency = params.getStr("currency");
        String methodCode = params.getStr("methodCode");
        String status = params.getStr("status");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(merchantAppId != null, "merchant_app_id", catalogMerchantAppId(merchantAppId));
        wrapper.eq(StringUtils.isNotBlank(direction), "direction", StringUtils.upperCase(StringUtils.trim(direction)));
        wrapper.eq(StringUtils.isNotBlank(countryCode), "country_code", StringUtils.upperCase(StringUtils.trim(countryCode)));
        wrapper.eq(StringUtils.isNotBlank(currency), "currency", StringUtils.upperCase(StringUtils.trim(currency)));
        wrapper.eq(StringUtils.isNotBlank(methodCode), "method_code", StringUtils.upperCase(StringUtils.trim(methodCode)));
        wrapper.eq(StringUtils.isNotBlank(status), "status", StringUtils.upperCase(StringUtils.trim(status)));
        return wrapper;
    }

    private IPage<PaymentPlanCatalogEntity> catalogPage(DynMap params) {
        long current = params.getLong("page", 1L);
        long size = params.getLong("limit", 10L);
        Page<PaymentPlanCatalogEntity> page = new Page<>(current, size);
        page.addOrder(OrderItem.desc("id"));
        return page;
    }

    private PaymentPlanCatalogEntity requireCatalog(Long catalogId) {
        if (catalogId == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Payment plan id is required");
        }
        PaymentPlanCatalogEntity catalog = paymentPlanCatalogDao.selectById(catalogId);
        if (catalog == null) {
            throw new GkException(ErrorCode.NOT_FOUND, "Payment plan does not exist");
        }
        return catalog;
    }

    private void evictCatalogCache(PaymentPlanCatalogEntity catalog) {
        if (catalog.getMerchantAppId() == null || catalog.getMerchantAppId() <= 0) {
            paymentPlanCacheService.evictAll();
            return;
        }
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

    private PaymentPlanVersionDTO toVersionDTO(PaymentPlanCatalogEntity catalog) {
        PaymentPlanVersionDTO response = new PaymentPlanVersionDTO();
        BeanUtils.copyProperties(catalog, response);
        return response;
    }

    private PaymentPlanDetailResponse.Catalog toDetailCatalog(PaymentPlanCatalogEntity catalog) {
        PaymentPlanDetailResponse.Catalog response = new PaymentPlanDetailResponse.Catalog();
        BeanUtils.copyProperties(catalog, response);
        return response;
    }

    private PaymentPlanDetailResponse.Bucket toDetailBucket(PaymentPlanBucketEntity bucket,
                                                            List<PaymentPlanRouteOptionEntity> routeOptions) {
        PaymentPlanDetailResponse.Bucket response = new PaymentPlanDetailResponse.Bucket();
        BeanUtils.copyProperties(bucket, response);
        response.setRouteOptions(routeOptions.stream().map(this::toDetailRouteOption).toList());
        return response;
    }

    private PaymentPlanDetailResponse.RouteOption toDetailRouteOption(PaymentPlanRouteOptionEntity option) {
        PaymentPlanDetailResponse.RouteOption response = new PaymentPlanDetailResponse.RouteOption();
        BeanUtils.copyProperties(option, response);
        return response;
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

    private PaymentPlanBatchPreviewResponse.Item batchPreviewItem(PaymentPlanBatchPreviewRequest request,
                                                                  PaymentRouteRuleEntity routeRule) {
        PaymentPlanBatchPreviewResponse.Item item = new PaymentPlanBatchPreviewResponse.Item();
        item.setRouteRuleId(routeRule.getId());
        item.setRouteRuleName(routeRule.getRuleName());
        item.setMerchantId(routeRule.getMerchantId());
        item.setMerchantAppId(routeRule.getMerchantAppId());
        item.setDirection(routeRule.getDirection());
        item.setCountryCode(routeRule.getCountryCode());
        item.setCurrency(routeRule.getCurrency());
        item.setMethodCode(routeRule.getMethodCode());
        item.setMinAmount(batchMinAmount(request));
        if (routeRule.getMerchantId() == null) {
            item.setMaxAmount(batchFallbackMaxAmount(request));
            item.setSkipped(true);
            item.setValid(false);
            item.setSkipReason("通用路由规则未指定商户，批量预览需要人工选择商户范围");
            item.getWarnings().add(new PaymentPlanPreviewResponse.Message("GENERIC_ROUTE_RULE_SKIPPED", item.getSkipReason()));
            item.setWarningCount(item.getWarnings().size());
            return item;
        }
        item.setMaxAmount(batchMaxAmount(request, routeRule));

        PaymentPlanCompileRequest compileRequest = toCompileRequest(request, routeRule, item.getMinAmount(), item.getMaxAmount());
        PaymentPlanPreviewResponse preview = toPreviewResponse(paymentPlanCompiler.compile(compileRequest));
        item.setPreview(preview);
        item.setValid(Boolean.TRUE.equals(preview.getValid()));
        item.setBucketCount(preview.getBucketCount());
        item.setRouteOptionCount(preview.getRouteOptionCount());
        item.setWarnings(preview.getWarnings());
        item.setErrors(preview.getErrors());
        item.setWarningCount(preview.getWarnings().size());
        item.setErrorCount(preview.getErrors().size());
        return item;
    }

    private PaymentPlanCompileRequest toCompileRequest(PaymentPlanBatchPreviewRequest request,
                                                       PaymentRouteRuleEntity routeRule,
                                                       BigDecimal minAmount,
                                                       BigDecimal maxAmount) {
        PaymentPlanCompileRequest compileRequest = new PaymentPlanCompileRequest();
        compileRequest.setTenantId(routeRule.getTenantId());
        compileRequest.setMerchantId(routeRule.getMerchantId());
        compileRequest.setMerchantAppId(routeRule.getMerchantAppId());
        compileRequest.setDirection(routeRule.getDirection());
        compileRequest.setCountryCode(routeRule.getCountryCode());
        compileRequest.setCurrency(routeRule.getCurrency());
        compileRequest.setMethodCode(routeRule.getMethodCode());
        compileRequest.setMinAmount(minAmount);
        compileRequest.setMaxAmount(maxAmount);
        compileRequest.setPspFeeRequired(request == null ? Boolean.FALSE : request.getPspFeeRequired());
        return compileRequest;
    }

    private Long tenantId(PaymentPlanBatchPreviewRequest request, PaymentRouteGroupEntity group) {
        return request.getTenantId() == null ? group.getTenantId() : request.getTenantId();
    }

    private BigDecimal batchMinAmount(PaymentPlanBatchPreviewRequest request) {
        if (request != null && request.getDefaultMinAmount() != null) {
            return request.getDefaultMinAmount();
        }
        return DEFAULT_BATCH_MIN_AMOUNT;
    }

    private BigDecimal batchMaxAmount(PaymentPlanBatchPreviewRequest request, PaymentRouteRuleEntity routeRule) {
        if (request != null && request.getDefaultMaxAmount() != null && request.getDefaultMaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            return request.getDefaultMaxAmount();
        }
        BigDecimal activeMaxAmount = activeMaxAmount(routeRule);
        return activeMaxAmount == null || activeMaxAmount.compareTo(BigDecimal.ZERO) <= 0 ? DEFAULT_BATCH_MAX_AMOUNT : activeMaxAmount;
    }

    private BigDecimal batchFallbackMaxAmount(PaymentPlanBatchPreviewRequest request) {
        if (request != null && request.getDefaultMaxAmount() != null && request.getDefaultMaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            return request.getDefaultMaxAmount();
        }
        return DEFAULT_BATCH_MAX_AMOUNT;
    }

    private BigDecimal activeMaxAmount(PaymentRouteRuleEntity routeRule) {
        PaymentPlanCatalogEntity activeCatalog = paymentPlanCatalogDao.selectOne(new QueryWrapper<PaymentPlanCatalogEntity>()
                .eq("tenant_id", routeRule.getTenantId())
                .eq("merchant_id", routeRule.getMerchantId())
                .eq("merchant_app_id", catalogMerchantAppId(routeRule.getMerchantAppId()))
                .eq("direction", routeRule.getDirection())
                .eq("country_code", routeRule.getCountryCode())
                .eq("currency", routeRule.getCurrency())
                .eq("method_code", routeRule.getMethodCode())
                .eq("status", PaymentPlanStatus.ACTIVE)
                .orderByDesc("version")
                .last("limit 1"));
        if (activeCatalog == null) {
            return null;
        }
        PaymentPlanBucketEntity maxBucket = paymentPlanBucketDao.selectOne(new QueryWrapper<PaymentPlanBucketEntity>()
                .eq("tenant_id", activeCatalog.getTenantId())
                .eq("catalog_id", activeCatalog.getId())
                .orderByDesc("bucket_end_amount")
                .last("limit 1"));
        return maxBucket == null ? null : displayEndAmount(maxBucket.getBucketEndAmount());
    }

    private PaymentPlanPreviewResponse toPreviewResponse(PaymentPlanCompileResult compileResult) {
        PaymentPlanPreviewResponse response = new PaymentPlanPreviewResponse();
        response.setValid(compileResult.isValid());
        response.setRequestInfo(toRequestInfoResponse(compileResult.getRequest()));
        if (compileResult.getRequest() != null) {
            response.setDirection(compileResult.getRequest().getDirection());
            response.setCurrency(compileResult.getRequest().getCurrency());
            response.setCountryCode(compileResult.getRequest().getCountryCode());
            response.setMethodCode(compileResult.getRequest().getMethodCode());
        }
        if (compileResult.getCatalog() != null) {
            response.setDirection(compileResult.getCatalog().getDirection());
            response.setCurrency(compileResult.getCatalog().getCurrency());
            response.setCountryCode(compileResult.getCatalog().getCountryCode());
            response.setMethodCode(compileResult.getCatalog().getMethodCode());
            response.setBucketCount(compileResult.getCatalog().getBucketCount());
            response.setRouteOptionCount(compileResult.getCatalog().getRouteOptionCount());
        }
        response.setMerchantFeeRules(compileResult.getMerchantFeeRules().stream().map(this::toMerchantFeeRuleResponse).toList());
        response.setRouteRules(compileResult.getPaymentRouteRules().stream().map(rule -> toRouteResponse(rule, null)).toList());
        response.setRouteGroups(compileResult.getRouteGroups().stream().map(this::toRouteGroupResponse).toList());
        response.setRouteChannels(compileResult.getRouteChannels().stream().map(this::toRouteChannelResponse).toList());
        response.setPspMethods(compileResult.getPspMethods().stream().map(this::toPspMethodResponse).toList());
        response.setPspFeeRules(compileResult.getPspFeeRules().stream().map(this::toPspFeeRuleResponse).toList());
        response.setRouteOptions(compileResult.getRouteOptionDiagnostics().stream().map(this::toRouteOptionResponse).toList());
        compileResult.getWarnings().forEach(item -> response.addWarning(item.code(), item.message()));
        compileResult.getErrors().forEach(item -> response.addError(item.code(), item.message()));
        response.setBuckets(compileResult.getBuckets().stream().map(this::toBucketResponse).toList());
        response.setTestResults(compileResult.getTestResults().stream().map(this::toTestResultResponse).toList());
        return response;
    }

    private PaymentPlanPreviewResponse.RequestInfo toRequestInfoResponse(PaymentPlanCompileRequest request) {
        if (request == null) {
            return null;
        }
        PaymentPlanPreviewResponse.RequestInfo response = new PaymentPlanPreviewResponse.RequestInfo();
        response.setTenantId(request.getTenantId());
        response.setMerchantId(request.getMerchantId());
        response.setMerchantAppId(request.getMerchantAppId());
        response.setMerchantFeeRuleId(request.getMerchantFeeRuleId());
        response.setDirection(request.getDirection());
        response.setCountryCode(request.getCountryCode());
        response.setCurrency(request.getCurrency());
        response.setMethodCode(request.getMethodCode());
        response.setMinAmount(request.getMinAmount());
        response.setMaxAmount(request.getMaxAmount());
        response.setPspFeeRequired(request.getPspFeeRequired());
        return response;
    }

    private PaymentPlanPreviewResponse.Bucket toBucketResponse(PaymentPlanCompileResult.CompiledBucket compiledBucket) {
        PaymentPlanPreviewResponse.Bucket bucket = new PaymentPlanPreviewResponse.Bucket();
        bucket.setStartAmount(compiledBucket.getBucket().getBucketStartAmount());
        bucket.setEndAmount(displayEndAmount(compiledBucket.getBucket().getBucketEndAmount()));
        bucket.setAmountRangeText(amountRangeText(bucket.getStartAmount(), bucket.getEndAmount()));
        bucket.setMerchantFeeRuleId(compiledBucket.getBucket().getMerchantFeeRuleId());
        bucket.setMerchantFeeRule(toMerchantFeeRuleResponse(compiledBucket.getMerchantFeeRule()));
        if (compiledBucket.getRouteOptionDetails() != null && !compiledBucket.getRouteOptionDetails().isEmpty()) {
            bucket.setRouteOptions(compiledBucket.getRouteOptionDetails().stream().map(this::toRouteOptionResponse).toList());
        } else {
            bucket.setRouteOptions(compiledBucket.getRouteOptions().stream().map(this::toRouteOptionResponse).toList());
        }
        return bucket;
    }

    private PaymentPlanPreviewResponse.RouteOption toRouteOptionResponse(PaymentPlanCompileResult.CompiledRouteOption detail) {
        PaymentPlanRouteOptionEntity option = detail.getOption();
        PaymentPlanPreviewResponse.RouteOption response = toRouteOptionResponse(option);
        response.setRoute(toRouteResponse(detail.getPaymentRouteRule(), option));
        response.setRouteGroup(toRouteGroupResponse(detail.getRouteGroup()));
        response.setRouteChannel(toRouteChannelResponse(detail.getRouteChannel()));
        response.setPsp(toPspResponse(detail.getProvider()));
        response.setPspMethod(toPspMethodResponse(detail.getMethod()));
        response.setPspAccount(toPspAccountResponse(detail.getAccount()));
        response.setPspFeeRule(toPspFeeRuleResponse(detail.getPspFeeRule()));
        return response;
    }

    private PaymentPlanPreviewResponse.RouteOption toRouteOptionResponse(PaymentPlanRouteOptionEntity option) {
        PaymentPlanPreviewResponse.RouteOption response = new PaymentPlanPreviewResponse.RouteOption();
        response.setRouteRuleId(option.getRouteRuleId());
        response.setRouteGroupId(option.getRouteGroupId());
        response.setRouteChannelId(option.getRouteChannelId());
        response.setPspId(option.getPspId());
        response.setPspCode(option.getPspCode());
        response.setPspMethodId(option.getPspMethodId());
        response.setPspMethodCode(option.getPspMethodCode());
        response.setPspAccountId(option.getPspAccountId());
        response.setPspAccountNo(option.getPspAccountNo());
        response.setPspFeeRuleId(option.getPspFeeRuleId());
        response.setPspFeeSnapshotJson(option.getPspFeeSnapshotJson());
        response.setRouteRuleSnapshotJson(option.getRouteRuleSnapshotJson());
        response.setRouteGroupSnapshotJson(option.getRouteGroupSnapshotJson());
        response.setRouteChannelSnapshotJson(option.getRouteChannelSnapshotJson());
        response.setPspProviderSnapshotJson(option.getPspProviderSnapshotJson());
        response.setPspMethodSnapshotJson(option.getPspMethodSnapshotJson());
        response.setPspAccountSnapshotJson(option.getPspAccountSnapshotJson());
        response.setPriority(option.getPriority());
        response.setWeight(option.getWeight());
        response.setFallbackOrder(option.getFallbackOrder());
        response.setStatus(option.getStatus());
        return response;
    }

    private PaymentPlanPreviewResponse.MerchantFeeRule toMerchantFeeRuleResponse(com.gk.payment.entity.MerchantFeeRuleEntity rule) {
        if (rule == null) {
            return null;
        }
        PaymentPlanPreviewResponse.MerchantFeeRule response = new PaymentPlanPreviewResponse.MerchantFeeRule();
        response.setId(rule.getId());
        response.setRuleName(rule.getRuleName());
        response.setDirection(rule.getDirection());
        response.setCountryCode(rule.getCountryCode());
        response.setCurrency(rule.getCurrency());
        response.setMethodCode(rule.getMethodCode());
        response.setMinAmount(rule.getMinAmount());
        response.setMaxAmount(rule.getMaxAmount());
        response.setFeeMode(rule.getFeeMode());
        response.setFeeModeName(enumLabel(FeeModeEnum.class, rule.getFeeMode()));
        response.setFeeRate(rule.getFeeRate());
        response.setFeeFixed(rule.getFeeFixed());
        response.setMinFee(rule.getMinFee());
        response.setMaxFee(rule.getMaxFee());
        response.setFeeBearer(rule.getFeeBearer());
        response.setFeeBearerName(enumLabel(FeeBearerEnum.class, rule.getFeeBearer()));
        response.setSettleMode(rule.getSettleMode());
        response.setPriority(rule.getPriority());
        response.setStatus(rule.getStatus());
        response.setRemark(rule.getRemark());
        return response;
    }

    private PaymentPlanPreviewResponse.Route toRouteResponse(PaymentRouteRuleEntity paymentRule,
                                                            PaymentPlanRouteOptionEntity option) {
        if (paymentRule == null && option == null) {
            return null;
        }
        PaymentPlanPreviewResponse.Route response = new PaymentPlanPreviewResponse.Route();
        response.setRouteRuleId(option == null ? routeRuleId(paymentRule) : option.getRouteRuleId());
        if (paymentRule != null) {
            response.setRouteName(paymentRule.getRuleName());
            response.setRouteMode(null);
            response.setCountryCode(paymentRule.getCountryCode());
            response.setCurrency(paymentRule.getCurrency());
            response.setMethodCode(paymentRule.getMethodCode());
            response.setDirection(paymentRule.getDirection());
            response.setMinAmount(paymentRule.getMinAmount());
            response.setMaxAmount(paymentRule.getMaxAmount());
            response.setPriority(option == null ? paymentRule.getPriority() : option.getPriority());
            response.setWeight(option == null ? null : option.getWeight());
            response.setFallbackOrder(option == null ? null : option.getFallbackOrder());
            response.setStatus(paymentRule.getStatus());
            response.setRemark(paymentRule.getRemark());
            return response;
        }
        response.setPriority(option.getPriority());
        response.setWeight(option.getWeight());
        response.setFallbackOrder(option.getFallbackOrder());
        return response;
    }

    private Long routeRuleId(PaymentRouteRuleEntity paymentRule) {
        return paymentRule == null ? null : paymentRule.getId();
    }

    private PaymentPlanPreviewResponse.RouteGroup toRouteGroupResponse(PaymentRouteGroupEntity group) {
        if (group == null) {
            return null;
        }
        PaymentPlanPreviewResponse.RouteGroup response = new PaymentPlanPreviewResponse.RouteGroup();
        response.setRouteGroupId(group.getId());
        response.setGroupCode(group.getGroupCode());
        response.setGroupName(group.getGroupName());
        response.setDirection(group.getDirection());
        response.setCountryCode(group.getCountryCode());
        response.setCurrency(group.getCurrency());
        response.setMethodCode(group.getMethodCode());
        response.setStrategy(group.getStrategy());
        response.setStatus(group.getStatus());
        response.setRemark(group.getRemark());
        return response;
    }

    private PaymentPlanPreviewResponse.RouteChannel toRouteChannelResponse(PaymentRouteChannelEntity channel) {
        if (channel == null) {
            return null;
        }
        PaymentPlanPreviewResponse.RouteChannel response = new PaymentPlanPreviewResponse.RouteChannel();
        response.setRouteChannelId(channel.getId());
        response.setRouteGroupId(channel.getGroupId());
        response.setPspId(channel.getPspId());
        response.setPspMethodId(channel.getPspMethodId());
        response.setPspAccountId(channel.getPspAccountId());
        response.setPspFeeRuleId(channel.getPspFeeRuleId());
        response.setPriority(channel.getPriority());
        response.setWeight(channel.getWeight());
        response.setFallbackOrder(channel.getFallbackOrder());
        response.setMinAmount(channel.getMinAmount());
        response.setMaxAmount(channel.getMaxAmount());
        response.setStatus(channel.getStatus());
        response.setRemark(channel.getRemark());
        return response;
    }

    private PaymentPlanPreviewResponse.Psp toPspResponse(PspProviderEntity provider) {
        if (provider == null) {
            return null;
        }
        PaymentPlanPreviewResponse.Psp response = new PaymentPlanPreviewResponse.Psp();
        response.setPspId(provider.getId());
        response.setPspCode(provider.getPspCode());
        response.setPspName(provider.getPspName());
        response.setCountryCode(provider.getCountryCode());
        response.setApiVersion(provider.getApiVersion());
        response.setSupportPayin(provider.getSupportPayin());
        response.setSupportPayout(provider.getSupportPayout());
        response.setStatus(provider.getStatus());
        response.setRemark(provider.getRemark());
        return response;
    }

    private PaymentPlanPreviewResponse.PspMethod toPspMethodResponse(PspMethodEntity method) {
        if (method == null) {
            return null;
        }
        PaymentPlanPreviewResponse.PspMethod response = new PaymentPlanPreviewResponse.PspMethod();
        response.setPspMethodId(method.getId());
        response.setPspId(method.getPspId());
        response.setPspCode(method.getPspCode());
        response.setMethodCode(method.getMethodCode());
        response.setPspMethodCode(method.getPspMethodCode());
        response.setMethodName(method.getMethodName());
        response.setCountryCode(method.getCountryCode());
        response.setCurrency(method.getCurrency());
        response.setDirection(method.getDirection());
        response.setMinAmount(method.getMinAmount());
        response.setMaxAmount(method.getMaxAmount());
        response.setDailyLimit(method.getDailyLimit());
        response.setStatus(method.getStatus());
        response.setRemark(method.getRemark());
        return response;
    }

    private PaymentPlanPreviewResponse.PspAccount toPspAccountResponse(PspAccountEntity account) {
        if (account == null) {
            return null;
        }
        PaymentPlanPreviewResponse.PspAccount response = new PaymentPlanPreviewResponse.PspAccount();
        response.setPspAccountId(account.getId());
        response.setPspId(account.getPspId());
        response.setPspAccountNo(account.getPspAccountNo());
        response.setPspAccountName(account.getPspAccountName());
        response.setSecretType(account.getSecretType());
        response.setStatus(account.getStatus());
        response.setRemark(account.getRemark());
        return response;
    }

    private PaymentPlanPreviewResponse.PspFeeRule toPspFeeRuleResponse(PspFeeRuleEntity rule) {
        if (rule == null) {
            return null;
        }
        PaymentPlanPreviewResponse.PspFeeRule response = new PaymentPlanPreviewResponse.PspFeeRule();
        response.setId(rule.getId());
        response.setPspId(rule.getPspId());
        response.setPspAccountId(rule.getPspAccountId());
        response.setPspMethodId(rule.getPspMethodId());
        response.setPspMethodCode(rule.getPspMethodCode());
        response.setRuleName(rule.getRuleName());
        response.setDirection(rule.getDirection());
        response.setCountryCode(rule.getCountryCode());
        response.setCurrency(rule.getCurrency());
        response.setMethodCode(rule.getMethodCode());
        response.setMinAmount(rule.getMinAmount());
        response.setMaxAmount(rule.getMaxAmount());
        response.setFeeMode(rule.getFeeMode());
        response.setFeeModeName(enumLabel(FeeModeEnum.class, rule.getFeeMode()));
        response.setFeeRate(rule.getFeeRate());
        response.setFeeFixed(rule.getFeeFixed());
        response.setMinFee(rule.getMinFee());
        response.setMaxFee(rule.getMaxFee());
        response.setPriority(rule.getPriority());
        response.setStatus(rule.getStatus());
        response.setRemark(rule.getRemark());
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
        PaymentPlanCompileResult.Message message = compileResult.getErrors().getFirst();
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

    private <E extends Enum<E> & StringCodeEnum> String enumLabel(Class<E> type, String code) {
        E item = StringCodeEnum.fromCode(type, code);
        return item == null ? null : item.label();
    }
}
