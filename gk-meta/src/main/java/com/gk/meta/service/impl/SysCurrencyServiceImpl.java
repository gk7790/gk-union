package com.gk.meta.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.dto.LabelDTO;
import com.gk.common.model.DynMap;
import com.gk.common.utils.ConvertUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.meta.dao.SysCurrencyDao;
import com.gk.meta.dao.SysTenantCurrencyDao;
import com.gk.meta.dto.SysCurrencyDTO;
import com.gk.meta.entity.SysCurrencyEntity;
import com.gk.meta.entity.SysTenantCurrencyEntity;
import com.gk.meta.service.SysCurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysCurrencyServiceImpl extends CrudServiceImpl<SysCurrencyDao, SysCurrencyEntity, SysCurrencyDTO> implements SysCurrencyService {
    private final SysTenantCurrencyDao sysTenantCurrencyDao;

    @Override
    public QueryWrapper<SysCurrencyEntity> getWrapper(DynMap params) {
        QueryWrapper<SysCurrencyEntity> wrapper = new QueryWrapper<>();
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String currency = params.getStr("currency");
        String currencyName = params.getStr("currencyName");

        wrapper.eq(status != null, "status", status);
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", currency);
        wrapper.like(StrUtil.isNotBlank(currencyName), "currency_name", currencyName);
        wrapper.orderByAsc("sort").orderByAsc("currency");
        return wrapper;
    }

    @Override
    public List<SysCurrencyDTO> getOptions(DynMap params) {
        List<Integer> statusList = statusList(params);
        if (ReqContextHolder.isPlatform()) {
            return listPlatformCurrencies(statusList);
        }
        return listTenantCurrencies(ReqContextHolder.getTenantId(), statusList);
    }

    private List<SysCurrencyDTO> listPlatformCurrencies(List<Integer> statusList) {
        QueryWrapper<SysCurrencyEntity> wrapper = new QueryWrapper<>();
        wrapper.select("id", "currency", "currency_name", "currency_symbol", "numeric_code", "minor_unit", "status", "sort", "remark");
        wrapper.in("status", statusList);
        wrapper.orderByAsc("sort").orderByAsc("currency");
        List<SysCurrencyEntity> list = baseDao.selectList(wrapper);
        return ConvertUtils.sourceToTarget(list, SysCurrencyDTO.class);
    }

    private List<SysCurrencyDTO> listTenantCurrencies(Long tenantId, List<Integer> statusList) {
        if (tenantId == null) {
            return List.of();
        }

        QueryWrapper<SysTenantCurrencyEntity> tenantWrapper = new QueryWrapper<>();
        tenantWrapper.select("currency", "sort");
        tenantWrapper.eq("tenant_id", tenantId);
        tenantWrapper.in("status", statusList);
        tenantWrapper.orderByAsc("sort").orderByAsc("currency");
        List<SysTenantCurrencyEntity> tenantCurrencies = sysTenantCurrencyDao.selectList(tenantWrapper);
        if (tenantCurrencies == null || tenantCurrencies.isEmpty()) {
            return List.of();
        }

        List<String> currencies = tenantCurrencies.stream()
                .map(SysTenantCurrencyEntity::getCurrency)
                .filter(StrUtil::isNotBlank)
                .toList();
        Map<String, Integer> sortMap = tenantCurrencies.stream()
                .filter(item -> StrUtil.isNotBlank(item.getCurrency()))
                .collect(Collectors.toMap(
                        SysTenantCurrencyEntity::getCurrency,
                        item -> item.getSort() == null ? 100 : item.getSort(),
                        (a, b) -> a
                ));
        if (currencies.isEmpty()) {
            return List.of();
        }

        QueryWrapper<SysCurrencyEntity> wrapper = new QueryWrapper<>();
        wrapper.select("id", "currency", "currency_name", "currency_symbol", "numeric_code", "minor_unit", "status", "sort", "remark");
        wrapper.in("status", statusList);
        wrapper.in("currency", currencies);
        List<SysCurrencyEntity> list = baseDao.selectList(wrapper);
        if (list == null || list.isEmpty()) {
            return List.of();
        }

        return list.stream()
                .map(item -> {
                    SysCurrencyDTO dto = ConvertUtils.sourceToTarget(item, SysCurrencyDTO.class);
                    dto.setSort(sortMap.getOrDefault(item.getCurrency(), item.getSort()));
                    return dto;
                })
                .sorted(Comparator.comparing(SysCurrencyDTO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SysCurrencyDTO::getCurrency, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private List<Integer> statusList(DynMap params) {
        List<Integer> statusList = params.getList("status", Integer.class, List.of(StatusEnum.NORMAL.code()));
        List<Integer> normalized = statusList.stream()
                .filter(item -> item != null)
                .distinct()
                .sorted()
                .toList();
        return normalized.isEmpty() ? List.of(StatusEnum.NORMAL.code()) : normalized;
    }

    @Override
    public List<LabelDTO> getDict(DynMap params) {
        QueryWrapper<SysCurrencyEntity> wrapper = new QueryWrapper<>();
        wrapper.select("currency");
        wrapper.in("status", StatusEnum.defaultStatus());
        wrapper.orderByAsc("sort").orderByAsc("currency");
        List<SysCurrencyEntity> list = baseDao.selectList(wrapper);
        return list.stream().map(e -> LabelDTO.of(e.getCurrency(), e.getCurrency())).toList();
    }
}
