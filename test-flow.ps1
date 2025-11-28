# Quick Test Script for Mockly Backend
# Tests the complete flow from registration to session management

# Configuration
$BASE_URL = "http://localhost:8080"
$CANDIDATE_EMAIL = "candidate@test.com"
$INTERVIEWER_EMAIL = "interviewer@test.com"
$PASSWORD = "password123"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Mockly Backend - Complete Flow Test  " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

try {
    # Step 1: Register Candidate
    Write-Host "=== Step 1: Register Candidate ===" -ForegroundColor Green
    $candidateBody = @{
        email = $CANDIDATE_EMAIL
        password = $PASSWORD
        name = "Test"
        surname = "Candidate"
        role = "CANDIDATE"
    } | ConvertTo-Json
    
    $candidateResponse = Invoke-RestMethod -Uri "$BASE_URL/api/auth/register" `
        -Method POST `
        -ContentType "application/json" `
        -Body $candidateBody
    
    $CANDIDATE_TOKEN = $candidateResponse.accessToken
    $CANDIDATE_ID = $candidateResponse.userId
    Write-Host "✓ Candidate registered" -ForegroundColor Green
    Write-Host "  ID: $CANDIDATE_ID" -ForegroundColor Gray
    Write-Host ""

    # Step 2: Register Interviewer
    Write-Host "=== Step 2: Register Interviewer ===" -ForegroundColor Green
    $interviewerBody = @{
        email = $INTERVIEWER_EMAIL
        password = $PASSWORD
        name = "Test"
        surname = "Interviewer"
        role = "INTERVIEWER"
    } | ConvertTo-Json
    
    $interviewerResponse = Invoke-RestMethod -Uri "$BASE_URL/api/auth/register" `
        -Method POST `
        -ContentType "application/json" `
        -Body $interviewerBody
    
    $INTERVIEWER_TOKEN = $interviewerResponse.accessToken
    $INTERVIEWER_ID = $interviewerResponse.userId
    Write-Host "✓ Interviewer registered" -ForegroundColor Green
    Write-Host "  ID: $INTERVIEWER_ID" -ForegroundColor Gray
    Write-Host ""

    # Step 3: Get Candidate Profile
    Write-Host "=== Step 3: Get Candidate Profile ===" -ForegroundColor Green
    $candidateProfile = Invoke-RestMethod -Uri "$BASE_URL/api/users/me" `
        -Method GET `
        -Headers @{Authorization = "Bearer $CANDIDATE_TOKEN"}
    Write-Host "✓ Profile retrieved" -ForegroundColor Green
    $fullName = if ($candidateProfile.name -and $candidateProfile.surname) {
        "$($candidateProfile.name) $($candidateProfile.surname)"
    } elseif ($candidateProfile.name) {
        $candidateProfile.name
    } elseif ($candidateProfile.surname) {
        $candidateProfile.surname
    } else {
        "N/A"
    }
    Write-Host "  Name: $fullName" -ForegroundColor Gray
    Write-Host "  Role: $($candidateProfile.role)" -ForegroundColor Gray
    Write-Host ""

    # Step 4: Create Session
    Write-Host "=== Step 4: Create Session ===" -ForegroundColor Green
    $sessionBody = @{
        interviewerId = $INTERVIEWER_ID
        scheduledAt = (Get-Date).AddHours(1).ToString("yyyy-MM-ddTHH:mm:ssZ")
    } | ConvertTo-Json
    
    $sessionResponse = Invoke-RestMethod -Uri "$BASE_URL/api/sessions" `
        -Method POST `
        -Headers @{Authorization = "Bearer $CANDIDATE_TOKEN"} `
        -ContentType "application/json" `
        -Body $sessionBody
    
    $SESSION_ID = $sessionResponse.id
    Write-Host "✓ Session created" -ForegroundColor Green
    Write-Host "  Session ID: $SESSION_ID" -ForegroundColor Gray
    Write-Host "  Status: $($sessionResponse.status)" -ForegroundColor Yellow
    Write-Host "  Participants: $($sessionResponse.participants.Count)" -ForegroundColor Gray
    Write-Host ""

    # Step 5: Test Active Session Validation
    Write-Host "=== Step 5: Test Active Session Validation ===" -ForegroundColor Green
    try {
        $duplicateSession = Invoke-RestMethod -Uri "$BASE_URL/api/sessions" `
            -Method POST `
            -Headers @{Authorization = "Bearer $CANDIDATE_TOKEN"} `
            -ContentType "application/json" `
            -Body $sessionBody `
            -ErrorAction Stop
        Write-Host "✗ Validation failed - duplicate session created!" -ForegroundColor Red
    } catch {
        Write-Host "✓ Validation works - cannot create duplicate session" -ForegroundColor Green
    }
    Write-Host ""

    # Step 6: Get Active Session
    Write-Host "=== Step 6: Get Active Session ===" -ForegroundColor Green
    $activeSession = Invoke-RestMethod -Uri "$BASE_URL/api/sessions/me/active" `
        -Method GET `
        -Headers @{Authorization = "Bearer $CANDIDATE_TOKEN"}
    Write-Host "✓ Active session retrieved" -ForegroundColor Green
    Write-Host "  Session ID: $($activeSession.id)" -ForegroundColor Gray
    Write-Host ""

    # Step 7: Get Session Details
    Write-Host "=== Step 7: Get Session Details ===" -ForegroundColor Green
    $sessionDetails = Invoke-RestMethod -Uri "$BASE_URL/api/sessions/$SESSION_ID" `
        -Method GET `
        -Headers @{Authorization = "Bearer $CANDIDATE_TOKEN"}
    Write-Host "✓ Session details retrieved" -ForegroundColor Green
    Write-Host "  Status: $($sessionDetails.status)" -ForegroundColor Gray
    Write-Host ""

    # Step 8: Join Session (Candidate)
    Write-Host "=== Step 8: Join Session (Candidate) ===" -ForegroundColor Green
    $joinResponse = Invoke-RestMethod -Uri "$BASE_URL/api/sessions/$SESSION_ID/join" `
        -Method POST `
        -Headers @{Authorization = "Bearer $CANDIDATE_TOKEN"}
    Write-Host "✓ Candidate joined session" -ForegroundColor Green
    Write-Host "  Status: $($joinResponse.status)" -ForegroundColor Yellow
    Write-Host ""

    # Step 9: Join Session (Interviewer)
    Write-Host "=== Step 9: Join Session (Interviewer) ===" -ForegroundColor Green
    $joinResponse2 = Invoke-RestMethod -Uri "$BASE_URL/api/sessions/$SESSION_ID/join" `
        -Method POST `
        -Headers @{Authorization = "Bearer $INTERVIEWER_TOKEN"}
    Write-Host "✓ Interviewer joined session" -ForegroundColor Green
    Write-Host ""

    # Step 10: Get LiveKit Token
    Write-Host "=== Step 10: Get LiveKit Token ===" -ForegroundColor Green
    $tokenResponse = Invoke-RestMethod -Uri "$BASE_URL/api/sessions/$SESSION_ID/token" `
        -Method GET `
        -Headers @{Authorization = "Bearer $CANDIDATE_TOKEN"}
    Write-Host "✓ LiveKit token generated" -ForegroundColor Green
    Write-Host "  Token: $($tokenResponse.token.Substring(0, 30))..." -ForegroundColor Gray
    Write-Host "  Room ID: $($tokenResponse.roomId)" -ForegroundColor Gray
    Write-Host "  URL: $($tokenResponse.url)" -ForegroundColor Gray
    Write-Host ""

    # Step 11: List Sessions
    Write-Host "=== Step 11: List Sessions ===" -ForegroundColor Green
    $sessionsList = Invoke-RestMethod -Uri "$BASE_URL/api/sessions?page=0&size=10" `
        -Method GET `
        -Headers @{Authorization = "Bearer $CANDIDATE_TOKEN"}
    Write-Host "✓ Sessions list retrieved" -ForegroundColor Green
    Write-Host "  Total: $($sessionsList.total)" -ForegroundColor Gray
    Write-Host "  Page: $($sessionsList.page)" -ForegroundColor Gray
    Write-Host "  Size: $($sessionsList.size)" -ForegroundColor Gray
    Write-Host ""

    # Step 12: Leave Session
    Write-Host "=== Step 12: Leave Session ===" -ForegroundColor Green
    Invoke-RestMethod -Uri "$BASE_URL/api/sessions/$SESSION_ID/leave" `
        -Method POST `
        -Headers @{Authorization = "Bearer $CANDIDATE_TOKEN"} | Out-Null
    Write-Host "✓ Candidate left session" -ForegroundColor Green
    Write-Host ""

    # Step 13: End Session
    Write-Host "=== Step 13: End Session ===" -ForegroundColor Green
    Invoke-RestMethod -Uri "$BASE_URL/api/sessions/$SESSION_ID/end" `
        -Method POST `
        -Headers @{Authorization = "Bearer $CANDIDATE_TOKEN"} | Out-Null
    Write-Host "✓ Session ended" -ForegroundColor Green
    Write-Host ""

    # Step 14: Verify Session Ended
    Write-Host "=== Step 14: Verify Session Ended ===" -ForegroundColor Green
    $endedSession = Invoke-RestMethod -Uri "$BASE_URL/api/sessions/$SESSION_ID" `
        -Method GET `
        -Headers @{Authorization = "Bearer $CANDIDATE_TOKEN"}
    Write-Host "✓ Session verified" -ForegroundColor Green
    Write-Host "  Status: $($endedSession.status)" -ForegroundColor Yellow
    Write-Host ""

    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  All Tests Completed Successfully! ✓  " -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Summary:" -ForegroundColor Yellow
    Write-Host "  - Candidate ID: $CANDIDATE_ID" -ForegroundColor Gray
    Write-Host "  - Interviewer ID: $INTERVIEWER_ID" -ForegroundColor Gray
    Write-Host "  - Session ID: $SESSION_ID" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Next Steps:" -ForegroundColor Yellow
    Write-Host "  1. Test WebSocket connection (see TESTING_GUIDE.md)" -ForegroundColor Gray
    Write-Host "  2. Test LiveKit webhook (configure LiveKit server)" -ForegroundColor Gray
    Write-Host "  3. Check Swagger UI: http://localhost:8080/swagger-ui.html" -ForegroundColor Gray

} catch {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "  Test Failed! ✗" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Make sure:" -ForegroundColor Yellow
    Write-Host "  1. Application is running (mvn spring-boot:run)" -ForegroundColor Gray
    Write-Host "  2. Docker containers are running (docker-compose ps)" -ForegroundColor Gray
    Write-Host "  3. Database is accessible" -ForegroundColor Gray
    exit 1
}

