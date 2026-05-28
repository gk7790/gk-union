package com.gk.infra.enums;

import com.gk.common.enums.CodeEnum;

public enum SmsEnum implements CodeEnum<Integer> {
    YUNPIAN(1, "云片"),
    ALIYUN(4, "阿里云"),
    QCLOUD(2, "腾讯云"),
    QINIU(3, "七牛"),
    SUB(5, "塞班云"),
    EJOIN(6, "EJOIN");

    private final Integer code;
    private final String label;

    SmsEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    @Override
    public Integer code() {
        return code;
    }

    @Override
    public String label() {
        return label;
    }
}
