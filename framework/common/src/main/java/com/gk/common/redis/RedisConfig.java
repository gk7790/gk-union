package com.gk.common.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // key 使用 String 序列化
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // value 使用 FastJSON2 序列化
        FastJson2RedisSerializer<Object> fastJson2Serializer = new FastJson2RedisSerializer<>(Object.class);

        // key
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // value
        template.setValueSerializer(fastJson2Serializer);
        template.setHashValueSerializer(fastJson2Serializer);

        template.afterPropertiesSet();
        return template;
    }


}
