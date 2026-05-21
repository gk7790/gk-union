package com.gk.common.redis;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.StandardCharsets;

public class FastJson2RedisSerializer<T> implements RedisSerializer<T> {

    private final Class<T> clazz;

    public FastJson2RedisSerializer(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public byte[] serialize(T value) throws SerializationException {
        if (value == null) {
            return new byte[0];
        }

        try {
            return JSON.toJSONString(value, JSONWriter.Feature.WriteClassName)
                    .getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SerializationException("FastJSON2 serialize error", e);
        }
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try {
            return JSON.parseObject(bytes, 0, bytes.length, StandardCharsets.UTF_8, clazz);
        } catch (Exception e) {
            throw new SerializationException("FastJSON2 deserialize error", e);
        }
    }

}