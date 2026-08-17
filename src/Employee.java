public class Employee implements java.io.Serializable {
    private static final long serialVersionUID=1L;
    private String id, companyName, companyAddress, name, photoPath, gender, dob, phone, email, address, department, designation;
    private String joiningDate, employmentEndDate, bankName, accountNumber, ifsc, pan, aadhar, esicIpNumber, uanNumber, status, salaryStructure, documentPaths;
    private double basicSalary; private boolean restrictEpfCeiling;
    // Legacy fields kept for compatibility with the original project.
    int workingDays; double grossSalary; double deduction; double netSalary;
    public Employee() { }
    public Employee(String name, int days, double gross, double deductions) {
        this.name=name; workingDays=days; grossSalary=gross; deduction=deductions; netSalary=gross-deductions;
    }
    public String getId() { return id; } public void setId(String v) { id=v; }
    public String getCompanyName() { return companyName; } public void setCompanyName(String v) { companyName=v; }
    public String getCompanyAddress() { return companyAddress; } public void setCompanyAddress(String v) { companyAddress=v; }
    public String getName() { return name; } public void setName(String v) { name=v; }
    public String getPhotoPath() { return photoPath; } public void setPhotoPath(String v) { photoPath=v; }
    public String getGender() { return gender; } public void setGender(String v) { gender=v; }
    public String getDob() { return dob; } public void setDob(String v) { dob=v; }
    public String getPhone() { return phone; } public void setPhone(String v) { phone=v; }
    public String getEmail() { return email; } public void setEmail(String v) { email=v; }
    public String getAddress() { return address; } public void setAddress(String v) { address=v; }
    public String getDepartment() { return department; } public void setDepartment(String v) { department=v; }
    public String getDesignation() { return designation; } public void setDesignation(String v) { designation=v; }
    public String getJoiningDate() { return joiningDate; } public void setJoiningDate(String v) { joiningDate=v; }
    public String getEmploymentEndDate() { return employmentEndDate; } public void setEmploymentEndDate(String v) { employmentEndDate=v; }
    public double getBasicSalary() { return basicSalary; } public void setBasicSalary(double v) { basicSalary=v; }
    public boolean isRestrictEpfCeiling(){return restrictEpfCeiling;} public void setRestrictEpfCeiling(boolean v){restrictEpfCeiling=v;}
    public String getBankName() { return bankName; } public void setBankName(String v) { bankName=v; }
    public String getAccountNumber() { return accountNumber; } public void setAccountNumber(String v) { accountNumber=v; }
    public String getIfsc() { return ifsc; } public void setIfsc(String v) { ifsc=v; }
    public String getPan() { return pan; } public void setPan(String v) { pan=v; }
    public String getAadhar() { return aadhar; } public void setAadhar(String v) { aadhar=v; }
    public String getEsicIpNumber() { return esicIpNumber; } public void setEsicIpNumber(String v) { esicIpNumber=v; }
    public String getUanNumber() { return uanNumber; } public void setUanNumber(String v) { uanNumber=v; }
    public String getStatus() { return status; } public void setStatus(String v) { status=v; }
    public String getSalaryStructure() { return salaryStructure; } public void setSalaryStructure(String v) { salaryStructure=v; }
    public String getDocumentPaths() { return documentPaths; } public void setDocumentPaths(String v) { documentPaths=v; }
    public boolean isWagesStructure() { return "Wages".equalsIgnoreCase(salaryStructure); }
    public String toString() { return id + " - " + name; }
}
