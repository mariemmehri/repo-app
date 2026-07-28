package com.example.hr.service;

import com.example.hr.model.LeaveRequest;
import com.example.hr.model.LeaveStatus;
import com.example.hr.repository.EmployeeRepository;
import com.example.hr.repository.LeaveRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Logique métier des demandes de congé (soumission + workflow de décision).
 */
@Service
public class LeaveService {

    private static final Logger log = LoggerFactory.getLogger(LeaveService.class);

    private final LeaveRequestRepository leaveRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkingDaysCalculator calculator;

    public LeaveService(LeaveRequestRepository leaveRepository,
                        EmployeeRepository employeeRepository,
                        WorkingDaysCalculator calculator) {
        this.leaveRepository = leaveRepository;
        this.employeeRepository = employeeRepository;
        this.calculator = calculator;
    }

    public List<LeaveRequest> history(Long employeeId) {
        List<LeaveRequest> list = leaveRepository.findByEmployeeId(employeeId);
        log.info("[LEAVE][HISTORY] emp={} -> {} demande(s)", employeeId, list.size());
        return list;
    }

    @Transactional
    public LeaveRequest submit(LeaveRequest request) {
        log.info("[LEAVE][SUBMIT] Nouvelle demande reçue : emp={} type={} {}->{}",
                request.getEmployeeId(), request.getType(),
                request.getStartDate(), request.getEndDate());

        if (request.getEmployeeId() == null || !employeeRepository.existsById(request.getEmployeeId())) {
            throw new IllegalArgumentException("Employé inconnu : " + request.getEmployeeId());
        }
        if (request.getType() == null) {
            throw new IllegalArgumentException("Type de congé obligatoire");
        }

        int workingDays = calculator.compute(request.getStartDate(), request.getEndDate());
        request.setWorkingDays(workingDays);
        request.setStatus(LeaveStatus.EN_ATTENTE);
        request.setRequestedAt(LocalDate.now());

        LeaveRequest saved = leaveRepository.save(request);
        log.info("[LEAVE][SUBMIT] Demande #{} enregistrée : {} jour(s) ouvré(s), statut={}",
                saved.getId(), saved.getWorkingDays(), saved.getStatus());
        return saved;
    }

    @Transactional
    public LeaveRequest decide(Long leaveId, LeaveStatus decision, String decisionComment) {
        LeaveRequest lr = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable : " + leaveId));
        if (decision != LeaveStatus.VALIDE && decision != LeaveStatus.REFUSE) {
            throw new IllegalArgumentException("Décision invalide : " + decision);
        }
        lr.setStatus(decision);
        lr.setDecisionComment(decisionComment);
        LeaveRequest saved = leaveRepository.save(lr);
        log.info("[LEAVE][DECISION] Demande #{} -> {} ({})",
                leaveId, decision, decisionComment == null ? "sans commentaire" : decisionComment);
        return saved;
    }
}
