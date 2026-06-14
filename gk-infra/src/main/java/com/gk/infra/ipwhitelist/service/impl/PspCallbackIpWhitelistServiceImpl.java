package com.gk.infra.ipwhitelist.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.common.utils.IpPatternUtils;
import com.gk.common.validator.AssertUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.infra.ipwhitelist.dao.PspCallbackIpWhitelistDao;
import com.gk.infra.ipwhitelist.dto.PspCallbackIpWhitelistDTO;
import com.gk.infra.ipwhitelist.entity.PspCallbackIpWhitelistEntity;
import com.gk.infra.ipwhitelist.service.PspCallbackIpWhitelistService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PspCallbackIpWhitelistServiceImpl extends CrudServiceImpl<PspCallbackIpWhitelistDao, PspCallbackIpWhitelistEntity, PspCallbackIpWhitelistDTO>
        implements PspCallbackIpWhitelistService {
    private static final long CACHE_SECONDS = 300L;

    private final RedisUtils redisUtils;

    @Override
    public QueryWrapper<PspCallbackIpWhitelistEntity> getWrapper(DynMap params) {
        QueryWrapper<PspCallbackIpWhitelistEntity> wrapper = new QueryWrapper<>();
        String pspCode = params.getStr("pspCode");
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String ruleName = params.getStr("ruleName");
        String ipPattern = params.getStr("ipPattern");

        if (!ReqContextHolder.isPlatform()) {
            wrapper.eq("id", -1L);
            return wrapper;
        }
        wrapper.eq(StrUtil.isNotBlank(pspCode), "psp_code", normalizePspCode(pspCode));
        wrapper.eq(status != null, "status", status);
        wrapper.like(StrUtil.isNotBlank(ruleName), "rule_name", ruleName);
        wrapper.like(StrUtil.isNotBlank(ipPattern), "ip_pattern", ipPattern);
        return wrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(PspCallbackIpWhitelistDTO dto) {
        prepare(dto);
        super.save(dto);
        evictCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PspCallbackIpWhitelistDTO dto) {
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
    public boolean isPspCallbackAllowed(String pspCode, String clientIp) {
        if (StrUtil.isBlank(pspCode)) {
            return false;
        }
        List<String> rules = loadRules(normalizePspCode(pspCode));
        return IpPatternUtils.matchesAny(clientIp, rules);
    }

    private void prepare(PspCallbackIpWhitelistDTO dto) {
        if (!ReqContextHolder.isPlatform()) {
            throw new GkException(ErrorCode.FORBIDDEN);
        }
        AssertUtils.isBlank(dto.getPspCode(), "pspCode");
        AssertUtils.isBlank(dto.getIpPattern(), "ipPattern");
        dto.setPspCode(normalizePspCode(dto.getPspCode()));
        if (dto.getStatus() == null) {
            dto.setStatus(StatusEnum.NORMAL.code());
        }
    }

    private List<String> loadRules(String pspCode) {
        String cacheKey = RedisKeys.getPspCallbackIpWhitelistKey(pspCode);
        Object cached = redisUtils.get(cacheKey);
        if (cached instanceof String cachedText && StringUtils.isNotBlank(cachedText)) {
            return JSON.parseArray(cachedText, String.class);
        }

        QueryWrapper<PspCallbackIpWhitelistEntity> wrapper = new QueryWrapper<PspCallbackIpWhitelistEntity>()
                .eq("status", StatusEnum.NORMAL.code())
                .eq("psp_code", pspCode);

        List<String> rules = baseDao.selectList(wrapper).stream()
                .map(PspCallbackIpWhitelistEntity::getIpPattern)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        redisUtils.set(cacheKey, JSON.toJSONString(rules), CACHE_SECONDS);
        return rules;
    }

    private String normalizePspCode(String pspCode) {
        return pspCode == null ? null : pspCode.trim().toUpperCase(Locale.ROOT);
    }

    private void evictCache() {
        deleteKeys(redisUtils.keys(RedisKeys.getPspCallbackIpWhitelistPattern()));
    }

    private void deleteKeys(Collection<String> keys) {
        if (keys != null && !keys.isEmpty()) {
            redisUtils.delete(keys.stream().filter(Objects::nonNull).toList());
        }
    }
}
