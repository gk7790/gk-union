package com.gk.psp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.amount.AmountRangeUtils;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.infra.enums.StatusEnum;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.dao.PspRouteRuleDao;
import com.gk.psp.dto.PspRouteRuleDTO;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.entity.PspMethodEntity;
import com.gk.psp.entity.PspRouteRuleEntity;
import com.gk.psp.service.PspLedgerAccountProvisioner;
import com.gk.psp.service.PspRouteRuleService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PspRouteRuleServiceImpl extends CrudServiceImpl<PspRouteRuleDao, PspRouteRuleEntity, PspRouteRuleDTO> implements PspRouteRuleService {

    private final ObjectProvider<PspLedgerAccountProvisioner> ledgerAccountProvisioner;
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
        java.util.List<Integer> statusList = StatusEnum.normalizeQueryStatus(params.getList("status", Integer.class, StatusEnum.defaultStatus()));
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
        wrapper.in("status", statusList);
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
        baseDao.update(null, new UpdateWrapper<PspRouteRuleEntity>()
                .set("status", StatusEnum.STOP.code())
                .in("id", java.util.Arrays.asList(ids)));
        evictPayinPlanCache();
    }

    @Override
    public void delete(Long id) {
        baseDao.update(null, new UpdateWrapper<PspRouteRuleEntity>()
                .set("status", StatusEnum.STOP.code())
                .eq("id", id));
        evictPayinPlanCache();
    }

    private void provisionPspLedgerAccounts(PspRouteRuleDTO dto) {
        if (dto == null || dto.getTenantId() == null || dto.getPspAccountId() == null || StrUtil.isBlank(dto.getCurrency())) {
            return;
        }
        PspLedgerAccountProvisioner provisioner = ledgerAccountProvisioner.getIfAvailable();
        if (provisioner != null) {
            provisioner.provisionPspAccounts(dto.getTenantId(), dto.getPspAccountId(), dto.getCurrency());
        }
    }

    private void validateRouteBinding(PspRouteRuleDTO dto) {
        if (dto == null) {
            return;
        }
        validateAmountRange(dto);
        PspMethodEntity method = dto.getPspMethodId() == null ? null : pspMethodDao.selectById(dto.getPspMethodId());
        if (method == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "PSP payment method not found");
        }
        if (notEqualsLong(dto.getPspId(), method.getPspId())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route rule provider does not match PSP method");
        }
        if (notEqualsCode(dto.getCurrency(), method.getCurrency())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route rule currency must match PSP method");
        }
        if (notEqualsCode(dto.getDirection(), method.getDirection())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route rule direction must match PSP method");
        }
        if (StringUtils.isNotBlank(dto.getCountryCode()) && notEqualsCode(dto.getCountryCode(), method.getCountryCode())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route rule country must match PSP method");
        }

        PspAccountEntity account = dto.getPspAccountId() == null ? null : pspAccountDao.selectById(dto.getPspAccountId());
        if (account == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "PSP account not found");
        }
        if (notEqualsLong(dto.getPspId(), account.getPspId())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route rule provider does not match PSP account");
        }
        if (account.getTenantId() == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "PSP account tenant is not configured");
        }

        dto.setTenantId(account.getTenantId());
        dto.setMethodCode(StrUtil.trim(method.getMethodCode()));
    }

    private void validateAmountRange(PspRouteRuleDTO dto) {
        if (dto == null) {
            return;
        }
        try {
            AmountRangeUtils.validateConfigRange(dto.getMinAmount(), dto.getMaxAmount());
        } catch (IllegalArgumentException ex) {
            throw new GkException(ErrorCode.BAD_REQUEST, ex.getMessage());
        }
    }
    private boolean notEqualsCode(String left, String right) {
        return !Strings.CI.equals(StringUtils.trim(left), StringUtils.trim(right));
    }

    private boolean notEqualsLong(Long left, Long right) {
        return left == null || !left.equals(right);
    }

    private void evictPayinPlanCache() {
        // PSP ģ鲻ֱ payment 棬ģ¼ͳһ
    }
}
