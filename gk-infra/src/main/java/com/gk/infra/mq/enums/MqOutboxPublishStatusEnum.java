package com.gk.infra.mq.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

@EnumDict("mqOutboxPublishStatus")
public enum MqOutboxPublishStatusEnum implements StringCodeEnum {
    @Style(StyleType.INFO)
    INIT("INIT", "待发布", "enum.mqOutboxPublishStatus.init"),

    @Style(StyleType.WARNING)
    LOCKED("LOCKED", "发布锁定中", "enum.mqOutboxPublishStatus.locked"),

    @Style(StyleType.SUCCESS)
    SENT("SENT", "发布完成", "enum.mqOutboxPublishStatus.sent"),

    @Style(StyleType.WARNING)
    FAILED("FAILED", "发布失败待重试", "enum.mqOutboxPublishStatus.failed"),

    @Style(StyleType.DANGER)
    DEAD("DEAD", "发布死信", "enum.mqOutboxPublishStatus.dead");

    private final String code;
    private final String label;
    private final String i18nKey;

    MqOutboxPublishStatusEnum(String code, String label, String i18nKey) {
        this.code = code;
        this.label = label;
        this.i18nKey = i18nKey;
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
