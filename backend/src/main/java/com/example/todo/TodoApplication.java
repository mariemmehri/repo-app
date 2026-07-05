package com.example.todo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Point d'entrée de l'application backend.
 *
 * NB : le nom de classe / du package (com.example.todo) est conservé volontairement.
 * Le nom du JAR produit dépend de l'artifactId Maven (todo-backend), pas du package —
 * changer le package ne rapporterait rien et risquerait d'introduire des écarts avec
 * la pipeline. On empile donc le métier RH dans le sous-package com.example.todo.hr.
 */
@SpringBootApplication
public class TodoApplication {

    private static final Logger log = LoggerFactory.getLogger(TodoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(TodoApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("═══════════════════════════════════════════════════════════");
        log.info("  Demo RH (inspiré Sopra HR4YOU) — backend PRÊT");
        log.info("  Port           : 8081");
        log.info("  Santé K8s      : GET /api/todos  (sonde) | GET /api/health");
        log.info("  Employés       : GET /api/employees");
        log.info("  Congés         : GET/POST /api/leaves , PUT /api/leaves/{id}/decision");
        log.info("  Bulletins paie : GET /api/payslips , GET /api/payslips/{id}/download");
        log.info("═══════════════════════════════════════════════════════════");
    }
}
