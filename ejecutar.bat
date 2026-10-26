@echo off
setlocal
title LoteSmart Enterprise - Arranque y Verificacion

echo ============================================================================
echo   LOTESMART ENTERPRISE - SISTEMA DE CONTROL DE INVENTARIO FEFO
echo ============================================================================
echo.

where mvn >nul 2>nul
if errorlevel 1 (
    if exist "C:\tools\apache-maven-3.9.9\bin" (
        set "PATH=C:\tools\apache-maven-3.9.9\bin;%PATH%"
    )
)

echo [1/3] Compilando el proyecto (mvn clean compile)...
call mvn clean compile
if errorlevel 1 (
    echo.
    echo [ERROR] La compilacion ha fallado.
    pause
    exit /b 1
)

echo.
echo [2/3] Ejecutando pruebas unitarias automatizadas (mvn test)...
call mvn test
if errorlevel 1 (
    echo.
    echo [ERROR] Una o mas pruebas unitarias han fallado.
    pause
    exit /b 1
)

echo.
echo [3/3] Iniciando interfaz grafica (mvn exec:java)...
call mvn exec:java -Dexec.mainClass="com.lotesmart.Main"

echo.
echo ============================================================================
echo   La aplicacion ha finalizado su ejecucion.
echo ============================================================================
pause