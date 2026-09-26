@echo off
rem One-time install of the PhoneVision vendor library into your WPILib offline vendor repo.
rem Usage: install.bat [frcYear=2026] [optional: path to a robot project]
set YEAR=%1
if "%YEAR%"=="" set YEAR=2026
set ROOT=%PUBLIC%\wpilib\%YEAR%
mkdir "%ROOT%\maven" 2>nul
mkdir "%ROOT%\vendordeps" 2>nul
xcopy /E /I /Y "%~dp0maven" "%ROOT%\maven" >nul
powershell -NoProfile -Command "(Get-Content '%~dp0PhoneVision.json') -replace '\"frcYear\": \"[0-9]+\"','\"frcYear\": \"%YEAR%\"' | Set-Content '%ROOT%\vendordeps\PhoneVision.json'"
echo Installed to %ROOT% (maven + vendordeps).
if not "%2"=="" (
  mkdir "%2\vendordeps" 2>nul
  copy /Y "%ROOT%\vendordeps\PhoneVision.json" "%2\vendordeps\" >nul
  echo Added to project %2\vendordeps\PhoneVision.json
) else (
  echo Now in VS Code: WPILib ^> Manage Vendor Libraries ^> Install new libraries ^(offline^) ^> PhoneVision.
)
