package com.gk.common.redis;

import cn.hutool.core.collection.CollectionUtil;
import com.gk.common.utils.ConvertUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis工具类
 *
 * @author Lowen
 */
@Slf4j
@Component
public class RedisUtils {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**  默认过期时长为24小时，单位：秒 */
    public final static long DEFAULT_EXPIRE = 60 * 60 * 24L;
    /**  不设置过期时长 */
    public final static long NOT_EXPIRE = -1L;

    /// ****************************************** Redis 公共基础方法 start *******************************************/
    public Set<String> keys(String key) {
        return redisTemplate.keys(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public void delete(Collection<String> keys) {
        redisTemplate.delete(keys);
    }

    public void expire(String key, long expire) {
        redisTemplate.expire(key, expire, TimeUnit.SECONDS);
    }

    public boolean isKeyExist(String key) {
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }

    /**
     * 获取过期时间
     * @param key redis Key
     */
    public long getExpireTime(String key) {
        Long expireSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ObjectUtils.isEmpty(expireSeconds)) {
            return -2;
        }
        return expireSeconds;
    }

    /**
     * 方法加锁
     * @param key
     * @param expireSeconds
     * @return
     */
    public boolean tryLock(String key, int expireSeconds) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(expireSeconds)));
        } catch (Exception e) {
            log.warn("Redis加锁失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 方法解锁
     * @param key
     */
    public void unlock(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis解锁失败: {}", e.getMessage());
        }
    }

    /**
     * 用于增加某个键的值。
     * 计算调用次数(例如: 做限流时,验证在规定时间内调用次数)
     */
    public long getIncrement(String key, long delta) {
        Long increment = redisTemplate.opsForValue().increment(key);
        long l = Optional.ofNullable(increment).orElse(0L);
        if (l == 1) {
            // 设置请求的过期时间
            redisTemplate.expire(key, delta, TimeUnit.SECONDS);
        }
        return l;
    }
    /// *************************************** Redis 公共基础方法 end *******************************************/

    /// ****************************************** Redis opsForValue 方法 satrt *******************************************/
    public void set(String key, Object value) {
        set(key, value, DEFAULT_EXPIRE);
    }

    public void set(String key, Object value, long expire) {
        redisTemplate.opsForValue().set(key, value);
        if (expire != NOT_EXPIRE) {
            expire(key, expire);
        }
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取指定类型的keu数据
     */
    public <T> T get(String key, Class<T> clazz) {
        return ConvertUtils.sourceToTarget(get(key), clazz);
    }

    /// ****************************************** Redis opsForValue 方法 end *******************************************/

    /// ****************************************** Redis opsForSet 方法 start *******************************************/

    /**
     * 添加 set
     */
    public void addSet(String key, Object... values) {
        if (ArrayUtils.isNotEmpty(values)) {
            redisTemplate.opsForSet().add(key, values);
            expire(key, DEFAULT_EXPIRE);
        }
    }

    public void addSet(String key, Collection<?> values, long expire) {
        if (CollectionUtil.isEmpty(values)) {
            return;
        }

        Object[] array = values.stream()
                .filter(Objects::nonNull)
                .toArray();

        // 关键：过滤 null 后可能为空
        if (array.length == 0) {
            return;
        }

        redisTemplate.opsForSet().add(key, array);

        if (expire != NOT_EXPIRE) {
            expire(key, expire);
        }
    }

    /**
     * 添加 set
     */
    public Set<Object> getSetObject(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 添加 set
     */
    public <T> Set<T> getSet(String key, Class<T> clazz) {
        Set<Object> members = getSetObject(key);
        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }
        return members.stream()
                .filter(Objects::nonNull)
                .map(item -> ConvertUtils.sourceToTarget(item, clazz))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 判断Set中是否存在value
     * @param key 键
     * @param value 值
     */
    public boolean isSetExist(String key, Object value) {
        try {
            Boolean member = redisTemplate.opsForSet().isMember(key, value);
            return member != null && member;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /// ****************************************** Redis opsForSet 方法 end *******************************************/

}
