# =============================================
# Hospital Management System - Build & Run
# PowerShell Script
# =============================================

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Hospital Resource Management System" -ForegroundColor Cyan
Write-Host "Build and Run Script (PowerShell)" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# ---- Check for Java ----
try {
    $javaVersion = java -version 2>&1
    $javacVersion = javac -version 2>&1
    Write-Host "[OK] Java and javac found." -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Java not found! Please install JDK 17+." -ForegroundColor Red
    Write-Host "        Download from: https://adoptium.net/" -ForegroundColor Yellow
    pause
    exit 1
}

# ---- Find MySQL Connector JAR ----
$mysqlJar = Get-ChildItem -Path . -Filter "mysql-connector-*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1

if (-not $mysqlJar) {
    Write-Host "[ERROR] No MySQL Connector JAR found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please download mysql-connector-j from:" -ForegroundColor Yellow
    Write-Host "  https://dev.mysql.com/downloads/connector/j/" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Place the JAR file in this directory and run again." -ForegroundColor Yellow
    pause
    exit 1
}

Write-Host "[OK] Found MySQL Connector: $($mysqlJar.Name)" -ForegroundColor Green

# ---- Clean previous build ----
Write-Host ""
Write-Host "[BUILD] Cleaning previous build..." -ForegroundColor Yellow
if (Test-Path "out") {
    Remove-Item -Recurse -Force "out"
}
New-Item -ItemType Directory -Force -Path "out" | Out-Null

# ---- Compile ----
Write-Host "[BUILD] Compiling Java source files..." -ForegroundColor Yellow
Write-Host ""

$sourceFiles = Get-ChildItem -Path src -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }

$compileResult = javac -cp "$($mysqlJar.Name)" -d "out" $sourceFiles 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Compilation failed!" -ForegroundColor Red
    Write-Host ""
    Write-Host $compileResult -ForegroundColor Red
    pause
    exit 1
}

Write-Host "[OK] Compilation successful!" -ForegroundColor Green

# ---- Run ----
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Starting Hospital Management System..." -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Make sure MySQL is running and the database" -ForegroundColor Yellow
Write-Host "'hospital_db' has been created." -ForegroundColor Yellow
Write-Host ""
Write-Host "If this is your first time, run the SQL script:" -ForegroundColor Yellow
Write-Host "  mysql -u root -p < sql\schema.sql" -ForegroundColor White
Write-Host ""
Write-Host "Also check your credentials in:" -ForegroundColor Yellow
Write-Host "  src\com\hospital\database\DatabaseConnection.java" -ForegroundColor White
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

java -cp "$($mysqlJar.Name);out" com.hospital.Main

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[ERROR] Application exited with error." -ForegroundColor Red
    pause
}

Write-Host ""
Write-Host "[DONE] Application closed." -ForegroundColor Green
