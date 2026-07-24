package com.example.hr;

import com.example.hr.model.HealthCheck;
import com.example.hr.repository.HealthCheckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Endpoint de santé interrogé par les sondes Kubernetes (readiness + liveness)
 * du chart Helm (charts/hr-app/templates/deployment-backend.yaml) sur /api/health-check.
 * Répond simplement 200 avec une liste vide.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final HealthCheckRepository healthCheckRepository;

    public HealthController(HealthCheckRepository healthCheckRepository) {
        this.healthCheckRepository = healthCheckRepository;
    }

    /** Route interrogée par les sondes K8s — doit rester en 200. */
    @GetMapping("/health-check")
    public List<Object> healthCheck() {
        log.debug("[HEALTH] GET /api/health-check (sonde K8s) -> 200 []");
        return List.of();
    }

    /** Endpoint de santé explicite. */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "app", "demo-hr",
                "message", "Backend RH opérationnel"
        );
    }

    /**
     * Preuve de connectivité Postgres réelle : écrit puis compte une ligne via
     * JPA/Hibernate (pas juste une ouverture de connexion JDBC brute).
     */
    @GetMapping("/db-health")
    public ResponseEntity<Map<String, Object>> dbHealth() {
        try {
            healthCheckRepository.save(new HealthCheck(Instant.now()));
            long totalChecks = healthCheckRepository.count();
            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "database", "postgresql",
                    "totalChecks", totalChecks
            ));
        } catch (DataAccessException e) {
            log.error("[DB-HEALTH] Postgres unreachable", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "status", "DOWN",
                    "database", "postgresql",
                    "error", e.getMostSpecificCause().getMessage()
            ));
        }
    }
}
