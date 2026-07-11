package com.gk.telegram.spi;

public interface NotificationSubjectNameResolver {
    String resolve(Long tenantId, String subjectType, Long subjectId);
}
