package com.example.todo.hr.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/**
 * Demande de congé d'un employé.
 * Le nombre de jours ouvrés est calculé automatiquement côté service.
 */
public class LeaveRequest {

    private Long id;
    private Long employeeId;
    private LeaveType type;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String comment;
    private int workingDays;        // jours ouvrés calculés
    private LeaveStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate requestedAt;

    private String decisionComment; // motif de validation/refus (optionnel)

    public LeaveRequest() {
    }

    public LeaveRequest(Long id, Long employeeId, LeaveType type, LocalDate startDate,
                        LocalDate endDate, String comment, int workingDays,
                        LeaveStatus status, LocalDate requestedAt, String decisionComment) {
        this.id = id;
        this.employeeId = employeeId;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.comment = comment;
        this.workingDays = workingDays;
        this.status = status;
        this.requestedAt = requestedAt;
        this.decisionComment = decisionComment;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public LeaveType getType() { return type; }
    public void setType(LeaveType type) { this.type = type; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public int getWorkingDays() { return workingDays; }
    public void setWorkingDays(int workingDays) { this.workingDays = workingDays; }

    public LeaveStatus getStatus() { return status; }
    public void setStatus(LeaveStatus status) { this.status = status; }

    public LocalDate getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDate requestedAt) { this.requestedAt = requestedAt; }

    public String getDecisionComment() { return decisionComment; }
    public void setDecisionComment(String decisionComment) { this.decisionComment = decisionComment; }
}
