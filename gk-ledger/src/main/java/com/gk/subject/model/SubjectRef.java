package com.gk.subject.model;

import java.util.Locale;

public record SubjectRef(Long tenantId, String subjectType, Long subjectId) {
    public SubjectRef {
        subjectType = subjectType == null ? null : subjectType.toUpperCase(Locale.ROOT);
    }
}
