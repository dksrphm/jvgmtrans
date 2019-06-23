@echo off
set VGMFILE="%1"
echo %VGMFILE%

java -DDEBUG -cp .;./jvgmtrans jvgmtrans/Jvgmtrans "%VGMFILE%" > jvgmtrans.log
rem java -cp .;./jvgmtrans jvgmtrans/Jvgmtrans "%VGMFILE%" > jvgmtrans.log

pause
