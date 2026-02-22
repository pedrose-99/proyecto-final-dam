@echo off
chcp 65001 >nul
title SmartCart - Comparador de precios

echo ============================================
echo          SmartCart - Inicio rapido
echo ============================================
echo.

:: Verificar que Docker esta corriendo
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker no esta en ejecucion.
    echo         Asegurate de tener Docker Desktop iniciado.
    echo.
    pause
    exit /b 1
)

echo [OK] Docker detectado.
echo.

:: Verificar que docker-compose esta disponible
docker compose version >nul 2>&1
if %errorlevel% neq 0 (
    docker-compose version >nul 2>&1
    if %errorlevel% neq 0 (
        echo [ERROR] docker-compose no esta disponible.
        echo         Instala Docker Desktop que incluye docker compose.
        echo.
        pause
        exit /b 1
    )
    set COMPOSE_CMD=docker-compose
) else (
    set COMPOSE_CMD=docker compose
)

echo [OK] Docker Compose detectado.
echo.

echo Selecciona una opcion:
echo.
echo   1. Iniciar todos los servicios
echo   2. Iniciar en segundo plano (detached)
echo   3. Parar todos los servicios
echo   4. Reiniciar todos los servicios
echo   5. Ver logs
echo   6. Ver estado de los contenedores
echo   7. Limpiar todo (para + elimina volumenes)
echo   0. Salir
echo.

set /p opcion="Opcion: "

if "%opcion%"=="1" goto start
if "%opcion%"=="2" goto start_detached
if "%opcion%"=="3" goto stop
if "%opcion%"=="4" goto restart
if "%opcion%"=="5" goto logs
if "%opcion%"=="6" goto status
if "%opcion%"=="7" goto clean
if "%opcion%"=="0" goto end

echo [ERROR] Opcion no valida.
pause
exit /b 1

:start
echo.
echo Iniciando SmartCart...
echo (Esto puede tardar unos minutos la primera vez)
echo.
%COMPOSE_CMD% up --build
goto end

:start_detached
echo.
echo Iniciando SmartCart en segundo plano...
echo (Esto puede tardar unos minutos la primera vez)
echo.
%COMPOSE_CMD% up --build -d
if %errorlevel% equ 0 (
    echo.
    echo ============================================
    echo   SmartCart iniciado correctamente
    echo ============================================
    echo.
    echo   Frontend:   http://localhost:4200
    echo   Backend:    http://localhost:8081
    echo   GraphiQL:   http://localhost:8081/graphiql
    echo   PostgreSQL: localhost:5434
    echo.
    echo   Para ver los logs: %COMPOSE_CMD% logs -f
    echo   Para parar:        %COMPOSE_CMD% down
    echo ============================================
)
pause
goto end

:stop
echo.
echo Parando SmartCart...
%COMPOSE_CMD% down
echo.
echo [OK] Todos los servicios han sido detenidos.
pause
goto end

:restart
echo.
echo Reiniciando SmartCart...
%COMPOSE_CMD% down
%COMPOSE_CMD% up --build -d
echo.
echo [OK] SmartCart reiniciado.
echo.
echo   Frontend:   http://localhost:4200
echo   Backend:    http://localhost:8081
echo   GraphiQL:   http://localhost:8081/graphiql
echo.
pause
goto end

:logs
echo.
echo Mostrando logs (Ctrl+C para salir)...
echo.
%COMPOSE_CMD% logs -f
goto end

:status
echo.
%COMPOSE_CMD% ps
echo.
pause
goto end

:clean
echo.
echo ATENCION: Esto eliminara todos los contenedores y los datos de la base de datos.
set /p confirmar="Estas seguro? (s/n): "
if /i "%confirmar%"=="s" (
    %COMPOSE_CMD% down -v
    echo.
    echo [OK] Todo limpio. Los volumenes han sido eliminados.
) else (
    echo Operacion cancelada.
)
pause
goto end

:end
