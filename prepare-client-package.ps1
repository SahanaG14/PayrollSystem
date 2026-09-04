param(
  [string]$SqliteToolsUrl = 'https://www.sqlite.org/2026/sqlite-tools-win-x64-3530400.zip'
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$msi = Join-Path $root 'target\installer\YASL Payroll-1.0.0.msi'
$release = Join-Path $root 'target\client-release'
$archive = Join-Path $root 'target\YASL-Payroll-Client-Package.zip'
$toolsZip = Join-Path $root 'target\sqlite-tools-win-x64.zip'
$toolsExtract = Join-Path $root 'target\sqlite-tools-win-x64'

if (!(Test-Path -LiteralPath $msi)) { throw "MSI not found: $msi. Run package-windows-msi.ps1 first." }
Remove-Item -Recurse -Force $release, $toolsExtract -ErrorAction SilentlyContinue
Remove-Item -Force $archive, $toolsZip -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $release, $toolsExtract | Out-Null

# Official SQLite command-line tools. sqlite3.exe opens the client's local database independently of YASL.
Invoke-WebRequest -Uri $SqliteToolsUrl -OutFile $toolsZip
Expand-Archive -LiteralPath $toolsZip -DestinationPath $toolsExtract -Force
$sqlite = Get-ChildItem -LiteralPath $toolsExtract -Filter 'sqlite3.exe' -Recurse | Select-Object -First 1
if ($null -eq $sqlite) { throw 'The official SQLite tools archive did not contain sqlite3.exe.' }

Copy-Item -LiteralPath $msi -Destination (Join-Path $release 'YASL Payroll-1.0.0.msi')
Copy-Item -LiteralPath $sqlite.FullName -Destination (Join-Path $release 'sqlite3.exe')

@'
@echo off
set "DB=%LOCALAPPDATA%\YASL Payroll\data\payroll.db"
if not exist "%DB%" (
  echo YASL Payroll database not found:
  echo %DB%
  echo Start YASL Payroll once and complete activation first.
  pause
  exit /b 1
)
"%~dp0sqlite3.exe" -readonly "%DB%"
'@ | Set-Content -LiteralPath (Join-Path $release 'Open YASL Database.cmd') -Encoding Ascii

@'
YASL Payroll Client Package

1. Install YASL Payroll-1.0.0.msi.
2. Close YASL Payroll, then double-click "Open YASL Database.cmd" to inspect the saved database independently.
3. At the SQLite prompt, type .tables and press Enter to list tables.
4. Type .quit and press Enter when finished.

The live database is stored here:
%LOCALAPPDATA%\YASL Payroll\data\payroll.db

sqlite3.exe is SQLite's official command-line database tool. It opens the database read-only, so records cannot be changed or deleted through this package.
'@ | Set-Content -LiteralPath (Join-Path $release 'README - Database Access.txt') -Encoding UTF8

Compress-Archive -LiteralPath (Get-ChildItem -LiteralPath $release | ForEach-Object FullName) -DestinationPath $archive -CompressionLevel Optimal
Write-Host "Client package created: $release"
Write-Host "Client package ZIP created: $archive"
