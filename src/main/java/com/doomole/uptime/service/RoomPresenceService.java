package com.doomole.uptime.service;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

@Service
public class RoomPresenceService {
    private static final Pattern ROOM_DEST =
            Pattern.compile("^/topic/rooms/([^/]+)$");

    private final ConcurrentMap<String, Set<String>> roomSessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> sessionRooms = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    public RoomPresenceService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /** 현재 방 인원 수 반환 */
    public int getCount(String roomId) {
        return roomSessions.getOrDefault(roomId, Collections.emptySet()).size();
    }

    /** 변경시 presence 토픽으로 브로드캐스트 */
    private void notifyPresence(String roomId) {
        int count = getCount(roomId);
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + ".presence",
                new PresencePayload(roomId, count));
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(event.getMessage(), StompHeaderAccessor.class);
        if (accessor == null) return;

        String dest = accessor.getDestination();
        String sessionId = accessor.getSessionId();
        if (dest == null || sessionId == null) return;

        var m = ROOM_DEST.matcher(dest);
        if (!m.matches()) return;

        String roomId = m.group(1);

        roomSessions.compute(roomId, (k, set) -> {
            if (set == null) set = ConcurrentHashMap.newKeySet();
            set.add(sessionId);
            return set;
        });
        sessionRooms.compute(sessionId, (k, set) -> {
            if (set == null) set = ConcurrentHashMap.newKeySet();
            set.add(roomId);
            return set;
        });

        notifyPresence(roomId);
    }

    @EventListener
    public void onUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(event.getMessage(), StompHeaderAccessor.class);
        if (accessor == null) return;

        String dest = accessor.getDestination(); // 브로커 설정에 따라 null일 수 있음
        String sessionId = accessor.getSessionId();
        if (sessionId == null) return;

        Set<String> rooms = sessionRooms.getOrDefault(sessionId, Collections.emptySet());

        // dest가 정확히 오는 환경이면 해당 방만 제거
        if (dest != null) {
            var m = ROOM_DEST.matcher(dest);
            if (m.matches()) {
                rooms = Set.of(m.group(1));
            }
        }

        for (String roomId : rooms) {
            roomSessions.computeIfPresent(roomId, (k, set) -> {
                set.remove(sessionId);
                return set.isEmpty() ? null : set;
            });
            sessionRooms.computeIfPresent(sessionId, (k, set) -> {
                set.remove(roomId);
                return set.isEmpty() ? null : set;
            });
            notifyPresence(roomId);
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId == null) return;

        Set<String> rooms = sessionRooms.remove(sessionId);
        if (rooms == null) return;

        for (String roomId : rooms) {
            roomSessions.computeIfPresent(roomId, (k, set) -> {
                set.remove(sessionId);
                return set.isEmpty() ? null : set;
            });
            notifyPresence(roomId);
        }
    }

    /** presence 전송용 간단 DTO */
    public static class PresencePayload {
        private final String roomId;
        private final int count;

        public PresencePayload(String roomId, int count) {
            this.roomId = roomId;
            this.count = count;
        }
        public String getRoomId() { return roomId; }
        public int getCount() { return count; }
    }
}
