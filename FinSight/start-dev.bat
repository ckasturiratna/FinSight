@echo off
setlocal EnableExtensions EnableDelayedExpansion

REM FinSight Windows Development Startup Script
REM Starts backend (Spring Boot) and frontend (Vite) concurrently

REM Resolve script directory
set "SCRIPT_DIR=%~dp0"
set "BACKEND_DIR=%SCRIPT_DIR%finsight_backend"
set "FRONTEND_DIR=%SCRIPT_DIR%finsight_frontend"

echo [INFO] Starting FinSight Development Environment (Windows)
echo [INFO] Backend directory: %BACKEND_DIR%
echo [INFO] Frontend directory: %FRONTEND_DIR%

REM Validate directories
if not exist "%BACKEND_DIR%" (
  echo [ERROR] Backend directory not found: %BACKEND_DIR%
  exit /b 1
)
if not exist "%FRONTEND_DIR%" (
  echo [ERROR] Frontend directory not found: %FRONTEND_DIR%
  exit /b 1
)

REM Optional: quick prerequisite hints
where java >nul 2>&1 || echo [WARN] Java not found in PATH. Spring Boot may fail to start.
where node >nul 2>&1 || echo [WARN] Node.js not found in PATH. Frontend may fail to start.
where npm  >nul 2>&1 || echo [WARN] npm not found in PATH. Frontend may fail to start.

REM Install frontend dependencies if needed
if not exist "%FRONTEND_DIR%\node_modules" (
  echo [INFO] Installing frontend dependencies...
  pushd "%FRONTEND_DIR%"
  call npm install
  if errorlevel 1 (
    echo [ERROR] npm install failed
    popd
    exit /b 1
  )
  popd
  echo [INFO] Frontend dependencies installed
)

REM Prepare logs at repo root
pushd "%SCRIPT_DIR%"
if not exist backend.log type nul > backend.log
if not exist frontend.log type nul > frontend.log
popd

REM Start backend (prefer Maven Wrapper if present)
if exist "%BACKEND_DIR%\mvnw.cmd" (
  echo [INFO] Starting backend using mvnw.cmd ...
  start "FinSight Backend" cmd /c ""cd /d \"%BACKEND_DIR%\" && call mvnw.cmd spring-boot:run >> \"%SCRIPT_DIR%backend.log\" 2>>&1""
) else (
  echo [INFO] Starting backend using mvn ...
  start "FinSight Backend" cmd /c ""cd /d \"%BACKEND_DIR%\" && mvn spring-boot:run >> \"%SCRIPT_DIR%backend.log\" 2>>&1""
)

REM Start frontend
echo [INFO] Starting frontend (Vite)...
start "FinSight Frontend" cmd /c ""cd /d \"%FRONTEND_DIR%\" && npm run dev >> \"%SCRIPT_DIR%frontend.log\" 2>>&1""

REM Give services a moment to start
ping 127.0.0.1 -n 4 >nul

echo.
echo [SUCCESS] FinSight Development Environment is launching.
echo   Backend:  http://localhost:8080
echo   Frontend: http://localhost:5173

echo.
echo [INFO] Logs (repo root):
echo   Backend:  backend.log  ^(type backend.log ^| more^)
echo   Frontend: frontend.log ^(type frontend.log ^| more^)

echo.
echo [INFO] Two new windows were opened. Close those windows to stop services.
echo Press any key to exit this helper window...
pause >nul

endlocal
