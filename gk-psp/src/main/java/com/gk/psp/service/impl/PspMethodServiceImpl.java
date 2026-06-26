package com.gk.psp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.common.amount.AmountRangeUtils;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.dto.LabelDTO;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.common.utils.ConvertUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.psp.dao.PspFeeRuleDao;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.dao.PspRouteRuleDao;
import com.gk.psp.dto.PspMethodDTO;
import com.gk.psp.dto.PspMethodDictDTO;
import com.gk.psp.entity.PspFeeRuleEntity;
import com.gk.psp.entity.PspMethodEntity;
import com.gk.psp.entity.PspRouteRuleEntity;
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

@Service
@Slf4j
public class PspMethodServiceImpl extends CrudServiceImpl<PspMethodDao, PspMethodEntity, PspMethodDTO> implements PspMethodService {
    private static final String EMPTY_CONFIG_JSON = "{}";
    private static final long PSP_METHOD_CODE_DICT_CACHE_SECONDS = 60 * 60L;
    @Autowired
    private PspFeeRuleDao pspFeeRuleDao;
    @Autowired
    private PspRouteRuleDao pspRouteRuleDao;
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

        if (pspId <= 0) {
            return Collections.emptyList();
        }

        String cacheKey = RedisKeys.getPspMethodCodeDictKey(pspId);
        List<PspMethodDTO> cached = getCachedPspMethodCodeDict(cacheKey);
        if (cached != null) {
            return cached;
        }

        wrapper.select("id", "psp_method_code", "currency", "direction", "country_code");
        wrapper.eq("status", StatusEnum.NORMAL.code());
        wrapper.eq("psp_id", pspId);

        List<PspMethodEntity> pspMethodEntities = baseDao.selectList(wrapper);
        List<PspMethodDTO> dict = ConvertUtils.sourceToTarget(pspMethodEntities, PspMethodDTO.class);
        cachePspMethodCodeDict(cacheKey, dict);
        return dict;
    }

    @Override
    public List<LabelDTO> getMethodCodeDict(DynMap params) {
        QueryWrapper<PspMethodEntity> wrapper = new QueryWrapper<>();
        wrapper.select("method_code");
        wrapper.isNotNull("method_code");
        wrapper.ne("method_code", "");
        wrapper.eq("status", StatusEnum.NORMAL.code());
        wrapper.groupBy("method_code");
        wrapper.orderByAsc("method_code");
        return baseDao.selectList(wrapper).stream()
                .map(PspMethodEntity::getMethodCode)
                .filter(StringUtils::isNotBlank)
                .map(item -> LabelDTO.of(item, item))
                .toList();
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

        // The fee-rule form selects an available PSP Method and backfills method snapshot fields.
        wrapper.eq("status", status);
        wrapper.eq(pspId != null, "psp_id", pspId);
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
        validateAmountRange(dto);
        normalizeConfigJson(dto);
        super.save(dto);
        evictPayinPlanCache();
        evictMethodDictCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PspMethodDTO dto) {
        PspMethodEntity before = dto == null || dto.getId() == null ? null : baseDao.selectById(dto.getId());
        validateAmountRange(dto);
        normalizeConfigJson(dto);
        super.update(dto);
        PspMethodEntity after = dto == null || dto.getId() == null ? null : baseDao.selectById(dto.getId());
        syncRuleMethodSnapshot(before, after);
        evictPayinPlanCache();
        evictMethodDictCache();
    }

    private void validateAmountRange(PspMethodDTO dto) {
        if (dto == null) {
            return;
        }
        try {
            AmountRangeUtils.validateConfigRange(dto.getMinAmount(), dto.getMaxAmount());
        } catch (IllegalArgumentException ex) {
            throw new GkException(ErrorCode.BAD_REQUEST, ex.getMessage());
        }
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

    private void syncRuleMethodSnapshot(PspMethodEntity before, PspMethodEntity after) {
        if (after == null || after.getId() == null) {
            return;
        }
        if (before != null
                && Objects.equals(normalize(before.getMethodCode()), normalize(after.getMethodCode()))
                && Objects.equals(StringUtils.trimToNull(before.getPspMethodCode()), StringUtils.trimToNull(after.getPspMethodCode()))
                && Objects.equals(before.getPspId(), after.getPspId())) {
            return;
        }

        // PSP Method is the source of truth for mapped method codes used by route and fee rules.
        syncRouteRuleMethodSnapshot(after);
        syncFeeRuleMethodSnapshot(after);
    }

    private void syncRouteRuleMethodSnapshot(PspMethodEntity after) {
        PspRouteRuleEntity update = new PspRouteRuleEntity();
        update.setPspId(after.getPspId());
        update.setMethodCode(normalize(after.getMethodCode()));
        pspRouteRuleDao.update(update, new UpdateWrapper<PspRouteRuleEntity>()
                .eq("psp_method_id", after.getId()));
    }

    private void syncFeeRuleMethodSnapshot(PspMethodEntity after) {
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
        // PSP ģ鲻ֱ payment 棬ģ¼ͳһ
    }

    private void evictMethodDictCache() {
        try {
            Set<String> keys = redisUtils.keys(RedisKeys.getPspMethodCodeDictPattern());
            if (keys != null && !keys.isEmpty()) {
                redisUtils.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Evict PSP method code dict cache failed: {}", e.getMessage());
        }
    }

    private List<PspMethodDTO> getCachedPspMethodCodeDict(String cacheKey) {
        try {
            Object cached = redisUtils.get(cacheKey);
            if (cached == null) {
                return null;
            }
            return switch (cached) {
                case String text -> JSON.parseArray(text, PspMethodDTO.class);
                case List<?> list -> {
                    List<PspMethodDTO> result = new ArrayList<>(list.size());
                    for (Object item : list) {
                        PspMethodDTO dto = ConvertUtils.sourceToTarget(item, PspMethodDTO.class);
                        if (dto != null) {
                            result.add(dto);
                        }
                    }
                    yield result;
                }
                default -> null;
            };
        } catch (Exception e) {
            log.warn("Get PSP method code dict cache failed: {}", e.getMessage());
        }
        return null;
    }

    private void cachePspMethodCodeDict(String cacheKey, List<PspMethodDTO> dict) {
        try {
            redisUtils.set(cacheKey, dict, PSP_METHOD_CODE_DICT_CACHE_SECONDS);
        } catch (Exception e) {
            log.warn("Set PSP method code dict cache failed: {}", e.getMessage());
        }
    }
}
