# Скрипт для проверки подключения к базе данных

Write-Host "=== Проверка Docker контейнеров ===" -ForegroundColor Cyan
docker ps --filter "name=mockly" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

Write-Host "`n=== Проверка подключения к PostgreSQL ===" -ForegroundColor Cyan
docker exec mockly-postgres psql -U mockly -d mockly -c "SELECT version();" 2>&1

Write-Host "`n=== Проверка таблиц ===" -ForegroundColor Cyan
docker exec mockly-postgres psql -U mockly -d mockly -c "\dt" 2>&1

Write-Host "`n=== Проверка Redis ===" -ForegroundColor Cyan
docker exec mockly-redis redis-cli ping 2>&1

Write-Host "`n=== Проверка портов ===" -ForegroundColor Cyan
netstat -an | findstr ":5432 :6379 :9000 :8080"

