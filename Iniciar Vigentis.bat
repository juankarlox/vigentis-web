@echo off
rem ============================================================
rem  Vigentis 1.0 - Inicia la aplicacion y abre el navegador.
rem  Doble clic aqui (o en el acceso "Vigentis" del Escritorio).
rem  Para APAGAR Vigentis: cierra la ventana "Vigentis" de la barra de tareas.
rem ============================================================
setlocal
cd /d "%~dp0"

rem --- La primera vez: crear el acceso directo "Vigentis" en el Escritorio ---
if not exist "%~dp0.acceso_creado" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$s=(New-Object -ComObject WScript.Shell).CreateShortcut([Environment]::GetFolderPath('Desktop')+'\Vigentis.lnk'); $s.TargetPath='%~f0'; $s.WorkingDirectory='%~dp0'; $s.IconLocation='%~dp0vigentis.ico'; $s.WindowStyle=7; $s.Description='Iniciar Vigentis'; $s.Save()" >nul 2>&1
  echo creado>"%~dp0.acceso_creado"
)

rem --- Si Vigentis ya esta encendido, solo abrir el navegador ---
powershell -NoProfile -Command "$ProgressPreference='SilentlyContinue'; try{$null=Invoke-WebRequest 'http://localhost:8080/' -UseBasicParsing -TimeoutSec 3; exit 0}catch{exit 1}" >nul 2>&1
if not errorlevel 1 (
  start "" "http://localhost:8080"
  exit /b
)

rem --- Volver a abrirse minimizado para no estorbar ---
if not "%~1"=="min" (
  start "Vigentis" /min cmd /c ""%~f0" min"
  exit /b
)

title Vigentis - NO cierres esta ventana (cerrarla apaga Vigentis)
echo.
echo  Iniciando Vigentis... el navegador se abrira solo en unos segundos.
echo  Para apagar Vigentis, cierra esta ventana.
echo.

rem --- En segundo plano: esperar a que arranque y abrir el navegador ---
start "" /b powershell -NoProfile -Command "$ProgressPreference='SilentlyContinue'; for($i=0;$i -lt 150;$i++){try{$null=Invoke-WebRequest 'http://localhost:8080/' -UseBasicParsing -TimeoutSec 2; Start-Process 'http://localhost:8080'; exit}catch{Start-Sleep -Seconds 2}}"

cd backend
call mvnw.cmd -q spring-boot:run

if errorlevel 1 (
  powershell -NoProfile -Command "Add-Type -AssemblyName PresentationFramework; [void][System.Windows.MessageBox]::Show('Vigentis no pudo iniciar o se detuvo con un error. Abre la ventana Vigentis en la barra de tareas para ver el motivo. Revisa que MySQL este encendido.','Vigentis','OK','Error')"
)
echo.
echo  Vigentis se detuvo.
pause
