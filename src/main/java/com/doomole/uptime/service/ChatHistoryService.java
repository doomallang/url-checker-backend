package com.doomole.uptime.service;

import com.doomole.uptime.dto.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private static final int MAX_PER_ROOM = 500;
    private static final Duration ROOM_TTL = Duration.ofDays(7);

    private final RedisTemplate<String, Object> redis; // 값 타입 Object로 둡니다.
    private final ObjectMapper objectMapper;           // ✅ 주입

    private String key(String roomId) { return "chat:room:" + roomId; }

    public void append(ChatMessage m) {
        if (m.getTimestamp() == 0) m.setTimestamp(System.currentTimeMillis());
        String k = key(m.getRoomId());
        redis.opsForList().rightPush(k, m);
        redis.opsForList().trim(k, -MAX_PER_ROOM, -1);
        redis.expire(k, ROOM_TTL);
        Long len = redis.opsForList().size(k);
        log.info("CHAT APPEND key={} len={} sender={} ts={}", k, len, m.getSender(), m.getTimestamp());
    }

    public List<ChatMessage> recent(String roomId, int limit) {
        String k = key(roomId);
        List<Object> raw = redis.opsForList().range(k, Math.max(-limit, -MAX_PER_ROOM), -1);
        int n = raw == null ? 0 : raw.size();
        log.info("CHAT RECENT key={} limit={} -> {} rows(raw)", k, limit, n);
        if (raw == null) return List.of();

        return raw.stream().map(o -> {
            try {
                if (o instanceof ChatMessage cm) return cm;
                if (o instanceof String s)       return objectMapper.readValue(s, ChatMessage.class); // ✅ 문자열 JSON
                if (o instanceof Map<?, ?> map) return objectMapper.convertValue(map, ChatMessage.class); // ✅ 맵
                log.warn("Unknown redis value type: {}", o.getClass());
                return null;
            } catch (Exception e) {
                log.error("Parse redis chat message failed: {}", o, e);
                return null;
            }
        }).filter(Objects::nonNull).toList();
    }
}
