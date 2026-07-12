package com.gk.infra.config.service;

import com.alibaba.fastjson2.JSON;
import com.gk.common.config.SysParamReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/** Generic system-parameter JSON reader. Business modules own their config models and defaults. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysParamConfigReader {
    private final SysParamReader sysParamReader;

    public <T> T getObject(String key, Class<T> type, T fallback) {
        try {
            String value = sysParamReader.getValue(key);
            if (StringUtils.isBlank(value)) {
                return fallback;
            }
            T config = JSON.parseObject(value, type);
            return config == null ? fallback : config;
        } catch (Exception ex) {
            log.warn("Load sys params config failed, key={}, err={}", key, ex.getMessage());
            return fallback;
        }
    }
}
