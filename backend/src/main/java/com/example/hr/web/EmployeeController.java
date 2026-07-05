package com.example.hr.web;

import com.example.hr.model.Employee;
import com.example.hr.service.HrDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints employés (annuaire de démo).
 */
@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "*")
public class EmployeeController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);

    private final HrDataStore store;

    public EmployeeController(HrDataStore store) {
        this.store = store;
    }

    @GetMapping
    public List<Employee> getAll() {
        log.info("[API] GET /api/employees");
        return store.getEmployees();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getOne(@PathVariable Long id) {
        log.info("[API] GET /api/employees/{}", id);
        Employee e = store.findEmployee(id);
        return e == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(e);
    }
}
