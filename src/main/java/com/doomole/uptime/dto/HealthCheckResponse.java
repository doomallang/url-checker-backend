package com.doomole.uptime.dto;

public record HealthCheckResponse(
        Long id,
        String name,
        String type,
        String url,
        int intervalSeconds,
        boolean enabled
) {
}
