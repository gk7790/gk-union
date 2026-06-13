package com.gk.ledger.query.service.impl;

import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.ledger.query.dao.TenantMerchantBalanceQueryDao;
import com.gk.ledger.query.dto.TenantMerchantBalanceDTO;
import com.gk.ledger.query.service.TenantMerchantBalanceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantMerchantBalanceQueryServiceImpl implements TenantMerchantBalanceQueryService {
    private static final long DEFAULT_PAGE = 1L;
    private static final long DEFAULT_LIMIT = 10L;

    private final TenantMerchantBalanceQueryDao queryDao;

    @Override
    public PageData<TenantMerchantBalanceDTO> page(DynMap params) {
        long page = Math.max(params.getLong(Constant.PAGE, DEFAULT_PAGE), DEFAULT_PAGE);
        long limit = Math.max(params.getLong(Constant.LIMIT, DEFAULT_LIMIT), 1L);
        params.put("offset", (page - 1) * limit);
        params.put("limitValue", limit);

        Long total = queryDao.countTenantMerchantBalances(params);
        List<TenantMerchantBalanceDTO> list = total == null || total == 0L
                ? List.of()
                : queryDao.pageTenantMerchantBalances(params);
        return new PageData<>(list, total == null ? 0L : total);
    }
}
