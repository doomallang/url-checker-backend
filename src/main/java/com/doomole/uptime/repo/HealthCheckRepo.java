package com.doomole.uptime.repo;

import com.doomole.uptime.entity.HealthCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HealthCheckRepo extends JpaRepository<HealthCheck, Long> {

    List<HealthCheck> findByEnabledTrue();

    boolean existsByUrl(String url);
}
