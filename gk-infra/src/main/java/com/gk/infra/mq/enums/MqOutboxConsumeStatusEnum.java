package com.gk.infra.mq.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

@EnumDict("mqOutboxConsumeStatus")
public enum MqOutboxConsumeStatusEnum implements StringCodeEnum {
    @Style(StyleType.INFO)
    INIT("INIT", "待消费", "enum.mqOutboxConsumeStatus.init"),

    @Style(StyleType.WARNING)
    LOCKED("LOCKED", "消费锁定中", "enum.mqOutboxConsumeStatus.locked"),

    @Style(StyleType.SUCCESS)
    DONE("DONE", "消费完成", "enum.mqOutboxConsumeStatus.done"),

    @Style(StyleType.WARNING)
    FAILED("FAILED", "消费失败待重试", "enum.mqOutboxConsumeStatus.failed"),

    @Style(StyleType.DANGER)
    DEAD("DEAD", "消费死信", "enum.mqOutboxConsumeStatus.dead"),

    @Style(StyleType.INFO)
    SKIPPED("SKIPPED", "已跳过", "enum.mqOutboxConsumeStatus.skipped");

    private final String code;
    private final String label;
    private final String i18nKey;

    MqOutboxConsumeStatusEnum(String code, String label, String i18nKey) {
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
