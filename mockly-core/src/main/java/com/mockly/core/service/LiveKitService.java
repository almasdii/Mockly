package com.mockly.core.service;

import com.mockly.core.dto.session.LiveKitTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Service for LiveKit WebRTC integration.
 * Handles token generation and room management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LiveKitService {

    @Value("${livekit.url:http://localhost:7880}")
    private String liveKitUrl;

    @Value("${livekit.api-key:}")
    private String apiKey;

    @Value("${livekit.api-secret:}")
    private String apiSecret;

    private static final String ROOM_PREFIX = "session-";
    private static final int TOKEN_EXPIRATION_HOURS = 6;

    /**
     * Generate a LiveKit access token for a user to join a session room.
     *
     * @param sessionId Session ID (used as room ID)
     * @param userId User ID (used as identity)
     * @param displayName User display name
     * @return LiveKitTokenResponse with token, roomId, and URL
     */
    public LiveKitTokenResponse generateToken(UUID sessionId, UUID userId, String displayName) {
        log.info("Generating LiveKit token for session: {}, user: {}", sessionId, userId);

        if (apiKey == null || apiKey.isEmpty() || apiSecret == null || apiSecret.isEmpty()) {
            throw new IllegalStateException("LiveKit API key and secret must be configured");
        }

        String roomId = ROOM_PREFIX + sessionId.toString();
        String identity = userId.toString();

        try {
            // Generate JWT token manually for LiveKit
            // Format: header.payload.signature
            long now = System.currentTimeMillis() / 1000;
            long exp = now + (TOKEN_EXPIRATION_HOURS * 3600);

            // Build claims with name/metadata
            String nameClaim = displayName != null && !displayName.isBlank() 
                    ? String.format(",\"name\":\"%s\"", escapeJson(displayName))
                    : "";
            String claimsJson = String.format(
                "{\"iss\":\"%s\",\"sub\":\"%s\",\"nbf\":%d,\"exp\":%d,\"video\":{\"room\":\"%s\",\"roomJoin\":true,\"canPublish\":true,\"canSubscribe\":true}%s}",
                apiKey, identity, now, exp, roomId, nameClaim
            );

            // Create JWT header
            String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String header = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(claimsJson.getBytes(StandardCharsets.UTF_8));

            // Create signature
            String data = header + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(signatureBytes);

            String tokenString = data + "." + signature;

            log.info("LiveKit token generated successfully for room: {}", roomId);

            return new LiveKitTokenResponse(tokenString, roomId, liveKitUrl);
        } catch (Exception e) {
            log.error("Failed to generate LiveKit token", e);
            throw new RuntimeException("Failed to generate LiveKit token: " + e.getMessage(), e);
        }
    }

    /**
     * Create a LiveKit room ID for a session.
     * In LiveKit, rooms are created automatically when the first participant joins.
     *
     * @param sessionId Session ID
     * @return Room ID
     */
    public String createRoom(UUID sessionId) {
        String roomId = ROOM_PREFIX + sessionId.toString();
        log.info("Created LiveKit room ID: {} for session: {}", roomId, sessionId);
        return roomId;
    }

    /**
     * Delete a LiveKit room (cleanup after session ends).
     * Note: This requires LiveKit RoomService API calls.
     * For now, rooms are automatically cleaned up by LiveKit after being empty.
     *
     * @param roomId Room ID to delete
     */
    public void deleteRoom(String roomId) {
            log.info("Room {} will be automatically cleaned up by LiveKit when empty", roomId);
        // TODO: Implement room deletion via LiveKit RoomService API if needed
        // This would require additional LiveKit SDK dependencies for room management
    }

    /**
     * Escape JSON string to prevent injection.
     */
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

