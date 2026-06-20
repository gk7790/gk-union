package com.gk.subject.service.impl;

import cn.hutool.core.util.StrUtil;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.ledger.enums.LedgerOwnerTypeEnum;
import com.gk.merchant.dao.MerchantDao;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.subject.model.SubjectDisplay;
import com.gk.subject.model.SubjectRef;
import com.gk.subject.service.SubjectDisplayService;
import com.gk.tenant.dao.TenantDao;
import com.gk.tenant.entity.TenantEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubjectDisplayServiceImpl implements SubjectDisplayService {
    private static final long CACHE_EXPIRE_SECONDS = 5 * 60L;

    private final TenantDao tenantDao;
    private final MerchantDao merchantDao;
    private final RedisUtils redisUtils;

    @Override
    public SubjectDisplay get(SubjectRef ref) {
        return batchGet(List.of(ref)).get(ref);
    }

    @Override
    public Map<SubjectRef, SubjectDisplay> batchGet(Collection<SubjectRef> refs) {
        Map<SubjectRef, SubjectDisplay> result = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(refs)) {
            return result;
        }
        List<SubjectRef> normalizedRefs = refs.stream()
                .filter(Objects::nonNull)
                .filter(ref -> StrUtil.isNotBlank(ref.subjectType()) && ref.subjectId() != null)
                .distinct()
                .toList();
        Map<String, SubjectDisplay> cachedDisplays = getCached(normalizedRefs);
        List<SubjectRef> misses = new ArrayList<>();
        normalizedRefs.forEach(ref -> {
            SubjectDisplay cached = cachedDisplays.get(cacheKey(ref));
            if (cached == null) {
                result.put(ref, fallback(ref));
                misses.add(ref);
            } else {
                result.put(ref, cached);
            }
        });
        fillTenants(misses, result);
        fillMerchants(misses, result);
        misses.forEach(ref -> cache(ref, result.get(ref)));
        return result;
    }

    private void fillTenants(List<SubjectRef> refs, Map<SubjectRef, SubjectDisplay> result) {
        Set<Long> tenantIds = refs.stream()
                .filter(ref -> SubjectTypeEnum.TENANT.matches(ref.subjectType()))
                .map(SubjectRef::subjectId)
                .collect(Collectors.toSet());
        if (tenantIds.isEmpty()) {
            return;
        }
        Map<Long, TenantEntity> tenants = tenantDao.selectBatchIds(tenantIds).stream()
                .collect(Collectors.toMap(TenantEntity::getId, Function.identity(), (left, right) -> left));
        refs.stream()
                .filter(ref -> SubjectTypeEnum.TENANT.matches(ref.subjectType()))
                .forEach(ref -> {
                    TenantEntity tenant = tenants.get(ref.subjectId());
                    if (tenant != null) {
                        result.put(ref, tenantDisplay(ref, tenant));
                    }
                });
    }

    private void fillMerchants(List<SubjectRef> refs, Map<SubjectRef, SubjectDisplay> result) {
        Set<Long> merchantIds = refs.stream()
                .filter(ref -> SubjectTypeEnum.MERCHANT.matches(ref.subjectType()))
                .map(SubjectRef::subjectId)
                .collect(Collectors.toSet());
        if (merchantIds.isEmpty()) {
            return;
        }
        Map<Long, MerchantEntity> merchants = merchantDao.selectBatchIds(merchantIds).stream()
                .collect(Collectors.toMap(MerchantEntity::getId, Function.identity(), (left, right) -> left));
        refs.stream()
                .filter(ref -> SubjectTypeEnum.MERCHANT.matches(ref.subjectType()))
                .forEach(ref -> {
                    MerchantEntity merchant = merchants.get(ref.subjectId());
                    if (merchant != null) {
                        result.put(ref, merchantDisplay(ref, merchant));
                    }
                });
    }

    private SubjectDisplay tenantDisplay(SubjectRef ref, TenantEntity tenant) {
        SubjectDisplay display = base(ref, true);
        display.setSubjectNo(tenant.getCode());
        display.setSubjectName(tenant.getName());
        display.setSubjectShortName(tenant.getName());
        display.setDisplayName(StrUtil.blankToDefault(tenant.getName(), fallbackName(ref)));
        display.setStatus(tenant.getStatus());
        return display;
    }

    private SubjectDisplay merchantDisplay(SubjectRef ref, MerchantEntity merchant) {
        SubjectDisplay display = base(ref, true);
        display.setSubjectNo(merchant.getMerchantNo());
        display.setSubjectName(merchant.getMerchantName());
        display.setSubjectShortName(StrUtil.blankToDefault(merchant.getMerchantShortName(), merchant.getMerchantName()));
        display.setDisplayName(StrUtil.blankToDefault(merchant.getMerchantName(), fallbackName(ref)));
        display.setStatus(merchant.getStatus());
        return display;
    }

    private SubjectDisplay fallback(SubjectRef ref) {
        SubjectDisplay display = base(ref, isKnownStatic(ref.subjectType()));
        String name = isKnownStatic(ref.subjectType()) ? ref.subjectType() : fallbackName(ref);
        display.setSubjectNo(name);
        display.setSubjectName(name);
        display.setSubjectShortName(name);
        display.setDisplayName(name);
        return display;
    }

    private SubjectDisplay base(SubjectRef ref, boolean found) {
        SubjectDisplay display = new SubjectDisplay();
        display.setTenantId(ref.tenantId());
        display.setSubjectType(ref.subjectType());
        display.setSubjectId(ref.subjectId());
        display.setFound(found);
        return display;
    }

    private boolean isKnownStatic(String subjectType) {
        return SubjectTypeEnum.PLATFORM.matches(subjectType)
                || LedgerOwnerTypeEnum.SYSTEM.code().equals(subjectType);
    }

    private String fallbackName(SubjectRef ref) {
        return ref.subjectType() + "#" + ref.subjectId();
    }

    private Map<String, SubjectDisplay> getCached(List<SubjectRef> refs) {
        if (refs.isEmpty()) {
            return Map.of();
        }
        try {
            return redisUtils.getBatch(refs.stream().map(this::cacheKey).toList(), SubjectDisplay.class);
        } catch (Exception e) {
            log.warn("Get subject display cache batch failed: {}", e.getMessage());
            return Map.of();
        }
    }

    private void cache(SubjectRef ref, SubjectDisplay display) {
        if (display == null) {
            return;
        }
        try {
            redisUtils.set(cacheKey(ref), display, CACHE_EXPIRE_SECONDS);
        } catch (Exception e) {
            log.warn("Set subject display cache failed: {}", e.getMessage());
        }
    }

    private String cacheKey(SubjectRef ref) {
        return RedisKeys.getSubjectDisplayKey(ref.tenantId(), ref.subjectType(), ref.subjectId());
    }
}
