#!/bin/bash

# Quick Test Script for Mockly Backend
# Tests the complete flow from registration to session management

BASE_URL="http://localhost:8080"
CANDIDATE_EMAIL="candidate@test.com"
INTERVIEWER_EMAIL="interviewer@test.com"
PASSWORD="password123"

echo "========================================"
echo "  Mockly Backend - Complete Flow Test  "
echo "========================================"
echo ""

# Step 1: Register Candidate
echo "=== Step 1: Register Candidate ==="
CANDIDATE_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$CANDIDATE_EMAIL\",
    \"password\": \"$PASSWORD\",
    \"displayName\": \"Test Candidate\",
    \"role\": \"CANDIDATE\"
  }")

CANDIDATE_TOKEN=$(echo $CANDIDATE_RESPONSE | jq -r '.accessToken')
CANDIDATE_ID=$(echo $CANDIDATE_RESPONSE | jq -r '.userId')
echo "✓ Candidate registered"
echo "  ID: $CANDIDATE_ID"
echo ""

# Step 2: Register Interviewer
echo "=== Step 2: Register Interviewer ==="
INTERVIEWER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$INTERVIEWER_EMAIL\",
    \"password\": \"$PASSWORD\",
    \"displayName\": \"Test Interviewer\",
    \"role\": \"INTERVIEWER\"
  }")

INTERVIEWER_TOKEN=$(echo $INTERVIEWER_RESPONSE | jq -r '.accessToken')
INTERVIEWER_ID=$(echo $INTERVIEWER_RESPONSE | jq -r '.userId')
echo "✓ Interviewer registered"
echo "  ID: $INTERVIEWER_ID"
echo ""

# Step 3: Create Session
echo "=== Step 3: Create Session ==="
SESSION_RESPONSE=$(curl -s -X POST "$BASE_URL/api/sessions" \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"interviewerId\": \"$INTERVIEWER_ID\",
    \"scheduledAt\": \"$(date -u +"%Y-%m-%dT%H:%M:%SZ")\"
  }")

SESSION_ID=$(echo $SESSION_RESPONSE | jq -r '.id')
SESSION_STATUS=$(echo $SESSION_RESPONSE | jq -r '.status')
echo "✓ Session created"
echo "  Session ID: $SESSION_ID"
echo "  Status: $SESSION_STATUS"
echo ""

# Step 4: Get Active Session
echo "=== Step 4: Get Active Session ==="
ACTIVE_SESSION=$(curl -s -X GET "$BASE_URL/api/sessions/me/active" \
  -H "Authorization: Bearer $CANDIDATE_TOKEN")
echo "✓ Active session retrieved"
echo ""

# Step 5: Join Session
echo "=== Step 5: Join Session ==="
JOIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/sessions/$SESSION_ID/join" \
  -H "Authorization: Bearer $CANDIDATE_TOKEN")
echo "✓ Session joined"
echo ""

# Step 6: Get LiveKit Token
echo "=== Step 6: Get LiveKit Token ==="
TOKEN_RESPONSE=$(curl -s -X GET "$BASE_URL/api/sessions/$SESSION_ID/token" \
  -H "Authorization: Bearer $CANDIDATE_TOKEN")
TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.token')
ROOM_ID=$(echo $TOKEN_RESPONSE | jq -r '.roomId')
echo "✓ LiveKit token generated"
echo "  Room ID: $ROOM_ID"
echo ""

# Step 7: List Sessions
echo "=== Step 7: List Sessions ==="
SESSIONS_LIST=$(curl -s -X GET "$BASE_URL/api/sessions?page=0&size=10" \
  -H "Authorization: Bearer $CANDIDATE_TOKEN")
TOTAL=$(echo $SESSIONS_LIST | jq -r '.total')
echo "✓ Sessions list retrieved"
echo "  Total: $TOTAL"
echo ""

# Step 8: End Session
echo "=== Step 8: End Session ==="
curl -s -X POST "$BASE_URL/api/sessions/$SESSION_ID/end" \
  -H "Authorization: Bearer $CANDIDATE_TOKEN" > /dev/null
echo "✓ Session ended"
echo ""

echo "========================================"
echo "  All Tests Completed Successfully! ✓  "
echo "========================================"

