package com.gk.tenant.service.impl;

import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.tenant.dao.MerchantBalanceQueryDao;
import com.gk.tenant.dto.MerchantBalanceDTO;
import com.gk.tenant.service.MerchantBalanceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantBalanceQueryServiceImpl implements MerchantBalanceQueryService {
    private static final long DEFAULT_PAGE = 1L;
    private static final long DEFAULT_LIMIT = 10L;
    private static final int MONEY_SCALE = 8;
    private static final int DISPLAY_SCALE = 2;

    private final MerchantBalanceQueryDao queryDao;

    @Override
    public PageData<MerchantBalanceDTO> page(DynMap params) {
        long page = Math.max(params.getLong(Constant.PAGE, DEFAULT_PAGE), DEFAULT_PAGE);
        long limit = Math.max(params.getLong(Constant.LIMIT, DEFAULT_LIMIT), 1L);
        params.put("offset", (page - 1) * limit);
        params.put("limitValue", limit);

        Long total = queryDao.countMerchantBalances(params);
        List<MerchantBalanceDTO> list = total == null || total == 0L
                ? List.of()
                : queryDao.pageMerchantBalances(params);
        list.forEach(this::normalizeBalanceView);
        return new PageData<>(list, total == null ? 0L : total);
    }

    private void normalizeBalanceView(MerchantBalanceDTO item) {
        item.setAvailableBalance(money(item.getAvailableBalance()));
        item.setFrozenBalance(money(item.getFrozenBalance()));
        item.setPendingSettleBalance(money(item.getPendingSettleBalance()));
        item.setEffectiveBalance(money(item.getEffectiveBalance()));
        item.setTotalBalance(money(item.getTotalBalance()));
        item.setAvailableBalanceText(display(item.getAvailableBalance()));
        item.setFrozenBalanceText(display(item.getFrozenBalance()));
        item.setPendingSettleBalanceText(display(item.getPendingSettleBalance()));
        item.setEffectiveBalanceText(display(item.getEffectiveBalance()));
        item.setTotalBalanceText(display(item.getTotalBalance()));
        item.setHasLedgerAccount(Boolean.TRUE.equals(item.getHasLedgerAccount()));
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private String display(BigDecimal value) {
        return value.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP).toPlainString();
    }
}
