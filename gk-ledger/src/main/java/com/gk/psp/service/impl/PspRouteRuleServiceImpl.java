package com.gk.psp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.ledger.service.LedgerAccountService;
import com.gk.psp.dao.PspRouteRuleDao;
import com.gk.psp.dto.PspRouteRuleDTO;
import com.gk.psp.entity.PspRouteRuleEntity;
import com.gk.psp.service.PspRouteRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PspRouteRuleServiceImpl extends CrudServiceImpl<PspRouteRuleDao, PspRouteRuleEntity, PspRouteRuleDTO> implements PspRouteRuleService {

    private final LedgerAccountService ledgerAccountService;

    @Override
    public QueryWrapper<PspRouteRuleEntity> getWrapper(DynMap params) {
        QueryWrapper<PspRouteRuleEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Long merchantAppId = params.getLong("merchantAppId", null);
        Long pspId = params.getLong("pspId", null);
        Long pspMethodId = params.getLong("pspMethodId", null);
        Long pspAccountId = params.getLong("pspAccountId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String routeName = params.getStr("routeName");
        String routeMode = params.getStr("routeMode");
        String countryCode = params.getStr("countryCode");
        String currency = params.getStr("currency");
        String methodCode = params.getStr("methodCode");
        String direction = params.getStr("direction");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(merchantAppId != null, "merchant_app_id", merchantAppId);
        wrapper.eq(pspId != null, "psp_id", pspId);
        wrapper.eq(pspMethodId != null, "psp_method_id", pspMethodId);
        wrapper.eq(pspAccountId != null, "psp_account_id", pspAccountId);
        wrapper.eq(status != null, "status", status);
        wrapper.like(StrUtil.isNotBlank(routeName), "route_name", routeName);
        wrapper.eq(StrUtil.isNotBlank(routeMode), "route_mode", routeMode);
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", countryCode);
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", currency);
        wrapper.eq(StrUtil.isNotBlank(methodCode), "method_code", methodCode);
        wrapper.eq(StrUtil.isNotBlank(direction), "direction", direction);
        return wrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(PspRouteRuleDTO dto) {
        super.save(dto);
        provisionPspLedgerAccounts(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PspRouteRuleDTO dto) {
        super.update(dto);
        provisionPspLedgerAccounts(dto);
    }

    private void provisionPspLedgerAccounts(PspRouteRuleDTO dto) {
        if (dto == null || dto.getTenantId() == null || dto.getPspAccountId() == null || StrUtil.isBlank(dto.getCurrency())) {
            return;
        }
        ledgerAccountService.provisionPspAccounts(dto.getTenantId(), dto.getPspAccountId(), dto.getCurrency());
    }
}
