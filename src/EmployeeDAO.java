import java.sql.*;
import java.util.*;

/** Persistence boundary for employee master data. */
public class EmployeeDAO {
    private static final List<Employee> MEMORY = new ArrayList<>();
    private static volatile List<Employee> ROSTER;
    public EmployeeDAO() { ensureTable(); }
    private void ensureTable() {
        try (Connection c=DBConnection.getConnection(); Statement s=c==null?null:c.createStatement()) {
            if (s != null) { ensureColumn(s,"is_deleted INTEGER NOT NULL DEFAULT 0"); ensureColumn(s,"deleted_at TEXT"); ensureColumn(s,"epf_ceiling INTEGER NOT NULL DEFAULT 0"); ensureColumn(s,"phone_ciphertext TEXT");ensureColumn(s,"pan_ciphertext TEXT");ensureColumn(s,"aadhaar_ciphertext TEXT"); }
        } catch (SQLException ignored) { }
    }
    private void ensureColumn(Statement statement,String definition){try{statement.executeUpdate("ALTER TABLE employee_master_data ADD COLUMN "+definition);}catch(SQLException ignored){}}
    public String nextId() { return ""; } // Employee IDs are company-defined and entered by the accountant.
    /** Display boundary for PAN: an undecryptable value must never escape as ciphertext. */
    public String panForDisplay(Employee employee){return employee==null?"":validPan(employee.getPan())?employee.getPan().trim().toUpperCase(Locale.ROOT):"";}
    public static boolean validPan(String value){return value!=null&&value.trim().toUpperCase(Locale.ROOT).matches("[A-Z]{5}\\d{4}[A-Z]");}
    public boolean existsId(String id) { return existsMasterDataId(id); }
    public boolean existsMasterDataId(String id) {
        if(id==null||id.trim().isEmpty()) return false;
        String employeeId=id.trim();Connection c=DBConnection.getConnection();
        if(c!=null)try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM employee_master_data WHERE employee_id=? LIMIT 1")){p.setString(1,employeeId);ResultSet r=p.executeQuery();if(r.next())return true;}catch(SQLException ignored){}
        return containsEmployeeId(MEMORY,employeeId)||containsEmployeeId(ROSTER,employeeId);
    }
    private boolean containsEmployeeId(List<Employee> employees,String employeeId){if(employees==null)return false;for(Employee employee:employees)if(employeeId.equals(employee.getId()))return true;return false;}
    public void save(Employee e) throws SQLException { if (e.getId()==null || e.getId().trim().isEmpty()) throw new SQLException("Employee ID is required"); if(existsId(e.getId()))throw new SQLException("Employee ID already exists"); upsert(e, false); }
    public void update(Employee e) throws SQLException { upsert(e, true); }
    private void upsert(Employee e, boolean update) throws SQLException {
        Connection c=DBConnection.getConnection();
        if (c==null) { synchronized(MEMORY) { MEMORY.removeIf(x->x.getId().equals(e.getId())); MEMORY.add(e); } updateRoster(e); cache(MEMORY); return; }
        String cols="employee_id,employee_name,gender,dob,department,designation,joining_date,status,leaving_date,email,phone,pan,aadhaar,phone_ciphertext,pan_ciphertext,aadhaar_ciphertext,esic,uan,bank_name,bank_acc,ifsc,salary_structure,photo_path,pan_doc_path,aadhaar_doc_path,epf_ceiling";
        String sql=update ? "UPDATE employee_master_data SET employee_name=?,gender=?,dob=?,department=?,designation=?,joining_date=?,status=?,leaving_date=?,email=?,phone=?,pan=?,aadhaar=?,phone_ciphertext=?,pan_ciphertext=?,aadhaar_ciphertext=?,esic=?,uan=?,bank_name=?,bank_acc=?,ifsc=?,salary_structure=?,photo_path=?,pan_doc_path=?,aadhaar_doc_path=?,epf_ceiling=? WHERE employee_id=? AND is_deleted=0" : "INSERT INTO employee_master_data ("+cols+") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement p=c.prepareStatement(sql)) { bind(p,e,update); if(p.executeUpdate()!=1)throw new SQLException(update?"Employee record was not found or has been deleted.":"Unable to save employee."); List<Employee> saved=readCache();saved.removeIf(x->x.getId().equals(e.getId()));saved.add(e);cache(saved);updateRoster(e); }
    }
    private void bind(PreparedStatement p, Employee e, boolean update) throws SQLException {
        String employeeId=e.getId()==null?"":e.getId().trim();if(employeeId.isEmpty())throw new SQLException("Employee ID is required");e.setId(employeeId);String phone=e.getPhone()==null?"":e.getPhone().replaceAll("\\D","");if(phone.length()!=10)throw new SQLException("Phone number must contain exactly 10 digits.");e.setPhone(phone);String pan=e.getPan()==null?"":e.getPan().trim().toUpperCase(Locale.ROOT);if(!pan.isEmpty()&&!validPan(pan))throw new SQLException("PAN must use the format AAAAA9999A.");e.setPan(pan);String[] documents=(e.getDocumentPaths()==null?"":e.getDocumentPaths()).split("\\|",-1);int i=1; if (!update) p.setString(i++,employeeId); p.setString(i++,e.getName()); p.setString(i++,e.getGender()); p.setString(i++,e.getDob()); p.setString(i++,e.getDepartment()); p.setString(i++,e.getDesignation()); p.setString(i++,e.getJoiningDate()); p.setString(i++,e.getStatus()); p.setString(i++,e.getEmploymentEndDate()); p.setString(i++,protect(e.getEmail())); p.setString(i++,"0000000000"); p.setString(i++,"XXXXXXXXXX"); p.setString(i++,"000000000000");p.setString(i++,protect(phone));p.setString(i++,protect(pan));p.setString(i++,protect(e.getAadhar())); p.setString(i++,protect(e.getEsicIpNumber())); p.setString(i++,protect(e.getUanNumber())); p.setString(i++,protect(e.getBankName())); p.setString(i++,protect(e.getAccountNumber())); p.setString(i++,protect(e.getIfsc())); p.setString(i++,e.getSalaryStructure()==null?"Wages":e.getSalaryStructure()); p.setString(i++,e.getPhotoPath()); p.setString(i++,documents.length>0?documents[0]:""); p.setString(i++,documents.length>1?documents[1]:"");p.setBoolean(i++,e.isRestrictEpfCeiling()); if(update)p.setString(i,employeeId);
    }
    public void delete(String id) throws SQLException {
        Connection c=DBConnection.getConnection();
        if(c==null){synchronized(MEMORY){MEMORY.removeIf(e->e.getId().equals(id));}cache(MEMORY);removeRoster(id);return;}
        try(PreparedStatement p=c.prepareStatement("UPDATE employee_master_data SET is_deleted=1,deleted_at=CURRENT_TIMESTAMP WHERE employee_id=? AND is_deleted=0")){p.setString(1,id);if(p.executeUpdate()!=1)throw new SQLException("Employee record was not found or is already deleted.");}
        List<Employee> saved=readCache();saved.removeIf(e->e.getId().equals(id));cache(saved);removeRoster(id);
    }
    public void deleteAll() throws SQLException { List<String> ids=new ArrayList<>(); for(Employee employee:new ArrayList<>(list(""))) ids.add(employee.getId()); for(String id:ids) delete(id); }
    private void purgeLocal(String id){DeductionStore.purgeEmployee(id);MonthlyEarningsStore.purgeEmployee(id);CTCStore.purgeEmployee(id);PayslipPerformanceStore.purgeEmployee(id);PayslipOtherEarningsStore.purgeEmployee(id);SalaryRevisionStore.purgeEmployee(id);AnnualSalaryOverrideStore.purgeEmployee(id);}
    public List<Employee> list(String search) {
        List<Employee> result=new ArrayList<>(); Connection c=DBConnection.getConnection();
        if(c==null) { List<Employee> source=MEMORY.isEmpty()?readCache():MEMORY;ROSTER=new ArrayList<>(source);return filter(source,search); }
        String q="%"+search+"%";String filter=" WHERE is_deleted=0 AND (employee_id LIKE ? OR employee_name LIKE ? OR department LIKE ?)";
        load(result,c,"SELECT * FROM employee_master_data"+filter+" ORDER BY employee_id",q,3);if(!result.isEmpty())cache(result);ROSTER=new ArrayList<>(result);return filter(ROSTER,search);
    }
    public List<Employee> listByEmployeeName(String search) {
        List<Employee> result=new ArrayList<>(); Connection c=DBConnection.getConnection(); String q=search==null?"":search.toLowerCase();
        if(ROSTER!=null){for(Employee e:ROSTER)if((e.getName()==null?"":e.getName()).toLowerCase().contains(q))result.add(e);return ordered(result);}
        if(c==null) { for(Employee e:MEMORY) if((e.getName()==null?"":e.getName()).toLowerCase().contains(q))result.add(e); return ordered(result); }
        String value="%"+search+"%";load(result,c,"SELECT * FROM employee_master_data WHERE is_deleted=0 AND employee_name LIKE ? ORDER BY employee_id",value,1);if(!result.isEmpty())cache(result);return ordered(result);
    }
    public List<Employee> listByEmployeeId(String search) {
        List<Employee> result=new ArrayList<>(); Connection c=DBConnection.getConnection(); String q=search==null?"":search.toLowerCase();
        if(ROSTER!=null){for(Employee e:ROSTER)if((e.getId()==null?"":e.getId()).toLowerCase().contains(q))result.add(e);return ordered(result);}
        if(c==null) { for(Employee e:MEMORY) if((e.getId()==null?"":e.getId()).toLowerCase().contains(q))result.add(e); return ordered(result); }
        String value="%"+search+"%";load(result,c,"SELECT * FROM employee_master_data WHERE is_deleted=0 AND employee_id LIKE ? ORDER BY employee_id",value,1);if(!result.isEmpty())cache(result);return ordered(result);
    }
    /** Active master-data employees are the source for all payroll modules. */
    public List<Employee> listActive(String search) {
        List<Employee> active=new ArrayList<>();
        for(Employee e:list(search)) if(isActive(e)) active.add(e);
        return active;
    }
    public List<Employee> listForMonth(String search, java.time.YearMonth month) { List<Employee> eligible=new ArrayList<>();for(Employee employee:listActive(search))if(isEligibleForMonth(employee,month))eligible.add(employee);return eligible; }
    public List<Employee> listForFinancialYear(String search,int startYear){java.time.YearMonth start=java.time.YearMonth.of(startYear,4),end=java.time.YearMonth.of(startYear+1,3);List<Employee> eligible=new ArrayList<>();for(Employee employee:listActive(search))if(isEligibleForMonth(employee,start)||isEligibleForMonth(employee,end)||overlapsFinancialYear(employee,start,end))eligible.add(employee);return eligible;}
    public boolean isEligibleForMonth(Employee employee,java.time.YearMonth month){if(employee==null||!isActive(employee))return false;java.time.LocalDate joining=date(employee.getJoiningDate(),null),leaving=date(employee.getEmploymentEndDate(),null);return (joining==null||!joining.isAfter(month.atEndOfMonth()))&&(leaving==null||!leaving.isBefore(month.atDay(1)));}
    private boolean overlapsFinancialYear(Employee employee,java.time.YearMonth start,java.time.YearMonth end){java.time.LocalDate joining=date(employee.getJoiningDate(),null),leaving=date(employee.getEmploymentEndDate(),null);return (joining==null||!joining.isAfter(end.atEndOfMonth()))&&(leaving==null||!leaving.isBefore(start.atDay(1)));}
    private boolean isActive(Employee employee){String status=employee.getStatus()==null?"":employee.getStatus().trim();return "Active".equalsIgnoreCase(status)||"Active Employee".equalsIgnoreCase(status);}
    private boolean isExEmployee(Employee employee){String status=employee.getStatus()==null?"":employee.getStatus().trim();return "Deleted".equalsIgnoreCase(status)||"Ex Employee".equalsIgnoreCase(status)||"Ex-Employee".equalsIgnoreCase(status)||"ExEmployee".equalsIgnoreCase(status);}
    private java.time.LocalDate date(String value,java.time.LocalDate fallback){if(value==null||value.trim().isEmpty())return fallback;try{return java.time.LocalDate.parse(value,java.time.format.DateTimeFormatter.ofPattern("dd-MM-uuuu"));}catch(Exception ignored){try{return java.time.LocalDate.parse(value);}catch(Exception x){return fallback;}}}
    private boolean load(List<Employee> result,Connection c,String sql,String value,int count){try(PreparedStatement p=c.prepareStatement(sql)){for(int i=1;i<=count;i++)p.setString(i,value);ResultSet r=p.executeQuery();while(r.next())result.add(read(r));return true;}catch(SQLException ignored){return false;}}
    private List<Employee> filterCache(String search){List<Employee> out=new ArrayList<>();String q=search==null?"":search.toLowerCase();for(Employee e:readCache())if((e.getId()+e.getName()+e.getDepartment()+e.getPhone()+e.getPan()).toLowerCase().contains(q))out.add(e);return out;}
    private List<Employee> filter(List<Employee> source,String search){List<Employee> out=new ArrayList<>();String q=search==null?"":search.toLowerCase();for(Employee e:source)if((e.getId()+e.getName()+e.getDepartment()+e.getPhone()+e.getPan()).toLowerCase().contains(q))out.add(e);return ordered(out);}
    private List<Employee> ordered(List<Employee> values){values.sort((left,right)->compareEmployeeIds(left.getId(),right.getId()));return values;}
    private int compareEmployeeIds(String left,String right){String a=left==null?"":left,b=right==null?"":right;int ai=a.length();while(ai>0&&Character.isDigit(a.charAt(ai-1)))ai--;int bi=b.length();while(bi>0&&Character.isDigit(b.charAt(bi-1)))bi--;int prefix=a.substring(0,ai).compareToIgnoreCase(b.substring(0,bi));if(prefix!=0)return prefix;try{return Long.compare(Long.parseLong(a.substring(ai)),Long.parseLong(b.substring(bi)));}catch(Exception ignored){return a.compareToIgnoreCase(b);}}
    private void updateRoster(Employee employee){List<Employee> roster=new ArrayList<>(ROSTER==null?readCache():ROSTER);roster.removeIf(item->item.getId().equals(employee.getId()));roster.add(employee);ROSTER=ordered(roster);}
    private void removeRoster(String id){if(ROSTER==null)return;List<Employee> roster=new ArrayList<>(ROSTER);roster.removeIf(item->item.getId().equals(id));ROSTER=roster;}
    /* Never deserialize employee data from a user-writable file. The old object cache was removed. */
    private List<Employee> readCache(){return new ArrayList<>();}
    private void cache(List<Employee> employees){/* Database is the sole persistent store. */}
    private Employee read(ResultSet r)throws SQLException { Employee e=new Employee();e.setId(get(r,"employee_id"));e.setName(get(r,"employee_name"));e.setGender(get(r,"gender"));e.setDob(get(r,"dob"));e.setDepartment(get(r,"department"));e.setDesignation(get(r,"designation"));e.setJoiningDate(get(r,"joining_date"));e.setStatus(get(r,"status"));e.setEmploymentEndDate(get(r,"leaving_date"));e.setEmail(unprotect(get(r,"email")));e.setPhone(unprotect(first(r,"phone_ciphertext","phone")));e.setPan(unprotect(first(r,"pan_ciphertext","pan")));e.setAadhar(unprotect(first(r,"aadhaar_ciphertext","aadhaar")));e.setEsicIpNumber(unprotect(get(r,"esic")));e.setUanNumber(unprotect(get(r,"uan")));e.setBankName(unprotect(get(r,"bank_name")));e.setAccountNumber(unprotect(get(r,"bank_acc")));e.setIfsc(unprotect(get(r,"ifsc")));e.setPhotoPath(get(r,"photo_path"));e.setDocumentPaths(get(r,"pan_doc_path")+"|"+get(r,"aadhaar_doc_path"));e.setRestrictEpfCeiling("1".equals(get(r,"epf_ceiling")));String structure=get(r,"salary_structure");e.setSalaryStructure("Salary".equalsIgnoreCase(structure)||"Wages".equalsIgnoreCase(structure)?structure:"Wages");return e; }
    private String first(ResultSet r,String encrypted,String plain)throws SQLException{String value=get(r,encrypted);return value==null||value.isBlank()?get(r,plain):value;}
    private String protect(String value){return value==null||value.isEmpty()?"":SecurityUtil.encrypt(value);}
    private String unprotect(String value){return value==null||value.isEmpty()?"":SecurityUtil.decrypt(value);}
    private String get(ResultSet r,String column){try{String value=r.getString(column);return value==null?"":value;}catch(SQLException ignored){return "";}}
}
