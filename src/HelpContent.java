/** Single maintainable source for in-application Help content and support placeholders. */
public final class HelpContent {
    private HelpContent() { }
    public static final String[][] FAQS = {
        {"How do I add an employee?", "Open Master Data, enter the Master Data password, select + Add Employee, complete the employee details, and select Save."},
        {"How do I edit or deactivate an employee?", "Open Master Data and use the Edit button in the final Actions column. Update the details or select Ex-Employee as the status, then save."},
        {"How do I find or export employee details?", "Use the search box in Master Data to find an employee. Use Export Excel for an employee-data spreadsheet, or Import Excel to import validated employee data."},
        {"What does the Employee tab show?", "The Employee tab is a summary view of Employee ID, Employee Name, Company Name, Department, Designation, Phone Number, and Email ID."},
        {"How do I save attendance?", "Open Attendance, select the financial year and month, enter working days and attendance, then select Save [Month] Attendance."},
        {"Can I import or export attendance?", "Yes. In the selected Attendance month, use Import Attendance to import data or Export Excel to download the attendance sheet."},
        {"How is attendance used in salary calculation?", "Saved attendance determines payable days. Salary and payslip calculations use the saved attendance for the selected period."},
        {"How are CL and EL balances calculated?", "Used CL and EL are deducted from their own leave entitlement. Unused balances carry forward only up to their separately configured limits."},
        {"How do I update Cost to Company (CTC)?", "Open Cost to Company (CTC), select an employee, select Edit, update values, use Calculate Allowances & Deductions if required, then select Save CTC."},
        {"How do I change Earnings and Allowances?", "Open Earnings & Allowances, select the financial year and month, select Edit, make changes, then select Save. Excel import and export are also available."},
        {"Why are CTC changes not appearing in Earnings and Allowances?", "Save the CTC change, then select Refresh current module. If the value is still not shown, switch to another tab and return to Earnings & Allowances."},
        {"How do I update deductions?", "Open Deductions, select the financial year and month, select Edit, update the permitted values, and select Save."},
        {"How do I calculate salary?", "Open Salary, select the required financial year and month, then use the available salary calculation actions for the selected employees."},
        {"How do I generate a payslip?", "Open Payslip, select the employee and payroll period, then generate or download the payslip using the available actions."},
        {"What is shown in the TDS report?", "Reports > TDS shows Employee ID, Employee Name, PAN, Gross Salary, and TDS / Income Tax. PAN comes from Master Data, Gross Salary from Salary, and TDS from Deductions."},
        {"How do I use Reports?", "Open Reports, select ESIC, EPF, TDS, or Bank Payment, choose the financial year and month, search if required, then select Export to Excel."},
        {"Why is a saved value not visible?", "Select Refresh current module at the top-right. If it is still not shown, switch to another tab and return. Do not refresh while you have unsaved edits."},
        {"How do I change settings?", "Open Settings and use the relevant tab: Company Details, Allowance Defaults, OT & Other Earnings, Attendance & Leave Rules, Deduction Rules, Revised Salary Settings, or Password management."},
        {"How do I change my login password?", "Open Settings > Password management > Application Login. Login passwords must contain 8 to 16 characters."},
        {"What should I do if I forget my username or passwords?", "Select “Forgot Username or Password?” on the Login page and provide the displayed Installation ID to YASL support. After verification, you will receive a one-time recovery code. Enter the recovery code to reset your login and Master Data credentials. Your payroll and employee data will remain safely stored and will not be deleted. After logging in with the temporary credentials, change them from Settings."},
        {"How do I set the Master Data password?", "Open Settings > Password management > Master Data Login. Master Data passwords must contain exactly 6 characters."},
        {"How do I back up payroll data?", "Open Backup & Restore, set the backup folder if needed, and select Backup Now."},
        {"How do I restore payroll data?", "Open Backup & Restore, select Import & Restore Backup, choose a payroll backup ZIP, and confirm. The application will ask you to restart after restoration."},
        {"What should I do if the software is not activated?", "Check the internet connection and enter the issued license key. If activation still fails, contact the company."},
        {"How do I move a license to another computer?", "Ask the company to revoke the old computer activation. Then install the application on the replacement computer and activate it with the same license key."},
        {"How do I contact software support?", "Open the Contact Details tab in this Help module and email the company."}
    };
    public static final String[][] CONTACT_DETAILS = {{"COMPANY NAME", Branding.COMPANY_NAME}, {"COMPANY ADDRESS", Branding.COMPANY_ADDRESS}, {"EMAIL", Branding.COMPANY_EMAIL}};
    public static final String CONTACT_EMAIL_NOTE = "Response within 24 hours.";
}
