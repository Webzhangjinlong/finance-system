package com.finance.framework.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 全局序列化配置。
 *
 * <p>雪花 ID(Long) 数值超过 JS Number 安全整数上限(2^53 ≈ 9.007e15)，
 * 若默认序列化为 JSON 数字，前端 JSON.parse 后精度丢失（如 2098020815916027905 → 2098020815916028000），
 * 导致按 id 请求详情/子资源时 404/500。统一将 Long 序列化为字符串，保证前后端 id 一致。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> builder
                .serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance);
    }
}
