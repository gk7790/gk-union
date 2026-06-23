package com.gk.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.ledger.service.LedgerAccountService;
import com.gk.payment.dao.PaymentRouteChannelDao;
import com.gk.payment.dao.PaymentRouteGroupDao;
import com.gk.payment.dto.PaymentRouteChannelDTO;
import com.gk.payment.entity.PaymentRouteChannelEntity;
import com.gk.payment.entity.PaymentRouteGroupEntity;
import com.gk.payment.plan.PayinPlanCache;
import com.gk.payment.plan.PaymentPlanCacheService;
import com.gk.payment.service.PaymentRouteChannelService;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.dao.PspFeeRuleDao;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.entity.PspFeeRuleEntity;
import com.gk.psp.entity.PspMethodEntity;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentRouteChannelServiceImpl extends CrudServiceImpl<PaymentRouteChannelDao, PaymentRouteChannelEntity, PaymentRouteChannelDTO> implements PaymentRouteChannelService {
    private final PaymentRouteGroupDao paymentRouteGroupDao;
    private final PspMethodDao pspMethodDao;
    private final PspAccountDao pspAccountDao;
    private final PspFeeRuleDao pspFeeRuleDao;
    private final LedgerAccountService ledgerAccountService;
    private final PayinPlanCache payinPlanCache;
    private final PaymentPlanCacheService paymentPlanCacheService;

    @Override
    public QueryWrapper<PaymentRouteChannelEntity> getWrapper(DynMap params) {
        QueryWrapper<PaymentRouteChannelEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long groupId = params.getLong("groupId", null);
        Long pspId = params.getLong("pspId", null);
        Long pspMethodId = params.getLong("pspMethodId", null);
        Long pspAccountId = params.getLong("pspAccountId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(groupId != null, "group_id", groupId);
        wrapper.eq(pspId != null, "psp_id", pspId);
        wrapper.eq(pspMethodId != null, "psp_method_id", pspMethodId);
        wrapper.eq(pspAccountId != null, "psp_account_id", pspAccountId);
        wrapper.eq(status != null, "status", status);
        return wrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(PaymentRouteChannelDTO dto) {
        PaymentRouteGroupEntity group = validateChannelBinding(dto);
        super.save(dto);
        provisionPspLedgerAccounts(dto, group);
        evictPlanCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PaymentRouteChannelDTO dto) {
        PaymentRouteGroupEntity group = validateChannelBinding(dto);
        super.update(dto);
        provisionPspLedgerAccounts(dto, group);
        evictPlanCache();
    }

    @Override
    public void delete(Long[] ids) {
        super.delete(ids);
        evictPlanCache();
    }

    @Override
    public void delete(Long id) {
        super.delete(id);
        evictPlanCache();
    }

    private PaymentRouteGroupEntity validateChannelBinding(PaymentRouteChannelDTO dto) {
        if (dto == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Payment route channel is required");
        }
        PaymentRouteGroupEntity group = dto.getGroupId() == null ? null : paymentRouteGroupDao.selectById(dto.getGroupId());
        if (group == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Payment route group does not exist");
        }
        PspMethodEntity method = dto.getPspMethodId() == null ? null : pspMethodDao.selectById(dto.getPspMethodId());
        if (method == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "PSP method does not exist");
        }
        if (!equalsLong(dto.getPspId(), method.getPspId())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route channel PSP must match PSP method");
        }
        if (!equalsCode(group.getDirection(), method.getDirection())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route group direction must match PSP method");
        }
        if (!equalsCode(group.getCurrency(), method.getCurrency())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route group currency must match PSP method");
        }
        if (!equalsCode(group.getMethodCode(), method.getMethodCode())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route group method must match PSP method");
        }
        if (StringUtils.isNotBlank(group.getCountryCode()) && !equalsCode(group.getCountryCode(), method.getCountryCode())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route group country must match PSP method");
        }

        PspAccountEntity account = dto.getPspAccountId() == null ? null : pspAccountDao.selectById(dto.getPspAccountId());
        if (account == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "PSP account does not exist");
        }
        if (!equalsLong(dto.getPspId(), account.getPspId())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route channel PSP must match PSP account");
        }
        if (account.getTenantId() == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "PSP account tenant is required");
        }
        if (!equalsLong(group.getTenantId(), account.getTenantId())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route group tenant must match PSP account tenant");
        }
        validatePspFeeRule(dto, group);

        dto.setTenantId(group.getTenantId());
        return group;
    }

    private void validatePspFeeRule(PaymentRouteChannelDTO dto, PaymentRouteGroupEntity group) {
        if (dto.getPspFeeRuleId() == null) {
            return;
        }
        PspFeeRuleEntity feeRule = pspFeeRuleDao.selectById(dto.getPspFeeRuleId());
        if (feeRule == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "PSP fee rule does not exist");
        }
        if (!equalsLong(group.getTenantId(), feeRule.getTenantId())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route group tenant must match PSP fee rule tenant");
        }
        if (!equalsLong(dto.getPspId(), feeRule.getPspId())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route channel PSP must match PSP fee rule");
        }
        if (feeRule.getPspMethodId() != null && !equalsLong(dto.getPspMethodId(), feeRule.getPspMethodId())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route channel PSP method must match PSP fee rule");
        }
        if (feeRule.getPspAccountId() != null && !equalsLong(dto.getPspAccountId(), feeRule.getPspAccountId())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route channel PSP account must match PSP fee rule");
        }
        if (!equalsCode(group.getDirection(), feeRule.getDirection())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route group direction must match PSP fee rule");
        }
        if (!equalsCode(group.getCurrency(), feeRule.getCurrency())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route group currency must match PSP fee rule");
        }
        if (StringUtils.isNotBlank(feeRule.getMethodCode()) && !equalsCode(group.getMethodCode(), feeRule.getMethodCode())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route group method must match PSP fee rule");
        }
        if (StringUtils.isBlank(group.getCountryCode()) && StringUtils.isNotBlank(feeRule.getCountryCode())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Generic route group must use generic PSP fee rule country");
        }
        if (StringUtils.isNotBlank(group.getCountryCode())
                && StringUtils.isNotBlank(feeRule.getCountryCode())
                && !equalsCode(group.getCountryCode(), feeRule.getCountryCode())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route group country must match PSP fee rule");
        }
    }

    private void provisionPspLedgerAccounts(PaymentRouteChannelDTO dto, PaymentRouteGroupEntity group) {
        if (dto == null || group == null || dto.getTenantId() == null || dto.getPspAccountId() == null || StringUtils.isBlank(group.getCurrency())) {
            return;
        }
        ledgerAccountService.provisionPspAccounts(dto.getTenantId(), dto.getPspAccountId(), group.getCurrency());
    }

    private boolean equalsCode(String left, String right) {
        return StringUtils.equalsIgnoreCase(StringUtils.trim(left), StringUtils.trim(right));
    }

    private boolean equalsLong(Long left, Long right) {
        return left != null && left.equals(right);
    }

    private void evictPlanCache() {
        payinPlanCache.evictAll();
        paymentPlanCacheService.evictAll();
    }
}
