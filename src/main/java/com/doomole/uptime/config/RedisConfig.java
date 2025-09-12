package com.doomole.uptime.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // 기본 localhost:6379 (필요 시 application.yml로 설정)
        return new LettuceConnectionFactory();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
        var tpl = new RedisTemplate<String, Object>();
        tpl.setConnectionFactory(cf);

        var keySer = new StringRedisSerializer();

        // ObjectMapper 구성은 생성자에서 주입 (setObjectMapper 더 이상 사용 X)
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        var valSer = new GenericJackson2JsonRedisSerializer(om);

        tpl.setKeySerializer(keySer);
        tpl.setHashKeySerializer(keySer);
        tpl.setValueSerializer(valSer);
        tpl.setHashValueSerializer(valSer);
        tpl.afterPropertiesSet();
        return tpl;
    }

}
