package com.gk.infra.ipwhitelist.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.common.utils.IpPatternUtils;
import com.gk.common.validator.AssertUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.infra.ipwhitelist.dao.SysApiIpWhitelistDao;
import com.gk.infra.ipwhitelist.dto.SysApiIpWhitelistDTO;
import com.gk.infra.ipwhitelist.entity.SysApiIpWhitelistEntity;
import com.gk.infra.ipwhitelist.enums.ApiIpWhitelistTypeEnum;
import com.gk.infra.ipwhitelist.service.SysApiIpWhitelistService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SysApiIpWhitelistServiceImpl extends CrudServiceImpl<SysApiIpWhitelistDao, SysApiIpWhitelistEntity, SysApiIpWhitelistDTO>
        implements SysApiIpWhitelistService {
    private static final long CACHE_SECONDS = 300L;

    private final RedisUtils redisUtils;

    @Override
    public QueryWrapper<SysApiIpWhitelistEntity> getWrapper(DynMap params) {
        QueryWrapper<SysApiIpWhitelistEntity> wrapper = new QueryWrapper<>();
        String apiType = params.getStr("apiType");
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Long merchantAppId = params.getLong("merchantAppId", null);
        String appId = params.getStr("appId");
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String ruleName = params.getStr("ruleName");
        String ipPattern = params.getStr("ipPattern");

        wrapper.eq(StrUtil.isNotBlank(apiType), "api_type", apiType);
        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(merchantAppId != null, "merchant_app_id", merchantAppId);
        wrapper.eq(StrUtil.isNotBlank(appId), "app_id", appId);
        wrapper.eq(status != null, "status", status);
        wrapper.like(StrUtil.isNotBlank(ruleName), "rule_name", ruleName);
        wrapper.like(StrUtil.isNotBlank(ipPattern), "ip_pattern", ipPattern);
        return wrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(SysApiIpWhitelistDTO dto) {
        prepare(dto);
        super.save(dto);
        evictCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysApiIpWhitelistDTO dto) {
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
    public boolean isMerchantApiAllowed(Long tenantId, Long merchantId, Long merchantAppId, String appId, String clientIp) {
        if (tenantId == null || merchantId == null) {
            return false;
        }
        List<String> rules = loadRules(ApiIpWhitelistTypeEnum.MERCHANT_OPENAPI.code(), tenantId, merchantId, merchantAppId, appId);
        return IpPatternUtils.matchesAny(clientIp, rules);
    }

    private void prepare(SysApiIpWhitelistDTO dto) {
        if (StrUtil.isBlank(dto.getApiType())) {
            dto.setApiType(ApiIpWhitelistTypeEnum.MERCHANT_OPENAPI.code());
        }
        AssertUtils.isNull(dto.getTenantId(), "tenantId");
        AssertUtils.isNull(dto.getMerchantId(), "merchantId");
        AssertUtils.isBlank(dto.getIpPattern(), "ipPattern");
        if (dto.getStatus() == null) {
            dto.setStatus(StatusEnum.NORMAL.code());
        }
    }

    private List<String> loadRules(String apiType, Long tenantId, Long merchantId, Long merchantAppId, String appId) {
        String cacheKey = RedisKeys.getApiIpWhitelistKey(apiType, tenantId, merchantId, merchantAppId, appId);
        Object cached = redisUtils.get(cacheKey);
        if (cached instanceof String cachedText && StringUtils.isNotBlank(cachedText)) {
            return JSON.parseArray(cachedText, String.class);
        }

        QueryWrapper<SysApiIpWhitelistEntity> wrapper = new QueryWrapper<SysApiIpWhitelistEntity>()
                .eq("status", StatusEnum.NORMAL.code())
                .eq("api_type", apiType)
                .eq("tenant_id", tenantId)
                .eq("merchant_id", merchantId);
        addNullableScope(wrapper, "merchant_app_id", merchantAppId);
        addNullableScope(wrapper, "app_id", appId);

        List<String> rules = baseDao.selectList(wrapper).stream()
                .map(SysApiIpWhitelistEntity::getIpPattern)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        redisUtils.set(cacheKey, JSON.toJSONString(rules), CACHE_SECONDS);
        return rules;
    }

    private void addNullableScope(QueryWrapper<SysApiIpWhitelistEntity> wrapper, String column, Object value) {
        wrapper.and(item -> {
            item.isNull(column);
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                item.or().eq(column, value);
            }
        });
    }

    private void evictCache() {
        deleteKeys(redisUtils.keys(RedisKeys.getApiIpWhitelistPattern()));
    }

    private void deleteKeys(Collection<String> keys) {
        if (keys != null && !keys.isEmpty()) {
            redisUtils.delete(keys.stream().filter(Objects::nonNull).toList());
        }
    }
}
