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
public class PayoutOrderCreateRequest {
    @JsonAlias("merchant_order_id")
    @NotBlank(message = "merchant_order_id is required")
    private String merchantOrderNo;
    @NotNull(message = "amount is required")
    private BigDecimal amount;
    private String currency;
    @JsonAlias("country_code")
    private String countryCode;
    @JsonAlias({"pay_channel", "payout_mode"})
    @NotBlank(message = "payout_mode is required")
    private String methodCode;
    private String purpose;
    @JsonAlias("notify_url")
    @NotBlank(message = "notify_url is required")
    private String notifyUrl;
    @JsonAlias("customer_name")
    private String customerName;
    @JsonAlias("customer_account_type")
    private String customerAccountType;
    @JsonAlias("customer_account_no")
    @NotBlank(message = "customer_account_no is required")
    private String customerAccountNo;
    @JsonAlias("customer_account_card_type")
    private String customerAccountCardType;
    @JsonAlias("customer_account_bank_cci")
    private String customerAccountBankCci;
    @JsonAlias("beneficiary")
    private Payee payee;
    private Map<String, Object> extra;

    @Data
    public static class Payee {
        private String name;
        @JsonAlias("account_no")
        private String accountNo;
        @JsonAlias("bank_code")
        private String bankCode;
        @JsonAlias("wallet_type")
        private String walletType;
        private String phone;
        private String email;
    }
}
