package com.gk.common.provider;

import com.gk.common.annotation.EnumDict;
import com.gk.common.dto.LabelDTO;
import com.gk.common.enums.SimpleEnum;
import com.gk.common.utils.EnumUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 扫描并组装带 {@link EnumDict} 注解的枚举字典。
 */
@Slf4j
@Component
public class EnumDictProvider {
    private static final String DEFAULT_BASE_PACKAGE = "com.gk";

    private final Map<String, EnumDictHolder> catalog;

    public EnumDictProvider() {
        this(DEFAULT_BASE_PACKAGE);
    }

    EnumDictProvider(String basePackage) {
        this.catalog = loadCatalog(basePackage);
    }

    public Map<String, List<LabelDTO>> list(Collection<String> keys) {
        Set<String> filter = normalizeKeys(keys);
        Map<String, List<LabelDTO>> result = new LinkedHashMap<>();
        for (EnumDictHolder holder : catalog.values()) {
            if (filter.isEmpty() || filter.contains(normalizeKey(holder.key()))) {
                result.put(holder.key(), holder.items());
            }
        }
        return result;
    }

    public List<LabelDTO> get(String key) {
        if (StringUtils.isBlank(key)) {
            return List.of();
        }
        EnumDictHolder holder = catalog.get(normalizeKey(key));
        return holder == null ? List.of() : holder.items();
    }

    private Map<String, EnumDictHolder> loadCatalog(String basePackage) {
        List<EnumDictHolder> holders = scanEnumDicts(basePackage).stream()
                .sorted(Comparator.comparingInt(EnumDictHolder::order).thenComparing(EnumDictHolder::key))
                .toList();
        Map<String, EnumDictHolder> result = new LinkedHashMap<>();
        for (EnumDictHolder holder : holders) {
            EnumDictHolder previous = result.putIfAbsent(normalizeKey(holder.key()), holder);
            if (previous != null) {
                throw new IllegalStateException("Duplicate enum dict key: " + holder.key());
            }
        }
        return result;
    }

    private List<EnumDictHolder> scanEnumDicts(String basePackage) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(EnumDict.class));
        return scanner.findCandidateComponents(basePackage).stream()
                .map(BeanDefinition::getBeanClassName)
                .filter(StringUtils::isNotBlank)
                .map(this::toEnumDictHolder)
                .flatMap(List::stream)
                .toList();
    }

    private List<EnumDictHolder> toEnumDictHolder(String className) {
        try {
            Class<?> enumClass = ClassUtils.forName(className, getClass().getClassLoader());
            if (!enumClass.isEnum() || !SimpleEnum.class.isAssignableFrom(enumClass)) {
                log.warn("@EnumDict only supports enum classes implementing SimpleEnum: {}", className);
                return List.of();
            }
            EnumDict annotation = enumClass.getAnnotation(EnumDict.class);
            if (!annotation.visible() || StringUtils.isBlank(annotation.value())) {
                return List.of();
            }
            return List.of(new EnumDictHolder(
                    annotation.value(),
                    annotation.order(),
                    toDictList(enumClass)
            ));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Load enum dict failed: " + className, e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<LabelDTO> toDictList(Class<?> enumClass) {
        return EnumUtils.toDictList((Class) enumClass);
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

    private record EnumDictHolder(String key, int order, List<LabelDTO> items) {
    }
}
