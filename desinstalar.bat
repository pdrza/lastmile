@echo off
chcp 65001 >nul
title Desinstalar - Sistema de Entregas Last Mile
echo.
echo ATENCAO: isto remove o sistema e APAGA TODOS OS DADOS
echo permanentemente (lojas, entregas e rotas cadastradas).
echo.
set /p resposta="Digite SIM em maiusculas para confirmar: "
if /i not "%resposta%"=="SIM" (
  echo.
  echo Operacao cancelada. Nada foi removido.
  echo.
  pause
  exit /b 0
)
echo.
echo Removendo o sistema e os dados...
docker compose -f docker-compose.full.yml down -v
echo.
echo Sistema removido por completo.
echo.
pause
