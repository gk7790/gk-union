package com.gk.payment.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.dto.LabelDTO;
import com.gk.common.model.DynMap;
import com.gk.common.redis.PaymentRedisKeys;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.common.utils.ConvertUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.payment.dao.PaymentMethodDao;
import com.gk.payment.dto.PaymentMethodDTO;
import com.gk.payment.entity.PaymentMethodEntity;
import com.gk.payment.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentMethodServiceImpl extends CrudServiceImpl<PaymentMethodDao, PaymentMethodEntity, PaymentMethodDTO>
        implements PaymentMethodService {
    private static final long PAYMENT_METHOD_DICT_CACHE_SECONDS = 60 * 60L;

    private final RedisUtils redisUtils;

    @Override
    public QueryWrapper<PaymentMethodEntity> getWrapper(DynMap params) {
        QueryWrapper<PaymentMethodEntity> wrapper = new QueryWrapper<>();
        List<Integer> statusList = StatusEnum.normalizeQueryStatus(params.getList("status", Integer.class, StatusEnum.defaultStatus()));
        String methodCode = params.getStr("methodCode");
        String methodName = params.getStr("methodName");
        String methodType = params.getStr("methodType");
        String direction = params.getStr("direction");
        String countryCode = params.getStr("countryCode");
        String currency = params.getStr("currency");

        wrapper.in("status", statusList);
        wrapper.eq(StrUtil.isNotBlank(methodCode), "method_code", normalize(methodCode));
        wrapper.like(StrUtil.isNotBlank(methodName), "method_name", methodName);
        wrapper.eq(StrUtil.isNotBlank(methodType), "method_type", normalize(methodType));
        wrapper.eq(StrUtil.isNotBlank(direction), "direction", normalize(direction));
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", normalize(countryCode));
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", normalize(currency));
        wrapper.orderByAsc("sort").orderByAsc("method_code");
        return wrapper;
    }

    @Override
    public List<PaymentMethodDTO> getOptions(DynMap params) {
        List<Integer> statusList = statusList(params);
        QueryWrapper<PaymentMethodEntity> wrapper = new QueryWrapper<>();
        wrapper.select("id", "method_code", "method_name", "method_type", "direction", "country_code", "currency", "status", "sort", "icon_url", "remark");
        wrapper.in("status", statusList);
        wrapper.orderByAsc("sort").orderByAsc("method_code").orderByAsc("country_code").orderByAsc("currency").orderByAsc("direction");
        return ConvertUtils.sourceToTarget(baseDao.selectList(wrapper), PaymentMethodDTO.class);
    }

    @Override
    public List<PaymentMethodDTO> getDict(DynMap params) {
        String countryCode = normalize(params.getStr("countryCode"));
        String currency = normalize(params.getStr("currency"));
        String direction = normalize(params.getStr("direction"));
        String methodType = normalize(params.getStr("methodType"));
        List<Integer> statusList = statusList(params);
        String statusKey = statusKey(statusList);
        String cacheKey = PaymentRedisKeys.getPaymentMethodDictKey(countryCode, currency, direction, methodType, statusKey);
        List<PaymentMethodDTO> cached = getCachedDict(cacheKey);
        if (cached != null) {
            return cached;
        }

        QueryWrapper<PaymentMethodEntity> wrapper = new QueryWrapper<>();
        wrapper.select("id", "method_code", "method_name", "method_type", "direction", "country_code", "currency", "status", "sort", "icon_url", "remark");
        wrapper.in("status", statusList);
        wrapper.eq(StrUtil.isNotBlank(methodType), "method_type", methodType);
        wrapper.and(StrUtil.isNotBlank(direction), item -> item.eq("direction", direction).or().eq("direction", "BOTH").or().isNull("direction").or().eq("direction", ""));
        wrapper.and(StrUtil.isNotBlank(countryCode), item -> item.eq("country_code", countryCode).or().isNull("country_code").or().eq("country_code", ""));
        wrapper.and(StrUtil.isNotBlank(currency), item -> item.eq("currency", currency).or().isNull("currency").or().eq("currency", ""));
        wrapper.orderByAsc("sort").orderByAsc("method_code");

        List<PaymentMethodDTO> list = ConvertUtils.sourceToTarget(baseDao.selectList(wrapper), PaymentMethodDTO.class);
        List<PaymentMethodDTO> dict = mergeByMethodCode(list, countryCode, currency, direction);
        cacheDict(cacheKey, dict);
        return dict;
    }

    @Override
    public List<LabelDTO> getLabelDict(DynMap params) {
        return getDict(params).stream()
                .map(item -> LabelDTO.of(item.getMethodCode(), StringUtils.defaultIfBlank(item.getMethodName(), item.getMethodCode())))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(PaymentMethodDTO dto) {
        normalizeDto(dto);
        validateUniqueScope(dto);
        super.save(dto);
        evictDictCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PaymentMethodDTO dto) {
        normalizeDto(dto);
        validateUniqueScope(dto);
        super.update(dto);
        evictDictCache();
    }

    @Override
    public void delete(Long[] ids) {
        baseDao.update(null, new UpdateWrapper<PaymentMethodEntity>()
                .set("status", StatusEnum.STOP.code())
                .in("id", Arrays.asList(ids)));
        evictDictCache();
    }

    @Override
    public void delete(Long id) {
        baseDao.update(null, new UpdateWrapper<PaymentMethodEntity>()
                .set("status", StatusEnum.STOP.code())
                .eq("id", id));
        evictDictCache();
    }

    private void normalizeDto(PaymentMethodDTO dto) {
        if (dto == null) {
            return;
        }
        dto.setMethodCode(normalize(dto.getMethodCode()));
        dto.setMethodType(StringUtils.trimToNull(normalize(dto.getMethodType())));
        dto.setDirection(StringUtils.trimToNull(normalize(dto.getDirection())));
        dto.setCountryCode(StringUtils.trimToNull(normalize(dto.getCountryCode())));
        dto.setCurrency(StringUtils.trimToNull(normalize(dto.getCurrency())));
        dto.setMethodName(StringUtils.trimToNull(dto.getMethodName()));
        dto.setIconUrl(StringUtils.trimToNull(dto.getIconUrl()));
        dto.setRemark(StringUtils.trimToNull(dto.getRemark()));
    }

    private void validateUniqueScope(PaymentMethodDTO dto) {
        if (dto == null) {
            return;
        }
        QueryWrapper<PaymentMethodEntity> wrapper = new QueryWrapper<PaymentMethodEntity>()
                .eq("method_code", dto.getMethodCode());
        eqOrNull(wrapper, "direction", dto.getDirection());
        eqOrNull(wrapper, "country_code", dto.getCountryCode());
        eqOrNull(wrapper, "currency", dto.getCurrency());
        wrapper.ne(dto.getId() != null, "id", dto.getId());
        wrapper.last("limit 1");
        if (baseDao.selectOne(wrapper) != null) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "payment method scope already exists");
        }
    }

    private void eqOrNull(QueryWrapper<PaymentMethodEntity> wrapper, String column, String value) {
        if (StringUtils.isBlank(value)) {
            wrapper.isNull(column);
        } else {
            wrapper.eq(column, value);
        }
    }

    private String normalize(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
    }

    private List<Integer> statusList(DynMap params) {
        List<Integer> statusList = params.getList("status", Integer.class, List.of(StatusEnum.NORMAL.code()));
        List<Integer> normalized = statusList.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        return normalized.isEmpty() ? List.of(StatusEnum.NORMAL.code()) : normalized;
    }

    private String statusKey(List<Integer> statusList) {
        if (statusList == null || statusList.isEmpty()) {
            return "none";
        }
        return StringUtils.join(statusList, ",");
    }

    private List<PaymentMethodDTO> mergeByMethodCode(List<PaymentMethodDTO> list, String countryCode, String currency, String direction) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        Map<String, PaymentMethodDTO> selected = new LinkedHashMap<>();
        for (PaymentMethodDTO item : list) {
            String methodCode = normalize(item.getMethodCode());
            if (StringUtils.isBlank(methodCode)) {
                continue;
            }
            PaymentMethodDTO current = selected.get(methodCode);
            if (current == null || compareScope(item, current, countryCode, currency, direction) < 0) {
                selected.put(methodCode, item);
            }
        }
        return selected.values().stream()
                .sorted(Comparator
                        .comparing((PaymentMethodDTO item) -> item.getSort() == null ? 100 : item.getSort())
                        .thenComparing(item -> StringUtils.defaultString(item.getMethodCode())))
                .toList();
    }

    private int compareScope(PaymentMethodDTO left, PaymentMethodDTO right, String countryCode, String currency, String direction) {
        int scoreCompare = Integer.compare(
                scopeScore(left, countryCode, currency, direction),
                scopeScore(right, countryCode, currency, direction)
        );
        if (scoreCompare != 0) {
            return scoreCompare;
        }
        int sortCompare = Integer.compare(
                left.getSort() == null ? 100 : left.getSort(),
                right.getSort() == null ? 100 : right.getSort()
        );
        if (sortCompare != 0) {
            return sortCompare;
        }
        return Long.compare(left.getId() == null ? Long.MAX_VALUE : left.getId(), right.getId() == null ? Long.MAX_VALUE : right.getId());
    }

    private int scopeScore(PaymentMethodDTO item, String countryCode, String currency, String direction) {
        return directionScore(item.getDirection(), direction)
                + fieldScore(item.getCountryCode(), countryCode)
                + fieldScore(item.getCurrency(), currency);
    }

    private int directionScore(String value, String requestValue) {
        if (StringUtils.isBlank(requestValue)) {
            return 0;
        }
        String normalizedValue = normalize(value);
        if (Objects.equals(normalizedValue, requestValue)) {
            return 0;
        }
        if ("BOTH".equals(normalizedValue)) {
            return 10;
        }
        return StringUtils.isBlank(normalizedValue) ? 20 : 100;
    }

    private int fieldScore(String value, String requestValue) {
        if (StringUtils.isBlank(requestValue)) {
            return 0;
        }
        String normalizedValue = normalize(value);
        if (Objects.equals(normalizedValue, requestValue)) {
            return 0;
        }
        return StringUtils.isBlank(normalizedValue) ? 10 : 100;
    }

    private List<PaymentMethodDTO> getCachedDict(String cacheKey) {
        try {
            Object cached = redisUtils.get(cacheKey);
            if (cached == null) {
                return null;
            }
            return switch (cached) {
                case String text -> JSON.parseArray(text, PaymentMethodDTO.class);
                case List<?> list -> {
                    List<PaymentMethodDTO> result = new ArrayList<>(list.size());
                    for (Object item : list) {
                        PaymentMethodDTO dto = ConvertUtils.sourceToTarget(item, PaymentMethodDTO.class);
                        if (dto != null) {
                            result.add(dto);
                        }
                    }
                    yield result;
                }
                default -> null;
            };
        } catch (Exception e) {
            log.warn("Get payment method dict cache failed: {}", e.getMessage());
        }
        return null;
    }

    private void cacheDict(String cacheKey, List<PaymentMethodDTO> dict) {
        try {
            redisUtils.set(cacheKey, dict, PAYMENT_METHOD_DICT_CACHE_SECONDS);
        } catch (Exception e) {
            log.warn("Set payment method dict cache failed: {}", e.getMessage());
        }
    }

    private void evictDictCache() {
        try {
            Set<String> keys = redisUtils.keys(PaymentRedisKeys.getPaymentMethodDictPattern());
            if (keys != null && !keys.isEmpty()) {
                redisUtils.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Evict payment method dict cache failed: {}", e.getMessage());
        }
    }
}
