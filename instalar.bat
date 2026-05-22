@echo off
chcp 65001 >nul
title Instalador - Sistema de Entregas Last Mile
echo.
echo ==================================================
echo    Instalando o Sistema de Entregas Last Mile
echo ==================================================
echo.

docker --version >nul 2>&1
if errorlevel 1 (
  echo [ERRO] Docker Desktop nao encontrado nesta maquina.
  echo Instale em: https://www.docker.com/products/docker-desktop
  echo Depois abra o Docker Desktop e rode este instalador de novo.
  echo.
  pause
  exit /b 1
)

docker info >nul 2>&1
if errorlevel 1 (
  echo [ERRO] O Docker Desktop esta instalado mas nao esta rodando.
  echo Abra o Docker Desktop, espere o icone ficar verde, e rode de novo.
  echo.
  pause
  exit /b 1
)

echo Construindo e iniciando o sistema...
echo (A primeira vez demora alguns minutos e precisa de internet.)
echo.
docker compose -f docker-compose.full.yml up -d --build
if errorlevel 1 (
  echo.
  echo [ERRO] Falha ao iniciar. Verifique a conexao com a internet.
  echo.
  pause
  exit /b 1
)

echo.
echo Aguardando o sistema ficar pronto...
powershell -NoProfile -Command "$ok=$false; for($i=0;$i -lt 60;$i++){ try{ if((Invoke-WebRequest -UseBasicParsing http://localhost:8080/actuator/health -TimeoutSec 3).StatusCode -eq 200){$ok=$true;break} }catch{}; Start-Sleep 3 }; if(-not $ok){ exit 1 }"
if errorlevel 1 (
  echo.
  echo [AVISO] O sistema demorou para responder.
  echo Espere mais um pouco e abra manualmente: http://localhost:8080
  echo.
  pause
  exit /b 1
)

echo.
echo ==================================================
echo    Pronto! O sistema esta rodando.
echo    Acesse no navegador: http://localhost:8080
echo ==================================================
echo.
start http://localhost:8080
echo Para parar o sistema, use o arquivo  parar.bat
echo.
pause
