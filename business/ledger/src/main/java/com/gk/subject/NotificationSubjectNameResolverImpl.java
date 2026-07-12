package com.gk.subject;

import com.gk.subject.model.SubjectDisplay;
import com.gk.subject.model.SubjectRef;
import com.gk.subject.service.SubjectDisplayService;
import com.gk.telegram.spi.NotificationSubjectNameResolver;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationSubjectNameResolverImpl implements NotificationSubjectNameResolver {
    private final SubjectDisplayService subjectDisplayService;

    @Override
    public String resolve(Long tenantId, String subjectType, Long subjectId) {
        if (tenantId == null || subjectId == null) return null;
        SubjectDisplay display = subjectDisplayService.get(new SubjectRef(tenantId, subjectType, subjectId));
        return display == null ? null : StringUtils.defaultIfBlank(display.getSubjectName(), display.getDisplayName());
    }
}
