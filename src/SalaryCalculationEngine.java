import java.time.YearMonth;
import java.sql.*;

public final class SalaryCalculationEngine {
    public static PayrollCalculator.Result calculate(Employee employee, YearMonth period) {
        AttendanceDAO attendance = new AttendanceDAO();
        AttendanceRecord record = attendance.load(employee.getId(), period.toString());
        double workingDays = attendance.workingDays(period.toString());
        if (workingDays <= 0) workingDays = record.workingDays;
        if (workingDays <= 0) workingDays = period.lengthOfMonth();
        AttendanceSettings leaveSettings = new AttendanceSettingsDAO().load();
        double payableDays = attendance.hasSavedAttendance(employee.getId(), period.toString()) ? LeaveBalanceService.payableDays(employee, period, workingDays, record.absentDays, record.paidLeaveDays, record.unpaidLeaveDays, leaveSettings) : workingDays;
        double ratio = Math.min(1.0, payableDays / workingDays);
        double basic = Money.round(SalaryRevisionStore.basicFor(employee, period) * ratio);
        MonthlyEarningsStore.Value saved = MonthlyEarningsStore.get(employee.getId(), period.toString());
        boolean fullAttendance = payableDays >= workingDays && workingDays > 0;
        CTCStore.Value ctc = CTCStore.get(employee.getId());
        double hra = ctc.hraOverride ? Money.round(basic * ctc.hraOverridePercent / 100.0) : allowance("HRA", basic, 40, true);
        double bonus = employee.isWagesStructure() && fullAttendance ? allowance("Attendance Bonus", basic, PayrollRulesStore.attendanceBonus(), false) : 0.0;
        double conveyance = allowance("Conveyance Allowance", basic, PayrollRulesStore.conveyance(), false);
        double performance = allowance("Performance Pay", basic, 0.0, false);
        double medical = allowance("Medical Allowance", basic, PayrollRulesStore.medical(), false);
        double special = allowance("Special Allowance", basic, 15, true);
        double fixed = allowance("Fixed Allowance", basic, 0.0, false);
        double other;
        if (saved != null) {
            if (saved.basic > 0) basic = saved.basic;
            hra = saved.hra; bonus = employee.isWagesStructure() && fullAttendance ? saved.attendance : 0.0; conveyance = saved.conveyance;
            performance = saved.performance; medical = saved.medical; special = saved.special; fixed = saved.fixed;
            other = employee.isWagesStructure() ? Money.round(saved.overtime + saved.reimbursements) : 0.0;
        } else {
            double hourlyRate = PayrollCalculator.effectiveOtRate(basic, workingDays * PayrollRulesStore.workingHours());
            other = employee.isWagesStructure() ? Money.round(hourlyRate * Math.max(0, record.overtimeHours + record.otherHours)) : 0.0;
        }
        double allowances = Money.round(hra + bonus + conveyance + performance + medical + special + fixed);
        double gross = Money.round(basic + allowances + other);
        DeductionStore.Value deductions = DeductionStore.get(employee.getId(), period.toString());
        PayrollCalculator.Result result = new PayrollCalculator.Result();
        result.earnings.put("Monthly Basic Pay", basic);
        result.earnings.put("Total Allowances", allowances);
        result.earnings.put("Total Other Earnings", other);
        if (employee.isWagesStructure()) result.earnings.put("OT Pay", saved == null ? other : saved.overtime);
        result.deductions.put("EPF", deductions.epf);
        result.deductions.put("PT", deductions.pt);
        result.deductions.put("ESIC", deductions.esic);
        result.deductions.put("Income Tax / TDS", deductions.tds);
        result.deductions.put("Total Deductions", Money.round(deductions.total()));
        result.gross = gross;
        result.net = Money.round(gross - deductions.total());
        persist(employee, period, basic, allowances, other, gross, deductions.total(), result.net);
        return result;
    }

    private static void persist(Employee e,YearMonth p,double basic,double allowances,double other,double gross,double deductions,double net){
        try(Connection c=DBConnection.getConnection();PreparedStatement s=c==null?null:c.prepareStatement("INSERT INTO salary(employee_id,employee_name,month,fy,basic_pay,total_allowances,total_other_earnings,gross_salary,total_deductions,net_salary) SELECT ?,employee_name,?,?,?,?,?,?,?,? FROM employee_master_data WHERE employee_id=? AND is_deleted=0 ON CONFLICT(employee_id,month,fy) DO UPDATE SET basic_pay=excluded.basic_pay,total_allowances=excluded.total_allowances,total_other_earnings=excluded.total_other_earnings,gross_salary=excluded.gross_salary,total_deductions=excluded.total_deductions,net_salary=excluded.net_salary")){
            if(s==null)return;int i=1;s.setString(i++,e.getId());s.setString(i++,p.getMonth().toString().substring(0,3));s.setString(i++,FinancialYear.text(p.getMonthValue()>=4?p.getYear():p.getYear()-1));for(double n:new double[]{basic,allowances,other,gross,deductions,net})s.setDouble(i++,Money.round(n));s.setString(i,e.getId());s.executeUpdate();
        }catch(SQLException ignored){}
    }

    private static double allowance(String name, double basic, double fallback, boolean fallbackPercentage) {
        boolean configured = CompanyPolicyStore.hasAllowance(name);
        double value = configured ? CompanyPolicyStore.allowance(name) : fallback;
        boolean percentage = configured ? CompanyPolicyStore.percentage(name) : fallbackPercentage;
        return Money.round(percentage ? basic * value / 100.0 : value);
    }

    private SalaryCalculationEngine() { }
}
