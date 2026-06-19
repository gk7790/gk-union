package com.gk.subject.service;

import com.gk.subject.model.SubjectDisplay;
import com.gk.subject.model.SubjectRef;

import java.util.Collection;
import java.util.Map;

public interface SubjectDisplayService {
    SubjectDisplay get(SubjectRef ref);

    Map<SubjectRef, SubjectDisplay> batchGet(Collection<SubjectRef> refs);
}
