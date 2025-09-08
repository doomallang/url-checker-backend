package com.doomole.uptime.dto;

import com.doomole.uptime.domain.CheckType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HealthCheckRequest(
        @NotBlank String name,
        @NotNull CheckType type,
        @NotBlank String url,
        Integer intervalSeconds,
        Integer thresholdN,
        Integer windowM,
        Boolean enabled
) {}
