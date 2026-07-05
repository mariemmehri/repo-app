package com.example.hr.model;

/**
 * Ligne de bulletin de paie (cotisation, retenue ou gain).
 * amount > 0 = gain ; amount < 0 = retenue/cotisation salariale.
 */
public class PayslipLine {

    private String label;      // ex: "Sécurité sociale - Maladie"
    private String base;       // assiette affichée, ex: "3 200,00 €"
    private String rate;       // taux affiché, ex: "0,75 %"
    private double amount;     // montant signé (EUR)

    public PayslipLine() {
    }

    public PayslipLine(String label, String base, String rate, double amount) {
        this.label = label;
        this.base = base;
        this.rate = rate;
        this.amount = amount;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getBase() { return base; }
    public void setBase(String base) { this.base = base; }

    public String getRate() { return rate; }
    public void setRate(String rate) { this.rate = rate; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}
