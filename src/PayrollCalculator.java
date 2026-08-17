import java.util.*;
public final class PayrollCalculator {
    public static class Result { public final Map<String,Double> earnings=new LinkedHashMap<>(), deductions=new LinkedHashMap<>(); public double gross,net; }
    public static Result calculate(double basic, PayrollSettings s, double manualBonus, double overtimeHours, double allowance) { Result r=new Result();r.earnings.put("Basic Salary",Money.round(basic)); double rate=0; for(Map.Entry<String,PayrollSettings.Component>x:s.getComponents().entrySet()){String n=x.getKey();PayrollSettings.Component c=x.getValue(); if(n.equals("Overtime Rate")){if(c.enabled)rate=c.value;continue;}if(!c.enabled)continue; double v=Money.round(c.percentage?basic*c.value/100:c.value); if(n.contains("PF")||n.contains("ESI")||n.contains("Tax")||n.contains("Fund")||n.contains("Deduction")||n.equals("Loan")||n.equals("Advance Salary"))r.deductions.put(n,v); else if(!n.equals("Bonus")&&!n.equals("Attendance Bonus"))r.earnings.put(n,v); } if(manualBonus!=0)r.earnings.put("Bonus",Money.round(manualBonus));if(overtimeHours!=0)r.earnings.put("Overtime Amount",Money.round(overtimeHours*rate));if(allowance!=0)r.earnings.put("Allowance",Money.round(allowance));finalizeTotals(r);return r; }
    public static void finalizeTotals(Result result) { result.gross=0;result.net=0;for(double value:result.earnings.values())result.gross+=value;for(double value:result.deductions.values())result.net-=value;result.gross=Money.round(result.gross);result.net=Money.round(result.net+result.gross); }
    public static double effectiveOtRate(double proratedMonthlyBasic,double standardMonthlyWorkingHours){return standardMonthlyWorkingHours<=0?0:(proratedMonthlyBasic/standardMonthlyWorkingHours)*PayrollRulesStore.otMultiplier();}
    public static double monthlyEpf(double monthlyBasic){double epfWage=CompanyPolicyStore.restrictEpfToCeiling()?Math.min(monthlyBasic,15000.0):monthlyBasic,epfRate=CompanyPolicyStore.hasDeduction("EPF")?CompanyPolicyStore.deduction("EPF"):PayrollRulesStore.epfRate();return Math.round(epfWage*epfRate/100.0);}
    public static double monthlyEpf(Employee employee,double monthlyBasic){double epfWage=employee!=null&&employee.isRestrictEpfCeiling()?Math.min(monthlyBasic,15000.0):monthlyBasic,epfRate=CompanyPolicyStore.hasDeduction("EPF")?CompanyPolicyStore.deduction("EPF"):PayrollRulesStore.epfRate();return Math.round(epfWage*epfRate/100.0);}
    public static double monthlyProfessionalTax(double monthlyGross){if(monthlyGross<25000.0)return 0.0;double configuredPT=CompanyPolicyStore.hasDeduction("PT")?CompanyPolicyStore.deduction("PT"):PayrollRulesStore.ptAmount();return Math.round(CompanyPolicyStore.deductionPercentage("PT")?monthlyGross*configuredPT/100.0:configuredPT);}
    public static double annualProfessionalTax(double annualGross){return Math.round(monthlyProfessionalTax(annualGross/12.0)*12.0);}
    public static Result applyAttendance(Result base, AttendanceRecord attendance, AttendanceSettings settings, double basic) {
        if (attendance == null || attendance.workingDays <= 0) return base;
        double originalGross=base.gross;
        double earned=originalGross/attendance.workingDays*attendance.payableDays();
        base.earnings.clear(); base.earnings.put("Prorated Salary ("+String.format("%.1f",attendance.payableDays())+" days)",earned);
        double proratedMonthlyBasic=basic/attendance.workingDays*attendance.payableDays();
        double standardMonthlyWorkingHours=attendance.workingDays*PayrollRulesStore.workingHours();
        double effectiveOtRate=effectiveOtRate(proratedMonthlyBasic,standardMonthlyWorkingHours);
        double overtimePay=attendance.overtimeHours*effectiveOtRate;
        double otherEarnings=attendance.otherHours*effectiveOtRate;
        if(overtimePay!=0)base.earnings.put("Overtime Pay",overtimePay);
        if(otherEarnings!=0)base.earnings.put("Other Earnings",otherEarnings);
        if(settings.attendanceBonusEnabled && attendance.attendancePercentage()>=settings.attendanceThresholdPercent){double bonus=settings.bonusIsPercentage?basic*settings.attendanceBonusPercent/100:settings.attendanceBonusAmount;base.earnings.put("Attendance Bonus",bonus);}
        base.gross=0;base.net=0;for(double v:base.earnings.values())base.gross+=v;for(double v:base.deductions.values())base.net-=v;base.gross=Money.round(base.gross);base.net=Money.round(base.net+base.gross);return base;
    }
    private PayrollCalculator() { }
}
