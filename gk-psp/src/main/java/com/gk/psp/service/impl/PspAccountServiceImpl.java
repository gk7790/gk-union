package com.gk.psp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.dto.LabelDTO;
import com.gk.common.model.DynMap;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.common.utils.ConvertUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.dto.PspAccountDTO;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.service.PspAccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class PspAccountServiceImpl extends CrudServiceImpl<PspAccountDao, PspAccountEntity, PspAccountDTO> implements PspAccountService {
    private static final long PSP_ACCOUNT_DICT_CACHE_SECONDS = 60 * 60L;
    @Autowired
    private RedisUtils redisUtils;

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
        super.save(dto);
        evictPayinPlanCache();
        evictDictCache();
    }

    @Override
    public void update(PspAccountDTO dto) {
        super.update(dto);
        evictPayinPlanCache();
        evictDictCache();
    }

    @Override
    public void delete(Long[] ids) {
        super.delete(ids);
        evictPayinPlanCache();
        evictDictCache();
    }

    @Override
    public void delete(Long id) {
        super.delete(id);
        evictPayinPlanCache();
        evictDictCache();
    }

    private void evictPayinPlanCache() {
        // PSP ģ鲻ֱ payment 棬ģ¼ͳһ
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
}
