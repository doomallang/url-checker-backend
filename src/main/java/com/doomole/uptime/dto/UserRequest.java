package com.doomole.uptime.dto;

public record UserRequest(
        String email,
        String password,
        String displayName
) {
}
