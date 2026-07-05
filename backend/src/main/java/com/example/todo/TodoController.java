package com.example.todo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoint de santé conservé à l'adresse historique /api/todos.
 *
 * Les sondes Kubernetes (readiness + liveness) du chart Helm
 * (charts/todo-app/templates/deployment-backend.yaml) interrogent /api/todos.
 * Pour NE PAS casser la pipeline / le déploiement, on garde cette route
 * vivante — elle répond simplement 200 avec une liste vide.
 *
 * Le vrai métier RH est servi par les contrôleurs du package com.example.todo.hr.web
 * (/api/employees, /api/leaves, /api/payslips).
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TodoController {

    private static final Logger log = LoggerFactory.getLogger(TodoController.class);

    /** Route historique interrogée par les sondes K8s — doit rester en 200. */
    @GetMapping("/todos")
    public List<Object> healthTodos() {
        log.debug("[HEALTH] GET /api/todos (sonde K8s) -> 200 []");
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
