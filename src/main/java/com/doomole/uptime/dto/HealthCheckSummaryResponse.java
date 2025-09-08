package com.doomole.uptime.dto;

public record HealthCheckSummaryResponse(
        Long healthCheckId,
        String window,
        double uptimePercent,
        Double avgLatencyMs,
        String latestStatus,
        Integer latestHttpCode
) {
}
