package com.superstore.app.config.session;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class VaadinSessionConfig {

    @Bean("springSessionDefaultRedisSerializer")
    @Primary
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        
        return RedisSerializer.java();
    }

    @Bean("sessionRedisTemplate")
    public RedisTemplate<Object, Object> sessionRedisTemplate(RedisConnectionFactory connectionFactory) {
        
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        
        template.setConnectionFactory(connectionFactory);
        
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.java());
        template.setHashValueSerializer(RedisSerializer.java());
        
        return template;
    }
}