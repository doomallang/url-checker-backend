package com.doomole.uptime.entity;

import com.doomole.uptime.domain.HealthStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "health_check_result")
public class HealthCheckResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "health_check_id")
    private HealthCheck healthCheck;

    @Column(name = "observed_at")
    private LocalDateTime observedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private HealthStatus healthStatus;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "http_code")
    private Integer httpCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
