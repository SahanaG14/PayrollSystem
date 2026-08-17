public class PayslipRecord {
 public String employeeId,employeeName,month,pdfPath,preview,generatedDate; public double netSalary,performancePay;
 public PayslipRecord(String id,String name,String month,String path,String preview){this(id,name,month,path,preview,0,0);}
 public PayslipRecord(String id,String name,String month,String path,String preview,double net){this(id,name,month,path,preview,net,0);}
 public PayslipRecord(String id,String name,String month,String path,String preview,double net,double performance){employeeId=id;employeeName=name;this.month=month;pdfPath=path;this.preview=preview;netSalary=net;performancePay=performance;generatedDate=java.time.LocalDate.now().toString();}
}
