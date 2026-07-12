package com.gk.infra.enums;

import com.gk.common.enums.CodeEnum;

public enum CloudEnum implements CodeEnum<Integer> {
    /**
     * 七牛云
     */
    QINIU(1, "七牛云"),
    /**
     * 阿里云
     */
    ALIYUN(2, "阿里云"),
    /**
     * 腾讯云
     */
    QCLOUD(3, "腾讯云"),
    /**
     * FASTDFS
     */
    FASTDFS(4, "FASTDFS"),
    /**
     * 本地
     */
    LOCAL(5, "本地"),
    /**
     * MinIO
     */
    MINIO(6, "MinIO");

    private final Integer code;
    private final String label;

    CloudEnum(int code, String label) {
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
