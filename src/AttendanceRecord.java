import java.time.YearMonth;
/** Monthly attendance values used directly when a payslip is calculated. */
public class AttendanceRecord {
    public String employeeId, month;
    public double workingDays;
    public int weeklyOffDays, publicHolidays, presentDays, lateArrivals, earlyDepartures;
    /** Leave and absence values support half-days and must never be truncated on save. */
    public double absentDays, halfDays, paidLeaveDays, unpaidLeaveDays, daysPayable;
    public double overtimeHours, otherHours;
    public double payableDays() { return presentDays + paidLeaveDays + (halfDays * 0.5); }
    public double attendancePercentage() { return workingDays == 0 ? 0 : (payableDays() / workingDays) * 100; }
    public static AttendanceRecord blank(String employeeId, String month) { AttendanceRecord r=new AttendanceRecord();r.employeeId=employeeId;r.month=month;r.workingDays=YearMonth.parse(month).lengthOfMonth();return r; }
}
