package com.gk.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LabelDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long pid;
    private String value;
    private String label;
    private String i18nKey;
    private List<LabelDTO> children;

    public LabelDTO() {
    }

    public LabelDTO(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public LabelDTO(Long id, String label) {
        this.id = id;
        this.label = label;
    }

    public LabelDTO(String value, String label, String i18nKey) {
        this.value = value;
        this.label = label;
        this.i18nKey = i18nKey;
    }

}
