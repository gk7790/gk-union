package com.gk.psp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.dto.LabelDTO;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.common.utils.ConvertUtils;
import com.gk.common.utils.BizKeyUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.dao.PspProviderDao;
import com.gk.psp.dto.PspAccountDTO;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.entity.PspProviderEntity;
import com.gk.psp.service.PspAccountService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
public class PspAccountServiceImpl extends CrudServiceImpl<PspAccountDao, PspAccountEntity, PspAccountDTO> implements PspAccountService {
    private static final String EMPTY_CONFIG_JSON = "{}";
    private static final long PSP_ACCOUNT_DICT_CACHE_SECONDS = 60 * 60L;
    private static final int PSP_ACCOUNT_NO_MAX_RETRY = 10;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private PspProviderDao pspProviderDao;

    @Override
    public QueryWrapper<PspAccountEntity> getWrapper(DynMap params) {
        QueryWrapper<PspAccountEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long pspId = params.getLong("pspId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String pspAccountNo = params.getStr("pspAccountNo");
        String pspAccountName = params.getStr("pspAccountName");
        String secretType = params.getStr("secretType");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(pspId != null, "psp_id", pspId);
        wrapper.eq(status != null, "status", status);
        wrapper.eq(StrUtil.isNotBlank(pspAccountNo), "psp_account_no", pspAccountNo);
        wrapper.like(StrUtil.isNotBlank(pspAccountName), "psp_account_name", pspAccountName);
        wrapper.eq(StrUtil.isNotBlank(secretType), "secret_type", secretType);
        return wrapper;
    }

    @Override
    public List<LabelDTO> getDict(DynMap params) {
        QueryWrapper<PspAccountEntity> wrapper = new QueryWrapper<>();
        Long pspId = params.getLong("pspId", 0L);

        if (pspId <= 0) {
            return Collections.emptyList();
        }

        String cacheKey = RedisKeys.getPspAccountDictKey(pspId);
        List<LabelDTO> cached = getCachedDict(cacheKey);
        if (cached != null) {
            return cached;
        }

        wrapper.select("id", "psp_account_no", "psp_account_name");
        wrapper.eq("psp_id", pspId);
        wrapper.eq("status", StatusEnum.NORMAL.code());
        wrapper.orderByAsc("psp_account_name").orderByAsc("psp_account_no").orderByAsc("id");
        List<LabelDTO> dict = baseDao.selectList(wrapper).stream()
                .map(item -> LabelDTO.of(item.getId(), StrUtil.blankToDefault(item.getPspAccountName(), item.getPspAccountNo())))
                .toList();
        cacheDict(cacheKey, dict);
        return dict;
    }

    @Override
    public void save(PspAccountDTO dto) {
        normalizeConfigJson(dto, true);
        fillTenantFromProvider(dto, null);
        if (StrUtil.isBlank(dto.getPspAccountNo())) {
            dto.setPspAccountNo(genUniquePspAccountNo());
        }
        super.save(dto);
        evictPayinPlanCache();
        evictDictCache();
        evictCallbackAccountCache();
    }

    @Override
    public void update(PspAccountDTO dto) {
        normalizeConfigJson(dto, false);
        PspAccountEntity existing = baseDao.selectById(dto.getId());
        if (existing == null) {
            throw new GkException(ErrorCode.NOT_FOUND, "PSP account not found");
        }
        fillTenantFromProvider(dto, existing);
        if (StrUtil.isNotBlank(dto.getPspAccountNo())
                && !StrUtil.equals(dto.getPspAccountNo(), existing.getPspAccountNo())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "pspAccountNo cannot be changed");
        }
        dto.setPspAccountNo(null);
        super.update(dto);
        evictPayinPlanCache();
        evictDictCache();
        evictCallbackAccountCache();
    }

    @Override
    public void delete(Long[] ids) {
        stopAccounts(Arrays.asList(ids));
        evictPayinPlanCache();
        evictDictCache();
        evictCallbackAccountCache();
    }

    @Override
    public void delete(Long id) {
        stopAccounts(List.of(id));
        evictPayinPlanCache();
        evictDictCache();
        evictCallbackAccountCache();
    }

    private void evictPayinPlanCache() {
        // PSP ģ鲻ֱ payment 棬ģ¼ͳһ
    }

    private void stopAccounts(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<Long> accountIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (accountIds.isEmpty()) {
            return;
        }
        List<PspAccountEntity> accounts = baseDao.selectBatchIds(accountIds);
        if (accounts.size() != accountIds.size()) {
            throw new GkException(ErrorCode.NOT_FOUND, "PSP account not found");
        }
        for (Long id : accountIds) {
            PspAccountEntity entity = new PspAccountEntity();
            entity.setId(id);
            entity.setStatus(StatusEnum.STOP.code());
            baseDao.updateById(entity);
        }
    }

    private String genUniquePspAccountNo() {
        for (int i = 0; i < PSP_ACCOUNT_NO_MAX_RETRY; i++) {
            String pspAccountNo = BizKeyUtils.genPspAccountNo();
            Long count = baseDao.selectCount(new QueryWrapper<PspAccountEntity>()
                    .eq("psp_account_no", pspAccountNo));
            if (count == null || count == 0) {
                return pspAccountNo;
            }
        }
        throw new GkException(ErrorCode.INTERNAL_SERVER_ERROR, "Generate PSP account no failed");
    }

    private void fillTenantFromProvider(PspAccountDTO dto, PspAccountEntity existing) {
        if (dto == null) {
            return;
        }
        Long pspId = dto.getPspId();
        if (pspId == null && existing != null) {
            pspId = existing.getPspId();
        }
        if (pspId == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "pspId is required");
        }
        PspProviderEntity provider = pspProviderDao.selectById(pspId);
        if (provider == null) {
            throw new GkException(ErrorCode.NOT_FOUND, "PSP provider not found");
        }
        if (provider.getTenantId() == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "PSP provider tenantId is required");
        }
        dto.setTenantId(provider.getTenantId());
    }

    private void normalizeConfigJson(PspAccountDTO dto, boolean defaultWhenMissing) {
        if (dto == null) {
            return;
        }
        if (dto.getConfigJson() == null && !defaultWhenMissing) {
            return;
        }
        String configJson = StringUtils.trimToNull(dto.getConfigJson());
        if (configJson == null) {
            dto.setConfigJson(EMPTY_CONFIG_JSON);
            return;
        }
        try {
            JSON.parse(configJson);
            dto.setConfigJson(configJson);
        } catch (JSONException ex) {
            throw new GkException(ErrorCode.JSON_FORMAT_ERROR, ex, "configJson");
        }
    }

    private List<LabelDTO> getCachedDict(String cacheKey) {
        try {
            Object cached = redisUtils.get(cacheKey);
            if (cached == null) {
                return null;
            }
            return switch (cached) {
                case String text -> JSON.parseArray(text, LabelDTO.class);
                case List<?> list -> {
                    List<LabelDTO> result = new ArrayList<>(list.size());
                    for (Object item : list) {
                        LabelDTO dto = ConvertUtils.sourceToTarget(item, LabelDTO.class);
                        if (dto != null) {
                            result.add(dto);
                        }
                    }
                    yield result;
                }
                default -> null;
            };
        } catch (Exception e) {
            log.warn("Get PSP account dict cache failed: {}", e.getMessage());
        }
        return null;
    }

    private void cacheDict(String cacheKey, List<LabelDTO> dict) {
        try {
            redisUtils.set(cacheKey, dict, PSP_ACCOUNT_DICT_CACHE_SECONDS);
        } catch (Exception e) {
            log.warn("Set PSP account dict cache failed: {}", e.getMessage());
        }
    }

    private void evictDictCache() {
        try {
            Set<String> keys = redisUtils.keys(RedisKeys.getPspAccountDictPattern());
            if (keys != null && !keys.isEmpty()) {
                redisUtils.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Evict PSP account dict cache failed: {}", e.getMessage());
        }
    }

    private void evictCallbackAccountCache() {
        try {
            Set<String> keys = redisUtils.keys(RedisKeys.getPspCallbackAccountPattern());
            if (keys != null && !keys.isEmpty()) {
                redisUtils.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Evict PSP callback account cache failed: {}", e.getMessage());
        }
    }
}
