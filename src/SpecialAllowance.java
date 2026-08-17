import java.time.YearMonth;

/** Calculates the Special Allowance residual from the employee's monthly CTC structure. */
public final class SpecialAllowance {
    private SpecialAllowance() { }

    /**
     * Employee Monthly CTC is the configured CTC target when present.  For legacy
     * profiles, the same target is reconstructed from the base components and the
     * global Special Allowance setting, so no per-employee Special Base is needed.
     */
    public static double base(Employee employee, YearMonth period) {
        double baseBasic = SalaryRevisionStore.basicFor(employee, period);
        if (baseBasic <= 0) return 0;
        double hra = Math.round(baseBasic * PayrollRulesStore.hraPercent() / 100.0);
        double conveyance = baseComponent("Conveyance Allowance", baseBasic);
        double medical = baseComponent("Medical Allowance", baseBasic);
        double fixed = baseComponent("Fixed Allowance", baseBasic);
        double components = baseBasic + hra + conveyance + medical + fixed;
        double configuredSpecial = baseComponent("Special Allowance", baseBasic);
        double target = CTCStore.get(employee.getId()).basic;
        if (target <= 0) target = components + configuredSpecial;
        return Money.round(Math.max(0, target - components));
    }

    public static double earned(Employee employee, YearMonth period, double workingDays, double daysPayable, double basicForMonth) {
        if (basicForMonth <= 0 || workingDays <= 0 || daysPayable <= 0) return 0;
        double configured = CompanyPolicyStore.allowance("Special Allowance");
        if (configured > 0 && CompanyPolicyStore.percentage("Special Allowance")) return Money.round(basicForMonth * configured / 100.0);
        return Math.round((base(employee, period) / workingDays) * daysPayable);
    }

    private static double baseComponent(String name, double baseBasic) {
        double value = CompanyPolicyStore.allowance(name);
        if (value <= 0) return 0;
        return CompanyPolicyStore.percentage(name) ? Money.round(value * baseBasic / 100.0) : value;
    }
}
