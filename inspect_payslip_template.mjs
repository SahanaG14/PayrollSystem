import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const input = await FileBlob.load("C:/Users/Sahana/OneDrive/Documents/PAYSLIP Template.xlsx");
const workbook = await SpreadsheetFile.importXlsx(input);
const overview = await workbook.inspect({
  kind: "workbook,sheet,table,region",
  maxChars: 12000,
  tableMaxRows: 40,
  tableMaxCols: 20,
  tableMaxCellChars: 100,
});
console.log(overview.ndjson);
const sheets = await workbook.inspect({ kind: "sheet", include: "id,name", maxChars: 1000 });
console.log(sheets.ndjson);
const preview = await workbook.render({ sheetName: "Sheet1", autoCrop: "all", scale: 1.5, format: "png" });
await fs.writeFile("template-preview.png", new Uint8Array(await preview.arrayBuffer()));
