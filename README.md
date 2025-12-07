# Mockly Backend

Backend platform for conducting mock interviews with support for WebRTC, audio processing via ML services, and report generation.

##  Table of Contents

- [Description](#описание)
- [echnologies](#технологии)
- [Requirements](#требования)
- [Installation](#установка)
- [Configuration](#конфигурация)
- [Running the Project](#запуск-проекта)
- [Testing](#тестирование)
- [API Documentation](#api-документация)
- [Project Structure](#структура-проекта)

##  Description
Mockly is a platform for conducting mock interviews with the following features:

- Authentication and Authorization – JWT tokens, registration, login
- Session Management – creating, joining, and finishing interviews
- WebRTC Integration – LiveKit for video calls
- Artifact Uploading – MinIO for storing audio files (up to 500MB)
- ML Processing – integration with an ML service for interview analysis
- Report Generation – automatic creation of reports with metrics
- WebSocket – real-time updates via STOMP
- Transcripts – storing interview transcripts

## Technologies

- Java 21 – programming language
- Spring Boot 3.3.2 – framework
- PostgreSQL 16 – database
- Redis 7 – caching
- MinIO – S3-compatible object storage
- Flyway – database migrations
- JWT – authentication
- WebSocket/STOMP – real-time communication
- LiveKit – WebRTC platform
- Maven – dependency management

##  Requirements

Before starting, make sure you have installed:

- Java 21 or higher
- Maven 3.8+
- Docker and Docker Compose (for infrastructure)
- Git

Check versions:

```bash
java -version  # Should be Java 21+
mvn -version   # Should be Maven 3.8+
docker --version
docker-compose --version
```

##  Installation

### 1. Clone the repository

```bash
git clone <repository-url>
cd mock4

```

### 2. Build the project

```bash
mvn clean install
```

This will build all project modules:

- mockly-data – data layer
- mockly-security – security
- mockly-core – business logic
- mockly-api – REST API



##  Configuration

### Environment Variable

The project uses environment variables for configuration. Main settings are located in: `mockly-api/src/main/resources/application.yml`.

#### Required variables (for production):

```bash
# JWT
JWT_SECRET=your-secret-key-must-be-at-least-64-bytes-long-for-hs512-algorithm

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mockly
SPRING_DATASOURCE_USERNAME=mockly
SPRING_DATASOURCE_PASSWORD=mockly_password

# MinIO
MINIO_ENDPOINT=http://localhost:19000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=mockly-artifacts

# LiveKit (optional)
LIVEKIT_URL=http://localhost:7880
LIVEKIT_API_KEY=your-api-key
LIVEKIT_API_SECRET=your-api-secret

# ML Service (optional)
ML_SERVICE_URL=http://localhost:8000

```

#### Creating a .env file (optional)

Create a .env file in the project root:

```env
JWT_SECRET=your-secret-key-must-be-at-least-64-bytes-long-for-hs512-algorithm-to-work-properly-and-securely-in-production-environment
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080
```

## Running the Project

### Step 1: Start the infrastructure (Docker Compose)

Run PostgreSQL, Redis, and MinIO:

```bash
docker-compose up -d
```

Check running services:

```bash
docker-compose ps
```

You should see:
- mockly-postgres (port 5432)
- mockly-redis (port 26379)
- mockly-minio (ports 19000, 19001)

### Step 2: Verify database connection

```bash
# Windows PowerShell
.\check-connection.ps1

# Linux/Mac
./check-connection.sh
```

Or manually:

```bash
docker exec -it mockly-postgres psql -U mockly -d mockly -c "SELECT 1;"
```

### Step 3: Run the application

#### Option A: Using Maven

```bash
cd mockly-api
mvn spring-boot:run
```

#### Option B: Using an IDE

1. Open the project in IntelliJ IDEA or Eclipse 
2. Locate MocklyApplication.java in
   mockly-api/src/main/java/com/mockly/
3. Run it as a Spring Boot application

#### Option C: Using the built JAR

```bash
mvn clean package
java -jar mockly-api/target/mockly-api-1.0.0-SNAPSHOT.jar
```

### Step 4: Verify the application

The app should run at: `http://localhost:8080`

Health check:

```bash
curl http://localhost:8080/actuator/health
```

Open in browser:
- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/v3/api-docs

## Testing

### 1. Testing via Swagger UI

1. Open http://localhost:8080/swagger-ui.html
2. Find Authentication section
3. Register a user:
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
4. Copy the returned accessToken
5. Click Authorize
6. Enter: Bearer <your-token>
7. Now you can test all secured endpoints

### 2. Testing via cURL

#### Register

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

Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "userId": "123e4567-e89b-12d3-a456-426614174000"
}
```

#### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "candidate@example.com",
    "password": "password123"
  }'
```

#### Create a session (requires auth)

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

#### Get current user

```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Testing WebSocket

#### ПConnect with JWT token

See examples in `WEBSOCKET_CLIENT_EXAMPLE.md`

Quick Node.js example

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

### 4. Testing artifact upload

#### Step 1: Request upload URL

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

Response:
```json
{
  "artifactId": "artifact-uuid",
  "uploadUrl": "http://localhost:19000/mockly-artifacts/...",
  "objectName": "sessions/.../artifacts/.../interview.mp3",
  "expiresInSeconds": 3600
}
```

#### Step 2: Upload file to MinIO

```bash
UPLOAD_URL="pre-signed-url-from-step-1"

curl -X PUT "$UPLOAD_URL" \
  -H "Content-Type: audio/mpeg" \
  --data-binary "@interview.mp3"
```

#### Step 3: Complete upload

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

### 5. Testing report generation

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

## API Documentation

### Main Endpoints

#### Authentication
- `POST /api/auth/register` - Register
- `POST /api/auth/login` - Login
- `POST /api/auth/refresh` - Refresh token
- `POST /api/auth/logout` - Logout

#### Users
- `GET /api/users/me` - Get current user
- `PATCH /api/users/me` - Update profile
- `GET /api/users/{id}` - Get user by ID

#### Sessions
- `POST /api/sessions` -Create a session
- `GET /api/sessions` - List sessions
- `GET /api/sessions/{id}` - Get session
- `POST /api/sessions/{id}/join` - Join session
- `POST /api/sessions/{id}/leave` - Leave session
- `POST /api/sessions/{id}/end` - End session
- `GET /api/sessions/{id}/token` - LiveKit 

#### Artifacts
- `POST /api/sessions/{id}/artifacts/request-upload` - Запросить URL загрузки
- `POST /api/sessions/{id}/artifacts/{artifactId}/complete` - Завершить загрузку
- `GET /api/sessions/{id}/artifacts` - Список артефактов
- `GET /api/sessions/{id}/artifacts/{artifactId}` - Артефакт по ID

#### Swagger UI
- `POST /api/sessions/{id}/report/trigger` - Запустить генерацию
- `GET /api/sessions/{id}/report` - Получить отчет

### Swagger UI

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

##  Project Structure

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

## 🔧  MinIO

After running Docker Compose, MinIO is available at:
- **API**: http://localhost:19000
- **Console**: http://localhost:19001
- **Credentials**: `minioadmin` / `minioadmin`

Bucket mockly-artifacts is created automatically on app startup.

## 🔧 LiveKit Setup (optional)

To enable WebRTC features via LiveKit:

1. Install  LiveKit: https://docs.livekit.io/home/self-hosting/deployment/
2. Obtain API key and secret
3. Set environment variables:
   ```bash
   LIVEKIT_URL=http://localhost:7880
   LIVEKIT_API_KEY=your-api-key
   LIVEKIT_API_SECRET=your-api-secret
   ```

## 🔧 ML Service Setup (optional)

For report generation:
Start ML service on port 8000
The service must expose endpoint: POST /api/process
Request/response format matches MLProcessRequest and MLProcessResponse

##  Troubleshooting

### Issue: Application won't start

1. Verify PostgreSQL is running:
   ```bash
   docker-compose ps
   ```

2. Check logs:
   ```bash
   docker-compose logs postgres
   ```

3. Test DB connection:
   ```bash
   docker exec -it mockly-postgres psql -U mockly -d mockly
   ```

### Issue: Flyway migration errors

1. Ensure DB exists:
   ```sql
   CREATE DATABASE mockly;
   ```

2. Reset everything:
   ```bash
   docker-compose down -v
   docker-compose up -d
   ```

### Issue: MinIO unavailable

1. Check container:
   ```bash
   docker-compose ps minio
   ```

2. Check logs:
   ```bash
   docker-compose logs minio
   ```

3. Ensure ports 19000/19001 are free

### Issue: WebSocket authentication errors

Verify JWT token
Ensure headers include:
   ```javascript
   { Authorization: `Bearer ${token}` }
   ```
3.Application logs appear in the console.
For production, configure logback to write to files.



Application logs appear in the console.
For production, configure logback to write to files.

Configure log levels via env vars:
LOG_LEVEL=DEBUG
SQL_LOG_LEVEL=DEBUG


JWT Secret — must be at least 64 bytes in production
HTTPS — required in production
CORS — configure allowed frontend origins
MinIO — change default credentials


