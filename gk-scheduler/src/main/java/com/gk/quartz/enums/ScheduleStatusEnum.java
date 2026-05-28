package com.gk.quartz.enums;

import com.gk.common.enums.SimpleEnum;

public enum ScheduleStatusEnum implements SimpleEnum<Integer> {
    NORMAL(1, "正常", "enum.status.normal"),
    PAUSE(2, "暂停", "enum.status.pause");

    private final Integer code;
    private final String label;
    private final String i18nKey;

    ScheduleStatusEnum(int code, String label, String i18nKey) {
        this.code = code;
        this.label = label;
        this.i18nKey = i18nKey;
    }

    @Override
    public Integer code() {
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
