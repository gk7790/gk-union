package com.gk.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.ledger.service.LedgerAccountService;
import com.gk.payment.dao.PaymentRouteChannelDao;
import com.gk.payment.dao.PaymentRouteGroupDao;
import com.gk.payment.domain.amount.AmountRangeUtils;
import com.gk.payment.dto.PaymentRouteChannelDTO;
import com.gk.payment.dto.PaymentRouteChannelOptionsResponse;
import com.gk.payment.entity.PaymentRouteChannelEntity;
import com.gk.payment.entity.PaymentRouteGroupEntity;
import com.gk.payment.plan.cache.PayinPlanCache;
import com.gk.payment.plan.cache.PaymentPlanCacheService;
import com.gk.payment.service.PaymentRouteChannelService;
import com.gk.payment.domain.enums.PayDirectionEnum;
import com.gk.infra.enums.StatusEnum;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.dao.PspFeeRuleDao;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.dao.PspProviderDao;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.entity.PspFeeRuleEntity;
import com.gk.psp.entity.PspMethodEntity;
import com.gk.psp.entity.PspProviderEntity;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class PaymentRouteChannelServiceImpl extends CrudServiceImpl<PaymentRouteChannelDao, PaymentRouteChannelEntity, PaymentRouteChannelDTO> implements PaymentRouteChannelService {
    private final PaymentRouteGroupDao paymentRouteGroupDao;
    private final PspProviderDao pspProviderDao;
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
        List<Integer> statusList = StatusEnum.normalizeQueryStatus(params.getList("status", Integer.class, StatusEnum.defaultStatus()));

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(groupId != null, "group_id", groupId);
        wrapper.eq(pspId != null, "psp_id", pspId);
        wrapper.eq(pspMethodId != null, "psp_method_id", pspMethodId);
        wrapper.eq(pspAccountId != null, "psp_account_id", pspAccountId);
        wrapper.in("status", statusList);
        return wrapper;
    }

    @Override
    public PaymentRouteChannelOptionsResponse options(DynMap params) {
        PaymentRouteGroupEntity group = resolveOptionGroup(params);
        PaymentRouteChannelOptionsResponse response = new PaymentRouteChannelOptionsResponse();
        response.setRouteGroup(toRouteGroupOption(group));

        List<PspMethodEntity> methods = matchingPspMethods(group);
        response.setPspMethods(methods.stream().map(this::toPspMethodOption).toList());
        Set<Long> pspIds = methods.stream().map(PspMethodEntity::getPspId).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        if (pspIds.isEmpty()) {
            return response;
        }

        Set<Long> methodIds = methods.stream().map(PspMethodEntity::getId).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        CompletableFuture<List<PspProviderEntity>> providersFuture = CompletableFuture.supplyAsync(() -> matchingPspProviders(group, pspIds));
        CompletableFuture<List<PspAccountEntity>> accountsFuture = CompletableFuture.supplyAsync(() -> matchingPspAccounts(group, pspIds));
        List<PspAccountEntity> accounts = accountsFuture.join();
        Set<Long> accountIds = accounts.stream().map(PspAccountEntity::getId).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        CompletableFuture<List<PspFeeRuleEntity>> feeRulesFuture = CompletableFuture.supplyAsync(() -> matchingPspFeeRules(group, pspIds, methodIds, accountIds));

        response.setPspProviders(providersFuture.join().stream().map(this::toPspProviderOption).toList());
        response.setPspAccounts(accounts.stream().map(this::toPspAccountOption).toList());
        response.setPspFeeRules(feeRulesFuture.join().stream().map(this::toPspFeeRuleOption).toList());
        return response;
    }

    private PaymentRouteGroupEntity resolveOptionGroup(DynMap params) {
        DynMap query = params == null ? DynMap.empty() : params;
        Long groupId = query.getLong("groupId", null);
        if (groupId != null) {
            PaymentRouteGroupEntity group = paymentRouteGroupDao.selectById(groupId);
            if (group == null) {
                throw new GkException(ErrorCode.BAD_REQUEST, "Payment route group does not exist");
            }
            return group;
        }

        Long tenantId = query.getLong("tenantId", query.getLong("tenant_id", null));
        String direction = paramStr(query, "direction");
        String currency = paramStr(query, "currency");
        String methodCode = paramStr(query, "methodCode", "method_code");
        if (tenantId == null || StringUtils.isAnyBlank(direction, currency, methodCode)) {
            throw new GkException(ErrorCode.BAD_REQUEST, "tenantId, direction, currency and methodCode are required");
        }

        PaymentRouteGroupEntity group = new PaymentRouteGroupEntity();
        group.setTenantId(tenantId);
        group.setGroupCode(paramStr(query, "groupCode", "group_code"));
        group.setGroupName(paramStr(query, "groupName", "group_name"));
        group.setDirection(direction);
        group.setCountryCode(paramStr(query, "countryCode", "country_code"));
        group.setCurrency(currency);
        group.setMethodCode(methodCode);
        group.setStrategy(paramStr(query, "strategy"));
        group.setStatus(StatusEnum.NORMAL.code());
        return group;
    }

    private List<PspMethodEntity> matchingPspMethods(PaymentRouteGroupEntity group) {
        QueryWrapper<PspMethodEntity> wrapper = new QueryWrapper<PspMethodEntity>()
                .eq("direction", normalize(group.getDirection()))
                .eq("currency", normalize(group.getCurrency()))
                .eq("method_code", normalize(group.getMethodCode()))
                .eq("status", StatusEnum.NORMAL.code())
                .orderByAsc("psp_id")
                .orderByAsc("id");
        applyCountryFilter(wrapper, group.getCountryCode());
        return pspMethodDao.selectList(wrapper);
    }

    private List<PspProviderEntity> matchingPspProviders(PaymentRouteGroupEntity group, Set<Long> pspIds) {
        QueryWrapper<PspProviderEntity> wrapper = new QueryWrapper<PspProviderEntity>()
                .in("id", pspIds)
                .eq("status", StatusEnum.NORMAL.code())
                .orderByAsc("psp_code")
                .orderByAsc("id");
        if (PayDirectionEnum.PAYOUT.code().equalsIgnoreCase(normalize(group.getDirection()))) {
            wrapper.eq("support_payout", 1);
        } else {
            wrapper.eq("support_payin", 1);
        }
        return pspProviderDao.selectList(wrapper);
    }

    private List<PspAccountEntity> matchingPspAccounts(PaymentRouteGroupEntity group, Set<Long> pspIds) {
        return pspAccountDao.selectList(new QueryWrapper<PspAccountEntity>()
                .eq("tenant_id", group.getTenantId())
                .in("psp_id", pspIds)
                .eq("status", StatusEnum.NORMAL.code())
                .orderByAsc("psp_id")
                .orderByAsc("psp_account_no")
                .orderByAsc("id"));
    }

    private List<PspFeeRuleEntity> matchingPspFeeRules(PaymentRouteGroupEntity group,
                                                       Set<Long> pspIds,
                                                       Set<Long> methodIds,
                                                       Set<Long> accountIds) {
        QueryWrapper<PspFeeRuleEntity> wrapper = new QueryWrapper<PspFeeRuleEntity>()
                .eq("tenant_id", group.getTenantId())
                .in("psp_id", pspIds)
                .eq("direction", normalize(group.getDirection()))
                .eq("currency", normalize(group.getCurrency()))
                .eq("status", StatusEnum.NORMAL.code())
                .and(item -> item.eq("method_code", normalize(group.getMethodCode())).or().isNull("method_code").or().eq("method_code", ""))
                .and(item -> item.le("effective_at", Instant.now()).or().isNull("effective_at"))
                .and(item -> item.gt("expire_at", Instant.now()).or().isNull("expire_at"))
                .orderByAsc("psp_id")
                .orderByAsc("priority")
                .orderByAsc("id");
        applyCountryFilter(wrapper, group.getCountryCode());
        if (methodIds.isEmpty()) {
            wrapper.isNull("psp_method_id");
        } else {
            wrapper.and(item -> item.in("psp_method_id", methodIds).or().isNull("psp_method_id"));
        }
        if (accountIds.isEmpty()) {
            wrapper.isNull("psp_account_id");
        } else {
            wrapper.and(item -> item.in("psp_account_id", accountIds).or().isNull("psp_account_id"));
        }
        return pspFeeRuleDao.selectList(wrapper);
    }

    private <T> void applyCountryFilter(QueryWrapper<T> wrapper, String countryCode) {
        String normalized = normalize(countryCode);
        if (StringUtils.isBlank(normalized)) {
            wrapper.and(item -> item.isNull("country_code").or().eq("country_code", ""));
        } else {
            wrapper.eq("country_code", normalized);
        }
    }

    private PaymentRouteChannelOptionsResponse.RouteGroupOption toRouteGroupOption(PaymentRouteGroupEntity group) {
        PaymentRouteChannelOptionsResponse.RouteGroupOption option = new PaymentRouteChannelOptionsResponse.RouteGroupOption();
        option.setId(group.getId());
        option.setTenantId(group.getTenantId());
        option.setGroupCode(group.getGroupCode());
        option.setGroupName(group.getGroupName());
        option.setDirection(group.getDirection());
        option.setCountryCode(group.getCountryCode());
        option.setCurrency(group.getCurrency());
        option.setMethodCode(group.getMethodCode());
        option.setStrategy(group.getStrategy());
        option.setStatus(group.getStatus());
        option.setLabel(label(group.getGroupCode(), group.getGroupName()));
        option.setValue(group.getId());
        return option;
    }

    private PaymentRouteChannelOptionsResponse.PspProviderOption toPspProviderOption(PspProviderEntity provider) {
        PaymentRouteChannelOptionsResponse.PspProviderOption option = new PaymentRouteChannelOptionsResponse.PspProviderOption();
        option.setLabel(label(provider.getPspCode(), provider.getPspName()));
        option.setValue(provider.getId());
        option.setPspId(provider.getId());
        option.setPspCode(provider.getPspCode());
        option.setPspName(provider.getPspName());
        option.setCountryCode(provider.getCountryCode());
        option.setApiVersion(provider.getApiVersion());
        option.setSupportPayin(provider.getSupportPayin());
        option.setSupportPayout(provider.getSupportPayout());
        option.setStatus(provider.getStatus());
        return option;
    }

    private PaymentRouteChannelOptionsResponse.PspMethodOption toPspMethodOption(PspMethodEntity method) {
        PaymentRouteChannelOptionsResponse.PspMethodOption option = new PaymentRouteChannelOptionsResponse.PspMethodOption();
        option.setLabel(label(method.getPspCode(), method.getMethodCode(), method.getPspMethodCode(), method.getCurrency()));
        option.setValue(method.getId());
        option.setPspMethodId(method.getId());
        option.setPspId(method.getPspId());
        option.setPspCode(method.getPspCode());
        option.setMethodCode(method.getMethodCode());
        option.setPspMethodCode(method.getPspMethodCode());
        option.setMethodName(method.getMethodName());
        option.setCountryCode(method.getCountryCode());
        option.setCurrency(method.getCurrency());
        option.setDirection(method.getDirection());
        option.setMinAmount(method.getMinAmount());
        option.setMaxAmount(method.getMaxAmount());
        option.setDailyLimit(method.getDailyLimit());
        option.setStatus(method.getStatus());
        return option;
    }

    private PaymentRouteChannelOptionsResponse.PspAccountOption toPspAccountOption(PspAccountEntity account) {
        PaymentRouteChannelOptionsResponse.PspAccountOption option = new PaymentRouteChannelOptionsResponse.PspAccountOption();
        option.setLabel(label(account.getPspAccountNo(), account.getPspAccountName()));
        option.setValue(account.getId());
        option.setPspAccountId(account.getId());
        option.setPspId(account.getPspId());
        option.setPspAccountNo(account.getPspAccountNo());
        option.setPspAccountName(account.getPspAccountName());
        option.setSecretType(account.getSecretType());
        option.setStatus(account.getStatus());
        return option;
    }

    private PaymentRouteChannelOptionsResponse.PspFeeRuleOption toPspFeeRuleOption(PspFeeRuleEntity rule) {
        PaymentRouteChannelOptionsResponse.PspFeeRuleOption option = new PaymentRouteChannelOptionsResponse.PspFeeRuleOption();
        option.setLabel(feeRuleLabel(rule));
        option.setValue(rule.getId());
        option.setPspFeeRuleId(rule.getId());
        option.setPspId(rule.getPspId());
        option.setPspAccountId(rule.getPspAccountId());
        option.setPspMethodId(rule.getPspMethodId());
        option.setPspMethodCode(rule.getPspMethodCode());
        option.setRuleName(rule.getRuleName());
        option.setDirection(rule.getDirection());
        option.setCountryCode(rule.getCountryCode());
        option.setCurrency(rule.getCurrency());
        option.setMethodCode(rule.getMethodCode());
        option.setMinAmount(rule.getMinAmount());
        option.setMaxAmount(rule.getMaxAmount());
        option.setFeeMode(rule.getFeeMode());
        option.setFeeRate(rule.getFeeRate());
        option.setFeeFixed(rule.getFeeFixed());
        option.setMinFee(rule.getMinFee());
        option.setMaxFee(rule.getMaxFee());
        option.setPriority(rule.getPriority());
        option.setStatus(rule.getStatus());
        return option;
    }

    private String feeRuleLabel(PspFeeRuleEntity rule) {
        return label(rule.getRuleName(), rule.getFeeMode(), decimalText(rule.getFeeRate()), decimalText(rule.getFeeFixed()));
    }

    private String label(String... parts) {
        return java.util.Arrays.stream(parts)
                .filter(StringUtils::isNotBlank)
                .map(StringUtils::trim)
                .collect(java.util.stream.Collectors.joining(" / "));
    }

    private String decimalText(java.math.BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private String normalize(String value) {
        return StringUtils.upperCase(StringUtils.trim(value));
    }

    private String paramStr(DynMap params, String... keys) {
        for (String key : keys) {
            String value = params.getStr(key);
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
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
        baseDao.update(null, new UpdateWrapper<PaymentRouteChannelEntity>()
                .set("status", StatusEnum.STOP.code())
                .in("id", Arrays.asList(ids)));
        evictPlanCache();
    }

    @Override
    public void delete(Long id) {
        baseDao.update(null, new UpdateWrapper<PaymentRouteChannelEntity>()
                .set("status", StatusEnum.STOP.code())
                .eq("id", id));
        evictPlanCache();
    }

    private PaymentRouteGroupEntity validateChannelBinding(PaymentRouteChannelDTO dto) {
        if (dto == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Payment route channel is required");
        }
        validateAmountRange(dto);
        PaymentRouteGroupEntity group = dto.getGroupId() == null ? null : paymentRouteGroupDao.selectById(dto.getGroupId());
        if (group == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Payment route group does not exist");
        }
        PspMethodEntity method = dto.getPspMethodId() == null ? null : pspMethodDao.selectById(dto.getPspMethodId());
        if (method == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "PSP method does not exist");
        }
        if (notEqualsLong(dto.getPspId(), method.getPspId())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route channel PSP must match PSP method");
        }
        if (notEqualsCode(group.getDirection(), method.getDirection())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route group direction must match PSP method");
        }
        if (notEqualsCode(group.getCurrency(), method.getCurrency())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route group currency must match PSP method");
        }
        if (notEqualsCode(group.getMethodCode(), method.getMethodCode())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route group method must match PSP method");
        }
        if (StringUtils.isNotBlank(group.getCountryCode()) && notEqualsCode(group.getCountryCode(), method.getCountryCode())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route group country must match PSP method");
        }

        PspAccountEntity account = dto.getPspAccountId() == null ? null : pspAccountDao.selectById(dto.getPspAccountId());
        if (account == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "PSP account does not exist");
        }
        if (notEqualsLong(dto.getPspId(), account.getPspId())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route channel PSP must match PSP account");
        }
        if (account.getTenantId() == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "PSP account tenant is required");
        }
        if (notEqualsLong(group.getTenantId(), account.getTenantId())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route group tenant must match PSP account tenant");
        }
        validatePspFeeRule(dto, group);

        dto.setTenantId(group.getTenantId());
        return group;
    }

    private void validateAmountRange(PaymentRouteChannelDTO dto) {
        if (dto == null) {
            return;
        }
        try {
            AmountRangeUtils.validateConfigRange(dto.getMinAmount(), dto.getMaxAmount());
        } catch (IllegalArgumentException ex) {
            throw new GkException(ErrorCode.BAD_REQUEST, ex.getMessage());
        }
    }

    private void validatePspFeeRule(PaymentRouteChannelDTO dto, PaymentRouteGroupEntity group) {
        if (dto.getPspFeeRuleId() == null) {
            return;
        }
        PspFeeRuleEntity feeRule = pspFeeRuleDao.selectById(dto.getPspFeeRuleId());
        if (feeRule == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "PSP fee rule does not exist");
        }
        if (notEqualsLong(group.getTenantId(), feeRule.getTenantId())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route group tenant must match PSP fee rule tenant");
        }
        if (notEqualsLong(dto.getPspId(), feeRule.getPspId())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route channel PSP must match PSP fee rule");
        }
        if (feeRule.getPspMethodId() != null && notEqualsLong(dto.getPspMethodId(), feeRule.getPspMethodId())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route channel PSP method must match PSP fee rule");
        }
        if (feeRule.getPspAccountId() != null && notEqualsLong(dto.getPspAccountId(), feeRule.getPspAccountId())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route channel PSP account must match PSP fee rule");
        }
        if (notEqualsCode(group.getDirection(), feeRule.getDirection())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route group direction must match PSP fee rule");
        }
        if (notEqualsCode(group.getCurrency(), feeRule.getCurrency())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route group currency must match PSP fee rule");
        }
        if (StringUtils.isNotBlank(feeRule.getMethodCode()) && notEqualsCode(group.getMethodCode(), feeRule.getMethodCode())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route group method must match PSP fee rule");
        }
        if (StringUtils.isBlank(group.getCountryCode()) && StringUtils.isNotBlank(feeRule.getCountryCode())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Generic route group must use generic PSP fee rule country");
        }
        if (StringUtils.isNotBlank(group.getCountryCode())
                && StringUtils.isNotBlank(feeRule.getCountryCode())
                && notEqualsCode(group.getCountryCode(), feeRule.getCountryCode())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route group country must match PSP fee rule");
        }
    }

    private void provisionPspLedgerAccounts(PaymentRouteChannelDTO dto, PaymentRouteGroupEntity group) {
        if (dto == null || group == null || dto.getTenantId() == null || dto.getPspAccountId() == null || StringUtils.isBlank(group.getCurrency())) {
            return;
        }
        ledgerAccountService.provisionPspAccounts(dto.getTenantId(), dto.getPspAccountId(), group.getCurrency());
    }

    private boolean notEqualsCode(String left, String right) {
        return !Strings.CI.equals(StringUtils.trim(left), StringUtils.trim(right));
    }

    private boolean notEqualsLong(Long left, Long right) {
        return left == null || !left.equals(right);
    }

    private void evictPlanCache() {
        payinPlanCache.evictAll();
        paymentPlanCacheService.evictAll();
    }
}
