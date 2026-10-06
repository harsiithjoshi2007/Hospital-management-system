# =============================================
# Hospital Management System - Run Unit Tests
# PowerShell Script
# =============================================

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Hospital Resource Management System" -ForegroundColor Cyan
Write-Host "Unit Test Runner (PowerShell)" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# ---- Find JUnit Standalone JAR ----
$junitJar = Get-ChildItem -Path . -Filter "junit-platform-console-standalone-*.jar" |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1

if (-not $junitJar) {
    Write-Host "[ERROR] JUnit Platform Console Standalone JAR not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Download from Maven Central:" -ForegroundColor Yellow
    Write-Host "  https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Place the JAR in this directory: junit-platform-console-standalone-1.10.3.jar" -ForegroundColor Yellow
    pause
    exit 1
}
Write-Host "[OK] Found JUnit: $($junitJar.Name)" -ForegroundColor Green

# ---- Find MySQL Connector (optional) ----
$mysqlJar = Get-ChildItem -Path . -Filter "mysql-connector-*.jar" |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1

if ($mysqlJar) {
    Write-Host "[OK] Found MySQL Connector: $($mysqlJar.Name)" -ForegroundColor Green
} else {
    Write-Host "[WARN] MySQL Connector not found - non-DB tests will still run." -ForegroundColor Yellow
}

# ---- Check Java ----
try {
    java -version 2>&1 | Out-Null
    javac -version 2>&1 | Out-Null
    Write-Host "[OK] Java and javac found." -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Java not found!" -ForegroundColor Red
    pause
    exit 1
}

# ---- Clean previous build ----
Write-Host ""
Write-Host "[BUILD] Cleaning previous build..." -ForegroundColor Yellow
if (Test-Path "test-out") { Remove-Item -Recurse -Force "test-out" }
New-Item -ItemType Directory -Force -Path "test-out" | Out-Null

# ---- Build the classpath ----
$cp = @()
$cp += $junitJar.FullName
if ($mysqlJar) { $cp += $mysqlJar.FullName }
$cp += "test-out"
$classpath = $cp -join ";"

# ---- Compile main sources ----
Write-Host "[BUILD] Compiling main source files..." -ForegroundColor Yellow
$mainSources = Get-ChildItem -Path src -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
if ($mysqlJar) {
    $compileResult = javac -cp "$($mysqlJar.FullName)" -d "test-out" $mainSources 2>&1
} else {
    $compileResult = javac -d "test-out" $mainSources 2>&1
}
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Main source compilation failed!" -ForegroundColor Red
    Write-Host $compileResult -ForegroundColor Red
    pause
    exit 1
}
Write-Host "[OK] Main sources compiled." -ForegroundColor Green

# ---- Compile test sources ----
Write-Host "[BUILD] Compiling test source files..." -ForegroundColor Yellow
$testSources = Get-ChildItem -Path test -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }

$testCp = "$($junitJar.FullName);test-out"
if ($mysqlJar) { $testCp += ";$($mysqlJar.FullName)" }

$compileResult = javac -cp "$testCp" -d "test-out" $testSources 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Test source compilation failed!" -ForegroundColor Red
    Write-Host $compileResult -ForegroundColor Red
    pause
    exit 1
}
Write-Host "[OK] Test sources compiled." -ForegroundColor Green

# ---- Run tests ----
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Running Unit Tests..." -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

$runCp = @($junitJar.FullName, "test-out")
if ($mysqlJar) { $runCp += $mysqlJar.FullName }

$result = java -cp ($runCp -join ";") `
    org.junit.platform.console.ConsoleLauncher `
    --scan-classpath `
    --include-classname ".*Test" `
    --reports-dir="test-reports" `
    --details=tree `
    --fail-if-no-tests 2>&1

Write-Host $result

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "============================================" -ForegroundColor Green
    Write-Host "[SUCCESS] All tests passed!" -ForegroundColor Green
    Write-Host "============================================" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "============================================" -ForegroundColor Red
    Write-Host "[FAILURE] Some tests failed. Check output above." -ForegroundColor Red
    Write-Host "============================================" -ForegroundColor Red
}

Write-Host ""
Write-Host "Test reports saved to: test-reports\" -ForegroundColor Yellow
pause
