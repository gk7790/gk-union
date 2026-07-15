package com.gk.reference.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.dto.LabelDTO;
import com.gk.common.model.DynMap;
import com.gk.infra.enums.StatusEnum;
import com.gk.reference.dao.SysCurrencyDao;
import com.gk.reference.dto.SysCurrencyDTO;
import com.gk.reference.entity.SysCurrencyEntity;
import com.gk.reference.service.SysCurrencyService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysCurrencyServiceImpl extends CrudServiceImpl<SysCurrencyDao, SysCurrencyEntity, SysCurrencyDTO> implements SysCurrencyService {

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
        List<SysCurrencyDTO> options = baseDao.selectOptions(statusList, StatusEnum.defaultStatus());
        options.forEach(item -> {
            if (item.getTenantIds() == null) {
                item.setTenantIds(List.of());
            }
        });
        return options;
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
