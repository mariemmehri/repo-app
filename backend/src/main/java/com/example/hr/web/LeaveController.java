package com.example.hr.web;

import com.example.hr.model.LeaveRequest;
import com.example.hr.service.LeaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints des demandes de congé (soumission, historique, décision).
 */
@RestController
@RequestMapping("/api/leaves")
@CrossOrigin(origins = "*")
public class LeaveController {

    private static final Logger log = LoggerFactory.getLogger(LeaveController.class);

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    /** Historique des demandes d'un employé (vue "mes demandes"). */
    @GetMapping
    public List<LeaveRequest> history(@RequestParam Long employeeId) {
        log.info("[API] GET /api/leaves?employeeId={}", employeeId);
        return leaveService.history(employeeId);
    }

    /** Soumission d'une nouvelle demande — jours ouvrés calculés automatiquement. */
    @PostMapping
    public ResponseEntity<?> submit(@RequestBody LeaveRequest request) {
        log.info("[API] POST /api/leaves emp={} type={}",
                request.getEmployeeId(), request.getType());
        try {
            LeaveRequest saved = leaveService.submit(request);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException ex) {
            log.warn("[API] POST /api/leaves rejeté : {}", ex.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    /** Décision manager : valider ou refuser une demande. */
    @PutMapping("/{id}/decision")
    public ResponseEntity<?> decide(@PathVariable Long id, @RequestBody LeaveDecisionRequest body) {
        log.info("[API] PUT /api/leaves/{}/decision -> {}", id, body.getDecision());
        try {
            LeaveRequest updated = leaveService.decide(id, body.getDecision(), body.getDecisionComment());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            log.warn("[API] PUT /api/leaves/{}/decision rejeté : {}", id, ex.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    /** Petite structure d'erreur JSON. */
    record ErrorResponse(String error) {
    }
}
