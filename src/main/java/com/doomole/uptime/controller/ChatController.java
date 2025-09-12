package com.doomole.uptime.controller;

import com.doomole.uptime.dto.ChatMessage;
import com.doomole.uptime.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatHistoryService chatHistoryService;

    // 클라이언트가 /app/chat.send 로 보낸 메시지를 특정 방으로 브로드캐스트
    @MessageMapping("/chat.send")
    public void send(@Payload ChatMessage message) {
        if (message.getTimestamp() == 0) {
            message.setTimestamp(System.currentTimeMillis());
        }
        chatHistoryService.append(message); // ✅ 저장
        messagingTemplate.convertAndSend("/topic/rooms/" + message.getRoomId(), message); // ✅ 브로드캐스트
    }
    // 초기 히스토리(최신 n개) 조회
    @GetMapping("/api/chat/history")
    public List<ChatMessage> history(
            @RequestParam String roomId,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return chatHistoryService.recent(roomId, Math.min(Math.max(limit, 1), 500));
    }
}

