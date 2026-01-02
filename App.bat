@echo off
setlocal

set RUNTIME=custom-runtime\bin\java.exe
set MODULEPATH=javafx-sdk\lib
set JAR=target\LindaVistaMusicPlayer-1.0-SNAPSHOT-shaded.jar

%RUNTIME% ^
  -Dprism.order=sw ^
  --module-path "%MODULEPATH%" ^
  --add-modules javafx.controls,javafx.fxml,javafx.media ^
  -jar "%JAR%"

endlocal
pause
