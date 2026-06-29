package com.gk.openapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@OpenApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayinOrderCreateRequest {
    @NotBlank(message = "merchant_order_id is required")
    private String merchantOrderId;
    @NotNull(message = "amount is required")
    private BigDecimal amount;
    private String currency;
    private String countryCode;
    @NotBlank(message = "method_code is required")
    private String methodCode;
    private String subject;
    private String description;
    @NotBlank(message = "notify_url is required")
    private String notifyUrl;
    @NotBlank(message = "return_url is required")
    private String returnUrl;
    private Map<String, Object> payer;
    private Map<String, Object> extra;
}
