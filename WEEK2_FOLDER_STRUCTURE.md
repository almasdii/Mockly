# Week 2: Detailed Folder Structure

## 📁 Complete File Structure

```
mockly-backend/
│
├── pom.xml                                    ✅ (existing - add LiveKit dependency)
│
├── mockly-api/
│   ├── pom.xml                                ✅ (existing - add WebSocket dependency)
│   └── src/main/java/com/mockly/api/
│       ├── MocklyApplication.java             ✅ (existing)
│       │
│       ├── controller/
│       │   ├── AuthController.java            ✅ (existing)
│       │   ├── UserController.java            ✅ (existing)
│       │   ├── SessionController.java         🆕 NEW FILE
│       │   └── LiveKitWebhookController.java  🆕 NEW FILE
│       │
│       ├── websocket/                          🆕 NEW DIRECTORY
│       │   ├── WebSocketConfig.java           🆕 NEW FILE
│       │   ├── SessionWebSocketHandler.java   🆕 NEW FILE
│       │   └── SessionEventPublisher.java     🆕 NEW FILE
│       │
│       ├── config/
│       │   └── OpenApiConfig.java             ✅ (existing)
│       │
│       └── exception/
│           └── ApiExceptionHandler.java        ✅ (existing)
│
├── mockly-core/
│   ├── pom.xml                                ✅ (existing - add LiveKit dependency)
│   └── src/main/java/com/mockly/core/
│       ├── dto/
│       │   ├── auth/                          ✅ (existing)
│       │   │   ├── RegisterRequest.java
│       │   │   ├── LoginRequest.java
│       │   │   ├── RefreshTokenRequest.java
│       │   │   └── AuthResponse.java
│       │   │
│       │   ├── user/                          ✅ (existing)
│       │   │   ├── UserResponse.java
│       │   │   ├── UpdateProfileRequest.java
│       │   │   └── ProfileResponse.java
│       │   │
│       │   └── session/                       🆕 NEW DIRECTORY
│       │       ├── CreateSessionRequest.java  🆕 NEW FILE
│       │       ├── JoinSessionRequest.java    🆕 NEW FILE
│       │       ├── SessionResponse.java       🆕 NEW FILE
│       │       ├── SessionParticipantResponse.java 🆕 NEW FILE
│       │       ├── ArtifactResponse.java      🆕 NEW FILE
│       │       ├── LiveKitTokenResponse.java  🆕 NEW FILE
│       │       └── SessionListResponse.java   🆕 NEW FILE
│       │
│       ├── mapper/                            🆕 NEW DIRECTORY
│       │   └── SessionMapper.java             🆕 NEW FILE
│       │
│       ├── service/
│       │   ├── AuthService.java               ✅ (existing)
│       │   ├── UserService.java               ✅ (existing)
│       │   ├── SessionService.java            🆕 NEW FILE
│       │   └── LiveKitService.java            🆕 NEW FILE
│       │
│       └── exception/                         ✅ (existing)
│           ├── BadRequestException.java
│           ├── ResourceNotFoundException.java
│           └── ...
│
├── mockly-data/
│   ├── pom.xml                                ✅ (existing)
│   └── src/main/java/com/mockly/data/
│       ├── entity/
│       │   ├── User.java                      ✅ (existing)
│       │   ├── Profile.java                   ✅ (existing)
│       │   ├── Session.java                   🆕 NEW FILE
│       │   ├── SessionParticipant.java        🆕 NEW FILE
│       │   └── Artifact.java                  🆕 NEW FILE
│       │
│       ├── enums/                             🆕 NEW DIRECTORY
│       │   ├── SessionStatus.java             🆕 NEW FILE
│       │   ├── ParticipantRole.java           🆕 NEW FILE
│       │   └── ArtifactType.java              🆕 NEW FILE
│       │
│       └── repository/
│           ├── UserRepository.java            ✅ (existing)
│           ├── ProfileRepository.java         ✅ (existing)
│           ├── SessionRepository.java         🆕 NEW FILE
│           ├── SessionParticipantRepository.java 🆕 NEW FILE
│           └── ArtifactRepository.java        🆕 NEW FILE
│
└── mockly-security/
    └── ...                                    ✅ (existing - no changes)
```

## 📝 File Creation Order

### Step 1: Enums (Foundation)
1. `mockly-data/.../enums/SessionStatus.java`
2. `mockly-data/.../enums/ParticipantRole.java`
3. `mockly-data/.../enums/ArtifactType.java`

### Step 2: Entities
4. `mockly-data/.../entity/Session.java`
5. `mockly-data/.../entity/SessionParticipant.java`
6. `mockly-data/.../entity/Artifact.java`

### Step 3: Repositories
7. `mockly-data/.../repository/SessionRepository.java`
8. `mockly-data/.../repository/SessionParticipantRepository.java`
9. `mockly-data/.../repository/ArtifactRepository.java`

### Step 4: DTOs
10. `mockly-core/.../dto/session/CreateSessionRequest.java`
11. `mockly-core/.../dto/session/JoinSessionRequest.java`
12. `mockly-core/.../dto/session/SessionResponse.java`
13. `mockly-core/.../dto/session/SessionParticipantResponse.java`
14. `mockly-core/.../dto/session/ArtifactResponse.java`
15. `mockly-core/.../dto/session/LiveKitTokenResponse.java`
16. `mockly-core/.../dto/session/SessionListResponse.java`

### Step 5: Mapper
17. `mockly-core/.../mapper/SessionMapper.java`

### Step 6: Services
18. `mockly-core/.../service/LiveKitService.java`
19. `mockly-core/.../service/SessionService.java`

### Step 7: Controllers
20. `mockly-api/.../controller/SessionController.java`
21. `mockly-api/.../controller/LiveKitWebhookController.java`

### Step 8: WebSocket
22. `mockly-api/.../websocket/WebSocketConfig.java`
23. `mockly-api/.../websocket/SessionEventPublisher.java`
24. `mockly-api/.../websocket/SessionWebSocketHandler.java`

## 🔧 Configuration Updates

### Files to Modify (not create):

1. **Root `pom.xml`**
   - Add LiveKit version property
   - Add LiveKit dependency to dependencyManagement

2. **`mockly-api/pom.xml`**
   - Add `spring-boot-starter-websocket`
   - Add LiveKit dependency

3. **`mockly-core/pom.xml`**
   - Add LiveKit dependency

4. **`mockly-api/src/main/resources/application.yml`**
   - Add LiveKit configuration section

## 📊 Summary

- **New Directories:** 3
  - `mockly-api/.../websocket/`
  - `mockly-core/.../dto/session/`
  - `mockly-core/.../mapper/`
  - `mockly-data/.../enums/`

- **New Files:** 24
  - Enums: 3
  - Entities: 3
  - Repositories: 3
  - DTOs: 7
  - Mapper: 1
  - Services: 2
  - Controllers: 2
  - WebSocket: 3

- **Files to Modify:** 4
  - Root pom.xml
  - mockly-api/pom.xml
  - mockly-core/pom.xml
  - application.yml

---

**Total: 24 new files + 4 modifications = 28 changes**

