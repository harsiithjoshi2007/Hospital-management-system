@echo off
REM =============================================
REM Hospital Management System - Build & Run
REM Windows Batch Script
REM =============================================
setlocal enabledelayedexpansion

echo.
echo ============================================
echo Hospital Resource Management System
echo Build and Run Script (Windows)
echo ============================================
echo.

REM ---- Configuration ----
REM Set the path to your MySQL Connector JAR file
REM Download from: https://dev.mysql.com/downloads/connector/j/
REM Place the JAR in this directory or update the path below
set MYSQL_JAR=mysql-connector-j-8.4.0.jar

REM ---- Check for MySQL Connector ----
if not exist "%MYSQL_JAR%" (
    echo [WARN] MySQL Connector JAR not found: %MYSQL_JAR%
    echo [WARN] Searching for mysql-connector-*.jar...
    dir /b mysql-connector-*.jar 2>nul
    if errorlevel 1 (
        echo [ERROR] No MySQL Connector JAR found!
        echo.
        echo Please download mysql-connector-j from:
        echo   https://dev.mysql.com/downloads/connector/j/
        echo.
        echo Then place the JAR file in this directory and run again.
        echo Alternatively, edit MYSQL_JAR in build.bat to point to your JAR.
        pause
        exit /b 1
    )
)

REM Find the connector jar
for /f "tokens=*" %%a in ('dir /b /o:-d mysql-connector-*.jar 2^>nul') do (
    set MYSQL_JAR=%%a
    goto :found_jar
)
:found_jar
echo [OK] Found MySQL Connector: %MYSQL_JAR%

REM ---- Check for Java ----
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found! Please install JDK 17+.
    echo Download from: https://adoptium.net/
    pause
    exit /b 1
)
javac -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] javac not found! Please install JDK 17+.
    pause
    exit /b 1
)
echo [OK] Java and javac found.

REM ---- Clean previous build ----
echo.
echo [BUILD] Cleaning previous build...
if exist out rmdir /s /q out
mkdir out >nul 2>&1

REM ---- Compile ----
echo [BUILD] Compiling Java source files...
echo.

dir /s /b src\*.java > sources.txt

javac -cp "%MYSQL_JAR%" -d out @sources.txt 2> compile_errors.txt

if errorlevel 1 (
    echo [ERROR] Compilation failed!
    echo.
    type compile_errors.txt
    del sources.txt 2>nul
    pause
    exit /b 1
)

del sources.txt 2>nul
echo [OK] Compilation successful!

REM ---- Run ----
echo.
echo ============================================
echo Starting Hospital Management System...
echo ============================================
echo.
echo Make sure MySQL is running and the database
echo 'hospital_db' has been created.
echo.
echo If this is your first time, run the SQL script first:
echo   mysql -u root -p ^< sql\schema.sql
echo.
echo ============================================
echo.

java -cp "%MYSQL_JAR%;out" com.hospital.Main

if errorlevel 1 (
    echo.
    echo [ERROR] Application exited with error.
    pause
)

del compile_errors.txt 2>nul
