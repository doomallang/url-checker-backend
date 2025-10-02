package com.doomole.uptime.controller;

import com.doomole.uptime.service.RoomPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class PresenceController {
    private final RoomPresenceService presenceService;

    @GetMapping("/rooms/{roomId}/presence")
    public PresenceDto presence(@PathVariable String roomId) {
        return new PresenceDto(roomId, presenceService.getCount(roomId));
    }

    public record PresenceDto(String roomId, int count) {}
}
