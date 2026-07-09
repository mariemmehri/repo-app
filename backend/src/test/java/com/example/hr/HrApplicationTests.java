package com.example.hr;

import com.example.hr.repository.EmployeeRepository;
import com.example.hr.repository.LeaveRequestRepository;
import com.example.hr.repository.PayslipRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class HrApplicationTests {

    @Autowired EmployeeRepository employeeRepository;
    @Autowired LeaveRequestRepository leaveRepository;
    @Autowired PayslipRepository payslipRepository;

    @Test
    void contextLoads() {
        // verifies Spring context starts and Postgres connection is established
    }

    @Test
    void seedDataIsLoaded() {
        assertThat(employeeRepository.count()).isEqualTo(5);
        assertThat(leaveRepository.count()).isEqualTo(10);
        assertThat(payslipRepository.count()).isEqualTo(15);
    }
}
