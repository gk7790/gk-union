package com.gk.infra.mq.enums;

import com.gk.common.enums.StringCodeEnum;

public enum MqOutboxConsumeStatusEnum implements StringCodeEnum {
    INIT("INIT", "待消费"),
    LOCKED("LOCKED", "消费锁定中"),
    DONE("DONE", "消费完成"),
    FAILED("FAILED", "消费失败待重试"),
    DEAD("DEAD", "消费死信"),
    SKIPPED("SKIPPED", "已跳过");

    private final String code;
    private final String label;

    MqOutboxConsumeStatusEnum(String code, String label) {
        this.code = code;
        this.label = label;
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
        return "";
    }
}
