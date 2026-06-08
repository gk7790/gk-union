package com.gk.openapi.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayOrderCreateRequest {
    @JsonAlias("merchant_order_id")
    @NotBlank(message = "merchant_order_id is required")
    private String merchantOrderNo;
    @NotNull(message = "amount is required")
    private BigDecimal amount;
    private String currency;
    @JsonAlias("country_code")
    private String countryCode;
    @JsonAlias("pay_channel")
    @NotBlank(message = "pay_channel is required")
    private String methodCode;
    private String subject;
    private String description;
    @JsonAlias("notify_url")
    @NotBlank(message = "notify_url is required")
    private String notifyUrl;
    @JsonAlias("page_return_url")
    @NotBlank(message = "page_return_url is required")
    private String returnUrl;
    @JsonAlias("customer_id")
    private String customerId;
    @JsonAlias("customer_type")
    private String customerType;
    @JsonAlias("customer_name")
    private String customerName;
    @JsonAlias("customer_phone")
    private String customerPhone;
    @JsonAlias("customer_email")
    private String customerEmail;
    @JsonAlias("customer_id_card")
    private String customerIdCard;
    @JsonAlias("bank_code")
    private String bankCode;
    @JsonAlias("select_channel")
    private String selectChannel;
    private Map<String, Object> payer;
    private Map<String, Object> extra;
}
