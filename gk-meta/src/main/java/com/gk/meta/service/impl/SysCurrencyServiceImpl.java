package com.gk.meta.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.common.utils.ConvertUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.meta.dao.SysCurrencyDao;
import com.gk.meta.dto.SysCurrencyDTO;
import com.gk.meta.entity.SysCurrencyEntity;
import com.gk.meta.service.SysCurrencyService;
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
    public List<SysCurrencyDTO> getDict(DynMap params) {
        List<Integer> statusList = params.getList("status", Integer.class, StatusEnum.defaultStatus());

        QueryWrapper<SysCurrencyEntity> wrapper = new QueryWrapper<>();
        wrapper.select("id", "currency", "currency_name", "currency_symbol", "numeric_code", "minor_unit", "sort", "remark");
        wrapper.in("status", statusList);
        wrapper.orderByAsc("sort").orderByAsc("currency");

        List<SysCurrencyEntity> list = baseDao.selectList(wrapper);
        return ConvertUtils.sourceToTarget(list, SysCurrencyDTO.class);
    }
}
