package com.doomole.uptime.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMessage {
    private String roomId;
    private String sender;
    private String content;
    private long timestamp;
    private String type;
}
