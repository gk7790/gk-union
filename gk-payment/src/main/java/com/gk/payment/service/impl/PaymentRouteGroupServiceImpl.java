package com.gk.payment.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.infra.enums.StatusEnum;
import com.gk.common.amount.AmountRangeUtils;
import com.gk.payment.dao.PaymentRouteChannelDao;
import com.gk.payment.dao.PaymentRouteGroupDao;
import com.gk.payment.dao.PaymentRouteRuleDao;
import com.gk.payment.dto.PaymentRouteGroupCheckResponse;
import com.gk.payment.dto.PaymentRouteGroupDTO;
import com.gk.payment.entity.PaymentRouteChannelEntity;
import com.gk.payment.entity.PaymentRouteGroupEntity;
import com.gk.payment.entity.PaymentRouteRuleEntity;
import com.gk.payment.plan.PaymentPlanCacheService;
import com.gk.payment.plan.PayinPlanCache;
import com.gk.payment.service.PaymentRouteGroupService;
import com.gk.psp.dao.PspFeeRuleDao;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.entity.PspFeeRuleEntity;
import com.gk.psp.entity.PspMethodEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentRouteGroupServiceImpl extends CrudServiceImpl<PaymentRouteGroupDao, PaymentRouteGroupEntity, PaymentRouteGroupDTO> implements PaymentRouteGroupService {
    private final PayinPlanCache payinPlanCache;
    private final PaymentPlanCacheService paymentPlanCacheService;
    private final PaymentRouteRuleDao paymentRouteRuleDao;
    private final PaymentRouteChannelDao paymentRouteChannelDao;
    private final PspMethodDao pspMethodDao;
    private final PspFeeRuleDao pspFeeRuleDao;

    @Override
    public QueryWrapper<PaymentRouteGroupEntity> getWrapper(DynMap params) {
        QueryWrapper<PaymentRouteGroupEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String groupCode = params.getStr("groupCode");
        String groupName = params.getStr("groupName");
        String direction = params.getStr("direction");
        String countryCode = params.getStr("countryCode");
        String currency = params.getStr("currency");
        String methodCode = params.getStr("methodCode");
        String strategy = params.getStr("strategy");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(status != null, "status", status);
        wrapper.like(StrUtil.isNotBlank(groupCode), "group_code", groupCode);
        wrapper.like(StrUtil.isNotBlank(groupName), "group_name", groupName);
        wrapper.eq(StrUtil.isNotBlank(direction), "direction", normalize(direction));
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", normalize(countryCode));
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", normalize(currency));
        wrapper.eq(StrUtil.isNotBlank(methodCode), "method_code", normalize(methodCode));
        wrapper.eq(StrUtil.isNotBlank(strategy), "strategy", normalize(strategy));
        return wrapper;
    }

    @Override
    public PaymentRouteGroupCheckResponse check(Long id) {
        PaymentRouteGroupEntity group = id == null ? null : baseDao.selectById(id);
        if (group == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Payment route group does not exist");
        }

        PaymentRouteGroupCheckResponse response = toCheckResponse(group);
        List<PaymentRouteRuleEntity> routeRules = activeRouteRules(group);
        List<PaymentRouteChannelEntity> channels = activeRouteChannels(group);
        if (routeRules.isEmpty()) {
            response.addWarning("PAYMENT_ROUTE_RULE_MISSING", "No active payment route rule uses this route group");
            return response;
        }
        if (channels.isEmpty()) {
            response.addError("PAYMENT_ROUTE_CHANNEL_MISSING", "No active payment route channel is configured for this route group");
        }

        for (PaymentRouteRuleEntity routeRule : routeRules) {
            PaymentRouteGroupCheckResponse.RouteRuleCheck ruleCheck = checkRouteRule(group, routeRule, channels);
            response.getRouteRules().add(ruleCheck);
            if (Boolean.FALSE.equals(ruleCheck.getValid())) {
                response.addError("AMOUNT_GAP", "Payment route rule has uncovered amount range: " + routeRule.getId());
            }
        }
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(PaymentRouteGroupDTO dto) {
        normalize(dto);
        super.save(dto);
        evictPlanCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PaymentRouteGroupDTO dto) {
        normalize(dto);
        super.update(dto);
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

    private List<PaymentRouteRuleEntity> activeRouteRules(PaymentRouteGroupEntity group) {
        Instant now = Instant.now();
        return paymentRouteRuleDao.selectList(new QueryWrapper<PaymentRouteRuleEntity>()
                .eq("tenant_id", group.getTenantId())
                .eq("group_id", group.getId())
                .eq("status", StatusEnum.NORMAL.code())
                .and(item -> item.le("effective_at", now).or().isNull("effective_at"))
                .and(item -> item.gt("expire_at", now).or().isNull("expire_at"))
                .orderByAsc("priority")
                .orderByAsc("id"));
    }

    private List<PaymentRouteChannelEntity> activeRouteChannels(PaymentRouteGroupEntity group) {
        return paymentRouteChannelDao.selectList(new QueryWrapper<PaymentRouteChannelEntity>()
                .eq("tenant_id", group.getTenantId())
                .eq("group_id", group.getId())
                .eq("status", StatusEnum.NORMAL.code())
                .orderByAsc("priority")
                .orderByAsc("fallback_order")
                .orderByAsc("id"));
    }

    private PaymentRouteGroupCheckResponse.RouteRuleCheck checkRouteRule(PaymentRouteGroupEntity group,
                                                                         PaymentRouteRuleEntity routeRule,
                                                                         List<PaymentRouteChannelEntity> channels) {
        PaymentRouteGroupCheckResponse.RouteRuleCheck result = toRouteRuleCheck(routeRule);
        List<PaymentRouteGroupCheckResponse.AmountRange> ranges = new ArrayList<>();
        for (PaymentRouteChannelEntity channel : channels) {
            PaymentRouteGroupCheckResponse.ChannelCoverage coverage = channelCoverage(group, routeRule, channel);
            result.getChannels().add(coverage);
            if (Boolean.TRUE.equals(coverage.getAvailable())) {
                ranges.add(new PaymentRouteGroupCheckResponse.AmountRange(coverage.getMinAmount(), coverage.getMaxAmount()));
            } else {
                result.addWarning("PAYMENT_ROUTE_CHANNEL_UNAVAILABLE", coverage.getMessage());
            }
        }

        List<PaymentRouteGroupCheckResponse.AmountRange> coveredRanges = mergeRanges(ranges);
        List<PaymentRouteGroupCheckResponse.AmountRange> gaps = amountGaps(
                AmountRangeUtils.effectiveMin(routeRule.getMinAmount()),
                AmountRangeUtils.effectiveMax(routeRule.getMaxAmount()),
                coveredRanges
        );
        result.setCoveredRanges(coveredRanges);
        result.setGaps(gaps);
        if (!gaps.isEmpty()) {
            result.addError("AMOUNT_GAP", "Payment route rule amount range is not fully covered by PSP channels");
        }
        return result;
    }

    private PaymentRouteGroupCheckResponse.ChannelCoverage channelCoverage(PaymentRouteGroupEntity group,
                                                                          PaymentRouteRuleEntity routeRule,
                                                                          PaymentRouteChannelEntity channel) {
        PaymentRouteGroupCheckResponse.ChannelCoverage coverage = new PaymentRouteGroupCheckResponse.ChannelCoverage();
        coverage.setRouteChannelId(channel.getId());
        coverage.setPspId(channel.getPspId());
        coverage.setPspMethodId(channel.getPspMethodId());
        coverage.setPspAccountId(channel.getPspAccountId());
        coverage.setPspFeeRuleId(channel.getPspFeeRuleId());

        PspMethodEntity method = channel.getPspMethodId() == null ? null : pspMethodDao.selectById(channel.getPspMethodId());
        if (!methodAvailable(group, channel, method)) {
            coverage.setAvailable(false);
            coverage.setMessage("PSP method is missing, disabled or does not match route group");
            return coverage;
        }
        coverage.setPspCode(method.getPspCode());
        coverage.setPspMethodCode(method.getPspMethodCode());

        PspFeeRuleEntity feeRule = null;
        if (channel.getPspFeeRuleId() != null) {
            feeRule = pspFeeRuleDao.selectById(channel.getPspFeeRuleId());
            if (!feeRuleAvailable(group, channel, feeRule)) {
                coverage.setAvailable(false);
                coverage.setMessage("PSP fee rule is missing, disabled or does not match route channel");
                return coverage;
            }
        }

        BigDecimal start = max(
                AmountRangeUtils.effectiveMin(routeRule.getMinAmount()),
                AmountRangeUtils.effectiveMin(channel.getMinAmount()),
                AmountRangeUtils.effectiveMin(method.getMinAmount()),
                feeRule == null ? null : AmountRangeUtils.effectiveMin(feeRule.getMinAmount())
        );
        BigDecimal end = min(
                AmountRangeUtils.effectiveMax(routeRule.getMaxAmount()),
                AmountRangeUtils.effectiveMax(channel.getMaxAmount()),
                AmountRangeUtils.effectiveMax(method.getMaxAmount()),
                feeRule == null ? null : AmountRangeUtils.effectiveMax(feeRule.getMaxAmount())
        );
        if (end != null && start.compareTo(end) > 0) {
            coverage.setAvailable(false);
            coverage.setMessage("Payment route channel amount range has no overlap with route rule");
            return coverage;
        }
        coverage.setMinAmount(start);
        coverage.setMaxAmount(end);
        coverage.setMessage("OK");
        return coverage;
    }

    private boolean methodAvailable(PaymentRouteGroupEntity group, PaymentRouteChannelEntity channel, PspMethodEntity method) {
        if (method == null || !StatusEnum.NORMAL.code().equals(method.getStatus())) {
            return false;
        }
        return Objects.equals(channel.getPspId(), method.getPspId())
                && equalsCode(group.getDirection(), method.getDirection())
                && equalsCode(group.getCurrency(), method.getCurrency())
                && equalsCode(group.getMethodCode(), method.getMethodCode())
                && (StrUtil.isBlank(group.getCountryCode()) || equalsCode(group.getCountryCode(), method.getCountryCode()));
    }

    private boolean feeRuleAvailable(PaymentRouteGroupEntity group, PaymentRouteChannelEntity channel, PspFeeRuleEntity feeRule) {
        if (feeRule == null || !StatusEnum.NORMAL.code().equals(feeRule.getStatus())) {
            return false;
        }
        Instant now = Instant.now();
        return Objects.equals(group.getTenantId(), feeRule.getTenantId())
                && Objects.equals(channel.getPspId(), feeRule.getPspId())
                && (feeRule.getPspMethodId() == null || Objects.equals(channel.getPspMethodId(), feeRule.getPspMethodId()))
                && (feeRule.getPspAccountId() == null || Objects.equals(channel.getPspAccountId(), feeRule.getPspAccountId()))
                && equalsCode(group.getDirection(), feeRule.getDirection())
                && equalsCode(group.getCurrency(), feeRule.getCurrency())
                && (StrUtil.isBlank(feeRule.getMethodCode()) || equalsCode(group.getMethodCode(), feeRule.getMethodCode()))
                && (StrUtil.isBlank(group.getCountryCode()) || StrUtil.isBlank(feeRule.getCountryCode()) || equalsCode(group.getCountryCode(), feeRule.getCountryCode()))
                && (feeRule.getEffectiveAt() == null || !feeRule.getEffectiveAt().isAfter(now))
                && (feeRule.getExpireAt() == null || feeRule.getExpireAt().isAfter(now));
    }

    private List<PaymentRouteGroupCheckResponse.AmountRange> mergeRanges(List<PaymentRouteGroupCheckResponse.AmountRange> ranges) {
        List<PaymentRouteGroupCheckResponse.AmountRange> sorted = ranges.stream()
                .sorted(Comparator.comparing(PaymentRouteGroupCheckResponse.AmountRange::minAmount))
                .toList();
        List<PaymentRouteGroupCheckResponse.AmountRange> merged = new ArrayList<>();
        for (PaymentRouteGroupCheckResponse.AmountRange range : sorted) {
            if (merged.isEmpty()) {
                merged.add(range);
                continue;
            }
            PaymentRouteGroupCheckResponse.AmountRange last = merged.getLast();
            if (last.maxAmount() == null || range.minAmount().compareTo(last.maxAmount()) <= 0) {
                merged.set(merged.size() - 1, new PaymentRouteGroupCheckResponse.AmountRange(last.minAmount(), maxEnd(last.maxAmount(), range.maxAmount())));
            } else {
                merged.add(range);
            }
        }
        return merged;
    }

    private List<PaymentRouteGroupCheckResponse.AmountRange> amountGaps(BigDecimal ruleStart,
                                                                        BigDecimal ruleEnd,
                                                                        List<PaymentRouteGroupCheckResponse.AmountRange> coveredRanges) {
        List<PaymentRouteGroupCheckResponse.AmountRange> gaps = new ArrayList<>();
        BigDecimal cursor = ruleStart;
        for (PaymentRouteGroupCheckResponse.AmountRange covered : coveredRanges) {
            if (ruleEnd != null && cursor.compareTo(ruleEnd) >= 0) {
                return gaps;
            }
            if (covered.maxAmount() != null && covered.maxAmount().compareTo(cursor) <= 0) {
                continue;
            }
            if (covered.minAmount().compareTo(cursor) > 0) {
                gaps.add(new PaymentRouteGroupCheckResponse.AmountRange(cursor, min(ruleEnd, covered.minAmount())));
            }
            if (covered.maxAmount() == null) {
                return gaps;
            }
            if (covered.maxAmount().compareTo(cursor) > 0) {
                cursor = covered.maxAmount();
            }
        }
        if (ruleEnd == null || cursor.compareTo(ruleEnd) < 0) {
            gaps.add(new PaymentRouteGroupCheckResponse.AmountRange(cursor, ruleEnd));
        }
        return gaps;
    }

    private PaymentRouteGroupCheckResponse toCheckResponse(PaymentRouteGroupEntity group) {
        PaymentRouteGroupCheckResponse response = new PaymentRouteGroupCheckResponse();
        response.setGroupId(group.getId());
        response.setGroupCode(group.getGroupCode());
        response.setGroupName(group.getGroupName());
        response.setDirection(group.getDirection());
        response.setCountryCode(group.getCountryCode());
        response.setCurrency(group.getCurrency());
        response.setMethodCode(group.getMethodCode());
        return response;
    }

    private PaymentRouteGroupCheckResponse.RouteRuleCheck toRouteRuleCheck(PaymentRouteRuleEntity routeRule) {
        PaymentRouteGroupCheckResponse.RouteRuleCheck result = new PaymentRouteGroupCheckResponse.RouteRuleCheck();
        result.setRouteRuleId(routeRule.getId());
        result.setRuleName(routeRule.getRuleName());
        result.setMerchantId(routeRule.getMerchantId());
        result.setMerchantAppId(routeRule.getMerchantAppId());
        result.setMinAmount(AmountRangeUtils.effectiveMin(routeRule.getMinAmount()));
        result.setMaxAmount(AmountRangeUtils.effectiveMax(routeRule.getMaxAmount()));
        return result;
    }

    private BigDecimal max(BigDecimal first, BigDecimal... rest) {
        BigDecimal result = first;
        for (BigDecimal item : rest) {
            if (item != null && item.compareTo(result) > 0) {
                result = item;
            }
        }
        return result;
    }

    private BigDecimal min(BigDecimal first, BigDecimal second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.compareTo(second) <= 0 ? first : second;
    }

    private BigDecimal min(BigDecimal first, BigDecimal... rest) {
        BigDecimal result = first;
        for (BigDecimal item : rest) {
            result = min(result, item);
        }
        return result;
    }

    private BigDecimal maxEnd(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return null;
        }
        return left.compareTo(right) >= 0 ? left : right;
    }

    private boolean equalsCode(String left, String right) {
        return StrUtil.equalsIgnoreCase(StrUtil.trim(left), StrUtil.trim(right));
    }

    private void normalize(PaymentRouteGroupDTO dto) {
        if (dto == null) {
            return;
        }
        dto.setGroupCode(normalize(dto.getGroupCode()));
        dto.setDirection(normalize(dto.getDirection()));
        dto.setCountryCode(normalize(dto.getCountryCode()));
        dto.setCurrency(normalize(dto.getCurrency()));
        dto.setMethodCode(normalize(dto.getMethodCode()));
        dto.setStrategy(StrUtil.blankToDefault(normalize(dto.getStrategy()), "PRIORITY_WEIGHT"));
    }

    private String normalize(String value) {
        return StrUtil.nullToEmpty(value).trim().toUpperCase(Locale.ROOT);
    }

    private void evictPlanCache() {
        payinPlanCache.evictAll();
        paymentPlanCacheService.evictAll();
    }
}
