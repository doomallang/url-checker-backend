package com.doomole.uptime.repo;

import com.doomole.uptime.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRuleRepo extends JpaRepository<AlertRule, Long> {
}
