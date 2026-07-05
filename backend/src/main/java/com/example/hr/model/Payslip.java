package com.example.hr.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Bulletin de paie mensuel (données factices réalistes).
 */
public class Payslip {

    private Long id;
    private Long employeeId;
    private int year;
    private int month;              // 1-12
    private String period;          // ex: "Mars 2026"

    private double grossSalary;     // salaire brut
    private double totalContributions; // total cotisations salariales (valeur positive)
    private double netSalary;       // net à payer
    private double netBeforeTax;    // net avant impôt
    private double incomeTax;       // prélèvement à la source

    // Cumuls annuels (année en cours)
    private double cumulativeGross;
    private double cumulativeNet;
    private double cumulativeTax;

    private List<PayslipLine> lines = new ArrayList<>();

    public Payslip() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public double getGrossSalary() { return grossSalary; }
    public void setGrossSalary(double grossSalary) { this.grossSalary = grossSalary; }

    public double getTotalContributions() { return totalContributions; }
    public void setTotalContributions(double totalContributions) { this.totalContributions = totalContributions; }

    public double getNetSalary() { return netSalary; }
    public void setNetSalary(double netSalary) { this.netSalary = netSalary; }

    public double getNetBeforeTax() { return netBeforeTax; }
    public void setNetBeforeTax(double netBeforeTax) { this.netBeforeTax = netBeforeTax; }

    public double getIncomeTax() { return incomeTax; }
    public void setIncomeTax(double incomeTax) { this.incomeTax = incomeTax; }

    public double getCumulativeGross() { return cumulativeGross; }
    public void setCumulativeGross(double cumulativeGross) { this.cumulativeGross = cumulativeGross; }

    public double getCumulativeNet() { return cumulativeNet; }
    public void setCumulativeNet(double cumulativeNet) { this.cumulativeNet = cumulativeNet; }

    public double getCumulativeTax() { return cumulativeTax; }
    public void setCumulativeTax(double cumulativeTax) { this.cumulativeTax = cumulativeTax; }

    public List<PayslipLine> getLines() { return lines; }
    public void setLines(List<PayslipLine> lines) { this.lines = lines; }
}
