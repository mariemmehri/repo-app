package com.example.todo.hr.service;

import com.example.todo.hr.model.Employee;
import com.example.todo.hr.model.LeaveRequest;
import com.example.todo.hr.model.LeaveStatus;
import com.example.todo.hr.model.LeaveType;
import com.example.todo.hr.model.Payslip;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Dépôt de données EN MÉMOIRE (aucune base, aucun volume persistant).
 * Même philosophie que l'app todo d'origine : les données sont recréées
 * au démarrage du pod et perdues au redémarrage — parfait pour valider un
 * déploiement sans introduire de dépendance externe qui casserait la pipeline.
 *
 * Jeu de démonstration :
 *   - 5 employés fictifs
 *   - un historique de demandes de congé
 *   - 3 bulletins de paie par employé
 */
@Service
public class HrDataStore {

    private static final Logger log = LoggerFactory.getLogger(HrDataStore.class);
    private static final Locale FR = Locale.FRANCE;

    /** Année de référence des données de démo (stable, indépendante de la date réelle). */
    private static final int REF_YEAR = 2026;

    private final List<Employee> employees = new ArrayList<>();
    private final List<LeaveRequest> leaveRequests = new ArrayList<>();
    private final List<Payslip> payslips = new ArrayList<>();

    private final AtomicLong leaveSeq = new AtomicLong();
    private final AtomicLong payslipSeq = new AtomicLong();

    private final WorkingDaysCalculator workingDaysCalculator;

    public HrDataStore(WorkingDaysCalculator workingDaysCalculator) {
        this.workingDaysCalculator = workingDaysCalculator;
    }

    @PostConstruct
    public void seed() {
        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║  [SEED] Initialisation du jeu de données RH (in-memory)  ║");
        log.info("╚══════════════════════════════════════════════════════════╝");

        seedEmployees();
        seedLeaveRequests();
        seedPayslips();

        log.info("[SEED] Terminé : {} employés, {} demandes de congé, {} bulletins de paie",
                employees.size(), leaveRequests.size(), payslips.size());
    }

    private void seedEmployees() {
        employees.add(new Employee(1L, "SHR-0001", "Amine", "Ben Salah",
                "amine.bensalah@demo-hr.local", "Direction des Systèmes d'Information",
                "Ingénieur DevOps", 3800, 18, 6));
        employees.add(new Employee(2L, "SHR-0002", "Claire", "Dupont",
                "claire.dupont@demo-hr.local", "Ressources Humaines",
                "Chargée de recrutement", 3200, 22, 4));
        employees.add(new Employee(3L, "SHR-0003", "Mehdi", "Trabelsi",
                "mehdi.trabelsi@demo-hr.local", "Développement",
                "Développeur Full-Stack", 3400, 12, 8));
        employees.add(new Employee(4L, "SHR-0004", "Sophie", "Martin",
                "sophie.martin@demo-hr.local", "Finance",
                "Contrôleuse de gestion", 4100, 25, 3));
        employees.add(new Employee(5L, "SHR-0005", "Yassine", "Gharbi",
                "yassine.gharbi@demo-hr.local", "Développement",
                "Tech Lead", 4600, 9, 7));

        employees.forEach(e ->
                log.info("[SEED][EMP] {} — {} ({}) — brut {} €/mois",
                        e.getMatricule(), e.getFullName(), e.getJobTitle(), e.getMonthlyGrossSalary()));
    }

    private void seedLeaveRequests() {
        addLeave(1L, LeaveType.CP, LocalDate.of(REF_YEAR, 2, 10), LocalDate.of(REF_YEAR, 2, 14),
                "Vacances d'hiver en famille", LeaveStatus.VALIDE, "Validé par le manager");
        addLeave(1L, LeaveType.RTT, LocalDate.of(REF_YEAR, 4, 3), LocalDate.of(REF_YEAR, 4, 3),
                "Pont du week-end", LeaveStatus.VALIDE, null);
        addLeave(1L, LeaveType.CP, LocalDate.of(REF_YEAR, 8, 3), LocalDate.of(REF_YEAR, 8, 21),
                "Congés d'été", LeaveStatus.EN_ATTENTE, null);

        addLeave(2L, LeaveType.CP, LocalDate.of(REF_YEAR, 3, 17), LocalDate.of(REF_YEAR, 3, 21),
                "Semaine de repos", LeaveStatus.VALIDE, null);
        addLeave(2L, LeaveType.SANS_SOLDE, LocalDate.of(REF_YEAR, 5, 26), LocalDate.of(REF_YEAR, 5, 30),
                "Projet personnel", LeaveStatus.REFUSE, "Période de forte activité — refusé");

        addLeave(3L, LeaveType.RTT, LocalDate.of(REF_YEAR, 1, 2), LocalDate.of(REF_YEAR, 1, 3),
                "Récupération", LeaveStatus.VALIDE, null);
        addLeave(3L, LeaveType.CP, LocalDate.of(REF_YEAR, 6, 9), LocalDate.of(REF_YEAR, 6, 13),
                "Mariage d'un proche", LeaveStatus.EN_ATTENTE, null);

        addLeave(4L, LeaveType.CP, LocalDate.of(REF_YEAR, 4, 14), LocalDate.of(REF_YEAR, 4, 25),
                "Vacances de printemps", LeaveStatus.VALIDE, null);

        addLeave(5L, LeaveType.RTT, LocalDate.of(REF_YEAR, 2, 20), LocalDate.of(REF_YEAR, 2, 21),
                "Rendez-vous personnels", LeaveStatus.VALIDE, null);
        addLeave(5L, LeaveType.SANS_SOLDE, LocalDate.of(REF_YEAR, 7, 1), LocalDate.of(REF_YEAR, 7, 4),
                "Déménagement", LeaveStatus.EN_ATTENTE, null);
    }

    private void addLeave(Long employeeId, LeaveType type, LocalDate start, LocalDate end,
                          String comment, LeaveStatus status, String decisionComment) {
        int days = workingDaysCalculator.compute(start, end);
        LeaveRequest lr = new LeaveRequest(
                leaveSeq.incrementAndGet(), employeeId, type, start, end,
                comment, days, status, start.minusDays(20), decisionComment);
        leaveRequests.add(lr);
        log.info("[SEED][LEAVE] #{} emp={} {} {}->{} ({} j) [{}]",
                lr.getId(), employeeId, type, start, end, days, status);
    }

    private void seedPayslips() {
        // 3 bulletins par employé : les 3 derniers mois de l'année de référence
        int[] months = {Month.MARCH.getValue(), Month.APRIL.getValue(), Month.MAY.getValue()};
        for (Employee e : employees) {
            for (int i = 0; i < months.length; i++) {
                int month = months[i];
                String period = capitalize(Month.of(month).getDisplayName(TextStyle.FULL, FR))
                        + " " + REF_YEAR;
                Payslip p = PayslipFactory.build(
                        payslipSeq.incrementAndGet(), e.getId(), REF_YEAR, month, period,
                        e.getMonthlyGrossSalary(), month);
                payslips.add(p);
                log.info("[SEED][PAY] #{} emp={} {} brut={} net={}",
                        p.getId(), e.getId(), period, p.getGrossSalary(), p.getNetSalary());
            }
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(FR) + s.substring(1);
    }

    // ─── Accès lecture ────────────────────────────────────────────────

    public List<Employee> getEmployees() {
        return employees;
    }

    public Employee findEmployee(Long id) {
        return employees.stream().filter(e -> e.getId().equals(id)).findFirst().orElse(null);
    }

    public List<LeaveRequest> getLeaveRequests() {
        return leaveRequests;
    }

    public List<LeaveRequest> getLeaveRequestsByEmployee(Long employeeId) {
        return leaveRequests.stream()
                .filter(lr -> lr.getEmployeeId().equals(employeeId))
                .toList();
    }

    public LeaveRequest findLeave(Long id) {
        return leaveRequests.stream().filter(lr -> lr.getId().equals(id)).findFirst().orElse(null);
    }

    public List<Payslip> getPayslips() {
        return payslips;
    }

    public List<Payslip> getPayslipsByEmployee(Long employeeId) {
        return payslips.stream()
                .filter(p -> p.getEmployeeId().equals(employeeId))
                .toList();
    }

    public Payslip findPayslip(Long id) {
        return payslips.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
    }

    // ─── Écriture ─────────────────────────────────────────────────────

    public LeaveRequest addLeaveRequest(LeaveRequest lr) {
        lr.setId(leaveSeq.incrementAndGet());
        leaveRequests.add(lr);
        return lr;
    }
}
