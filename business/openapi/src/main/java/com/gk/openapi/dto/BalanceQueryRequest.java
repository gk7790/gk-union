package com.gk.openapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@OpenApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceQueryRequest {
    private String currency;
}
