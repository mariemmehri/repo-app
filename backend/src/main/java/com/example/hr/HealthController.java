package com.example.hr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoint de santé interrogé par les sondes Kubernetes (readiness + liveness)
 * du chart Helm (charts/hr-app/templates/deployment-backend.yaml) sur /api/health-check.
 * Répond simplement 200 avec une liste vide.
 *
 * Le vrai métier RH est servi par les contrôleurs du package com.example.hr.web
 * (/api/employees, /api/leaves, /api/payslips).
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

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
}
