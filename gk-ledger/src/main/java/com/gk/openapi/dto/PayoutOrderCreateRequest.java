package com.gk.openapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@OpenApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayoutOrderCreateRequest {
    @NotBlank(message = "merchant_order_id is required")
    private String merchantOrderId;
    @NotNull(message = "amount is required")
    private BigDecimal amount;
    private String currency;
    private String countryCode;
    @NotBlank(message = "method_code is required")
    private String methodCode;
    private String purpose;
    @NotBlank(message = "notify_url is required")
    private String notifyUrl;
    @Valid
    @NotNull(message = "payee is required")
    private Payee payee;
    private Map<String, Object> extra;

    @Data
    @OpenApiModel
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Payee {
        private String name;
        @NotBlank(message = "payee.account_no is required")
        private String accountNo;
        private String bankCode;
        private String walletType;
        private String phone;
        private String email;
    }
}
