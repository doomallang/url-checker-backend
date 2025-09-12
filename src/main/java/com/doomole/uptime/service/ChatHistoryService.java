package com.doomole.uptime.service;

import com.doomole.uptime.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private static final int MAX_PER_ROOM = 500;
    private static final Duration ROOM_TTL = Duration.ofDays(7);

    private final RedisTemplate<String, Object> redis;

    private String key(String roomId) { return "chat:room:" + roomId; }

    public void append(ChatMessage m) {
        if (m.getTimestamp() == 0) m.setTimestamp(System.currentTimeMillis());
        var k = key(m.getRoomId());
        redis.opsForList().rightPush(k, m);
        redis.opsForList().trim(k, -MAX_PER_ROOM, -1);
        redis.expire(k, ROOM_TTL);
    }

    @SuppressWarnings("unchecked")
    public List<ChatMessage> recent(String roomId, int limit) {
        var k = key(roomId);
        var raw = redis.opsForList().range(k, Math.max(-limit, -MAX_PER_ROOM), -1);
        if (raw == null) return List.of();
        // GenericJackson2JsonRedisSerializer는 타입 정보를 포함하므로 대부분 ChatMessage로 바로 역직렬화됩니다.
        // 혹시 모를 경우에만 방어적 캐스팅 처리.
        return raw.stream().map(o -> (o instanceof ChatMessage cm) ? cm : null)
                .filter(cm -> cm != null)
                .collect(Collectors.toList());
    }
}
