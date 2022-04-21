@echo off
set BATPATH=%~dp0

if "%1"== "" goto USAGE

set VGMFILE=%1
echo %VGMFILE%

java -DDEBUG -classpath "%BATPATH%/bin" jvgmtrans.Jvgmtrans "%VGMFILE%" > jvgmtrans.log

rem pause
exit /b

:USAGE
echo Usage: jvgmtrans.bat filename.vgm
exit /b 1
