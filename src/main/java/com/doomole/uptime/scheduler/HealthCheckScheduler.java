package com.doomole.uptime.scheduler;

import com.doomole.uptime.entity.HealthCheck;
import com.doomole.uptime.repo.HealthCheckRepo;
import com.doomole.uptime.repo.HealthCheckResultRepo;
import com.doomole.uptime.service.HealthCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class HealthCheckScheduler {
    private final HealthCheckRepo healthCheckRepo;
    private final HealthCheckResultRepo healthCheckResultRepo;
    private final HealthCheckService healthCheckService;

    @Value("${uptime.results.retentionDays:30}")
    private int retentionDays;

    @Scheduled(fixedDelayString = "30000")
    public void tick() {
        LocalDateTime now = LocalDateTime.now();
        for (HealthCheck healthCheck : healthCheckRepo.findByEnabledTrue()) {
            if (!healthCheckService.shouldRun(healthCheck, now)) continue;

            String corrId = "sched-" + healthCheck.getId() + "-" + java.util.UUID.randomUUID().toString().substring(0,8);
            org.slf4j.MDC.put(com.doomole.uptime.filter.CorrelationIdFilter.MDC_KEY, corrId);
            try {
                log.info("Scheduler run check id={} url={}", healthCheck.getId(), healthCheck.getUrl());
                healthCheckService.runOneCheck(healthCheck, now);
            } finally {
                org.slf4j.MDC.remove(com.doomole.uptime.filter.CorrelationIdFilter.MDC_KEY);
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanup() {
        String corrId = "cleanup-" + java.util.UUID.randomUUID().toString().substring(0,8);
        org.slf4j.MDC.put(com.doomole.uptime.filter.CorrelationIdFilter.MDC_KEY, corrId);
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
            long deleted = healthCheckResultRepo.deleteByObservedAtBefore(cutoff);
            log.info("Cleanup deleted {} rows older than {}", deleted, cutoff);
        } finally {
            org.slf4j.MDC.remove(com.doomole.uptime.filter.CorrelationIdFilter.MDC_KEY);
        }
    }
}
