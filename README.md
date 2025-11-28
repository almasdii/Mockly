# Mockly Backend

Backend платформа для проведения mock интервью с поддержкой WebRTC, обработки аудио через ML сервисы и генерации отчетов.

## 📋 Содержание

- [Описание](#описание)
- [Технологии](#технологии)
- [Требования](#требования)
- [Установка](#установка)
- [Конфигурация](#конфигурация)
- [Запуск проекта](#запуск-проекта)
- [Тестирование](#тестирование)
- [API Документация](#api-документация)
- [Структура проекта](#структура-проекта)

## 📖 Описание

Mockly - это платформа для проведения mock интервью с следующими возможностями:

- **Аутентификация и авторизация** - JWT токены, регистрация, логин
- **Управление сессиями** - создание, присоединение, завершение интервью
- **WebRTC интеграция** - LiveKit для видеозвонков
- **Загрузка артефактов** - MinIO для хранения аудио файлов (до 500MB)
- **ML обработка** - интеграция с ML сервисом для анализа интервью
- **Генерация отчетов** - автоматическая генерация отчетов с метриками
- **WebSocket** - real-time обновления через STOMP
- **Транскрипты** - сохранение транскриптов интервью

## 🛠 Технологии

- **Java 21** - язык программирования
- **Spring Boot 3.3.2** - фреймворк
- **PostgreSQL 16** - база данных
- **Redis 7** - кэширование
- **MinIO** - S3-совместимое хранилище
- **Flyway** - миграции БД
- **JWT** - аутентификация
- **WebSocket/STOMP** - real-time коммуникация
- **LiveKit** - WebRTC платформа
- **Maven** - управление зависимостями

## 📦 Требования

Перед запуском убедитесь, что установлены:

- **Java 21** или выше
- **Maven 3.8+**
- **Docker** и **Docker Compose** (для инфраструктуры)
- **Git**

Проверка версий:

```bash
java -version  # Должна быть Java 21+
mvn -version   # Должна быть Maven 3.8+
docker --version
docker-compose --version
```

## 🚀 Установка

### 1. Клонирование репозитория

```bash
git clone <repository-url>
cd mock4
```

### 2. Сборка проекта

```bash
mvn clean install
```

Это соберет все модули проекта:
- `mockly-data` - слой данных
- `mockly-security` - безопасность
- `mockly-core` - бизнес-логика
- `mockly-api` - REST API

## ⚙️ Конфигурация

### Переменные окружения

Проект использует переменные окружения для конфигурации. Основные настройки находятся в `mockly-api/src/main/resources/application.yml`.

#### Обязательные переменные (для продакшена):

```bash
# JWT
JWT_SECRET=your-secret-key-must-be-at-least-64-bytes-long-for-hs512-algorithm

# База данных
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mockly
SPRING_DATASOURCE_USERNAME=mockly
SPRING_DATASOURCE_PASSWORD=mockly_password

# MinIO
MINIO_ENDPOINT=http://localhost:19000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=mockly-artifacts

# LiveKit (опционально)
LIVEKIT_URL=http://localhost:7880
LIVEKIT_API_KEY=your-api-key
LIVEKIT_API_SECRET=your-api-secret

# ML Service (опционально)
ML_SERVICE_URL=http://localhost:8000
```

#### Создание .env файла (опционально)

Создайте файл `.env` в корне проекта:

```env
JWT_SECRET=your-secret-key-must-be-at-least-64-bytes-long-for-hs512-algorithm-to-work-properly-and-securely-in-production-environment
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080
```

## 🏃 Запуск проекта

### Шаг 1: Запуск инфраструктуры (Docker Compose)

Запустите PostgreSQL, Redis и MinIO:

```bash
docker-compose up -d
```

Проверьте, что все сервисы запущены:

```bash
docker-compose ps
```

Должны быть запущены:
- `mockly-postgres` (порт 5432)
- `mockly-redis` (порт 26379)
- `mockly-minio` (порты 19000, 19001)

### Шаг 2: Проверка подключения к БД

Убедитесь, что PostgreSQL доступен:

```bash
# Windows PowerShell
.\check-connection.ps1

# Linux/Mac
./check-connection.sh
```

Или вручную:

```bash
docker exec -it mockly-postgres psql -U mockly -d mockly -c "SELECT 1;"
```

### Шаг 3: Запуск приложения

#### Вариант A: Через Maven

```bash
cd mockly-api
mvn spring-boot:run
```

#### Вариант B: Через IDE

1. Откройте проект в IntelliJ IDEA или Eclipse
2. Найдите класс `MocklyApplication.java` в `mockly-api/src/main/java/com/mockly/`
3. Запустите как Spring Boot приложение

#### Вариант C: Собранный JAR

```bash
mvn clean package
java -jar mockly-api/target/mockly-api-1.0.0-SNAPSHOT.jar
```

### Шаг 4: Проверка запуска

Приложение должно запуститься на `http://localhost:8080`

Проверьте health endpoint:

```bash
curl http://localhost:8080/actuator/health
```

Или откройте в браузере:
- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/v3/api-docs

## 🧪 Тестирование

### 1. Тестирование через Swagger UI

1. Откройте http://localhost:8080/swagger-ui.html
2. Найдите секцию **Authentication**
3. Зарегистрируйте пользователя:
   - Endpoint: `POST /api/auth/register`
   - Body:
     ```json
     {
       "email": "candidate@example.com",
       "password": "password123",
       "role": "CANDIDATE",
       "name": "John",
       "surname": "Doe"
     }
     ```
4. Скопируйте `accessToken` из ответа
5. Нажмите кнопку **Authorize** вверху страницы
6. Введите: `Bearer <ваш-token>`
7. Теперь можете тестировать защищенные endpoints

### 2. Тестирование через cURL

#### Регистрация пользователя

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "candidate@example.com",
    "password": "password123",
    "role": "CANDIDATE",
    "name": "John",
    "surname": "Doe"
  }'
```

Ответ:
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "userId": "123e4567-e89b-12d3-a456-426614174000"
}
```

#### Логин

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "candidate@example.com",
    "password": "password123"
  }'
```

#### Создание сессии (требует авторизации)

```bash
TOKEN="your-access-token-here"

curl -X POST http://localhost:8080/api/sessions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "interviewerId": "interviewer-uuid-here",
    "scheduledAt": "2024-12-31T10:00:00Z"
  }'
```

#### Получение текущего пользователя

```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Тестирование WebSocket

#### Подключение с JWT токеном

Используйте примеры из файла `WEBSOCKET_CLIENT_EXAMPLE.md`

Быстрый тест с Node.js:

```javascript
const SockJS = require('sockjs-client');
const Stomp = require('stompjs');

const token = 'your-access-token-here';
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect(
    { Authorization: `Bearer ${token}` },
    function(frame) {
        console.log('Connected: ' + frame);
        
        // Подписаться на события сессии
        const sessionId = 'your-session-id';
        stompClient.subscribe(`/topic/sessions/${sessionId}`, function(message) {
            console.log('Event:', JSON.parse(message.body));
        });
    },
    function(error) {
        console.error('Connection error:', error);
    }
);
```

### 4. Тестирование загрузки артефактов

#### Шаг 1: Запросить URL для загрузки

```bash
SESSION_ID="your-session-id"
TOKEN="your-access-token"

curl -X POST "http://localhost:8080/api/sessions/$SESSION_ID/artifacts/request-upload" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "type": "AUDIO_MIXED",
    "fileName": "interview.mp3",
    "fileSizeBytes": 1048576,
    "contentType": "audio/mpeg"
  }'
```

Ответ:
```json
{
  "artifactId": "artifact-uuid",
  "uploadUrl": "http://localhost:19000/mockly-artifacts/...",
  "objectName": "sessions/.../artifacts/.../interview.mp3",
  "expiresInSeconds": 3600
}
```

#### Шаг 2: Загрузить файл на MinIO

```bash
UPLOAD_URL="pre-signed-url-from-step-1"

curl -X PUT "$UPLOAD_URL" \
  -H "Content-Type: audio/mpeg" \
  --data-binary "@interview.mp3"
```

#### Шаг 3: Завершить загрузку

```bash
ARTIFACT_ID="artifact-id-from-step-1"

curl -X POST "http://localhost:8080/api/sessions/$SESSION_ID/artifacts/$ARTIFACT_ID/complete" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "fileSizeBytes": 1048576,
    "durationSec": 120
  }'
```

### 5. Тестирование генерации отчета

```bash
SESSION_ID="your-session-id"
TOKEN="your-access-token"

# Запустить генерацию отчета
curl -X POST "http://localhost:8080/api/sessions/$SESSION_ID/report/trigger" \
  -H "Authorization: Bearer $TOKEN"

# Проверить статус отчета
curl -X GET "http://localhost:8080/api/sessions/$SESSION_ID/report" \
  -H "Authorization: Bearer $TOKEN"
```

## 📚 API Документация

### Основные эндпоинты

#### Аутентификация
- `POST /api/auth/register` - Регистрация
- `POST /api/auth/login` - Вход
- `POST /api/auth/refresh` - Обновление токена
- `POST /api/auth/logout` - Выход

#### Пользователи
- `GET /api/users/me` - Текущий пользователь
- `PATCH /api/users/me` - Обновление профиля
- `GET /api/users/{id}` - Пользователь по ID

#### Сессии
- `POST /api/sessions` - Создать сессию
- `GET /api/sessions` - Список сессий
- `GET /api/sessions/{id}` - Сессия по ID
- `POST /api/sessions/{id}/join` - Присоединиться
- `POST /api/sessions/{id}/leave` - Покинуть
- `POST /api/sessions/{id}/end` - Завершить
- `GET /api/sessions/{id}/token` - LiveKit токен

#### Артефакты
- `POST /api/sessions/{id}/artifacts/request-upload` - Запросить URL загрузки
- `POST /api/sessions/{id}/artifacts/{artifactId}/complete` - Завершить загрузку
- `GET /api/sessions/{id}/artifacts` - Список артефактов
- `GET /api/sessions/{id}/artifacts/{artifactId}` - Артефакт по ID

#### Отчеты
- `POST /api/sessions/{id}/report/trigger` - Запустить генерацию
- `GET /api/sessions/{id}/report` - Получить отчет

### Swagger UI

Интерактивная документация доступна по адресу:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## 📁 Структура проекта

```
mock4/
├── mockly-api/              # REST API слой
│   ├── controller/         # REST контроллеры
│   ├── websocket/          # WebSocket конфигурация
│   └── config/             # Конфигурация API
├── mockly-core/             # Бизнес-логика
│   ├── service/            # Сервисы
│   ├── dto/                # Data Transfer Objects
│   └── config/             # Конфигурация сервисов
├── mockly-data/            # Слой данных
│   ├── entity/             # JPA сущности
│   ├── repository/         # Репозитории
│   └── resources/
│       └── db/migration/    # Flyway миграции
├── mockly-security/        # Безопасность
│   ├── jwt/                # JWT провайдер
│   └── config/             # Security конфигурация
├── docker-compose.yml      # Docker Compose конфигурация
└── pom.xml                 # Родительский POM
```

## 🔧 Настройка MinIO

После запуска Docker Compose, MinIO доступен:
- **API**: http://localhost:19000
- **Console**: http://localhost:19001
- **Credentials**: `minioadmin` / `minioadmin`

Bucket `mockly-artifacts` создается автоматически при старте приложения.

## 🔧 Настройка LiveKit (опционально)

Для работы с WebRTC нужен запущенный LiveKit сервер:

1. Установите LiveKit: https://docs.livekit.io/home/self-hosting/deployment/
2. Получите API ключ и секрет
3. Настройте переменные окружения:
   ```bash
   LIVEKIT_URL=http://localhost:7880
   LIVEKIT_API_KEY=your-api-key
   LIVEKIT_API_SECRET=your-api-secret
   ```

## 🔧 Настройка ML Service (опционально)

Для генерации отчетов нужен ML сервис:

1. Запустите ML сервис на порту 8000
2. Сервис должен иметь endpoint: `POST /api/process`
3. Формат запроса/ответа описан в `MLProcessRequest` и `MLProcessResponse`

## 🐛 Решение проблем

### Проблема: Приложение не запускается

1. Проверьте, что PostgreSQL запущен:
   ```bash
   docker-compose ps
   ```

2. Проверьте логи:
   ```bash
   docker-compose logs postgres
   ```

3. Проверьте подключение к БД:
   ```bash
   docker exec -it mockly-postgres psql -U mockly -d mockly
   ```

### Проблема: Ошибка миграций Flyway

1. Проверьте, что БД создана:
   ```sql
   CREATE DATABASE mockly;
   ```

2. Очистите схему и перезапустите:
   ```bash
   docker-compose down -v
   docker-compose up -d
   ```

### Проблема: MinIO не доступен

1. Проверьте, что контейнер запущен:
   ```bash
   docker-compose ps minio
   ```

2. Проверьте логи:
   ```bash
   docker-compose logs minio
   ```

3. Убедитесь, что порты 19000 и 19001 свободны

### Проблема: WebSocket не подключается

1. Проверьте, что JWT токен валиден
2. Убедитесь, что токен передается в заголовках:
   ```javascript
   { Authorization: `Bearer ${token}` }
   ```
3. Проверьте логи приложения на наличие ошибок аутентификации

## 📝 Логи

Логи приложения выводятся в консоль. Для продакшена настройте логирование в файл через `logback-spring.xml`.

Уровни логирования настраиваются через переменные окружения:
- `LOG_LEVEL=DEBUG` - для детальных логов
- `SQL_LOG_LEVEL=DEBUG` - для SQL запросов

## 🔒 Безопасность

- **JWT Secret**: В продакшене используйте длинный случайный секрет (минимум 64 байта)
- **HTTPS**: Используйте HTTPS в продакшене
- **CORS**: Настройте CORS для вашего фронтенда
- **MinIO**: Измените дефолтные credentials MinIO

## 📄 Лицензия

[Укажите лицензию проекта]

## 👥 Авторы

[Укажите авторов проекта]

## 🤝 Вклад в проект

[Инструкции по контрибуции]

