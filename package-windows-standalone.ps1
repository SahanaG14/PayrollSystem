param([ValidateSet('zip')][string]$Type='zip')
$ErrorActionPreference='Stop'
$root=$PSScriptRoot;$target=Join-Path $root 'target';$classes=Join-Path $target 'standalone-classes';$stage=Join-Path $target 'PayrollSystem-Standalone';$zip=Join-Path $target 'PayrollSystem-windows-standalone.zip'
if(!(Get-Command javac -ErrorAction SilentlyContinue)){throw 'A JDK with javac is required.'}
if(!(Test-Path (Join-Path $root 'payroll.db'))){throw 'payroll.db is required to build the standalone package.'}
if(!(Test-Path (Join-Path $root 'lib\sqlite-jdbc-3.46.1.0.jar'))){throw 'The runtime libraries in lib are required.'}
Remove-Item -Recurse -Force $classes,$stage -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $classes,$stage,(Join-Path $stage 'lib') | Out-Null
$sources=Get-ChildItem (Join-Path $root 'src') -Filter '*.java' | ForEach-Object FullName
& javac -d $classes $sources
if($LASTEXITCODE -ne 0){throw 'Compilation failed.'}
Copy-Item (Join-Path $root 'assets\yasl-logo.png') (Join-Path $classes 'yasl-logo.png')
Copy-Item (Join-Path $root 'assets\yasl-app-icon.png') (Join-Path $classes 'yasl-app-icon.png')
# Do not distribute the online licensing gate or activation client in this offline package.
Remove-Item -Force (Join-Path $classes 'Main.class'),(Join-Path $classes 'LicenseService.class'),(Join-Path $classes 'LicenseService$Result.class'),(Join-Path $classes 'LicenseActivationFrame.class') -ErrorAction SilentlyContinue
Copy-Item $classes (Join-Path $stage 'classes') -Recurse
Copy-Item (Join-Path $root 'lib\*') (Join-Path $stage 'lib')
Copy-Item (Join-Path $root 'payroll.db') $stage
@'
@echo off
cd /d "%~dp0"
java -Dpayroll.db.path="%~dp0payroll.db" -cp "classes;lib\*" StandaloneMain
'@ | Set-Content (Join-Path $stage 'Run PayrollSystem.bat') -Encoding Ascii
Remove-Item $zip -Force -ErrorAction SilentlyContinue
Compress-Archive -Path $stage -DestinationPath $zip -Force
Write-Host "Standalone ZIP created: $zip"
