import java.time.YearMonth;

/** Converts the employee's saved annual CTC deductions into attendance-adjusted monthly deductions. */
public final class CtcDeductionCalculator {
    public static DeductionStore.Value forMonth(Employee employee, YearMonth period, double attendanceRatio) {
        CTCStore.Value ctc = CTCStore.get(employee.getId());
        double ratio = Math.max(0.0, Math.min(1.0, attendanceRatio));
        DeductionStore.Value value = new DeductionStore.Value();
        value.epf = monthly(ctc.employeeEpf, ratio);
        value.pt = monthly(ctc.pt, ratio);
        value.esic = monthly(ctc.esic, ratio);
        value.tds = monthly(ctc.incomeTax, ratio);
        return value;
    }

    private static double monthly(double annualAmount, double attendanceRatio) {
        return annualAmount <= 0.0 ? 0.0 : Money.round(annualAmount / 12.0 * attendanceRatio);
    }

    private CtcDeductionCalculator() { }
}
