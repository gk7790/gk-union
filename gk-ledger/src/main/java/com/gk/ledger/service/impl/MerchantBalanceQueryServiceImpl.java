package com.gk.ledger.service.impl;

import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.ledger.dao.MerchantBalanceQueryDao;
import com.gk.ledger.dto.MerchantBalanceDTO;
import com.gk.ledger.dto.MerchantWalletBalanceDTO;
import com.gk.ledger.service.MerchantBalanceQueryService;
import com.gk.merchant.dao.MerchantDao;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static com.gk.dashboard.support.DashboardSupport.money;

@Service
@RequiredArgsConstructor
public class MerchantBalanceQueryServiceImpl implements MerchantBalanceQueryService {
    private final MerchantBalanceQueryDao queryDao;
    private final MerchantDao merchantDao;

    @Override
    public PageData<MerchantBalanceDTO> page(DynMap params) {
        applyDataScope(params);
        long page = Math.max(params.getLong(Constant.PAGE, 1L), 1L);
        long limit = Math.max(params.getLong(Constant.LIMIT, 10L), 1L);
        params.put("offset", (page - 1) * limit);
        params.put("limitValue", limit);

        Long total = queryDao.countMerchantBalances(params);
        List<MerchantBalanceDTO> list = total == null || total == 0L
                ? List.of()
                : queryDao.pageMerchantBalances(params);
        fillTgChatBound(list);
        list.forEach(this::normalizeBalanceView);
        return new PageData<>(list, total == null ? 0L : total);
    }

    @Override
    public List<MerchantWalletBalanceDTO> listForCurrentMerchant(String currency) {
        assertMerchantScope();
        DynMap params = new DynMap();
        params.put("tenantId", ReqContextHolder.getTenantId());
        params.put("merchantId", ReqContextHolder.getMerchantId());
        if (StringUtils.isNotBlank(currency)) {
            params.put("currency", currency.trim().toUpperCase(Locale.ROOT));
        }
        return queryDao.listMerchantBalances(params).stream()
                .peek(this::normalizeBalanceView)
                .map(this::toWalletBalance)
                .toList();
    }

    private void applyDataScope(DynMap params) {
        if (SubjectTypeEnum.PLATFORM.matches(ReqContextHolder.getSubjectType())) {
            return;
        }
        params.put("tenantId", ReqContextHolder.getTenantId());
        if (SubjectTypeEnum.MERCHANT.matches(ReqContextHolder.getSubjectType())) {
            params.put("merchantId", ReqContextHolder.getMerchantId());
        }
    }

    private MerchantWalletBalanceDTO toWalletBalance(MerchantBalanceDTO source) {
        MerchantWalletBalanceDTO target = new MerchantWalletBalanceDTO();
        target.setCurrency(source.getCurrency());
        target.setAvailableBalance(source.getAvailableBalance());
        target.setAvailableBalanceText(source.getAvailableBalanceText());
        target.setPendingSettleBalance(source.getPendingSettleBalance());
        target.setPendingSettleBalanceText(source.getPendingSettleBalanceText());
        target.setFrozenBalance(source.getFrozenBalance());
        target.setFrozenBalanceText(source.getFrozenBalanceText());
        target.setTotalBalance(source.getTotalBalance());
        target.setTotalBalanceText(source.getTotalBalanceText());
        target.setLastPostedAt(source.getLastPostedAt());
        return target;
    }

    private void assertMerchantScope() {
        if (!SubjectTypeEnum.MERCHANT.matches(ReqContextHolder.getSubjectType())) {
            throw new GkException(ErrorCode.FORBIDDEN);
        }
        if (ReqContextHolder.getTenantId() == null || ReqContextHolder.getMerchantId() == null) {
            throw new GkException(ErrorCode.DATA_SCOPE_PARAMS_ERROR);
        }
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
        item.setTgChatBound(Boolean.TRUE.equals(item.getTgChatBound()));
    }

    private void fillTgChatBound(List<MerchantBalanceDTO> merchants) {
        if (merchants == null || merchants.isEmpty()) {
            return;
        }
        List<Long> merchantIds = merchants.stream()
                .map(MerchantBalanceDTO::getMerchantId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (merchantIds.isEmpty()) {
            merchants.forEach(item -> item.setTgChatBound(false));
            return;
        }
        Set<Long> boundIds = merchantDao.selectMerchantIdsWithActiveTgChat(merchantIds).stream()
                .collect(Collectors.toSet());
        merchants.forEach(item -> item.setTgChatBound(boundIds.contains(item.getMerchantId())));
    }

    private String display(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
