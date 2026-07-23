package com.example.hr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Point d'entrée de l'application backend.
 */
@SpringBootApplication
public class HrApplication {

    private static final Logger log = LoggerFactory.getLogger(HrApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(HrApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("═══════════════════════════════════════════════════════════");
        log.info("  Demo RH (inspiré Sopra HR4YOU) — backend PRÊT");
        log.info("  Port           : 8081");
        log.info("  Santé K8s      : GET /api/health-check  (sonde) | GET /api/health");
        log.info("═══════════════════════════════════════════════════════════");
    }
}
