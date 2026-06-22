package com.gk.psp.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.dto.LabelDTO;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.common.utils.ConvertUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.common.model.DynMap;
import com.gk.payment.plan.PaymentPlanCacheService;
import com.gk.payment.plan.PayinPlanCache;
import com.gk.psp.dao.PspFeeRuleDao;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.dto.PspMethodDictDTO;
import com.gk.psp.dto.PspMethodDTO;
import com.gk.psp.entity.PspFeeRuleEntity;
import com.gk.psp.entity.PspMethodEntity;
import com.gk.psp.service.PspMethodService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PspMethodServiceImpl extends CrudServiceImpl<PspMethodDao, PspMethodEntity, PspMethodDTO> implements PspMethodService {
    private static final String EMPTY_CONFIG_JSON = "{}";
    private static final long PSP_METHOD_DICT_CACHE_SECONDS = 60 * 60L;

    @Autowired
    private PayinPlanCache payinPlanCache;
    @Autowired
    private PaymentPlanCacheService paymentPlanCacheService;
    @Autowired
    private PspFeeRuleDao pspFeeRuleDao;
    @Autowired
    private RedisUtils redisUtils;

    @Override
    public QueryWrapper<PspMethodEntity> getWrapper(DynMap params) {
        QueryWrapper<PspMethodEntity> wrapper = new QueryWrapper<>();
        Long pspId = params.getLong("pspId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String pspCode = params.getStr("pspCode");
        String methodCode = params.getStr("methodCode");
        String pspMethodCode = params.getStr("pspMethodCode");
        String countryCode = params.getStr("countryCode");
        String currency = params.getStr("currency");
        String direction = params.getStr("direction");
        if (StrUtil.isBlank(direction)) {
            direction = params.getStr("orderType");
        }

        wrapper.eq(pspId != null, "psp_id", pspId);
        wrapper.eq(status != null, "status", status);
        wrapper.eq(StrUtil.isNotBlank(pspCode), "psp_code", pspCode);
        wrapper.eq(StrUtil.isNotBlank(methodCode), "method_code", methodCode);
        wrapper.eq(StrUtil.isNotBlank(pspMethodCode), "psp_method_code", pspMethodCode);
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", countryCode);
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", currency);
        wrapper.eq(StrUtil.isNotBlank(direction), "direction", direction);
        return wrapper;
    }

    @Override
    public List<PspMethodDTO> getPspMethodCodeDict(DynMap params) {
        QueryWrapper<PspMethodEntity> wrapper = new QueryWrapper<>();
        Long pspId = params.getLong("pspId", 0L);

        if  (pspId <= 0) {
            return Collections.emptyList();
        }

        wrapper.select("id", "psp_method_code", "currency", "direction", "country_code");
        wrapper.eq("status", StatusEnum.NORMAL.code());
        wrapper.eq("psp_id", pspId);

        List<PspMethodEntity> pspMethodEntities = baseDao.selectList(wrapper);
        return ConvertUtils.sourceToTarget(pspMethodEntities, PspMethodDTO.class);
    }

    @Override
    public List<LabelDTO> getMethodCodeDict(DynMap params) {
        QueryWrapper<PspMethodEntity> wrapper = new QueryWrapper<>();
        String countryCode = params.getStr("countryCode");
        String currency = params.getStr("currency");
        String direction = params.getStr("direction");
        String cacheKey = RedisKeys.getPspMethodDictKey(normalize(countryCode), normalize(currency), normalize(direction));
        List<LabelDTO> cached = getCachedMethodDict(cacheKey);
        if (cached != null) {
            return cached;
        }

        wrapper.select("method_code", "MIN(method_name) AS method_name");
        wrapper.eq("status", StatusEnum.NORMAL.code());
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", normalize(countryCode));
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", normalize(currency));
        wrapper.eq(StrUtil.isNotBlank(direction), "direction", normalize(direction));
        wrapper.groupBy("method_code");
        wrapper.orderByAsc("method_code");

        List<LabelDTO> dict = baseDao.selectList(wrapper).stream()
                .map(item -> new LabelDTO(item.getMethodCode(), StringUtils.defaultIfBlank(item.getMethodName(), item.getMethodCode())))
                .collect(Collectors.toCollection(ArrayList::new));
        cacheMethodDict(cacheKey, dict);
        return dict;
    }

    @Override
    public List<PspMethodDictDTO> getFeeRuleMethodDict(DynMap params) {
        QueryWrapper<PspMethodEntity> wrapper = new QueryWrapper<>();
        Long pspId = params.getLong("pspId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : StatusEnum.NORMAL.code();
        String pspCode = params.getStr("pspCode");
        String methodCode = params.getStr("methodCode");
        String pspMethodCode = params.getStr("pspMethodCode");
        String countryCode = params.getStr("countryCode");
        String currency = params.getStr("currency");
        String direction = params.getStr("direction");

        // 成本规则表单只需要可用的 PSP Method，选择后回填 psp_method_id/method_code/psp_method_code。
        wrapper.eq(pspId != null, "psp_id", pspId);
        wrapper.eq(status != null, "status", status);
        wrapper.eq(StrUtil.isNotBlank(pspCode), "psp_code", normalize(pspCode));
        wrapper.eq(StrUtil.isNotBlank(methodCode), "method_code", normalize(methodCode));
        wrapper.eq(StrUtil.isNotBlank(pspMethodCode), "psp_method_code", pspMethodCode);
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", normalize(countryCode));
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", normalize(currency));
        wrapper.eq(StrUtil.isNotBlank(direction), "direction", normalize(direction));
        wrapper.orderByAsc("psp_code", "direction", "country_code", "currency", "method_code", "psp_method_code");

        return baseDao.selectList(wrapper).stream()
                .map(this::toFeeRuleMethodDict)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(PspMethodDTO dto) {
        normalizeConfigJson(dto);
        super.save(dto);
        evictPayinPlanCache();
        evictMethodDictCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PspMethodDTO dto) {
        PspMethodEntity before = dto == null || dto.getId() == null ? null : baseDao.selectById(dto.getId());
        normalizeConfigJson(dto);
        super.update(dto);
        PspMethodEntity after = dto == null || dto.getId() == null ? null : baseDao.selectById(dto.getId());
        syncFeeRuleMethodSnapshot(before, after);
        evictPayinPlanCache();
        evictMethodDictCache();
    }

    @Override
    public void delete(Long[] ids) {
        super.delete(ids);
        evictPayinPlanCache();
        evictMethodDictCache();
    }

    @Override
    public void delete(Long id) {
        super.delete(id);
        evictPayinPlanCache();
        evictMethodDictCache();
    }

    private void normalizeConfigJson(PspMethodDTO dto) {
        if (dto == null) {
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

    private String normalize(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
    }

    private void syncFeeRuleMethodSnapshot(PspMethodEntity before, PspMethodEntity after) {
        if (after == null || after.getId() == null) {
            return;
        }
        if (before != null
                && Objects.equals(normalize(before.getMethodCode()), normalize(after.getMethodCode()))
                && Objects.equals(StringUtils.trimToNull(before.getPspMethodCode()), StringUtils.trimToNull(after.getPspMethodCode()))
                && Objects.equals(before.getPspId(), after.getPspId())) {
            return;
        }

        PspFeeRuleEntity update = new PspFeeRuleEntity();
        update.setPspId(after.getPspId());
        update.setMethodCode(normalize(after.getMethodCode()));
        update.setPspMethodCode(StringUtils.trimToNull(after.getPspMethodCode()));
        pspFeeRuleDao.update(update, new UpdateWrapper<PspFeeRuleEntity>()
                .eq("psp_method_id", after.getId()));
    }

    private PspMethodDictDTO toFeeRuleMethodDict(PspMethodEntity entity) {
        PspMethodDictDTO item = new PspMethodDictDTO();
        item.setValue(entity.getId());
        item.setPspMethodId(entity.getId());
        item.setPspId(entity.getPspId());
        item.setPspCode(entity.getPspCode());
        item.setMethodCode(entity.getMethodCode());
        item.setPspMethodCode(entity.getPspMethodCode());
        item.setMethodName(entity.getMethodName());
        item.setCountryCode(entity.getCountryCode());
        item.setCurrency(entity.getCurrency());
        item.setDirection(entity.getDirection());
        item.setMinAmount(entity.getMinAmount());
        item.setMaxAmount(entity.getMaxAmount());
        item.setLabel(methodLabel(entity));
        return item;
    }

    private String methodLabel(PspMethodEntity entity) {
        String label = String.join(" / ",
                StringUtils.defaultString(entity.getMethodCode()),
                StringUtils.defaultString(entity.getPspMethodCode()));
        if (StringUtils.isNotBlank(entity.getMethodName())) {
            label = label + " - " + StringUtils.trim(entity.getMethodName());
        }
        return label;
    }

    private void evictPayinPlanCache() {
        // PSP Method 配置会影响路由和上游提交参数，变更后必须清空 PayinPlan 缓存。
        if (payinPlanCache != null) {
            payinPlanCache.evictAll();
        }
        if (paymentPlanCacheService != null) {
            paymentPlanCacheService.evictAll();
        }
    }

    private List<LabelDTO> getCachedMethodDict(String cacheKey) {
        try {
            Object cached = redisUtils.get(cacheKey);
            if (cached == null) {
                return null;
            }
            if (cached instanceof String text) {
                return JSON.parseArray(text, LabelDTO.class);
            }
            if (cached instanceof List<?> list) {
                List<LabelDTO> result = new ArrayList<>(list.size());
                for (Object item : list) {
                    LabelDTO dto = ConvertUtils.sourceToTarget(item, LabelDTO.class);
                    if (dto != null) {
                        result.add(dto);
                    }
                }
                return result;
            }
        } catch (Exception e) {
            log.warn("Get PSP method dict cache failed: {}", e.getMessage());
        }
        return null;
    }

    private void cacheMethodDict(String cacheKey, List<LabelDTO> dict) {
        try {
            redisUtils.set(cacheKey, dict, PSP_METHOD_DICT_CACHE_SECONDS);
        } catch (Exception e) {
            log.warn("Set PSP method dict cache failed: {}", e.getMessage());
        }
    }

    private void evictMethodDictCache() {
        try {
            Set<String> keys = redisUtils.keys(RedisKeys.getPspMethodDictPattern());
            if (keys != null && !keys.isEmpty()) {
                redisUtils.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Evict PSP method dict cache failed: {}", e.getMessage());
        }
    }
}
