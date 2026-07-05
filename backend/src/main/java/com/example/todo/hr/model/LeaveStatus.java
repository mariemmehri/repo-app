package com.example.todo.hr.model;

/**
 * Statut d'une demande de congé dans le workflow de validation.
 */
public enum LeaveStatus {
    EN_ATTENTE,   // soumise, en attente de décision manager
    VALIDE,       // acceptée
    REFUSE        // refusée
}
