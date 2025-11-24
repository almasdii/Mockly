# Complete Testing Guide - Registration to Session Management

This guide walks you through testing the entire flow from user registration to session management.

## Prerequisites

1. **Application is running:**
   ```bash
   cd mockly-api
   mvn spring-boot:run
   ```

2. **Docker containers are running:**
   ```bash
   docker-compose ps
   # Should show: postgres, redis, minio
   ```

3. **Swagger UI available:**
   - Open: http://localhost:8080/swagger-ui.html

## Step-by-Step Testing

### Step 1: Register Two Users (Candidate and Interviewer)

#### Register Candidate
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "candidate@test.com",
    "password": "password123",
    "displayName": "Test Candidate",
    "role": "CANDIDATE"
  }'
```

**Expected Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "userId": "uuid-here"
}
```

**Save the candidate's:**
- `accessToken` → `CANDIDATE_TOKEN`
- `userId` → `CANDIDATE_ID`

#### Register Interviewer
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "interviewer@test.com",
    "password": "password123",
    "displayName": "Test Interviewer",
    "role": "INTERVIEWER"
  }'
```

**Expected Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "userId": "uuid-here"
}
```

**Save the interviewer's:**
- `accessToken` → `INTERVIEWER_TOKEN`
- `userId` → `INTERVIEWER_ID`

---

### Step 2: Verify User Profiles

#### Get Candidate Profile
```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer YOUR_CANDIDATE_TOKEN"
```

#### Get Interviewer Profile
```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer YOUR_INTERVIEWER_TOKEN"
```

---

### Step 3: Create a Session (as Candidate)

```bash
curl -X POST http://localhost:8080/api/sessions \
  -H "Authorization: Bearer YOUR_CANDIDATE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "interviewerId": "YOUR_INTERVIEWER_ID",
    "scheduledAt": "2024-12-01T10:00:00Z"
  }'
```

**Expected Response:**
```json
{
  "id": "session-uuid",
  "createdBy": "candidate-uuid",
  "creatorDisplayName": "Test Candidate",
  "status": "SCHEDULED",
  "startsAt": "2024-12-01T10:00:00Z",
  "endsAt": null,
  "roomProvider": "livekit",
  "roomId": "session-session-uuid",
  "recordingId": null,
  "createdAt": "2024-11-24T...",
  "updatedAt": "2024-11-24T...",
  "participants": [
    {
      "id": "participant-uuid-1",
      "userId": "candidate-uuid",
      "userDisplayName": "Test Candidate",
      "userEmail": "candidate@test.com",
      "roleInSession": "CANDIDATE",
      "joinedAt": null,
      "leftAt": null
    },
    {
      "id": "participant-uuid-2",
      "userId": "interviewer-uuid",
      "userDisplayName": "Test Interviewer",
      "userEmail": "interviewer@test.com",
      "roleInSession": "INTERVIEWER",
      "joinedAt": null,
      "leftAt": null
    }
  ],
  "artifacts": []
}
```

**Save:**
- `id` → `SESSION_ID`

---

### Step 4: Test Active Session Validation

Try creating another session (should fail):
```bash
curl -X POST http://localhost:8080/api/sessions \
  -H "Authorization: Bearer YOUR_CANDIDATE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "interviewerId": "YOUR_INTERVIEWER_ID",
    "scheduledAt": "2024-12-02T10:00:00Z"
  }'
```

**Expected Response (Error):**
```json
{
  "timestamp": "2024-11-24T...",
  "status": 400,
  "error": "Bad Request",
  "message": "User already has an active session. Please end the current session before creating a new one.",
  "path": "/api/sessions"
}
```

✅ **Validation works!**

---

### Step 5: Get Active Session

```bash
curl -X GET http://localhost:8080/api/sessions/me/active \
  -H "Authorization: Bearer YOUR_CANDIDATE_TOKEN"
```

**Expected Response:**
```json
{
  "id": "session-uuid",
  "status": "SCHEDULED",
  ...
}
```

---

### Step 6: Get Session Details

```bash
curl -X GET http://localhost:8080/api/sessions/YOUR_SESSION_ID \
  -H "Authorization: Bearer YOUR_CANDIDATE_TOKEN"
```

---

### Step 7: Join Session (as Candidate)

```bash
curl -X POST http://localhost:8080/api/sessions/YOUR_SESSION_ID/join \
  -H "Authorization: Bearer YOUR_CANDIDATE_TOKEN"
```

**Expected Response:**
- Session status changes to `ACTIVE`
- `joinedAt` timestamp is set
- `startsAt` is set to current time

---

### Step 8: Join Session (as Interviewer)

```bash
curl -X POST http://localhost:8080/api/sessions/YOUR_SESSION_ID/join \
  -H "Authorization: Bearer YOUR_INTERVIEWER_TOKEN"
```

**Expected Response:**
- Interviewer's `joinedAt` timestamp is set
- Session remains `ACTIVE`

---

### Step 9: Get LiveKit Token

```bash
curl -X GET http://localhost:8080/api/sessions/YOUR_SESSION_ID/token \
  -H "Authorization: Bearer YOUR_CANDIDATE_TOKEN"
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "roomId": "session-session-uuid",
  "url": "http://localhost:7880"
}
```

**Note:** Token generation works even without LiveKit server running. The token is valid for 6 hours.

---

### Step 10: List Sessions

#### List all sessions (as Candidate)
```bash
curl -X GET "http://localhost:8080/api/sessions?page=0&size=20" \
  -H "Authorization: Bearer YOUR_CANDIDATE_TOKEN"
```

#### List sessions with status filter
```bash
curl -X GET "http://localhost:8080/api/sessions?page=0&size=20&status=ACTIVE" \
  -H "Authorization: Bearer YOUR_CANDIDATE_TOKEN"
```

**Expected Response:**
```json
{
  "sessions": [...],
  "total": 1,
  "page": 0,
  "size": 20
}
```

---

### Step 11: Leave Session

```bash
curl -X POST http://localhost:8080/api/sessions/YOUR_SESSION_ID/leave \
  -H "Authorization: Bearer YOUR_CANDIDATE_TOKEN"
```

**Expected Response:**
- Status: 200 OK
- Participant's `leftAt` timestamp is set

---

### Step 12: End Session

```bash
curl -X POST http://localhost:8080/api/sessions/YOUR_SESSION_ID/end \
  -H "Authorization: Bearer YOUR_CANDIDATE_TOKEN"
```

**Expected Response:**
- Status: 200 OK
- Session status changes to `ENDED`
- `endsAt` timestamp is set
- All participants' `leftAt` timestamps are set

---

### Step 13: Verify Session Ended

```bash
curl -X GET http://localhost:8080/api/sessions/YOUR_SESSION_ID \
  -H "Authorization: Bearer YOUR_CANDIDATE_TOKEN"
```

**Expected Response:**
```json
{
  "id": "session-uuid",
  "status": "ENDED",
  "endsAt": "2024-11-24T...",
  ...
}
```

---

### Step 14: Test WebSocket Connection

#### Using JavaScript (Browser Console or Node.js)

```javascript
// Install dependencies first:
// npm install sockjs-client stompjs

const SockJS = require('sockjs-client');
const Stomp = require('stompjs');

// Replace with your session ID and user ID
const sessionId = 'YOUR_SESSION_ID';
const userId = 'YOUR_CANDIDATE_ID';

// Connect to WebSocket
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
  console.log('Connected: ' + frame);
  
  // Subscribe to session-specific updates
  stompClient.subscribe('/topic/sessions/' + sessionId, function(message) {
    const event = JSON.parse(message.body);
    console.log('Session Event:', event.type);
    console.log('Session Data:', event.data);
  });
  
  // Subscribe to user-specific updates
  stompClient.subscribe('/topic/users/' + userId + '/sessions', function(message) {
    const event = JSON.parse(message.body);
    console.log('User Event:', event.type);
    console.log('Session Data:', event.data);
  });
  
  console.log('Subscribed to WebSocket topics');
});

// Disconnect
function disconnect() {
  if (stompClient !== null) {
    stompClient.disconnect();
  }
  console.log("Disconnected");
}
```

#### Test WebSocket Events

1. **Create a new session** (from another terminal) - should trigger `SESSION_CREATED` event
2. **Join the session** - should trigger `PARTICIPANT_JOINED` event
3. **End the session** - should trigger `SESSION_ENDED` event

---

## Quick Test Script (PowerShell)

Save this as `test-flow.ps1`:

```powershell
# Configuration
$BASE_URL = "http://localhost:8080"
$CANDIDATE_EMAIL = "candidate@test.com"
$INTERVIEWER_EMAIL = "interviewer@test.com"
$PASSWORD = "password123"

Write-Host "=== Step 1: Register Candidate ===" -ForegroundColor Green
$candidateResponse = Invoke-RestMethod -Uri "$BASE_URL/api/auth/register" `
    -Method POST `
    -ContentType "application/json" `
    -Body (@{
        email = $CANDIDATE_EMAIL
        password = $PASSWORD
        displayName = "Test Candidate"
        role = "CANDIDATE"
    } | ConvertTo-Json)
$CANDIDATE_TOKEN = $candidateResponse.accessToken
$CANDIDATE_ID = $candidateResponse.userId
Write-Host "Candidate ID: $CANDIDATE_ID" -ForegroundColor Cyan

Write-Host "`n=== Step 2: Register Interviewer ===" -ForegroundColor Green
$interviewerResponse = Invoke-RestMethod -Uri "$BASE_URL/api/auth/register" `
    -Method POST `
    -ContentType "application/json" `
    -Body (@{
        email = $INTERVIEWER_EMAIL
        password = $PASSWORD
        displayName = "Test Interviewer"
        role = "INTERVIEWER"
    } | ConvertTo-Json)
$INTERVIEWER_TOKEN = $interviewerResponse.accessToken
$INTERVIEWER_ID = $interviewerResponse.userId
Write-Host "Interviewer ID: $INTERVIEWER_ID" -ForegroundColor Cyan

Write-Host "`n=== Step 3: Create Session ===" -ForegroundColor Green
$sessionResponse = Invoke-RestMethod -Uri "$BASE_URL/api/sessions" `
    -Method POST `
    -Headers @{Authorization = "Bearer $CANDIDATE_TOKEN"} `
    -ContentType "application/json" `
    -Body (@{
        interviewerId = $INTERVIEWER_ID
        scheduledAt = (Get-Date).AddHours(1).ToString("yyyy-MM-ddTHH:mm:ssZ")
    } | ConvertTo-Json)
$SESSION_ID = $sessionResponse.id
Write-Host "Session ID: $SESSION_ID" -ForegroundColor Cyan
Write-Host "Status: $($sessionResponse.status)" -ForegroundColor Yellow

Write-Host "`n=== Step 4: Get Active Session ===" -ForegroundColor Green
$activeSession = Invoke-RestMethod -Uri "$BASE_URL/api/sessions/me/active" `
    -Method GET `
    -Headers @{Authorization = "Bearer $CANDIDATE_TOKEN"}
Write-Host "Active Session ID: $($activeSession.id)" -ForegroundColor Cyan

Write-Host "`n=== Step 5: Join Session (Candidate) ===" -ForegroundColor Green
$joinResponse = Invoke-RestMethod -Uri "$BASE_URL/api/sessions/$SESSION_ID/join" `
    -Method POST `
    -Headers @{Authorization = "Bearer $CANDIDATE_TOKEN"}
Write-Host "Status: $($joinResponse.status)" -ForegroundColor Yellow

Write-Host "`n=== Step 6: Get LiveKit Token ===" -ForegroundColor Green
$tokenResponse = Invoke-RestMethod -Uri "$BASE_URL/api/sessions/$SESSION_ID/token" `
    -Method GET `
    -Headers @{Authorization = "Bearer $CANDIDATE_TOKEN"}
Write-Host "Token generated: $($tokenResponse.token.Substring(0, 20))..." -ForegroundColor Cyan
Write-Host "Room ID: $($tokenResponse.roomId)" -ForegroundColor Cyan

Write-Host "`n=== Step 7: List Sessions ===" -ForegroundColor Green
$sessionsList = Invoke-RestMethod -Uri "$BASE_URL/api/sessions?page=0&size=10" `
    -Method GET `
    -Headers @{Authorization = "Bearer $CANDIDATE_TOKEN"}
Write-Host "Total sessions: $($sessionsList.total)" -ForegroundColor Cyan

Write-Host "`n=== Step 8: End Session ===" -ForegroundColor Green
Invoke-RestMethod -Uri "$BASE_URL/api/sessions/$SESSION_ID/end" `
    -Method POST `
    -Headers @{Authorization = "Bearer $CANDIDATE_TOKEN"}
Write-Host "Session ended successfully" -ForegroundColor Green

Write-Host "`n=== All Tests Completed! ===" -ForegroundColor Green
```

Run it:
```powershell
.\test-flow.ps1
```

---

## Testing Checklist

- [ ] User registration works (Candidate and Interviewer)
- [ ] User login works
- [ ] Get user profile works
- [ ] Create session works
- [ ] Active session validation works (can't create second session)
- [ ] Get active session works
- [ ] Get session by ID works
- [ ] Join session works (updates status to ACTIVE)
- [ ] Get LiveKit token works
- [ ] List sessions works (with pagination)
- [ ] Leave session works
- [ ] End session works
- [ ] WebSocket connection works
- [ ] WebSocket events are received

---

## Common Issues & Solutions

### Issue: "User already has an active session"
**Solution:** End the existing session first, or wait for it to expire.

### Issue: "Session not found"
**Solution:** Check that you're using the correct session ID and user has access.

### Issue: "Access denied"
**Solution:** Make sure you're using a valid JWT token in the Authorization header.

### Issue: WebSocket connection fails
**Solution:** 
- Check CORS settings in `WebSocketConfig.java`
- Make sure WebSocket endpoint is accessible
- Check browser console for errors

---

## Swagger UI Testing

1. Open: http://localhost:8080/swagger-ui.html
2. Click "Authorize" button (top right)
3. Enter: `Bearer YOUR_TOKEN`
4. Test endpoints directly from Swagger UI

---

**Happy Testing! 🚀**

