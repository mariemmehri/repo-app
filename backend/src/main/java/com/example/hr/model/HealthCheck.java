package com.example.hr.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

/**
 * Written by GET /api/db-health on every call to prove the JPA -> Postgres
 * round trip actually works (insert + count), not just that a connection opens.
 */
@Entity
public class HealthCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant checkedAt;

    public HealthCheck() {
    }

    public HealthCheck(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }

    public Long getId() {
        return id;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }
}
