package com.example.hr.service;

import com.example.hr.model.Employee;
import com.example.hr.model.LeaveRequest;
import com.example.hr.model.LeaveStatus;
import com.example.hr.model.LeaveType;
import com.example.hr.model.Payslip;
import com.example.hr.repository.EmployeeRepository;
import com.example.hr.repository.LeaveRequestRepository;
import com.example.hr.repository.PayslipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Insère le jeu de données de démo au premier démarrage (base vide uniquement).
 * Les redémarrages ultérieurs détectent la base non-vide et sautent le seed.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final Locale FR = Locale.FRANCE;
    private static final int REF_YEAR = 2026;

    private final EmployeeRepository employeeRepo;
    private final LeaveRequestRepository leaveRepo;
    private final PayslipRepository payslipRepo;
    private final WorkingDaysCalculator workingDaysCalculator;

    public DataSeeder(EmployeeRepository employeeRepo,
                      LeaveRequestRepository leaveRepo,
                      PayslipRepository payslipRepo,
                      WorkingDaysCalculator workingDaysCalculator) {
        this.employeeRepo = employeeRepo;
        this.leaveRepo = leaveRepo;
        this.payslipRepo = payslipRepo;
        this.workingDaysCalculator = workingDaysCalculator;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (employeeRepo.count() > 0) {
            log.info("[SEED] Base déjà peuplée ({} employés) — seed ignoré.", employeeRepo.count());
            return;
        }
        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║  [SEED] Initialisation du jeu de données RH (PostgreSQL) ║");
        log.info("╚══════════════════════════════════════════════════════════╝");

        List<Employee> employees = seedEmployees();
        seedLeaveRequests(employees);
        seedPayslips(employees);

        log.info("[SEED] Terminé : {} employés, {} demandes de congé, {} bulletins de paie",
                employeeRepo.count(), leaveRepo.count(), payslipRepo.count());
    }

    private List<Employee> seedEmployees() {
        List<Employee> saved = employeeRepo.saveAll(List.of(
            new Employee(null, "SHR-0001", "Amine", "Ben Salah",
                    "amine.bensalah@demo-hr.local", "Direction des Systèmes d'Information",
                    "Ingénieur DevOps", 3800, 18, 6),
            new Employee(null, "SHR-0002", "Claire", "Dupont",
                    "claire.dupont@demo-hr.local", "Ressources Humaines",
                    "Chargée de recrutement", 3200, 22, 4),
            new Employee(null, "SHR-0003", "Mehdi", "Trabelsi",
                    "mehdi.trabelsi@demo-hr.local", "Développement",
                    "Développeur Full-Stack", 3400, 12, 8),
            new Employee(null, "SHR-0004", "Sophie", "Martin",
                    "sophie.martin@demo-hr.local", "Finance",
                    "Contrôleuse de gestion", 4100, 25, 3),
            new Employee(null, "SHR-0005", "Yassine", "Gharbi",
                    "yassine.gharbi@demo-hr.local", "Développement",
                    "Tech Lead", 4600, 9, 7)
        ));
        saved.forEach(e ->
                log.info("[SEED][EMP] {} — {} ({}) — brut {} €/mois",
                        e.getMatricule(), e.getFullName(), e.getJobTitle(), e.getMonthlyGrossSalary()));
        return saved;
    }

    private void seedLeaveRequests(List<Employee> employees) {
        // resolve IDs from saved entities (DB-assigned, not hardcoded)
        Long id1 = empId(employees, "SHR-0001");
        Long id2 = empId(employees, "SHR-0002");
        Long id3 = empId(employees, "SHR-0003");
        Long id4 = empId(employees, "SHR-0004");
        Long id5 = empId(employees, "SHR-0005");

        addLeave(id1, LeaveType.CP, LocalDate.of(REF_YEAR, 2, 10), LocalDate.of(REF_YEAR, 2, 14),
                "Vacances d'hiver en famille", LeaveStatus.VALIDE, "Validé par le manager");
        addLeave(id1, LeaveType.RTT, LocalDate.of(REF_YEAR, 4, 3), LocalDate.of(REF_YEAR, 4, 3),
                "Pont du week-end", LeaveStatus.VALIDE, null);
        addLeave(id1, LeaveType.CP, LocalDate.of(REF_YEAR, 8, 3), LocalDate.of(REF_YEAR, 8, 21),
                "Congés d'été", LeaveStatus.EN_ATTENTE, null);

        addLeave(id2, LeaveType.CP, LocalDate.of(REF_YEAR, 3, 17), LocalDate.of(REF_YEAR, 3, 21),
                "Semaine de repos", LeaveStatus.VALIDE, null);
        addLeave(id2, LeaveType.SANS_SOLDE, LocalDate.of(REF_YEAR, 5, 26), LocalDate.of(REF_YEAR, 5, 30),
                "Projet personnel", LeaveStatus.REFUSE, "Période de forte activité — refusé");

        addLeave(id3, LeaveType.RTT, LocalDate.of(REF_YEAR, 1, 2), LocalDate.of(REF_YEAR, 1, 3),
                "Récupération", LeaveStatus.VALIDE, null);
        addLeave(id3, LeaveType.CP, LocalDate.of(REF_YEAR, 6, 9), LocalDate.of(REF_YEAR, 6, 13),
                "Mariage d'un proche", LeaveStatus.EN_ATTENTE, null);

        addLeave(id4, LeaveType.CP, LocalDate.of(REF_YEAR, 4, 14), LocalDate.of(REF_YEAR, 4, 25),
                "Vacances de printemps", LeaveStatus.VALIDE, null);

        addLeave(id5, LeaveType.RTT, LocalDate.of(REF_YEAR, 2, 20), LocalDate.of(REF_YEAR, 2, 21),
                "Rendez-vous personnels", LeaveStatus.VALIDE, null);
        addLeave(id5, LeaveType.SANS_SOLDE, LocalDate.of(REF_YEAR, 7, 1), LocalDate.of(REF_YEAR, 7, 4),
                "Déménagement", LeaveStatus.EN_ATTENTE, null);
    }

    private void addLeave(Long employeeId, LeaveType type, LocalDate start, LocalDate end,
                          String comment, LeaveStatus status, String decisionComment) {
        int days = workingDaysCalculator.compute(start, end);
        LeaveRequest lr = new LeaveRequest(
                null, employeeId, type, start, end,
                comment, days, status, start.minusDays(20), decisionComment);
        LeaveRequest saved = leaveRepo.save(lr);
        log.info("[SEED][LEAVE] #{} emp={} {} {}->{} ({} j) [{}]",
                saved.getId(), employeeId, type, start, end, days, status);
    }

    private void seedPayslips(List<Employee> employees) {
        int[] months = {Month.MARCH.getValue(), Month.APRIL.getValue(), Month.MAY.getValue()};
        for (Employee e : employees) {
            for (int month : months) {
                String period = capitalize(Month.of(month).getDisplayName(TextStyle.FULL, FR))
                        + " " + REF_YEAR;
                Payslip p = PayslipFactory.build(
                        null, e.getId(), REF_YEAR, month, period,
                        e.getMonthlyGrossSalary(), month);
                Payslip saved = payslipRepo.save(p);
                log.info("[SEED][PAY] #{} emp={} {} brut={} net={}",
                        saved.getId(), e.getId(), period, saved.getGrossSalary(), saved.getNetSalary());
            }
        }
    }

    private Long empId(List<Employee> employees, String matricule) {
        return employees.stream()
                .filter(e -> matricule.equals(e.getMatricule()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Employé non trouvé : " + matricule))
                .getId();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(FR) + s.substring(1);
    }
}
