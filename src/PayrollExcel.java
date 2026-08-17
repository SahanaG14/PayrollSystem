import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import java.awt.Component;
import javax.xml.parsers.*;
import org.w3c.dom.*;

/** Small dependency-free XLSX/CSV reader-writer used by the payroll import screens. */
public final class PayrollExcel {
  private PayrollExcel() {}
  public static final class Sheet { public final List<List<String>> rows; Sheet(List<List<String>> r){rows=r;} }
  public static void export(Component parent, String title, String name, String[] headers, List<Object[]> rows) {
    javax.swing.JFileChooser c=new javax.swing.JFileChooser(); c.setSelectedFile(new File(name));
    if(c.showSaveDialog(parent)!=javax.swing.JFileChooser.APPROVE_OPTION)return;
    File f=c.getSelectedFile(); if(!f.getName().toLowerCase().endsWith(".xlsx"))f=new File(f.getParentFile(),f.getName()+".xlsx"); f=ExportFileName.unique(f);
    try { write(f,title,headers,rows); javax.swing.JOptionPane.showMessageDialog(parent,"Excel exported: "+f.getName()); }
    catch(Exception ex){javax.swing.JOptionPane.showMessageDialog(parent,"Excel export failed: "+ex.getMessage());}
  }
  public static void exportBoldHeader(Component parent,String title,String name,String[] headers,List<Object[]> rows){
    javax.swing.JFileChooser c=new javax.swing.JFileChooser();c.setSelectedFile(new File(name));if(c.showSaveDialog(parent)!=javax.swing.JFileChooser.APPROVE_OPTION)return;
    File f=c.getSelectedFile();if(!f.getName().toLowerCase().endsWith(".xlsx"))f=new File(f.getParentFile(),f.getName()+".xlsx");f=ExportFileName.unique(f);
    try{writeBoldHeader(f,title,headers,rows);javax.swing.JOptionPane.showMessageDialog(parent,"Excel exported: "+f.getName());}catch(Exception ex){javax.swing.JOptionPane.showMessageDialog(parent,"Excel export failed: "+ex.getMessage());}
  }
  public static void exportWithDetails(Component parent,String title,String name,String[] details,String[] headers,List<Object[]> rows){
    javax.swing.JFileChooser c=new javax.swing.JFileChooser();c.setSelectedFile(new File(name));if(c.showSaveDialog(parent)!=javax.swing.JFileChooser.APPROVE_OPTION)return;
    File f=c.getSelectedFile();if(!f.getName().toLowerCase().endsWith(".xlsx"))f=new File(f.getParentFile(),f.getName()+".xlsx");f=ExportFileName.unique(f);
    try{writeWithDetails(f,title,details,headers,rows);javax.swing.JOptionPane.showMessageDialog(parent,"Excel exported: "+f.getName());}catch(Exception ex){javax.swing.JOptionPane.showMessageDialog(parent,"Excel export failed: "+ex.getMessage());}
  }
  public static Sheet importSheet(Component parent) throws Exception {
    javax.swing.JFileChooser c=new javax.swing.JFileChooser(); c.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Excel or CSV", "xlsx","xls","csv"));
    if(c.showOpenDialog(parent)!=javax.swing.JFileChooser.APPROVE_OPTION)return null; return read(c.getSelectedFile());
  }
  public static void requireHeaders(Sheet s,String... expected) throws Exception {
    if(s==null||s.rows.isEmpty())throw new Exception("The selected file is empty."); List<String> got=s.rows.get(0);
    if(got.size()!=expected.length)throw new Exception("<html><div style='width:350px;'>Invalid headers. Expected exactly:<br/>"+Arrays.toString(expected)+"</div></html>");
    for(int i=0;i<expected.length;i++)if(!expected[i].equals(got.get(i).trim()))throw new Exception("<html><div style='width:350px;'>Invalid headers. Expected exactly:<br/>"+Arrays.toString(expected)+"</div></html>");
  }
  public static String cell(List<String> r,int i){return textCell(r,i).trim();}
  /** Returns the worksheet cell payload as text; never parses identifiers as numeric values. */
  public static String textCell(List<String> r,int i){return i<r.size()&&r.get(i)!=null?String.valueOf(r.get(i)):"";}
  public static double number(String v){try{return Double.parseDouble(v.replace(",","").trim());}catch(Exception e){return 0;}}
  public static void write(File file,String sheet,String[] headers,List<Object[]> rows)throws Exception{
    try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(file))){
      put(z,"[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
      put(z,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
      put(z,"xl/workbook.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\""+esc(sheet)+"\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
      put(z,"xl/_rels/workbook.xml.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
      StringBuilder x=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
      row(x,1,headers); int r=2; for(Object[] values:rows)row(x,r++,values); x.append("</sheetData></worksheet>"); put(z,"xl/worksheets/sheet1.xml",x.toString());
    }
  }
  private static void writeBoldHeader(File file,String sheet,String[] headers,List<Object[]> rows)throws Exception{
    try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(file))){
      put(z,"[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/></Types>");
      put(z,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
      put(z,"xl/workbook.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\""+esc(sheet)+"\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
      put(z,"xl/_rels/workbook.xml.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>");
      put(z,"xl/styles.xml",boldStyles());String widths="IT Computation".equals(sheet)?"<cols><col min=\"1\" max=\"1\" width=\"42\" customWidth=\"1\"/><col min=\"2\" max=\"4\" width=\"18\" customWidth=\"1\"/></cols>":"";StringBuilder x=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">").append(widths).append("<sheetData>");row(x,1,headers,true);int r=2;for(Object[] values:rows)row(x,r++,values,false);x.append("</sheetData></worksheet>");put(z,"xl/worksheets/sheet1.xml",x.toString());
    }
  }
  private static void writeWithDetails(File file,String sheet,String[] details,String[] headers,List<Object[]> rows)throws Exception{
    try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(file))){
      put(z,"[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
      put(z,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
      put(z,"xl/workbook.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\""+esc(sheet)+"\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
      put(z,"xl/_rels/workbook.xml.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
      StringBuilder x=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
      row(x,1,new Object[]{sheet+" Employee Details"});row(x,2,details);row(x,3,new Object[]{});row(x,4,headers);int r=5;for(Object[] values:rows)row(x,r++,values);x.append("</sheetData></worksheet>");put(z,"xl/worksheets/sheet1.xml",x.toString());
    }
  }
  private static void row(StringBuilder b,int row,Object[] cells){b.append("<row r=\"").append(row).append("\">");for(int i=0;i<cells.length;i++){Object v=cells[i];String ref=column(i)+(row);if(v instanceof Number)b.append("<c r=\"").append(ref).append("\"><v>").append(((Number)v).doubleValue()).append("</v></c>");else b.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t>").append(esc(v==null?"":String.valueOf(v))).append("</t></is></c>");}b.append("</row>");}
  private static void row(StringBuilder b,int row,Object[] cells,boolean bold){b.append("<row r=\"").append(row).append("\">");for(int i=0;i<cells.length;i++){Object v=cells[i];String ref=column(i)+row,style=bold?" s=\"1\"":"";if(v instanceof Number)b.append("<c r=\"").append(ref).append("\"").append(style).append("><v>").append(((Number)v).doubleValue()).append("</v></c>");else b.append("<c r=\"").append(ref).append("\"").append(style).append(" t=\"inlineStr\"><is><t>").append(esc(v==null?"":String.valueOf(v))).append("</t></is></c>");}b.append("</row>");}
  private static String boldStyles(){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts><fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills><borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs><cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/><xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" applyFont=\"1\"/></cellXfs></styleSheet>";}
  private static String column(int n){StringBuilder s=new StringBuilder();do{s.insert(0,(char)('A'+n%26));n=n/26-1;}while(n>=0);return s.toString();}
  private static void put(ZipOutputStream z,String n,String v)throws IOException{z.putNextEntry(new ZipEntry(n));z.write(v.getBytes(StandardCharsets.UTF_8));z.closeEntry();}
  private static String esc(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
  public static Sheet read(File f)throws Exception {String n=f.getName().toLowerCase();if(n.endsWith(".csv"))return csv(f);if(n.endsWith(".xls"))throw new Exception("Legacy .xls files are not supported. Please save the file as .xlsx.");try(ZipFile z=new ZipFile(f)){ZipEntry e=z.getEntry("xl/worksheets/sheet1.xml");if(e==null)throw new Exception("No worksheet found.");Document d=parse(z.getInputStream(e));List<String> shared=new ArrayList<>();ZipEntry ss=z.getEntry("xl/sharedStrings.xml");if(ss!=null){NodeList ts=parse(z.getInputStream(ss)).getElementsByTagNameNS("*","t");for(int i=0;i<ts.getLength();i++)shared.add(ts.item(i).getTextContent());}List<List<String>> out=new ArrayList<>();NodeList rr=d.getElementsByTagNameNS("*","row");for(int i=0;i<rr.getLength();i++){Element row=(Element)rr.item(i);List<String> values=new ArrayList<>();NodeList cc=row.getElementsByTagNameNS("*","c");for(int j=0;j<cc.getLength();j++){Element c=(Element)cc.item(j);int idx=colIndex(c.getAttribute("r"));while(values.size()<idx)values.add("");String type=c.getAttribute("t"),v="";NodeList is=c.getElementsByTagNameNS("*","t");if(is.getLength()>0)v=is.item(0).getTextContent();else {NodeList vs=c.getElementsByTagNameNS("*","v");if(vs.getLength()>0)v=vs.item(0).getTextContent();if("s".equals(type)&&!v.isEmpty())v=shared.get(Integer.parseInt(v));}values.add(v);}out.add(values);}return new Sheet(out);}}
  private static int colIndex(String ref){int x=0;for(int i=0;i<ref.length()&&Character.isLetter(ref.charAt(i));i++)x=x*26+(Character.toUpperCase(ref.charAt(i))-'A'+1);return Math.max(0,x-1);}
  private static Document parse(InputStream in)throws Exception{DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);return f.newDocumentBuilder().parse(in);}
  private static Sheet csv(File f)throws Exception{List<List<String>> r=new ArrayList<>();try(BufferedReader b=new BufferedReader(new FileReader(f))){String s;while((s=b.readLine())!=null)r.add(Arrays.asList(s.replace("\uFEFF","").split(",",-1)));}return new Sheet(r);}
}
