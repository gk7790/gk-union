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
import com.gk.infra.ipwhitelist.dao.SysLoginIpWhitelistDao;
import com.gk.infra.ipwhitelist.dto.SysLoginIpWhitelistDTO;
import com.gk.infra.ipwhitelist.entity.SysLoginIpWhitelistEntity;
import com.gk.infra.ipwhitelist.service.SysLoginIpWhitelistService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SysLoginIpWhitelistServiceImpl extends CrudServiceImpl<SysLoginIpWhitelistDao, SysLoginIpWhitelistEntity, SysLoginIpWhitelistDTO>
        implements SysLoginIpWhitelistService {
    private static final long CACHE_SECONDS = 300L;

    private final RedisUtils redisUtils;

    @Override
    public QueryWrapper<SysLoginIpWhitelistEntity> getWrapper(DynMap params) {
        QueryWrapper<SysLoginIpWhitelistEntity> wrapper = new QueryWrapper<>();
        String subjectType = params.getStr("subjectType");
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Long subjectId = params.getLong("subjectId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String ruleName = params.getStr("ruleName");
        String ipPattern = params.getStr("ipPattern");

        wrapper.eq(StrUtil.isNotBlank(subjectType), "subject_type", subjectType);
        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(subjectId != null, "subject_id", subjectId);
        wrapper.eq(status != null, "status", status);
        wrapper.like(StrUtil.isNotBlank(ruleName), "rule_name", ruleName);
        wrapper.like(StrUtil.isNotBlank(ipPattern), "ip_pattern", ipPattern);
        return wrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(SysLoginIpWhitelistDTO dto) {
        prepare(dto);
        super.save(dto);
        evictCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysLoginIpWhitelistDTO dto) {
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
    public boolean isLoginAllowed(String subjectType, Long tenantId, Long merchantId, Long subjectId, String clientIp) {
        if (StringUtils.isBlank(subjectType)) {
            return true;
        }
        List<String> rules = loadRules(subjectType, tenantId, merchantId, subjectId);
        if (rules.isEmpty()) {
            return true;
        }
        return IpPatternUtils.matchesAny(clientIp, rules);
    }

    private void prepare(SysLoginIpWhitelistDTO dto) {
        AssertUtils.isBlank(dto.getSubjectType(), "subjectType");
        AssertUtils.isBlank(dto.getIpPattern(), "ipPattern");
        if (dto.getStatus() == null) {
            dto.setStatus(StatusEnum.NORMAL.code());
        }
    }

    private List<String> loadRules(String subjectType, Long tenantId, Long merchantId, Long subjectId) {
        String cacheKey = RedisKeys.getLoginIpWhitelistKey(subjectType, tenantId, merchantId, subjectId);
        Object cached = redisUtils.get(cacheKey);
        if (cached instanceof String cachedText && StringUtils.isNotBlank(cachedText)) {
            return JSON.parseArray(cachedText, String.class);
        }

        QueryWrapper<SysLoginIpWhitelistEntity> wrapper = new QueryWrapper<SysLoginIpWhitelistEntity>()
                .eq("status", StatusEnum.NORMAL.code())
                .eq("subject_type", subjectType);
        addNullableScope(wrapper, "tenant_id", tenantId);
        addNullableScope(wrapper, "merchant_id", merchantId);
        addNullableScope(wrapper, "subject_id", subjectId);

        List<String> rules = baseDao.selectList(wrapper).stream()
                .map(SysLoginIpWhitelistEntity::getIpPattern)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        redisUtils.set(cacheKey, JSON.toJSONString(rules), CACHE_SECONDS);
        return rules;
    }

    private void addNullableScope(QueryWrapper<SysLoginIpWhitelistEntity> wrapper, String column, Object value) {
        wrapper.and(item -> {
            item.isNull(column);
            if (value != null) {
                item.or().eq(column, value);
            }
        });
    }

    private void evictCache() {
        deleteKeys(redisUtils.keys(RedisKeys.getLoginIpWhitelistPattern()));
    }

    private void deleteKeys(Collection<String> keys) {
        if (keys != null && !keys.isEmpty()) {
            redisUtils.delete(keys.stream().filter(Objects::nonNull).toList());
        }
    }
}
