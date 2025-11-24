# Руководство по тестированию

Подробное руководство по тестированию всех функций Mockly Backend.

## Содержание

1. [Подготовка](#подготовка)
2. [Тестирование аутентификации](#тестирование-аутентификации)
3. [Тестирование сессий](#тестирование-сессий)
4. [Тестирование артефактов](#тестирование-артефактов)
5. [Тестирование отчетов](#тестирование-отчетов)
6. [Тестирование WebSocket](#тестирование-websocket)
7. [E2E сценарий](#e2e-сценарий)

## Подготовка

### Переменные для тестирования

Сохраните эти переменные для использования в примерах:

```bash
# Базовый URL
BASE_URL="http://localhost:8080"

# Токены (будут получены после регистрации/логина)
CANDIDATE_TOKEN=""
INTERVIEWER_TOKEN=""

# ID пользователей
CANDIDATE_ID=""
INTERVIEWER_ID=""

# ID сессии
SESSION_ID=""
```

## Тестирование аутентификации

### 1. Регистрация кандидата

```bash
curl -X POST $BASE_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "candidate@test.com",
    "password": "password123",
    "role": "CANDIDATE",
    "displayName": "John Candidate"
  }' | jq
```

Сохраните `accessToken` в `CANDIDATE_TOKEN` и `userId` в `CANDIDATE_ID`.

### 2. Регистрация интервьюера

```bash
curl -X POST $BASE_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "interviewer@test.com",
    "password": "password123",
    "role": "INTERVIEWER",
    "displayName": "Jane Interviewer"
  }' | jq
```

Сохраните `accessToken` в `INTERVIEWER_TOKEN` и `userId` в `INTERVIEWER_ID`.

### 3. Логин

```bash
curl -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "candidate@test.com",
    "password": "password123"
  }' | jq
```

### 4. Обновление токена

```bash
curl -X POST $BASE_URL/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your-refresh-token-here"
  }' | jq
```

### 5. Получение текущего пользователя

```bash
curl -X GET $BASE_URL/api/users/me \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" | jq
```

### 6. Обновление профиля

```bash
curl -X PATCH $BASE_URL/api/users/me \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "displayName": "John Updated",
    "level": "Senior",
    "skills": ["Java", "Spring", "PostgreSQL"]
  }' | jq
```

## Тестирование сессий

### 1. Создание сессии

```bash
curl -X POST $BASE_URL/api/sessions \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"interviewerId\": \"$INTERVIEWER_ID\",
    \"scheduledAt\": \"2024-12-31T10:00:00Z\"
  }" | jq
```

Сохраните `id` в `SESSION_ID`.

### 2. Получение сессии

```bash
curl -X GET $BASE_URL/api/sessions/$SESSION_ID \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" | jq
```

### 3. Присоединение к сессии

```bash
# Кандидат присоединяется
curl -X POST $BASE_URL/api/sessions/$SESSION_ID/join \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" | jq

# Интервьюер присоединяется
curl -X POST $BASE_URL/api/sessions/$SESSION_ID/join \
  -H "Authorization: Bearer $INTERVIEWER_TOKEN" | jq
```

### 4. Получение LiveKit токена

```bash
curl -X GET $BASE_URL/api/sessions/$SESSION_ID/token \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" | jq
```

Проверьте, что в токене есть поле `name` с реальным display name пользователя.

### 5. Список сессий

```bash
curl -X GET "$BASE_URL/api/sessions?page=0&size=10" \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" | jq
```

### 6. Активная сессия

```bash
curl -X GET $BASE_URL/api/sessions/me/active \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" | jq
```

### 7. Завершение сессии

```bash
curl -X POST $BASE_URL/api/sessions/$SESSION_ID/end \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" | jq
```

## Тестирование артефактов

### 1. Запрос URL для загрузки

```bash
curl -X POST "$BASE_URL/api/sessions/$SESSION_ID/artifacts/request-upload" \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "AUDIO_MIXED",
    "fileName": "interview.mp3",
    "fileSizeBytes": 1048576,
    "contentType": "audio/mpeg"
  }' | jq
```

Сохраните `artifactId` и `uploadUrl`.

### 2. Валидация размера файла (должна быть ошибка)

```bash
curl -X POST "$BASE_URL/api/sessions/$SESSION_ID/artifacts/request-upload" \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "AUDIO_MIXED",
    "fileName": "large.mp3",
    "fileSizeBytes": 600000000,
    "contentType": "audio/mpeg"
  }' | jq
```

Должна вернуться ошибка о превышении максимального размера (500MB).

### 3. Валидация типа файла (должна быть ошибка)

```bash
curl -X POST "$BASE_URL/api/sessions/$SESSION_ID/artifacts/request-upload" \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "AUDIO_MIXED",
    "fileName": "document.pdf",
    "fileSizeBytes": 1024,
    "contentType": "application/pdf"
  }' | jq
```

Должна вернуться ошибка о недопустимом типе файла.

### 4. Загрузка файла на MinIO

Создайте тестовый файл:

```bash
# Создать тестовый аудио файл (1MB)
dd if=/dev/zero of=test-audio.mp3 bs=1024 count=1024
```

Загрузите на MinIO:

```bash
UPLOAD_URL="pre-signed-url-from-step-1"
curl -X PUT "$UPLOAD_URL" \
  -H "Content-Type: audio/mpeg" \
  --data-binary "@test-audio.mp3"
```

### 5. Завершение загрузки

```bash
ARTIFACT_ID="artifact-id-from-step-1"

curl -X POST "$BASE_URL/api/sessions/$SESSION_ID/artifacts/$ARTIFACT_ID/complete" \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fileSizeBytes": 1048576,
    "durationSec": 120
  }' | jq
```

### 6. Список артефактов

```bash
curl -X GET "$BASE_URL/api/sessions/$SESSION_ID/artifacts" \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" | jq
```

### 7. Получение артефакта

```bash
curl -X GET "$BASE_URL/api/sessions/$SESSION_ID/artifacts/$ARTIFACT_ID" \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" | jq
```

## Тестирование отчетов

### 1. Запуск генерации отчета

```bash
curl -X POST "$BASE_URL/api/sessions/$SESSION_ID/report/trigger" \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" | jq
```

Проверьте, что статус `PENDING` или `PROCESSING`.

### 2. Проверка статуса отчета

```bash
curl -X GET "$BASE_URL/api/sessions/$SESSION_ID/report" \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" | jq
```

Статусы:
- `PENDING` - ожидает обработки
- `PROCESSING` - обрабатывается
- `READY` - готов
- `FAILED` - ошибка

### 3. Повторная генерация (если FAILED)

```bash
curl -X POST "$BASE_URL/api/sessions/$SESSION_ID/report/trigger" \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" | jq
```

## Тестирование WebSocket

### Подготовка

Установите зависимости:

```bash
npm install sockjs-client stompjs
```

### JavaScript пример

Создайте файл `test-websocket.js`:

```javascript
const SockJS = require('sockjs-client');
const Stomp = require('stompjs');

const token = process.argv[2] || 'your-token-here';
const sessionId = process.argv[3] || 'your-session-id';

const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

console.log('Connecting to WebSocket...');

stompClient.connect(
    { Authorization: `Bearer ${token}` },
    function(frame) {
        console.log('✅ Connected:', frame);
        
        // Подписаться на события сессии
        const sessionTopic = `/topic/sessions/${sessionId}`;
        stompClient.subscribe(sessionTopic, function(message) {
            const event = JSON.parse(message.body);
            console.log('📨 Session event:', event);
        });
        
        // Подписаться на события отчета
        const reportTopic = `/topic/sessions/${sessionId}/report`;
        stompClient.subscribe(reportTopic, function(message) {
            const event = JSON.parse(message.body);
            console.log('📊 Report event:', event);
        });
        
        console.log(`📡 Subscribed to: ${sessionTopic} and ${reportTopic}`);
    },
    function(error) {
        console.error('❌ Connection error:', error);
        if (error.headers && error.headers['message']) {
            console.error('Auth error:', error.headers['message']);
        }
    }
);

// Graceful shutdown
process.on('SIGINT', () => {
    console.log('\nDisconnecting...');
    if (stompClient && stompClient.connected) {
        stompClient.disconnect();
    }
    process.exit(0);
});
```

Запуск:

```bash
node test-websocket.js $CANDIDATE_TOKEN $SESSION_ID
```

### Тестирование без токена (должна быть ошибка)

```javascript
stompClient.connect(
    {}, // Без токена
    onConnect,
    function(error) {
        console.log('✅ Correctly rejected:', error);
    }
);
```

## E2E сценарий

Полный сценарий от регистрации до получения отчета:

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"

echo "1. Регистрация кандидата..."
CANDIDATE_RESPONSE=$(curl -s -X POST $BASE_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "e2e-candidate@test.com",
    "password": "password123",
    "role": "CANDIDATE",
    "displayName": "E2E Candidate"
  }')

CANDIDATE_TOKEN=$(echo $CANDIDATE_RESPONSE | jq -r '.accessToken')
CANDIDATE_ID=$(echo $CANDIDATE_RESPONSE | jq -r '.userId')
echo "✅ Candidate ID: $CANDIDATE_ID"

echo "2. Регистрация интервьюера..."
INTERVIEWER_RESPONSE=$(curl -s -X POST $BASE_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "e2e-interviewer@test.com",
    "password": "password123",
    "role": "INTERVIEWER",
    "displayName": "E2E Interviewer"
  }')

INTERVIEWER_TOKEN=$(echo $INTERVIEWER_RESPONSE | jq -r '.accessToken')
INTERVIEWER_ID=$(echo $INTERVIEWER_RESPONSE | jq -r '.userId')
echo "✅ Interviewer ID: $INTERVIEWER_ID"

echo "3. Создание сессии..."
SESSION_RESPONSE=$(curl -s -X POST $BASE_URL/api/sessions \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"interviewerId\": \"$INTERVIEWER_ID\",
    \"scheduledAt\": \"2024-12-31T10:00:00Z\"
  }")

SESSION_ID=$(echo $SESSION_RESPONSE | jq -r '.id')
echo "✅ Session ID: $SESSION_ID"

echo "4. Присоединение к сессии..."
curl -s -X POST "$BASE_URL/api/sessions/$SESSION_ID/join" \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" > /dev/null
echo "✅ Candidate joined"

curl -s -X POST "$BASE_URL/api/sessions/$SESSION_ID/join" \
  -H "Authorization: Bearer $INTERVIEWER_TOKEN" > /dev/null
echo "✅ Interviewer joined"

echo "5. Запрос URL для загрузки артефакта..."
UPLOAD_RESPONSE=$(curl -s -X POST "$BASE_URL/api/sessions/$SESSION_ID/artifacts/request-upload" \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "AUDIO_MIXED",
    "fileName": "e2e-test.mp3",
    "fileSizeBytes": 1048576,
    "contentType": "audio/mpeg"
  }')

ARTIFACT_ID=$(echo $UPLOAD_RESPONSE | jq -r '.artifactId')
UPLOAD_URL=$(echo $UPLOAD_RESPONSE | jq -r '.uploadUrl')
echo "✅ Artifact ID: $ARTIFACT_ID"

echo "6. Загрузка файла (симуляция)..."
# В реальном сценарии здесь была бы загрузка файла
echo "⚠️  Пропущено (требуется реальный файл)"

echo "7. Завершение загрузки..."
curl -s -X POST "$BASE_URL/api/sessions/$SESSION_ID/artifacts/$ARTIFACT_ID/complete" \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fileSizeBytes": 1048576,
    "durationSec": 120
  }' > /dev/null
echo "✅ Upload completed"

echo "8. Запуск генерации отчета..."
curl -s -X POST "$BASE_URL/api/sessions/$SESSION_ID/report/trigger" \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" > /dev/null
echo "✅ Report generation triggered"

echo "9. Проверка статуса отчета..."
sleep 2
REPORT_RESPONSE=$(curl -s -X GET "$BASE_URL/api/sessions/$SESSION_ID/report" \
  -H "Authorization: Bearer $CANDIDATE_TOKEN")

REPORT_STATUS=$(echo $REPORT_RESPONSE | jq -r '.status')
echo "✅ Report status: $REPORT_STATUS"

echo ""
echo "🎉 E2E тест завершен!"
echo "Session ID: $SESSION_ID"
echo "Report Status: $REPORT_STATUS"
```

Сохраните как `test-e2e.sh` и запустите:

```bash
chmod +x test-e2e.sh
./test-e2e.sh
```

## Проверка MinIO

### Доступ к консоли MinIO

1. Откройте http://localhost:19001
2. Логин: `minioadmin`
3. Пароль: `minioadmin`
4. Проверьте bucket `mockly-artifacts`
5. Убедитесь, что файлы загружаются в правильную структуру:
   ```
   sessions/
     {sessionId}/
       artifacts/
         {artifactId}/
           {fileName}
   ```

## Проверка базы данных

### Подключение к PostgreSQL

```bash
docker exec -it mockly-postgres psql -U mockly -d mockly
```

### Полезные запросы

```sql
-- Проверить пользователей
SELECT id, email, created_at FROM users;

-- Проверить сессии
SELECT id, status, created_at FROM sessions;

-- Проверить артефакты
SELECT id, type, size_bytes, created_at FROM artifacts;

-- Проверить отчеты
SELECT id, session_id, status, created_at FROM reports;

-- Проверить транскрипты
SELECT id, session_id, source, created_at FROM transcripts;
```

## Автоматизированное тестирование

### Запуск unit тестов

```bash
mvn test
```

### Запуск integration тестов

```bash
mvn verify
```

### Запуск тестов конкретного модуля

```bash
cd mockly-core
mvn test
```

## Чек-лист тестирования

- [ ] Регистрация и логин работают
- [ ] JWT токены валидны и работают
- [ ] Создание сессии работает
- [ ] Присоединение к сессии работает
- [ ] LiveKit токен содержит реальный display name
- [ ] Загрузка артефактов работает
- [ ] Валидация размера файла работает (500MB лимит)
- [ ] Валидация типа файла работает
- [ ] Генерация отчета запускается
- [ ] WebSocket подключение требует JWT токен
- [ ] WebSocket события приходят корректно
- [ ] Отчеты сохраняются в БД
- [ ] Транскрипты сохраняются в БД

