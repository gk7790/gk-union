package com.gk.merchant.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gk.common.context.ReqContext;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.utils.BizKeyUtils;
import com.gk.common.utils.ConvertUtils;
import com.gk.common.validator.AssertUtils;
import com.gk.common.enums.SignTypeEnum;
import com.gk.infra.enums.StatusEnum;
import com.gk.merchant.enums.MerchantAppEnvEnum;
import com.gk.merchant.enums.EncryptTypeEnum;
import com.gk.merchant.enums.MerchantAppTypeEnum;
import com.gk.merchant.dao.MerchantAppDao;
import com.gk.merchant.dto.MerchantAppDTO;
import com.gk.merchant.entity.MerchantAppEntity;
import com.gk.common.openapi.OpenApiAuthCacheEvictor;
import com.gk.merchant.service.MerchantAppService;
import com.gk.merchant.service.MerchantPaymentPlanCacheEvictor;
import com.gk.merchant.support.MerchantAppSecrets;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MerchantAppServiceImpl extends CrudServiceImpl<MerchantAppDao, MerchantAppEntity, MerchantAppDTO> implements MerchantAppService {
    private static final int DEFAULT_RATE_LIMIT_QPS = 100;
    private static final int DEFAULT_NONCE_TTL_SECONDS = 3000;
    private static final int APP_ID_GENERATE_MAX_ATTEMPTS = 5;

    private final MerchantPaymentPlanCacheEvictor paymentPlanCacheEvictor;
    private final OpenApiAuthCacheEvictor openApiAuthCacheEvictor;

    @Override
    public QueryWrapper<MerchantAppEntity> getWrapper(DynMap params) {
        QueryWrapper<MerchantAppEntity> wrapper = new QueryWrapper<>();
        ReqContext context = ReqContextHolder.get();
        if (SubjectTypeEnum.PLATFORM.code().equals(context.getSubjectType())) {
            Long tenantId = params.getLong("tenantId", null);
            Long merchantId = params.getLong("merchantId", null);
            wrapper.eq(tenantId != null, "tenant_id", tenantId);
            wrapper.eq(merchantId != null, "merchant_id", merchantId);
        } else if (SubjectTypeEnum.TENANT.code().equals(context.getSubjectType())) {
            wrapper.eq("tenant_id", context.getTenantId());
            if (!params.getBool("isAll", false)) {
                Long merchantId = params.getLong("merchantId", 0L);
                wrapper.eq(merchantId > 0, "merchant_id", merchantId);
            }
        } else {
            wrapper.eq("tenant_id", context.getTenantId());
            wrapper.eq("merchant_id", context.getMerchantId());
        }
        List<Integer> statusList = StatusEnum.normalizeQueryStatus(params.getList("status", Integer.class, StatusEnum.defaultStatus()));
        String appId = params.getStr("appId");
        String appName = params.getStr("appName");
        String appType = params.getStr("appType");
        String appEnv = params.getStr("appEnv");
        String signType = params.getStr("signType");

        wrapper.in("status", statusList);
        wrapper.eq(StrUtil.isNotBlank(appId), "app_id", appId);
        wrapper.like(StrUtil.isNotBlank(appName), "app_name", appName);
        wrapper.eq(StrUtil.isNotBlank(appType), "app_type", appType);
        wrapper.eq(StrUtil.isNotBlank(appEnv), "app_env", appEnv);
        wrapper.eq(StrUtil.isNotBlank(signType), "sign_type", signType);

        return wrapper;
    }

    @Override
    public PageData<MerchantAppDTO> page(DynMap params) {
        IPage<MerchantAppEntity> page = baseDao.selectPage(
                getPage(params, "id", false),
                getWrapper(params)
        );
        PageData<MerchantAppDTO> page1 = getPageData(page, currentDtoClass());
        maskSecrets(page1.getItems());
        return page1;
    }

    @Override
    public MerchantAppDTO get(Long id) {
        return super.get(id);
    }

    @Override
    public List<MerchantAppDTO> getDict(DynMap params) {
        QueryWrapper<MerchantAppEntity> wrapper = new QueryWrapper<>();
        wrapper.select("id", "tenant_id", "merchant_id", "app_id", "app_name", "app_type", "app_env");
        wrapper.eq("status", StatusEnum.NORMAL.code());
        ReqContext context = ReqContextHolder.get();
        if (SubjectTypeEnum.PLATFORM.code().equals(context.getSubjectType())) {
            Long tenantId = params.getLong("tenantId", 0L);
            Long merchantId = params.getLong("merchantId", 0L);
            wrapper.eq(tenantId > 0, "tenant_id", tenantId);
            wrapper.eq(merchantId > 0, "merchant_id", merchantId);
        } else if (SubjectTypeEnum.TENANT.code().equals(context.getSubjectType())) {
            wrapper.eq("tenant_id", context.getTenantId());
            Long merchantId = params.getLong("merchantId", 0L);
            wrapper.eq(merchantId > 0, "merchant_id", merchantId);
        } else {
            wrapper.eq("tenant_id", context.getTenantId());
            wrapper.eq("merchant_id", context.getMerchantId());
        }
        List<MerchantAppEntity> list = baseDao.selectList(wrapper);
        return ConvertUtils.sourceToTarget(list,  MerchantAppDTO.class);
    }

    @Override
    public void save(MerchantAppDTO dto) {
        AssertUtils.isNull(dto.getTenantId(), "tenantId");
        AssertUtils.isNull(dto.getMerchantId(), "merchantId");
        AssertUtils.isBlank(dto.getAppName(), "appName");

        MerchantAppEntity entity = ConvertUtils.sourceToTarget(dto, MerchantAppEntity.class);
        entity.setId(null);
        entity.setAppId(null);
        entity.setApiSecret(null);
        applyCreateDefaults(entity);
        validateTestAppUnique(entity, null);

        String apiSecret = BizKeyUtils.genApiSecret();
        entity.setApiSecret(apiSecret);
        entity.setSecretVersion(1);
        entity.setSecretUpdatedAt(Instant.now());

        insertWithGeneratedAppId(entity);

        dto.setId(entity.getId());
        dto.setAppId(entity.getAppId());
        dto.setAppEnv(entity.getAppEnv());
        dto.setApiSecret(apiSecret);
        dto.setSecretVersion(entity.getSecretVersion());
        dto.setSecretUpdatedAt(entity.getSecretUpdatedAt());
        evictPayinPlanCache();
        evictMerchantAppCache(entity.getAppId());
    }

    @Override
    public void update(MerchantAppDTO dto) {
        AssertUtils.isNull(dto.getId(), "id");
        MerchantAppEntity existed = baseDao.selectById(dto.getId());
        if (existed == null) {
            throw new GkException(ErrorCode.NOT_FOUND);
        }

        MerchantAppEntity entity = ConvertUtils.sourceToTarget(dto, MerchantAppEntity.class);
        if (entity.getTenantId() == null) {
            entity.setTenantId(existed.getTenantId());
        }
        if (entity.getMerchantId() == null) {
            entity.setMerchantId(existed.getMerchantId());
        }
        if (StrUtil.isBlank(entity.getAppEnv())) {
            entity.setAppEnv(existed.getAppEnv());
        } else {
            entity.setAppEnv(normalizeAppEnv(entity.getAppEnv()));
        }
        validateTestAppUnique(entity, existed.getId());
        entity.setAppId(existed.getAppId());
        entity.setApiSecret(existed.getApiSecret());
        entity.setSecretVersion(existed.getSecretVersion());
        entity.setSecretUpdatedAt(existed.getSecretUpdatedAt());
        updateById(entity);

        dto.setAppId(existed.getAppId());
        dto.setApiSecret(null);
        evictPayinPlanCache();
        evictMerchantAppCache(existed.getAppId());
    }

    @Override
    public void delete(Long[] ids) {
        List<String> appIds = selectAppIds(ids);
        baseDao.update(null, new UpdateWrapper<MerchantAppEntity>()
                .set("status", StatusEnum.STOP.code())
                .in("id", Arrays.asList(ids)));
        evictPayinPlanCache();
        evictMerchantAppCache(appIds);
    }

    @Override
    public void delete(Long id) {
        MerchantAppEntity existed = id == null ? null : baseDao.selectById(id);
        baseDao.update(null, new UpdateWrapper<MerchantAppEntity>()
                .set("status", StatusEnum.STOP.code())
                .eq("id", id));
        evictPayinPlanCache();
        evictMerchantAppCache(existed == null ? null : existed.getAppId());
    }

    @Override
    public MerchantAppDTO resetApiSecret(Long id) {
        AssertUtils.isNull(id, "id");
        MerchantAppEntity existed = baseDao.selectById(id);
        if (existed == null) {
            throw new GkException(ErrorCode.NOT_FOUND);
        }

        String apiSecret = BizKeyUtils.genApiSecret();
        int secretVersion = existed.getSecretVersion() == null ? 1 : existed.getSecretVersion() + 1;
        Instant secretUpdatedAt = Instant.now();

        MerchantAppEntity update = new MerchantAppEntity();
        update.setId(id);
        update.setApiSecret(apiSecret);
        update.setSecretVersion(secretVersion);
        update.setSecretUpdatedAt(secretUpdatedAt);
        updateById(update);

        MerchantAppDTO dto = ConvertUtils.sourceToTarget(existed, MerchantAppDTO.class);
        dto.setApiSecret(apiSecret);
        dto.setSecretVersion(secretVersion);
        dto.setSecretUpdatedAt(secretUpdatedAt);
        evictMerchantAppCache(existed.getAppId());
        return dto;
    }

    @Override
    public MerchantAppDTO createProductionApp(Long testAppId) {
        AssertUtils.isNull(testAppId, "id");
        MerchantAppEntity testApp = baseDao.selectById(testAppId);
        if (testApp == null) {
            throw new GkException(ErrorCode.NOT_FOUND);
        }
        if (!MerchantAppEnvEnum.TEST.code().equals(testApp.getAppEnv())) {
            throw new GkException("only TEST app can create PROD app");
        }
        MerchantAppDTO dto = ConvertUtils.sourceToTarget(testApp, MerchantAppDTO.class);
        dto.setId(null);
        dto.setAppId(null);
        dto.setApiSecret(null);
        dto.setAppEnv(MerchantAppEnvEnum.PROD.code());
        save(dto);
        return dto;
    }

    private void applyCreateDefaults(MerchantAppEntity entity) {
        if (entity.getStatus() == null) {
            entity.setStatus(StatusEnum.NORMAL.code());
        }
        if (StrUtil.isBlank(entity.getAppType())) {
            entity.setAppType(MerchantAppTypeEnum.API.code());
        }
        if (StrUtil.isBlank(entity.getAppEnv())) {
            entity.setAppEnv(MerchantAppEnvEnum.PROD.code());
        } else {
            entity.setAppEnv(normalizeAppEnv(entity.getAppEnv()));
        }
        if (StrUtil.isBlank(entity.getSignType())) {
            entity.setSignType(SignTypeEnum.HMAC_SHA256.code());
        }
        if (StrUtil.isBlank(entity.getEncryptType())) {
            entity.setEncryptType(EncryptTypeEnum.NONE.code());
        }
        if (entity.getRateLimitQps() == null) {
            entity.setRateLimitQps(DEFAULT_RATE_LIMIT_QPS);
        }
        if (entity.getNonceTtlSeconds() == null) {
            entity.setNonceTtlSeconds(DEFAULT_NONCE_TTL_SECONDS);
        }
    }

    private String normalizeAppEnv(String appEnv) {
        MerchantAppEnvEnum env = StringCodeEnum.fromCode(MerchantAppEnvEnum.class, appEnv);
        if (env == null) {
            throw new GkException("invalid app_env");
        }
        return env.code();
    }

    private void validateTestAppUnique(MerchantAppEntity entity, Long excludeId) {
        if (MerchantAppEnvEnum.TEST.matches(entity.getAppEnv())
                && existsAppEnv(entity.getTenantId(), entity.getMerchantId(), entity.getAppEnv(), excludeId)) {
            throw new GkException("TEST app already exists");
        }
    }

    private boolean existsAppEnv(Long tenantId, Long merchantId, String appEnv, Long excludeId) {
        QueryWrapper<MerchantAppEntity> wrapper = new QueryWrapper<MerchantAppEntity>()
                .eq("tenant_id", tenantId)
                .eq("merchant_id", merchantId)
                .eq("app_env", appEnv);
        wrapper.ne(excludeId != null, "id", excludeId);
        Long count = baseDao.selectCount(wrapper);
        return count != null && count > 0;
    }

    private void insertWithGeneratedAppId(MerchantAppEntity entity) {
        for (int attempt = 0; attempt < APP_ID_GENERATE_MAX_ATTEMPTS; attempt++) {
            entity.setAppId(generateUniqueAppId());
            try {
                insert(entity);
                return;
            } catch (DuplicateKeyException ignored) {
                if (MerchantAppEnvEnum.TEST.matches(entity.getAppEnv())
                        && existsAppEnv(entity.getTenantId(), entity.getMerchantId(), entity.getAppEnv(), null)) {
                    throw new GkException("TEST app already exists");
                }
                // app_id collision, retry with a new id
            }
        }
        throw new GkException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private String generateUniqueAppId() {
        for (int attempt = 0; attempt < APP_ID_GENERATE_MAX_ATTEMPTS; attempt++) {
            String appId = BizKeyUtils.genAppId();
            Long count = baseDao.selectCount(new QueryWrapper<MerchantAppEntity>().eq("app_id", appId));
            if (count == null || count == 0L) {
                return appId;
            }
        }
        throw new GkException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private void maskSecrets(List<MerchantAppDTO> list) {
        if (list == null) {
            return;
        }
        list.forEach(this::maskSecret);
    }

    private void maskSecret(MerchantAppDTO dto) {
        if (dto == null) {
            return;
        }
        dto.setApiSecret(MerchantAppSecrets.mask(dto.getApiSecret()));
    }

    private void evictPayinPlanCache() {
        // Merchant App changes can affect compiled payment plans.
        if (paymentPlanCacheEvictor != null) {
            paymentPlanCacheEvictor.evictAll();
        }
    }

    private List<String> selectAppIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return List.of();
        }
        List<Long> idList = Arrays.stream(ids)
                .filter(Objects::nonNull)
                .toList();
        if (idList.isEmpty()) {
            return List.of();
        }
        return baseDao.selectByIds(idList).stream()
                .map(MerchantAppEntity::getAppId)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
    }

    private void evictMerchantAppCache(String appId) {
        if (openApiAuthCacheEvictor != null) {
            openApiAuthCacheEvictor.evictByAppId(appId);
        }
    }

    private void evictMerchantAppCache(List<String> appIds) {
        if (openApiAuthCacheEvictor == null || appIds == null) {
            return;
        }
        appIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .forEach(openApiAuthCacheEvictor::evictByAppId);
    }
}
