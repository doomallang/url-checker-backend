package com.doomole.uptime.repo;

import com.doomole.uptime.domain.HealthStatus;
import com.doomole.uptime.entity.HealthCheckResult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface HealthCheckResultRepo extends JpaRepository<HealthCheckResult, Long> {

    List<HealthCheckResult> findByHealthCheck_IdOrderByObservedAtDesc(Long healthCheckId, Pageable pageable);

    long countByHealthCheck_IdAndObservedAtGreaterThanEqual(Long healthCheckId, LocalDateTime from);

    long countByHealthCheck_IdAndHealthStatusAndObservedAtGreaterThanEqual(
            Long healthCheckId, HealthStatus status, LocalDateTime from
    );

    HealthCheckResult findTop1ByHealthCheck_IdOrderByObservedAtDesc(Long healthCheckId);

    long deleteByObservedAtBefore(LocalDateTime cutoff);
}
