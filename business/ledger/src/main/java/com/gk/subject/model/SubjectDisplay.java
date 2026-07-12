package com.gk.subject.model;

import lombok.Data;

@Data
public class SubjectDisplay {
    private Long tenantId;
    private String subjectType;
    private Long subjectId;
    private String subjectNo;
    private String subjectName;
    private String subjectShortName;
    private String displayName;
    private Integer status;
    private Boolean found;
}
