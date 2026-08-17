public final class TaxCalculator {
    public static TaxResult calculateNewRegimeTax(double taxableIncome) {
        double income = Math.max(0, taxableIncome), slabTax = slabTax(income);
        double rebate = income <= 700000 ? slabTax : 0;
        double taxPayable = Math.max(0, slabTax - rebate);
        double surcharge = Money.round(taxPayable * getSurchargeRate(income));
        double taxPlusSurcharge = Money.round(taxPayable + surcharge);
        double cess = Money.round(taxPlusSurcharge * .04);
        return new TaxResult(slabTax, rebate, taxPayable, surcharge, cess, Money.round(taxPlusSurcharge + cess));
    }

    private static double slabTax(double income) {
        double tax = 0;
        if (income > 300000) tax += Math.min(income - 300000, 400000) * .05;
        if (income > 700000) tax += Math.min(income - 700000, 300000) * .10;
        if (income > 1000000) tax += Math.min(income - 1000000, 200000) * .15;
        if (income > 1200000) tax += Math.min(income - 1200000, 300000) * .20;
        if (income > 1500000) tax += (income - 1500000) * .30;
        return Money.round(tax);
    }

    private static double getSurchargeRate(double taxableIncome) { return taxableIncome > 10000000 ? .15 : 0.0; }

    public record TaxResult(double slabTax, double rebate87A, double taxPayable, double surcharge, double cess, double totalTaxPayable) { }
    private TaxCalculator() { }
}
