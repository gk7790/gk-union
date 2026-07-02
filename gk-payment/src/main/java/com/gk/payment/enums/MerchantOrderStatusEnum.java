package com.gk.payment.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.enums.StringCodeEnum;
import org.apache.commons.lang3.StringUtils;

/**
 * Merchant-facing order status and reason mapping.
 */
@EnumDict("merchantOrderStatus")
public enum MerchantOrderStatusEnum implements StringCodeEnum {
    PROCESSING("PROCESSING", "Processing", "enum.merchantOrderStatus.processing", "Order is processing"),
    SUCCESS("SUCCESS", "Success", "enum.merchantOrderStatus.success", "Transaction successful"),
    FAILED("FAILED", "Failed", "enum.merchantOrderStatus.failed", "Transaction failed"),
    CLOSED("CLOSED", "Closed", "enum.merchantOrderStatus.closed", "Order closed"),
    CANCELLED("CANCELLED", "Cancelled", "enum.merchantOrderStatus.cancelled", "Order cancelled");

    private final String code;
    private final String label;
    private final String i18nKey;
    private final String statusReason;

    MerchantOrderStatusEnum(String code, String label, String i18nKey, String statusReason) {
        this.code = code;
        this.label = label;
        this.i18nKey = i18nKey;
        this.statusReason = statusReason;
    }

    public static MerchantOrderStatusEnum defaultByOrderStatus(String status) {
        if (PayinOrderStatusEnum.SUCCESS.code().equals(status)
                || PayoutOrderStatusEnum.SUCCESS.code().equals(status)) {
            return SUCCESS;
        }
        if (PayinOrderStatusEnum.MANUAL_REVIEW.code().equals(status)
                || PayoutOrderStatusEnum.MANUAL_REVIEW.code().equals(status)) {
            return PROCESSING;
        }
        if (PayinOrderStatusEnum.CLOSED.code().equals(status)) {
            return CLOSED;
        }
        if (PayoutOrderStatusEnum.CANCELLED.code().equals(status)) {
            return CANCELLED;
        }
        if (PayinOrderStatusEnum.PROCESSING.code().equals(status)
                || PayinOrderStatusEnum.CREATED.code().equals(status)
                || PayoutOrderStatusEnum.PROCESSING.code().equals(status)
                || PayoutOrderStatusEnum.CREATED.code().equals(status)
                || PayoutOrderStatusEnum.FROZEN.code().equals(status)) {
            return PROCESSING;
        }
        return FAILED;
    }

    public static String defaultCodeByOrderStatus(String status) {
        MerchantOrderStatusEnum merchantStatus = defaultByOrderStatus(status);
        return merchantStatus == null ? null : merchantStatus.code();
    }

    public static String defaultReasonByOrderStatus(String status) {
        MerchantOrderStatusEnum merchantStatus = defaultByOrderStatus(status);
        return merchantStatus == null ? null : merchantStatus.statusReason();
    }

    public static String merchantCode(String code, String status) {
        return isMerchantStatusCode(code) ? code : defaultCodeByOrderStatus(status);
    }

    public static String merchantReason(String reason, String status) {
        return StringUtils.isBlank(reason) ? defaultReasonByOrderStatus(status) : reason;
    }

    private static boolean isMerchantStatusCode(String code) {
        if (StringUtils.isBlank(code)) {
            return false;
        }
        for (MerchantOrderStatusEnum value : values()) {
            if (value.code().equals(code)) {
                return true;
            }
        }
        return false;
    }

    public String statusReason() {
        return statusReason;
    }

    public static String insufficientBalanceReason() {
        return "Insufficient balance";
    }

    public static String routeUnavailableReason() {
        return "Route unavailable";
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String i18nKey() {
        return i18nKey;
    }
}
