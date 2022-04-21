@echo off
set BATPATH=%~dp0

rem check how execute this batch file
rem from explorer -> PFLAG=1, pause when exit
rem from CMD.EXE -> PFLAG=0, not pause when exit
set PFLAG=0
set CSTR=%cmdcmdline%
set CSTR=%CSTR:"=%
rem "
if not "%CSTR%" == "%CSTR:/c=%" set PFLAG=1

if "%~1"=="" goto USAGE

set VGMFILE="%~1"
echo Processing %VGMFILE%

java -DDEBUG -classpath "%BATPATH%/bin" jvgmtrans.Jvgmtrans %VGMFILE% > jvgmtrans.log

if %PFLAG%==1 pause
exit /b

:USAGE
echo Usage: jvgmtrans.bat filename.vgm
if %PFLAG%==1 pause
exit /b 1
