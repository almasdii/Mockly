package com.mockly.api.controller;

import com.mockly.api.websocket.SessionEventPublisher;
import com.mockly.core.mapper.SessionMapper;
import com.mockly.data.enums.SessionStatus;
import com.mockly.data.repository.SessionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for handling LiveKit webhook events.
 * LiveKit sends webhooks for room and participant events.
 */
@RestController
@RequestMapping("/api/webhooks/livekit")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "LiveKit webhook endpoints")
public class LiveKitWebhookController {

    private final SessionRepository sessionRepository;
    private final SessionEventPublisher eventPublisher;
    private final SessionMapper sessionMapper;

    @Value("${livekit.webhook-secret:}")
    private String webhookSecret;

    @PostMapping
    @Operation(
            summary = "Handle LiveKit webhook",
            description = "Receives and processes LiveKit webhook events (room_started, room_finished, participant_joined, participant_left)"
    )
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> payload) {
        
        log.info("Received LiveKit webhook: {}", payload);


        if (webhookSecret != null && !webhookSecret.isEmpty() && authorization != null) {
            if (!verifyWebhookSignature(authorization, payload)) {
                log.warn("Invalid webhook signature");
                return ResponseEntity.status(401).build();
            }
        }

        String event = (String) payload.get("event");
        if (event == null) {
            log.warn("Webhook missing event field");
            return ResponseEntity.badRequest().build();
        }

        try {
            switch (event) {
                case "room_started":
                    handleRoomStarted(payload);
                    break;
                case "room_finished":
                    handleRoomFinished(payload);
                    break;
                case "participant_joined":
                    handleParticipantJoined(payload);
                    break;
                case "participant_left":
                    handleParticipantLeft(payload);
                    break;
                default:
                    log.info("Unhandled webhook event: {}", event);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing webhook event: {}", event, e);
            return ResponseEntity.status(500).build();
        }
    }

    private void handleRoomStarted(Map<String, Object> payload) {
        String roomName = (String) payload.get("room");
        if (roomName == null || !roomName.startsWith("session-")) {
            log.warn("Invalid room name in room_started event: {}", roomName);
            return;
        }

        UUID sessionId = extractSessionId(roomName);
        if (sessionId == null) {
            return;
        }

        sessionRepository.findById(sessionId).ifPresent(session -> {
            if (session.getStatus() == SessionStatus.SCHEDULED) {
                session.setStatus(SessionStatus.ACTIVE);
                session.setStartsAt(OffsetDateTime.now());
                session = sessionRepository.save(session);
                log.info("Session {} started (room_started event)", sessionId);
                
                // Publish WebSocket event
                var sessionResponse = sessionMapper.toResponse(session);
                eventPublisher.publishSessionUpdated(session, sessionResponse);
            }
        });
    }

    private void handleRoomFinished(Map<String, Object> payload) {
        String roomName = (String) payload.get("room");
        if (roomName == null || !roomName.startsWith("session-")) {
            log.warn("Invalid room name in room_finished event: {}", roomName);
            return;
        }

        UUID sessionId = extractSessionId(roomName);
        if (sessionId == null) {
            return;
        }

        sessionRepository.findById(sessionId).ifPresent(session -> {
            if (session.getStatus() != SessionStatus.ENDED) {
                session.setStatus(SessionStatus.ENDED);
                session.setEndsAt(OffsetDateTime.now());
                session = sessionRepository.save(session);
                log.info("Session {} finished (room_finished event)", sessionId);
                
                // Publish WebSocket event
                var sessionResponse = sessionMapper.toResponse(session);
                eventPublisher.publishSessionEnded(session, sessionResponse);
            }
        });
    }

    private void handleParticipantJoined(Map<String, Object> payload) {
        String roomName = (String) payload.get("room");
        log.info("Participant joined room: {}", roomName);
        // Additional processing if needed
    }

    private void handleParticipantLeft(Map<String, Object> payload) {
        String roomName = (String) payload.get("room");
        log.info("Participant left room: {}", roomName);
        // Additional processing if needed
    }

    private UUID extractSessionId(String roomName) {
        try {
            String sessionIdStr = roomName.substring("session-".length());
            return UUID.fromString(sessionIdStr);
        } catch (Exception e) {
            log.error("Failed to extract session ID from room name: {}", roomName, e);
            return null;
        }
    }

    private boolean verifyWebhookSignature(String authorization, Map<String, Object> payload) {
        try {

            if (!authorization.startsWith("Bearer ")) {
                return false;
            }

            String signature = authorization.substring(7);

            String payloadJson = payload.toString();
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), 
                    "HmacSHA256"
            );
            mac.init(secretKey);
            byte[] hashBytes = mac.doFinal(payloadJson.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = java.util.Base64.getEncoder().encodeToString(hashBytes);

            return signature.equals(expectedSignature);
        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }
}

