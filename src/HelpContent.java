/** Single maintainable source for in-application Help content and support placeholders. */
public final class HelpContent {
    private HelpContent() { }
    public static final String[][] FAQS = {
        {"How do I add an employee?", "Open Master Data, authenticate, then select Add Employee and save the completed record."},
        {"How is attendance used in salary calculation?", "Saved attendance determines payable days, which payroll uses when calculating salary."},
        {"How are CL and EL balances calculated?", "Monthly CL and EL usage is deducted from its own entitlement. Unused saved leave carries forward within the financial year only up to its configured maximum."},
        {"How do I generate a payslip?", "Open Payslip, select the employee and period, then generate or download the payslip."},
        {"How do I back up payroll data?", "Open Backup & Restore from the sidebar and create a backup before making major changes."},
        {"How do I restore payroll data?", "Open Backup & Restore, choose a verified backup, and follow the restore prompts."},
        {"What should I do if the software is not activated?", "Contact software support and provide your organisation and activation details."},
        {"How do I contact software support?", "Open the Contact Details tab in this Help module and use the details supplied by your administrator."}
    };
    public static final String[][] CONTACT_DETAILS = {
        {"Support/Company Name", "[To be provided]"}, {"Contact Person", "[To be provided]"}, {"Phone Number", "[To be provided]"},
        {"Email Address", "[To be provided]"}, {"Office Address", "[To be provided]"}, {"Support Timings", "[To be provided]"}
    };
}
