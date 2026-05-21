package com.gk.common.beans;

import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 参数管理
 * @author Lowen
 * @since 1.0.0
 */
@Component
@AllArgsConstructor
public class SysParamsRedis {
    private final RedisUtils redisUtils;

    public void delete(String paramCodes) {
        String key = RedisKeys.getSysParamsKey(paramCodes);
        redisUtils.delete(key);
    }

    public void set(String paramCode, String paramValue) {
        if (paramValue == null) {
            return;
        }
        String key = RedisKeys.getSysParamsKey(paramCode);
        redisUtils.set(key, paramValue, -1L);
    }

    public String get(String paramCode) {
        String key = RedisKeys.getSysParamsKey(paramCode);
        return redisUtils.get(key, String.class);
    }
}
