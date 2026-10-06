@echo off
REM =============================================
REM Hospital Management System - Run Unit Tests
REM Windows Batch Script
REM =============================================
setlocal enabledelayedexpansion

echo.
echo ============================================
echo Hospital Resource Management System
echo Unit Test Runner (Windows)
echo ============================================
echo.

REM ---- Configuration ----
set JUNIT_JAR=junit-platform-console-standalone-1.10.3.jar
set MYSQL_JAR=mysql-connector-j-8.4.0.jar

REM ---- Check for JUnit ----
if not exist "%JUNIT_JAR%" (
    echo [WARN] JUnit standalone JAR not found: %JUNIT_JAR%
    echo Searching for junit-platform-console-standalone-*.jar...
    dir /b junit-platform-console-standalone-*.jar 2>nul
    if errorlevel 1 (
        echo [ERROR] JUnit Platform Console Standalone JAR not found!
        echo.
        echo Please download from Maven Central:
        echo   https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/
        echo.
        echo Download the latest: junit-platform-console-standalone-1.10.3.jar
        echo Place it in this directory and run again.
        echo Alternatively, edit JUNIT_JAR in run_tests.bat to point to your JAR.
        pause
        exit /b 1
    )
)

REM Find the JUnit jar
for /f "tokens=*" %%a in ('dir /b /o:-d junit-platform-console-standalone-*.jar 2^>nul') do (
    set JUNIT_JAR=%%a
    goto :found_junit
)
:found_junit
echo [OK] Found JUnit: %JUNIT_JAR%

REM ---- Find MySQL Connector ----
if not exist "%MYSQL_JAR%" (
    echo [WARN] MySQL Connector not found: %MYSQL_JAR%
    dir /b mysql-connector-*.jar 2>nul
    if errorlevel 1 (
        echo [WARN] MySQL Connector JAR not found - tests that don't need DB will still run.
        set MYSQL_JAR=
    ) else (
        for /f "tokens=*" %%a in ('dir /b /o:-d mysql-connector-*.jar 2^>nul') do (
            set MYSQL_JAR=%%a
            goto :found_mysql
        )
    )
)
:found_mysql
if not "%MYSQL_JAR%" == "" (
    echo [OK] Found MySQL Connector: %MYSQL_JAR%
)

REM ---- Check for Java ----
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found!
    pause
    exit /b 1
)
javac -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] javac not found!
    pause
    exit /b 1
)
echo [OK] Java and javac found.

REM ---- Clean previous test build ----
echo.
echo [BUILD] Cleaning previous build...
if exist test-out rmdir /s /q test-out
mkdir test-out >nul 2>&1

REM ---- Compile main sources ----
echo [BUILD] Compiling main source files...
dir /s /b src\*.java > main_sources.txt

set MAIN_CP=%MYSQL_JAR%
if "%MYSQL_JAR%" == "" set MAIN_CP=.

javac -cp "%MAIN_CP%" -d test-out @main_sources.txt 2> compile_errors.txt
if errorlevel 1 (
    echo [ERROR] Main source compilation failed!
    type compile_errors.txt
    del main_sources.txt 2>nul
    pause
    exit /b 1
)
del main_sources.txt 2>nul
echo [OK] Main sources compiled.

REM ---- Compile test sources ----
echo [BUILD] Compiling test source files...
dir /s /b test\*.java > test_sources.txt

javac -cp "%JUNIT_JAR%;test-out" -d test-out @test_sources.txt 2> compile_errors.txt
if errorlevel 1 (
    echo [ERROR] Test source compilation failed!
    type compile_errors.txt
    del test_sources.txt 2>nul
    pause
    exit /b 1
)
del test_sources.txt 2>nul
echo [OK] Test sources compiled.

REM ---- Run tests ----
echo.
echo ============================================
echo Running Unit Tests...
echo ============================================
echo.

java -cp "%JUNIT_JAR%;test-out;%MAIN_CP%" ^
    org.junit.platform.console.ConsoleLauncher ^
    --scan-classpath ^
    --include-classname ".*Test" ^
    --reports-dir="test-reports" ^
    --details=tree ^
    --fail-if-no-tests

set TEST_EXIT_CODE=%errorlevel%

echo.
if %TEST_EXIT_CODE% equ 0 (
    echo ============================================
    echo [SUCCESS] All tests passed!
    echo ============================================
) else (
    echo ============================================
    echo [FAILURE] Some tests failed. Check output above.
    echo ============================================
)

del compile_errors.txt 2>nul
echo.
echo Test reports saved to: test-reports\
pause
exit /b %TEST_EXIT_CODE%
