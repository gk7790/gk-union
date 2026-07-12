package com.gk.reference.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.common.utils.ConvertUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.reference.dao.SysBankDao;
import com.gk.reference.dto.SysBankDTO;
import com.gk.reference.entity.SysBankEntity;
import com.gk.reference.service.SysBankService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysBankServiceImpl extends CrudServiceImpl<SysBankDao, SysBankEntity, SysBankDTO> implements SysBankService {

    @Override
    public QueryWrapper<SysBankEntity> getWrapper(DynMap params) {
        QueryWrapper<SysBankEntity> wrapper = new QueryWrapper<>();
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String countryCode = params.getStr("countryCode");
        String currency = params.getStr("currency");
        String bankCode = params.getStr("bankCode");
        String bankName = params.getStr("bankName");

        wrapper.eq(status != null, "status", status);
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", countryCode);
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", currency);
        wrapper.eq(StrUtil.isNotBlank(bankCode), "bank_code", bankCode);
        wrapper.like(StrUtil.isNotBlank(bankName), "bank_name", bankName);
        wrapper.orderByAsc("sort").orderByAsc("bank_code");
        return wrapper;
    }

    @Override
    public List<SysBankDTO> getDict(DynMap params) {
        List<Integer> statusList = params.getList("status", Integer.class, List.of(StatusEnum.NORMAL.code()));

        QueryWrapper<SysBankEntity> wrapper = new QueryWrapper<>();
        wrapper.select("id", "country_code", "currency", "bank_code", "bank_name", "bank_short_name", "sort", "remark");
        wrapper.in("status", statusList);
        wrapper.eq(StrUtil.isNotBlank(params.getStr("countryCode")), "country_code", params.getStr("countryCode"));
        wrapper.eq(StrUtil.isNotBlank(params.getStr("currency")), "currency", params.getStr("currency"));
        wrapper.orderByAsc("sort").orderByAsc("bank_code");

        List<SysBankEntity> list = baseDao.selectList(wrapper);
        return ConvertUtils.sourceToTarget(list, SysBankDTO.class);
    }
}
