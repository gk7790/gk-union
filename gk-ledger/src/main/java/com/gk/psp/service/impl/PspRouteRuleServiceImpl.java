package com.gk.psp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.ledger.service.LedgerAccountService;
import com.gk.payment.plan.PaymentPlanCacheService;
import com.gk.payment.plan.PayinPlanCache;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.dao.PspRouteRuleDao;
import com.gk.psp.dto.PspRouteRuleDTO;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.entity.PspMethodEntity;
import com.gk.psp.entity.PspRouteRuleEntity;
import com.gk.psp.service.PspRouteRuleService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PspRouteRuleServiceImpl extends CrudServiceImpl<PspRouteRuleDao, PspRouteRuleEntity, PspRouteRuleDTO> implements PspRouteRuleService {

    private final LedgerAccountService ledgerAccountService;
    private final PayinPlanCache payinPlanCache;
    private final PaymentPlanCacheService paymentPlanCacheService;
    private final PspMethodDao pspMethodDao;
    private final PspAccountDao pspAccountDao;

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
        validateRouteBinding(dto);
        super.save(dto);
        provisionPspLedgerAccounts(dto);
        evictPayinPlanCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PspRouteRuleDTO dto) {
        validateRouteBinding(dto);
        super.update(dto);
        provisionPspLedgerAccounts(dto);
        evictPayinPlanCache();
    }

    @Override
    public void delete(Long[] ids) {
        super.delete(ids);
        evictPayinPlanCache();
    }

    @Override
    public void delete(Long id) {
        super.delete(id);
        evictPayinPlanCache();
    }

    private void provisionPspLedgerAccounts(PspRouteRuleDTO dto) {
        if (dto == null || dto.getTenantId() == null || dto.getPspAccountId() == null || StrUtil.isBlank(dto.getCurrency())) {
            return;
        }
        ledgerAccountService.provisionPspAccounts(dto.getTenantId(), dto.getPspAccountId(), dto.getCurrency());
    }

    private void validateRouteBinding(PspRouteRuleDTO dto) {
        if (dto == null) {
            return;
        }
        PspMethodEntity method = dto.getPspMethodId() == null ? null : pspMethodDao.selectById(dto.getPspMethodId());
        if (method == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "PSP支付方式不存在");
        }
        if (!equalsLong(dto.getPspId(), method.getPspId())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "路由规则的PSP与PSP支付方式不一致");
        }
        if (!equalsCode(dto.getCurrency(), method.getCurrency())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "路由规则的币种必须与PSP支付方式一致");
        }
        if (!equalsCode(dto.getDirection(), method.getDirection())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "路由规则的方向必须与PSP支付方式一致");
        }
        if (StringUtils.isNotBlank(dto.getCountryCode()) && !equalsCode(dto.getCountryCode(), method.getCountryCode())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "路由规则的国家/地区必须与PSP支付方式一致");
        }

        PspAccountEntity account = dto.getPspAccountId() == null ? null : pspAccountDao.selectById(dto.getPspAccountId());
        if (account == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "PSP账号不存在");
        }
        if (!equalsLong(dto.getPspId(), account.getPspId())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "路由规则的PSP与PSP账号不一致");
        }
        if (account.getTenantId() == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "PSP账号未配置租户");
        }

        // Route rule tenant follows the selected PSP account, so clients do not need to submit tenantId.
        dto.setTenantId(account.getTenantId());
        // Route methodCode is derived from psp_method_id; client methodCode is ignored on save/update.
        dto.setMethodCode(StrUtil.trim(method.getMethodCode()));
    }

    private boolean equalsCode(String left, String right) {
        return StringUtils.equalsIgnoreCase(StringUtils.trim(left), StringUtils.trim(right));
    }

    private boolean equalsLong(Long left, Long right) {
        return left != null && left.equals(right);
    }

    private void evictPayinPlanCache() {
        // PSP route rules affect upstream selection, so config changes must clear plan caches.
        payinPlanCache.evictAll();
        paymentPlanCacheService.evictAll();
    }
}
