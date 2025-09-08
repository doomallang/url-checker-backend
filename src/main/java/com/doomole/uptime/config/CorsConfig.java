package com.doomole.uptime.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:3000") // 프론트 주소
                        .allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)   // 쿠키/인증 사용할 계획 없으면 false로
                        .maxAge(3600);            // preflight 캐시(초)
            }
        };
    }
}
