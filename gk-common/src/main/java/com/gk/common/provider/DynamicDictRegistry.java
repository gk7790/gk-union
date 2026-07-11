package com.gk.common.provider;

import com.gk.common.dto.LabelDTO;
import com.gk.common.model.DynMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 动态字典注册中心，收集并调用所有 {@link DynamicDictProvider}。
 */
@Slf4j
@Component
public class DynamicDictRegistry {

    private final Map<String, DynamicDictProvider> providers;

    public DynamicDictRegistry(List<DynamicDictProvider> providerList) {
        this.providers = buildIndex(providerList);
    }

    public Map<String, List<LabelDTO>> list(Collection<String> keys, DynMap data) {
        DynMap params = data == null ? DynMap.empty() : data;
        Set<String> filter = normalizeKeys(keys);
        Map<String, List<LabelDTO>> result = new LinkedHashMap<>();
        for (DynamicDictProvider provider : providers.values()) {
            if (!filter.isEmpty() && !filter.contains(normalizeKey(provider.type()))) {
                continue;
            }
            result.put(provider.type(), load(provider, params));
        }
        return result;
    }

    public List<LabelDTO> get(String type, DynMap data) {
        if (StringUtils.isBlank(type)) {
            return List.of();
        }
        DynamicDictProvider provider = providers.get(normalizeKey(type));
        return provider == null ? List.of() : load(provider, data == null ? DynMap.empty() : data);
    }

    private List<LabelDTO> load(DynamicDictProvider provider, DynMap data) {
        try {
            List<LabelDTO> list = provider.list(data);
            if (list == null || list.isEmpty()) {
                return List.of();
            }
            return list.stream().filter(Objects::nonNull).toList();
        } catch (Exception e) {
            log.error("Load dynamic dict failed, type={}", provider.type(), e);
            return List.of();
        }
    }

    private Map<String, DynamicDictProvider> buildIndex(List<DynamicDictProvider> providerList) {
        if (providerList == null || providerList.isEmpty()) {
            return Map.of();
        }
        Map<String, DynamicDictProvider> index = new LinkedHashMap<>();
        providerList.stream()
                .filter(Objects::nonNull)
                .filter(provider -> StringUtils.isNotBlank(provider.type()))
                .sorted(Comparator.comparingInt(DynamicDictProvider::order).thenComparing(DynamicDictProvider::type))
                .forEach(provider -> {
                    String key = normalizeKey(provider.type());
                    DynamicDictProvider previous = index.putIfAbsent(key, provider);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate dynamic dict type: " + provider.type());
                    }
                });
        log.info("Dynamic dict providers loaded: {}", index.keySet());
        return Collections.unmodifiableMap(index);
    }

    private Set<String> normalizeKeys(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Set.of();
        }
        return keys.stream()
                .filter(StringUtils::isNotBlank)
                .map(this::normalizeKey)
                .collect(Collectors.toSet());
    }

    private String normalizeKey(String key) {
        return key.trim().toLowerCase(Locale.ROOT);
    }
}
