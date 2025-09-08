package com.doomole.uptime.entity;

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
@Table(name = "alert_rule")
public class AlertRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "health_check_id")
    private HealthCheck healthCheck;

    private String channel; // slack

    @Column(name = "endpoint_url")
    private String endPointUrl;

    private boolean enabled;

    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
