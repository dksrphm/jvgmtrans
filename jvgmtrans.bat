@echo off
if "%1"== "" goto USAGE

set VGMFILE=%1
echo %VGMFILE%

java -DDEBUG -classpath .;./bin jvgmtrans.Jvgmtrans "%VGMFILE%" > jvgmtrans.log

rem pause
exit /b

:USAGE
echo Usage: jvgmtrans.bat filename.vgm
exit /b 1
