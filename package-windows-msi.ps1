param(
  [ValidateSet('msi','exe','app-image')][string]$Type='msi',
  [string]$AppVersion='1.0.0',
  [string]$Vendor='Yashasvi Accounting Solutions LLP',
  [string]$UpgradeUuid='4B17CE77-12A3-47EA-9F09-C8C09B4AD10B',
  [string]$LicenseUrl='https://payroll-license-api.yasl-server.workers.dev'
)
$ErrorActionPreference='Stop'
$root=$PSScriptRoot;$target=Join-Path $root 'target';$classes=Join-Path $target 'standalone-classes';$input=Join-Path $target 'jpackage-input';$runtime=Join-Path $target 'payroll-runtime';$output=Join-Path $target 'installer'

# A full JDK is required. Set JAVA_HOME to it if jpackage/jlink are not on PATH.
function Find-JdkTool([string]$name) { if($env:JAVA_HOME){$candidate=Join-Path $env:JAVA_HOME "bin\$name.exe";if(Test-Path $candidate){return $candidate}}$command=Get-Command $name -ErrorAction SilentlyContinue;if($command){return $command.Source}throw "A full JDK 17+ with $name is required. Set JAVA_HOME to that JDK." }
$javac=Find-JdkTool 'javac';$java=Find-JdkTool 'java';$jar=Find-JdkTool 'jar';$jlink=Find-JdkTool 'jlink';$jpackage=Find-JdkTool 'jpackage'
$bundledWix=Join-Path $root 'tools\wix311';if(Test-Path (Join-Path $bundledWix 'candle.exe')){$env:Path="$bundledWix;$env:Path"}
if(!(Test-Path (Join-Path $root 'lib\sqlite-jdbc-3.46.1.0.jar'))){throw 'Required runtime libraries are missing from lib.'}

Remove-Item -Recurse -Force $classes,$input,$runtime,$output -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $classes,$input,$output,(Join-Path $input 'lib') | Out-Null
$sources=Get-ChildItem (Join-Path $root 'src') -Filter '*.java' | ForEach-Object FullName
& $javac -d $classes $sources;if($LASTEXITCODE -ne 0){throw 'Compilation failed.'}
# Include a blank, fully initialized database. Client data is never packaged.
$seed=Join-Path $classes 'empty-payroll.db';& $java "-Dpayroll.db.path=$seed" -cp "$classes;$root\lib\*" SchemaSeedBuilder;if($LASTEXITCODE -ne 0){throw 'Schema seed creation failed.'}
Copy-Item (Join-Path $root 'assets\yasl-logo.png') (Join-Path $classes 'yasl-logo.png')
Copy-Item (Join-Path $root 'assets\yasl-app-icon.png') (Join-Path $classes 'yasl-app-icon.png')
Copy-Item (Join-Path $root 'assets\icons') (Join-Path $classes 'icons') -Recurse
# Ship the normal entry point: it shows activation before database initialization and login.
& $jar --create --file (Join-Path $input 'PayrollSystem.jar') --main-class Main -C $classes .;if($LASTEXITCODE -ne 0){throw 'JAR creation failed.'}
Copy-Item (Join-Path $root 'lib\*') (Join-Path $input 'lib')

# Embedded runtime: target laptops need no installed Java. Add modules required by Swing, HTTP, Preferences, JDBC and SQLite.
& $jlink --add-modules 'java.base,java.desktop,java.logging,java.net.http,java.prefs,java.sql,java.naming,jdk.crypto.ec' --strip-debug --no-header-files --no-man-pages --compress=2 --output $runtime;if($LASTEXITCODE -ne 0){throw 'Runtime-image creation failed.'}
$args=@('--type',$Type,'--name','YASL Payroll','--app-version',$AppVersion,'--vendor',$Vendor,'--description','YASL Payroll management system','--input',$input,'--dest',$output,'--main-jar','PayrollSystem.jar','--main-class','Main','--runtime-image',$runtime,'--icon',(Join-Path $root 'assets\yasl-logo.ico'),'--java-options','-Dpayroll.packaged=true')
if(-not [string]::IsNullOrWhiteSpace($LicenseUrl)){$args+=@('--java-options',"-Dpayroll.license.url=$LicenseUrl")}
if($Type -ne 'app-image'){$args+=@('--win-menu','--win-menu-group','YASL Payroll','--win-shortcut','--win-dir-chooser','--win-per-user-install','--win-upgrade-uuid',$UpgradeUuid)}
& $jpackage @args;if($LASTEXITCODE -ne 0){throw "jpackage $Type generation failed. MSI and EXE builds require the WiX Toolset to be installed and available to jpackage."}
Write-Host "Native installer created in $output"
