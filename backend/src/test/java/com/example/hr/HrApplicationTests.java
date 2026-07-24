package com.example.hr;

import com.example.hr.model.HealthCheck;
import com.example.hr.repository.HealthCheckRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class HrApplicationTests {

    @Autowired HealthCheckRepository healthCheckRepository;

    @Test
    void contextLoads() {
        // verifies the Spring context starts and the Postgres connection is established
    }

    @Test
    void healthCheckRoundTripsThroughPostgres() {
        long before = healthCheckRepository.count();
        healthCheckRepository.save(new HealthCheck(Instant.now()));
        assertThat(healthCheckRepository.count()).isEqualTo(before + 1);
    }
}
