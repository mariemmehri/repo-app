package com.example.hr.service;

import com.example.hr.model.Payslip;
import com.example.hr.model.PayslipLine;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Construit un bulletin de paie factice mais réaliste à partir d'un salaire brut.
 * Les taux sont approximés (barème simplifié salarié France) — données de démo.
 */
public final class PayslipFactory {

    private static final Locale FR = Locale.FRANCE;

    // Taux de cotisations salariales simplifiés (part salariale)
    private static final double RATE_SECU_MALADIE = 0.0075;
    private static final double RATE_VIEILLESSE   = 0.0690;
    private static final double RATE_RETRAITE_COMP = 0.0387;
    private static final double RATE_CSG_CRDS     = 0.0970; // sur ~98.25% du brut
    private static final double RATE_INCOME_TAX   = 0.0700; // prélèvement à la source (taux neutre démo)

    private PayslipFactory() {
    }

    public static Payslip build(Long id, Long employeeId, int year, int month,
                                String periodLabel, double grossSalary,
                                int monthIndexInYear) {
        Payslip p = new Payslip();
        p.setId(id);
        p.setEmployeeId(employeeId);
        p.setYear(year);
        p.setMonth(month);
        p.setPeriod(periodLabel);
        p.setGrossSalary(round(grossSalary));

        double csgBase = grossSalary * 0.9825;

        PayslipLine secu   = line("Sécurité sociale - Maladie", grossSalary, RATE_SECU_MALADIE);
        PayslipLine vieil  = line("Assurance vieillesse", grossSalary, RATE_VIEILLESSE);
        PayslipLine retc   = line("Retraite complémentaire", grossSalary, RATE_RETRAITE_COMP);
        PayslipLine csg    = line("CSG déductible / CRDS", csgBase, RATE_CSG_CRDS);

        p.getLines().add(new PayslipLine("Salaire de base", null, null, round(grossSalary)));
        p.getLines().add(secu);
        p.getLines().add(vieil);
        p.getLines().add(retc);
        p.getLines().add(csg);

        double totalContributions = Math.abs(secu.getAmount())
                + Math.abs(vieil.getAmount())
                + Math.abs(retc.getAmount())
                + Math.abs(csg.getAmount());

        double netBeforeTax = grossSalary - totalContributions;
        double incomeTax = netBeforeTax * RATE_INCOME_TAX;
        double net = netBeforeTax - incomeTax;

        p.getLines().add(new PayslipLine("Net imposable", null, null, round(netBeforeTax)));
        p.getLines().add(line("Prélèvement à la source", netBeforeTax, RATE_INCOME_TAX));

        p.setTotalContributions(round(totalContributions));
        p.setNetBeforeTax(round(netBeforeTax));
        p.setIncomeTax(round(incomeTax));
        p.setNetSalary(round(net));

        // Cumuls année : mois écoulés * valeurs (monthIndexInYear = numéro du mois cumulé, 1-based)
        p.setCumulativeGross(round(grossSalary * monthIndexInYear));
        p.setCumulativeNet(round(net * monthIndexInYear));
        p.setCumulativeTax(round(incomeTax * monthIndexInYear));

        return p;
    }

    private static PayslipLine line(String label, double base, double rate) {
        double amount = -(base * rate);
        return new PayslipLine(label, formatEur(base), formatPct(rate), round(amount));
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static String formatEur(double v) {
        NumberFormat nf = NumberFormat.getNumberInstance(FR);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(v) + " €";
    }

    private static String formatPct(double rate) {
        NumberFormat nf = NumberFormat.getNumberInstance(FR);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(3);
        return nf.format(rate * 100) + " %";
    }
}
