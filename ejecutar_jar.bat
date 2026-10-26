@echo off
setlocal
title LoteSmart Enterprise - Arranque Directo JAR

echo ============================================================================
echo   LOTESMART ENTERPRISE - INICIANDO DESDE FAT JAR
echo ============================================================================
echo.

if not exist "target\lotesmart-enterprise-1.0.0.jar" (
    echo El archivo target\lotesmart-enterprise-1.0.0.jar no existe.
    echo Compilando y generando el JAR...
    call ejecutar.bat
    exit /b
)

java -jar target\lotesmart-enterprise-1.0.0.jar
pause