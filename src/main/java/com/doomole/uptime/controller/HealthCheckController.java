package com.doomole.uptime.controller;

import com.doomole.uptime.dto.HealthCheckRequest;
import com.doomole.uptime.dto.HealthCheckResponse;
import com.doomole.uptime.dto.HealthCheckResultResponse;
import com.doomole.uptime.dto.HealthCheckSummaryResponse;
import com.doomole.uptime.service.HealthCheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/health/check")
@RequiredArgsConstructor
public class HealthCheckController {
    private final HealthCheckService healthCheckService;

    @PostMapping
    public ResponseEntity<HealthCheckResponse> addHealthCheck(@Valid @RequestBody HealthCheckRequest request) {
        HealthCheckResponse healthCheckResponse = healthCheckService.addHealthCheck(request);

        return ResponseEntity.ok(healthCheckResponse);
    }

    @GetMapping
    public ResponseEntity<List<HealthCheckResponse>> getHealthCheckList() {
        var list = healthCheckService.getHealthCheckList();

        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<List<HealthCheckResultResponse>> getHealthCheckResultRecent(
            @PathVariable("id") Long id,
            @RequestParam(name = "limit", defaultValue = "100") int limit
    ) {
        var list = healthCheckService.getHealthCheckResultRecent(id, limit);

        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<HealthCheckSummaryResponse> getHealthCheckSummary(
            @PathVariable("id") Long id,
            @RequestParam(name = "window", defaultValue = "1h") String window
    ) {
        return ResponseEntity.ok(healthCheckService.getHealthCheckSummary(id, window));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HealthCheckResponse> update(
            @PathVariable Long id, @RequestBody HealthCheckRequest request) {
        return ResponseEntity.ok(healthCheckService.updateHealthCheck(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        healthCheckService.deleteHealthCheck(id);
        return ResponseEntity.noContent().build();
    }
}
