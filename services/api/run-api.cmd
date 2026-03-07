@echo off
REM Run the Loan Shark API (Spring Boot). Uses Maven if available.
where mvn >nul 2>&1
if %ERRORLEVEL% equ 0 (
  mvn spring-boot:run
  exit /b %ERRORLEVEL%
)
echo Maven (mvn) is not in your PATH.
echo.
echo To run the API, do one of the following:
echo.
echo 1. Install Maven (recommended), then run: mvn spring-boot:run
echo    - Using winget:  winget install Apache.Maven
echo    - Then close and reopen PowerShell, cd to this folder, run: mvn spring-boot:run
echo.
echo 2. Run from your IDE: open this project in IntelliJ IDEA, Eclipse, or VS Code
echo    with the Spring Boot extension, and run the main class
echo    com.loanshark.api.LoanSharkApiApplication
echo.
pause
