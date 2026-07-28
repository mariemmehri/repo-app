package com.example.hr.web;

import com.example.hr.model.LeaveStatus;

/**
 * Corps de requête pour valider/refuser une demande de congé.
 */
public class LeaveDecisionRequest {

    private LeaveStatus decision;    // VALIDE ou REFUSE
    private String decisionComment;

    public LeaveStatus getDecision() { return decision; }
    public void setDecision(LeaveStatus decision) { this.decision = decision; }

    public String getDecisionComment() { return decisionComment; }
    public void setDecisionComment(String decisionComment) { this.decisionComment = decisionComment; }
}
