package com.doomole.uptime.dto;

import com.doomole.uptime.domain.HealthStatus;

import java.time.LocalDateTime;

public record HealthCheckResultResponse(
        Long id,
        LocalDateTime observedAt,
        HealthStatus status,
        Integer httpCode,
        Integer latencyMs,
        String errorMessage
) {
}
