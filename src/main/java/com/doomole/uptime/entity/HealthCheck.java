package com.doomole.uptime.entity;

import com.doomole.uptime.domain.CheckType;
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
@Table(name = "health_check")
public class HealthCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private CheckType type;

    @Column(name = "target_url")
    private String url;

    @Column(name = "interval_seconds")
    private int intervalSeconds;

    @Column(name = "threshold_n")
    private int thresholdN;

    @Column(name = "window_m")
    private int windowM;

    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable=false, length=16)
    private HealthStatus status; // UNKNOWN/UP/DOWN

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Column(name = "fail_count", nullable=false)
    private int failCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
