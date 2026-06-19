package com.gk.subject.service.impl;

import cn.hutool.core.util.StrUtil;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.ledger.enums.LedgerOwnerTypeEnum;
import com.gk.merchant.dao.MerchantDao;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.subject.model.SubjectDisplay;
import com.gk.subject.model.SubjectRef;
import com.gk.subject.service.SubjectDisplayService;
import com.gk.tenant.dao.TenantDao;
import com.gk.tenant.entity.TenantEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
public class SubjectDisplayServiceImpl implements SubjectDisplayService {
    private final TenantDao tenantDao;
    private final MerchantDao merchantDao;

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
        normalizedRefs.forEach(ref -> result.put(ref, fallback(ref)));
        fillTenants(normalizedRefs, result);
        fillMerchants(normalizedRefs, result);
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
        display.setSubjectShortName(merchant.getMerchantShortName());
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
}
