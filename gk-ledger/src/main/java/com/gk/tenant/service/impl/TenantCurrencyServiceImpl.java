package com.gk.tenant.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.context.ReqContext;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.common.utils.ConvertUtils;
import com.gk.common.validator.AssertUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.meta.dao.SysCurrencyDao;
import com.gk.meta.dto.SysCurrencyDTO;
import com.gk.meta.entity.SysCurrencyEntity;
import com.gk.tenant.dao.TenantCurrencyDao;
import com.gk.tenant.dto.TenantCurrencyDTO;
import com.gk.tenant.entity.TenantCurrencyEntity;
import com.gk.tenant.service.TenantCurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantCurrencyServiceImpl extends CrudServiceImpl<TenantCurrencyDao, TenantCurrencyEntity, TenantCurrencyDTO>
        implements TenantCurrencyService {

    private final SysCurrencyDao sysCurrencyDao;

    @Override
    public QueryWrapper<TenantCurrencyEntity> getWrapper(DynMap params) {
        QueryWrapper<TenantCurrencyEntity> wrapper = new QueryWrapper<>();
        Long tenantId = resolveTenantIdForQuery(params);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String currency = params.getStr("currency");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(status != null, "status", status);
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", currency);
        wrapper.orderByAsc("sort").orderByAsc("currency");
        return wrapper;
    }

    @Override
    public List<SysCurrencyDTO> getDict(DynMap params) {
        Long tenantId = resolveTenantId(params);
        List<Integer> statusList = params.getList("status", Integer.class, List.of(StatusEnum.NORMAL.code()));

        QueryWrapper<TenantCurrencyEntity> tenantWrapper = new QueryWrapper<>();
        tenantWrapper.select("currency", "sort");
        tenantWrapper.eq("tenant_id", tenantId);
        tenantWrapper.in("status", statusList);
        tenantWrapper.orderByAsc("sort").orderByAsc("currency");
        List<TenantCurrencyEntity> tenantCurrencies = baseDao.selectList(tenantWrapper);
        if (CollectionUtils.isEmpty(tenantCurrencies)) {
            return List.of();
        }

        List<String> currencies = tenantCurrencies.stream()
                .map(TenantCurrencyEntity::getCurrency)
                .toList();
        Map<String, Integer> sortMap = tenantCurrencies.stream()
                .collect(Collectors.toMap(TenantCurrencyEntity::getCurrency, TenantCurrencyEntity::getSort, (a, b) -> a));

        QueryWrapper<SysCurrencyEntity> currencyWrapper = new QueryWrapper<>();
        currencyWrapper.select("id", "currency");
        currencyWrapper.in("status", statusList);
        List<SysCurrencyEntity> currencyEntities = sysCurrencyDao.selectList(currencyWrapper);
        if (CollectionUtils.isEmpty(currencyEntities)) {
            return List.of();
        }

        Map<String, SysCurrencyEntity> currencyMap = currencyEntities.stream()
                .collect(Collectors.toMap(SysCurrencyEntity::getCurrency, Function.identity(), (a, b) -> a));

        return currencies.stream()
                .map(currencyMap::get)
                .filter(item -> item != null)
                .map(item -> {
                    SysCurrencyDTO dto = ConvertUtils.sourceToTarget(item, SysCurrencyDTO.class);
                    dto.setSort(sortMap.getOrDefault(item.getCurrency(), item.getSort()));
                    return dto;
                })
                .sorted(Comparator.comparing(SysCurrencyDTO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SysCurrencyDTO::getCurrency, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    @Override
    public List<TenantCurrencyEntity> getProviderDict(Long tenantId) {
        QueryWrapper<TenantCurrencyEntity> wrapper = new QueryWrapper<>();
        wrapper.select( "currency");
        wrapper.eq("tenant_id", tenantId);
        return baseDao.selectList(wrapper);
    }

    @Override
    public void save(TenantCurrencyDTO dto) {
        applyTenantScope(dto);
        validateCurrency(dto.getCurrency());
        if (dto.getStatus() == null) {
            dto.setStatus(StatusEnum.NORMAL.code());
        }
        if (dto.getSort() == null) {
            dto.setSort(100);
        }
        super.save(dto);
    }

    @Override
    public void update(TenantCurrencyDTO dto) {
        applyTenantScope(dto);
        validateCurrency(dto.getCurrency());
        super.update(dto);
    }

    private void validateCurrency(String currency) {
        AssertUtils.isBlank(currency, "currency");
        QueryWrapper<SysCurrencyEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("currency", currency);
        wrapper.in("status", StatusEnum.defaultStatus());
        if (sysCurrencyDao.selectCount(wrapper) <= 0) {
            throw new GkException(ErrorCode.NOT_NULL, "currency not found or disabled");
        }
    }

    private void applyTenantScope(TenantCurrencyDTO dto) {
        if (SubjectTypeEnum.TENANT.matches(ReqContextHolder.getSubjectType())) {
            dto.setTenantId(ReqContextHolder.getTenantId());
        }
        AssertUtils.isNull(dto.getTenantId(), "tenantId");
        AssertUtils.isBlank(dto.getCurrency(), "currency");
    }

    private Long resolveTenantId(DynMap params) {
        ReqContext context = ReqContextHolder.get();
        if (SubjectTypeEnum.PLATFORM.matches(context.getSubjectType())) {
            Long tenantId = params.getLong("tenantId", null);
            AssertUtils.isNull(tenantId, "tenantId");
            return tenantId;
        }
        Long tenantId = context.getTenantId();
        AssertUtils.isNull(tenantId, "tenantId");
        return tenantId;
    }

    private Long resolveTenantIdForQuery(DynMap params) {
        ReqContext context = ReqContextHolder.get();
        if (SubjectTypeEnum.PLATFORM.matches(context.getSubjectType())) {
            return params.getLong("tenantId", null);
        }
        return context.getTenantId();
    }
}
