package com.example.hr.model;

/**
 * Employé fictif de la démo RH (inspiré Sopra HR4YOU).
 * Aucune donnée réelle — jeu de démonstration uniquement.
 */
public class Employee {

    private Long id;
    private String matricule;     // ex: SHR-0001
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private String jobTitle;
    private double monthlyGrossSalary;   // salaire brut mensuel de référence (EUR)
    private int leaveBalanceCp;          // solde congés payés (jours)
    private int leaveBalanceRtt;         // solde RTT (jours)

    public Employee() {
    }

    public Employee(Long id, String matricule, String firstName, String lastName,
                    String email, String department, String jobTitle,
                    double monthlyGrossSalary, int leaveBalanceCp, int leaveBalanceRtt) {
        this.id = id;
        this.matricule = matricule;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.department = department;
        this.jobTitle = jobTitle;
        this.monthlyGrossSalary = monthlyGrossSalary;
        this.leaveBalanceCp = leaveBalanceCp;
        this.leaveBalanceRtt = leaveBalanceRtt;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public double getMonthlyGrossSalary() { return monthlyGrossSalary; }
    public void setMonthlyGrossSalary(double monthlyGrossSalary) { this.monthlyGrossSalary = monthlyGrossSalary; }

    public int getLeaveBalanceCp() { return leaveBalanceCp; }
    public void setLeaveBalanceCp(int leaveBalanceCp) { this.leaveBalanceCp = leaveBalanceCp; }

    public int getLeaveBalanceRtt() { return leaveBalanceRtt; }
    public void setLeaveBalanceRtt(int leaveBalanceRtt) { this.leaveBalanceRtt = leaveBalanceRtt; }
}
