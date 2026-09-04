import java.time.YearMonth;

/** Derives leave carry-forward from immutable monthly attendance rows; no duplicate balance ledger is needed. */
public final class LeaveBalanceService {
    private LeaveBalanceService() { }
    public static double carried(double previousCarry, double monthlyEntitlement, double used, double maximum) {
        return Money.round(Math.max(0, Math.min(Math.max(0, maximum), Math.max(0, previousCarry) + Math.max(0, monthlyEntitlement) - Math.max(0, used))));
    }
    public static double carryForward(Employee employee, YearMonth period, boolean cl, AttendanceSettings settings) {
        AttendanceDAO attendance = new AttendanceDAO(); EmployeeDAO employees = new EmployeeDAO(); double carry = 0;
        int start = period.getMonthValue() < 4 ? period.getYear() - 1 : period.getYear();
        for (YearMonth prior : FinancialYear.months(start)) {
            if (!prior.isBefore(period)) break;
            if (!employees.isEligibleForMonth(employee, prior) || !attendance.hasSavedAttendance(employee.getId(), prior.toString())) continue;
            AttendanceRecord record = attendance.load(employee.getId(), prior.toString());
            carry = carried(carry, cl ? settings.casualLeaveLimit : settings.earnedLeaveLimit, cl ? record.paidLeaveDays : record.unpaidLeaveDays, cl ? settings.maximumClCarryForward : settings.maximumElCarryForward);
        }
        return carry;
    }
    public static double payableDays(Employee employee, YearMonth period, double workingDays, double absent, double clUsed, double elUsed, AttendanceSettings settings) { return LeaveLedgerService.payableDays(employee,period,workingDays,absent,clUsed,elUsed,settings); }
}
