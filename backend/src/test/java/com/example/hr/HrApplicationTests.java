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
        // exact counts on CI (fresh DB); >= tolerates local mutations on top of seed
        assertThat(employeeRepository.count()).isEqualTo(5);
        assertThat(leaveRepository.count()).isGreaterThanOrEqualTo(10);
        assertThat(payslipRepository.count()).isGreaterThanOrEqualTo(15);
    }
}
