package com.gk.api.config;

import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class ApiComponentExcludeFilter implements TypeFilter {
    private static final List<String> EXCLUDED_PACKAGE_PREFIXES = List.of(
            "com.gk.auth.config.",
            "com.gk.quartz.",
            "com.gk.telegram.",
            "com.gk.devtools."
    );

    private static final Set<String> ALLOWED_CONTROLLERS = Set.of(
            "com.gk.openapi.controller.OpenApiV1Controller",
            "com.gk.payment.callback.PspCallbackController"
    );

    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
        String className = metadataReader.getClassMetadata().getClassName();
        if (isAllowedPublicController(className)) {
            return false;
        }
        return isExcludedPackage(className) || isController(metadataReader);
    }

    private boolean isAllowedPublicController(String className) {
        return ALLOWED_CONTROLLERS.contains(className);
    }

    private boolean isExcludedPackage(String className) {
        return EXCLUDED_PACKAGE_PREFIXES.stream().anyMatch(className::startsWith);
    }

    private boolean isController(MetadataReader metadataReader) {
        return metadataReader.getAnnotationMetadata().hasAnnotation("org.springframework.stereotype.Controller")
                || metadataReader.getAnnotationMetadata().hasMetaAnnotation("org.springframework.stereotype.Controller")
                || metadataReader.getAnnotationMetadata().hasAnnotation("org.springframework.web.bind.annotation.RestController")
                || metadataReader.getAnnotationMetadata().hasMetaAnnotation("org.springframework.web.bind.annotation.RestController");
    }
}
