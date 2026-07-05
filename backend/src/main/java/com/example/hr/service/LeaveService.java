package com.example.hr.service;

import com.example.hr.model.LeaveRequest;
import com.example.hr.model.LeaveStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Logique métier des demandes de congé (soumission + workflow de décision).
 */
@Service
public class LeaveService {

    private static final Logger log = LoggerFactory.getLogger(LeaveService.class);

    private final HrDataStore store;
    private final WorkingDaysCalculator calculator;

    public LeaveService(HrDataStore store, WorkingDaysCalculator calculator) {
        this.store = store;
        this.calculator = calculator;
    }

    public List<LeaveRequest> history(Long employeeId) {
        List<LeaveRequest> list = store.getLeaveRequestsByEmployee(employeeId);
        log.info("[LEAVE][HISTORY] emp={} -> {} demande(s)", employeeId, list.size());
        return list;
    }

    public LeaveRequest submit(LeaveRequest request) {
        log.info("[LEAVE][SUBMIT] Nouvelle demande reçue : emp={} type={} {}->{}",
                request.getEmployeeId(), request.getType(),
                request.getStartDate(), request.getEndDate());

        if (request.getEmployeeId() == null || store.findEmployee(request.getEmployeeId()) == null) {
            throw new IllegalArgumentException("Employé inconnu : " + request.getEmployeeId());
        }
        if (request.getType() == null) {
            throw new IllegalArgumentException("Type de congé obligatoire");
        }

        // Calcul automatique des jours ouvrés
        int workingDays = calculator.compute(request.getStartDate(), request.getEndDate());
        request.setWorkingDays(workingDays);
        request.setStatus(LeaveStatus.EN_ATTENTE);
        request.setRequestedAt(LocalDate.now());

        LeaveRequest saved = store.addLeaveRequest(request);
        log.info("[LEAVE][SUBMIT] Demande #{} enregistrée : {} jour(s) ouvré(s), statut={}",
                saved.getId(), saved.getWorkingDays(), saved.getStatus());
        return saved;
    }

    public LeaveRequest decide(Long leaveId, LeaveStatus decision, String decisionComment) {
        LeaveRequest lr = store.findLeave(leaveId);
        if (lr == null) {
            throw new IllegalArgumentException("Demande introuvable : " + leaveId);
        }
        if (decision != LeaveStatus.VALIDE && decision != LeaveStatus.REFUSE) {
            throw new IllegalArgumentException("Décision invalide : " + decision);
        }
        lr.setStatus(decision);
        lr.setDecisionComment(decisionComment);
        log.info("[LEAVE][DECISION] Demande #{} -> {} ({})",
                leaveId, decision, decisionComment == null ? "sans commentaire" : decisionComment);
        return lr;
    }
}
