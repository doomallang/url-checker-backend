package com.doomole.uptime.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")               // WS 엔드포인트
                .setAllowedOriginPatterns("*")
                .withSockJS();                    // 필요 시 SockJS fallback
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트 → 서버로 보내는 prefix
        registry.setApplicationDestinationPrefixes("/app");
        // 서버 → 클라이언트로 브로드캐스트할 prefix
        registry.enableSimpleBroker("/topic", "/queue"); // 소규모: in-memory broker
    }
}
