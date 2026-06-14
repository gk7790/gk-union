package com.gk.infra.ipwhitelist.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.model.DynMap;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.common.utils.IpPatternUtils;
import com.gk.common.validator.AssertUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.infra.ipwhitelist.dao.MerchantApiIpWhitelistDao;
import com.gk.infra.ipwhitelist.dto.MerchantApiIpWhitelistDTO;
import com.gk.infra.ipwhitelist.entity.MerchantApiIpWhitelistEntity;
import com.gk.infra.ipwhitelist.service.MerchantApiIpWhitelistService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MerchantApiIpWhitelistServiceImpl extends CrudServiceImpl<MerchantApiIpWhitelistDao, MerchantApiIpWhitelistEntity, MerchantApiIpWhitelistDTO>
        implements MerchantApiIpWhitelistService {
    private static final long CACHE_SECONDS = 300L;

    private final RedisUtils redisUtils;

    @Override
    public QueryWrapper<MerchantApiIpWhitelistEntity> getWrapper(DynMap params) {
        QueryWrapper<MerchantApiIpWhitelistEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String ruleName = params.getStr("ruleName");
        String ipPattern = params.getStr("ipPattern");

        applyReadableScope(wrapper, tenantId, merchantId);
        wrapper.eq(status != null, "status", status);
        wrapper.like(StrUtil.isNotBlank(ruleName), "rule_name", ruleName);
        wrapper.like(StrUtil.isNotBlank(ipPattern), "ip_pattern", ipPattern);
        return wrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(MerchantApiIpWhitelistDTO dto) {
        prepare(dto);
        super.save(dto);
        evictCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(MerchantApiIpWhitelistDTO dto) {
        AssertUtils.isNull(dto.getId(), "id");
        prepare(dto);
        super.update(dto);
        evictCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long[] ids) {
        super.delete(ids);
        evictCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        super.delete(id);
        evictCache();
    }

    @Override
    public boolean isMerchantApiAllowed(Long tenantId, Long merchantId, String clientIp) {
        if (tenantId == null || merchantId == null) {
            return false;
        }
        List<String> rules = loadRules(tenantId, merchantId);
        return IpPatternUtils.matchesAny(clientIp, rules);
    }

    private void prepare(MerchantApiIpWhitelistDTO dto) {
        applySaveScope(dto);
        AssertUtils.isNull(dto.getTenantId(), "tenantId");
        AssertUtils.isNull(dto.getMerchantId(), "merchantId");
        AssertUtils.isBlank(dto.getIpPattern(), "ipPattern");
        if (dto.getStatus() == null) {
            dto.setStatus(StatusEnum.NORMAL.code());
        }
    }

    private List<String> loadRules(Long tenantId, Long merchantId) {
        String cacheKey = RedisKeys.getMerchantApiIpWhitelistKey(tenantId, merchantId);
        Object cached = redisUtils.get(cacheKey);
        if (cached instanceof String cachedText && StringUtils.isNotBlank(cachedText)) {
            return JSON.parseArray(cachedText, String.class);
        }

        QueryWrapper<MerchantApiIpWhitelistEntity> wrapper = new QueryWrapper<MerchantApiIpWhitelistEntity>()
                .eq("status", StatusEnum.NORMAL.code())
                .eq("tenant_id", tenantId)
                .eq("merchant_id", merchantId);

        List<String> rules = baseDao.selectList(wrapper).stream()
                .map(MerchantApiIpWhitelistEntity::getIpPattern)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        redisUtils.set(cacheKey, JSON.toJSONString(rules), CACHE_SECONDS);
        return rules;
    }

    private void applySaveScope(MerchantApiIpWhitelistDTO dto) {
        if (ReqContextHolder.isPlatform()) {
            return;
        }
        Long tenantId = ReqContextHolder.getTenantId();
        AssertUtils.isNull(tenantId, "tenantId");
        dto.setTenantId(tenantId);
        if (SubjectTypeEnum.MERCHANT.matches(ReqContextHolder.getSubjectType())) {
            Long merchantId = ReqContextHolder.getMerchantId();
            AssertUtils.isNull(merchantId, "merchantId");
            dto.setMerchantId(merchantId);
        }
    }

    private void applyReadableScope(QueryWrapper<MerchantApiIpWhitelistEntity> wrapper, Long tenantId, Long merchantId) {
        if (ReqContextHolder.isPlatform()) {
            wrapper.eq(tenantId != null, "tenant_id", tenantId);
            wrapper.eq(merchantId != null, "merchant_id", merchantId);
            return;
        }
        Long currentTenantId = ReqContextHolder.getTenantId();
        AssertUtils.isNull(currentTenantId, "tenantId");
        wrapper.eq("tenant_id", currentTenantId);
        if (SubjectTypeEnum.MERCHANT.matches(ReqContextHolder.getSubjectType())) {
            Long currentMerchantId = ReqContextHolder.getMerchantId();
            AssertUtils.isNull(currentMerchantId, "merchantId");
            wrapper.eq("merchant_id", currentMerchantId);
            return;
        }
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
    }

    private void evictCache() {
        deleteKeys(redisUtils.keys(RedisKeys.getMerchantApiIpWhitelistPattern()));
    }

    private void deleteKeys(Collection<String> keys) {
        if (keys != null && !keys.isEmpty()) {
            redisUtils.delete(keys.stream().filter(Objects::nonNull).toList());
        }
    }
}
