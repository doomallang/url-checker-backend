package com.doomole.uptime.service;

import com.doomole.uptime.domain.CheckType;
import com.doomole.uptime.domain.HealthStatus;
import com.doomole.uptime.dto.HealthCheckRequest;
import com.doomole.uptime.dto.HealthCheckResponse;
import com.doomole.uptime.dto.HealthCheckResultResponse;
import com.doomole.uptime.dto.HealthCheckSummaryResponse;
import com.doomole.uptime.entity.HealthCheck;
import com.doomole.uptime.entity.HealthCheckResult;
import com.doomole.uptime.exception.ClientErrorException;
import com.doomole.uptime.repo.HealthCheckRepo;
import com.doomole.uptime.repo.HealthCheckResultRepo;
import com.doomole.uptime.utils.CommonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class HealthCheckService {
    private final HealthCheckRepo healthCheckRepo;
    private final WebClient webClient;
    private final HealthCheckResultRepo healthCheckResultRepo;

    private final SimpMessagingTemplate messagingTemplate;

    public HealthCheckResponse addHealthCheck(HealthCheckRequest request) {
        String url = CommonUtil.normalizeUrl(request.url());

        if (healthCheckRepo.existsByUrl(url)) {
            throw new ClientErrorException("이미 존재하는 URL 입니다: " + url);
        }
        if (request.type() == CheckType.HTTP && !(url.startsWith("http://") || url.startsWith("https://"))) {
            throw new ClientErrorException("HTTP 체크는 http/https URL만 허용됩니다.");
        }

        HealthCheck healthCheck = healthCheckRepo.save(HealthCheck.builder()
                .name(request.name())
                .type(request.type() == null ? CheckType.HTTP : request.type())
                .url(url)
                .intervalSeconds(request.intervalSeconds())
                .thresholdN(1)
                .windowM(3)
                .enabled(true)
                .status(HealthStatus.UNKNOWN)
                .failCount(0)
                .lastCheckedAt(null)
                .responseTimeMs(null)
                .lastError(null)
                .createdAt(LocalDateTime.now())
                .build()
        );

        return new HealthCheckResponse(healthCheck.getId(), healthCheck.getName(), healthCheck.getType().name(), healthCheck.getUrl(), healthCheck.getIntervalSeconds(), healthCheck.isEnabled());
    }

    @Transactional
    public HealthCheckResponse updateHealthCheck(Long id, HealthCheckRequest request) {
        HealthCheck healthCheck = healthCheckRepo.findById(id)
                .orElseThrow(() -> new ClientErrorException("대상을 찾을 수 없습니다: " + id));

        if (request.name() != null) healthCheck.setName(request.name());
        if (request.type() != null) healthCheck.setType(request.type());
        if (request.url() != null) {
            String url = CommonUtil.normalizeUrl(request.url());
            if (healthCheck.getType() == CheckType.HTTP && !(url.startsWith("http://") || url.startsWith("https://"))) {
                throw new ClientErrorException("HTTP 체크는 http/https URL만 허용됩니다.");
            }
            if (!url.equals(healthCheck.getUrl()) && healthCheckRepo.existsByUrl(url)) {
                throw new ClientErrorException("이미 존재하는 URL 입니다: " + url);
            }
            healthCheck.setUrl(url);
        }
        if (request.intervalSeconds() != null) healthCheck.setIntervalSeconds(request.intervalSeconds());
        if (request.thresholdN() != null) healthCheck.setThresholdN(request.thresholdN());
        if (request.windowM() != null) healthCheck.setWindowM(request.windowM());
        if (request.enabled() != null) healthCheck.setEnabled(request.enabled());

        healthCheck.setUpdatedAt(java.time.LocalDateTime.now());
        healthCheckRepo.save(healthCheck);

        return new HealthCheckResponse(healthCheck.getId(), healthCheck.getName(), healthCheck.getType().name(),
                healthCheck.getUrl(), healthCheck.getIntervalSeconds(), healthCheck.isEnabled());
    }

    public void deleteHealthCheck(Long id) {
        if (!healthCheckRepo.existsById(id)) {
            throw new ClientErrorException("대상을 찾을 수 없습니다: " + id);
        }
        healthCheckRepo.deleteById(id);
    }

    public List<HealthCheckResponse> getHealthCheckList() {
        return healthCheckRepo.findAll().stream()
                .map(healthCheck -> new HealthCheckResponse(healthCheck.getId(), healthCheck.getName(), healthCheck.getType().name(),
                        healthCheck.getUrl(), healthCheck.getIntervalSeconds(), healthCheck.isEnabled()))
                .toList();
    }

    public List<HealthCheckResultResponse> getHealthCheckResultRecent(Long healthCheckId, int limit) {
        var list = healthCheckResultRepo.findByHealthCheck_IdOrderByObservedAtDesc(
                healthCheckId, org.springframework.data.domain.PageRequest.of(0, Math.min(limit, 500))
        );
        return list.stream()
                .map(r -> new HealthCheckResultResponse(
                        r.getId(),
                        r.getObservedAt(),
                        r.getHealthStatus(),
                        r.getHttpCode(),
                        r.getLatencyMs(),
                        r.getErrorMessage()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public HealthCheckSummaryResponse getHealthCheckSummary(Long healthCheckId, String window) {
        LocalDateTime from = switch (window) {
            case "1h"  -> LocalDateTime.now().minusHours(1);
            case "24h" -> LocalDateTime.now().minusHours(24);
            default    -> throw new ClientErrorException("window 값은 1h 또는 24h만 허용됩니다.");
        };

        long total = healthCheckResultRepo.countByHealthCheck_IdAndObservedAtGreaterThanEqual(healthCheckId, from);
        long up    = healthCheckResultRepo.countByHealthCheck_IdAndHealthStatusAndObservedAtGreaterThanEqual(
                healthCheckId, HealthStatus.UP, from
        );

        // JPA 메서드로 avg 직접 구하는 건 없으니, 최근 window 범위의 결과를 직접 불러서 평균 내면 돼
        var results = healthCheckResultRepo.findByHealthCheck_IdOrderByObservedAtDesc(
                healthCheckId, org.springframework.data.domain.PageRequest.of(0, Math.min((int) total, 1000))
        );
        Double avgLatency = results.isEmpty() ? null :
                results.stream()
                        .map(r -> r.getLatencyMs())
                        .filter(Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(Double.NaN);

        double uptimePercent = (total == 0) ? 0.0 : (up * 100.0 / total);

        var latest = healthCheckResultRepo.findTop1ByHealthCheck_IdOrderByObservedAtDesc(healthCheckId);
        String latestStatus = (latest == null) ? "UNKNOWN" : latest.getHealthStatus().name();
        Integer latestHttp  = (latest == null) ? null : latest.getHttpCode();

        return new HealthCheckSummaryResponse(
                healthCheckId,
                window,
                Math.round(uptimePercent * 10.0) / 10.0,
                avgLatency == null ? null : Math.round(avgLatency * 10.0) / 10.0,
                latestStatus,
                latestHttp
        );
    }

    @Transactional
    public void runOneCheck(HealthCheck healthCheck, LocalDateTime now) {
        if (!healthCheck.isEnabled()) return; // 🔹비활성 스킵

        if (healthCheck.getType() != CheckType.HTTP) {
            return;
        }

        int httpCode = 0;
        Integer latencyMs = null;
        String error = null;
        long startNs = System.nanoTime();
        try {
            httpCode = webClient.get()
                    .uri(healthCheck.getUrl())
                    .exchangeToMono(resp -> Mono.just(resp.statusCode().value()))
                    .timeout(Duration.ofSeconds(15))
                    .blockOptional()   // ← blockOptional() 쓰면 null 안전
                    .orElse(0);        // 응답 없으면 0으로

            latencyMs = (int) ((System.nanoTime() - startNs) / 1_000_000);
        } catch (Exception e) {
            latencyMs = (int) ((System.nanoTime() - startNs) / 1_000_000);
            error = e.getMessage();
        }

        boolean up = httpCode >= 200 && httpCode < 400;
        HealthStatus newStatus = up ? HealthStatus.UP : HealthStatus.DOWN;

        // 이력 저장
        HealthCheckResult healthCheckResult = HealthCheckResult.builder()
                .healthCheck(healthCheck)
                .observedAt(now)
                .healthStatus(newStatus)
                .latencyMs(latencyMs)
                .httpCode(httpCode == 0 ? null : httpCode)
                .errorMessage(error)
                .build();
        healthCheckResultRepo.save(healthCheckResult);

        if (newStatus == HealthStatus.DOWN) {
            healthCheck.setFailCount(healthCheck.getFailCount() + 1);
            healthCheck.setLastError(error);
        } else {
            healthCheck.setFailCount(0);
            healthCheck.setLastError(null);
        }
        healthCheck.setStatus(newStatus);
        healthCheck.setLastCheckedAt(now);
        healthCheck.setResponseTimeMs(latencyMs == null ? null : latencyMs.longValue());
        healthCheck.setUpdatedAt(now);

        healthCheckRepo.save(healthCheck);

        // ✅ WS 브로드캐스트 (프론트 훅이 구독하는 채널)
        var event = com.doomole.uptime.dto.HealthCheckEvent.builder()
                .healthCheckId(healthCheck.getId())
                .status(newStatus.name())
                .httpCode(httpCode == 0 ? null : httpCode)
                .latencyMs(latencyMs)
                .error(error)
                .observedAt(now.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
                .build();

        messagingTemplate.convertAndSend("/topic/health/" + healthCheck.getId(), event);
    }

    // interval 충족 여부(스케줄러에서 사용)
    public boolean shouldRun(HealthCheck healthCheck, LocalDateTime now) {
        LocalDateTime last = healthCheck.getLastCheckedAt();
        if (last == null) return true;
        long elapsed = java.time.Duration.between(last, now).getSeconds();
        return elapsed >= healthCheck.getIntervalSeconds();
    }
}
