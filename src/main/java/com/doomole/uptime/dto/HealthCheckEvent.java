package com.doomole.uptime.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HealthCheckEvent {
    private Long healthCheckId;
    private String status;     // "UP" | "DOWN" | "UNKNOWN"
    private Integer httpCode;  // nullable
    private Integer latencyMs; // nullable
    private String error;      // nullable
    private long observedAt;   // epoch millis
}
