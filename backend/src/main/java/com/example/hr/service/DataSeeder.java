package com.example.hr.service;

import com.example.hr.model.Employee;
import com.example.hr.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insère le jeu d'employés de démo au premier démarrage (base vide uniquement).
 * Les redémarrages ultérieurs détectent la base non-vide et sautent le seed.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final EmployeeRepository employeeRepo;

    public DataSeeder(EmployeeRepository employeeRepo) {
        this.employeeRepo = employeeRepo;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (employeeRepo.count() > 0) {
            log.info("[SEED] Base déjà peuplée ({} employés) — seed ignoré.", employeeRepo.count());
            return;
        }
        log.info("[SEED] Initialisation du jeu d'employés de démo (PostgreSQL)");

        employeeRepo.saveAll(java.util.List.of(
            new Employee(null, "SHR-0001", "Amine", "Ben Salah",
                    "amine.bensalah@demo-hr.local", "Direction des Systèmes d'Information",
                    "Ingénieur DevOps", 3800, 18, 6),
            new Employee(null, "SHR-0002", "Claire", "Dupont",
                    "claire.dupont@demo-hr.local", "Ressources Humaines",
                    "Chargée de recrutement", 3200, 22, 4),
            new Employee(null, "SHR-0003", "Mehdi", "Trabelsi",
                    "mehdi.trabelsi@demo-hr.local", "Développement",
                    "Développeur Full-Stack", 3400, 12, 8)
        ));

        log.info("[SEED] Terminé : {} employé(s)", employeeRepo.count());
    }
}
