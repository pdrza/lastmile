@echo off
chcp 65001 >nul
title Parar - Sistema de Entregas Last Mile
echo.
echo Parando o sistema...
docker compose -f docker-compose.full.yml stop
echo.
echo Sistema parado. Seus dados foram preservados.
echo Use o arquivo  instalar.bat  para ligar de novo.
echo.
pause
