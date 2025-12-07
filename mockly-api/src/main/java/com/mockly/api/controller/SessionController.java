package com.mockly.api.controller;

import com.mockly.api.websocket.SessionEventPublisher;
import com.mockly.core.dto.session.CreateSessionRequest;
import com.mockly.core.dto.session.LiveKitTokenResponse;
import com.mockly.core.dto.session.SessionListResponse;
import com.mockly.core.dto.session.SessionResponse;
import com.mockly.core.service.LiveKitService;
import com.mockly.core.service.SessionService;
import com.mockly.data.entity.Session;
import com.mockly.data.enums.SessionStatus;
import com.mockly.data.repository.SessionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Tag(name = "Sessions", description = "Interview session management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class SessionController {

    private final SessionService sessionService;
    private final LiveKitService liveKitService;
    private final SessionEventPublisher eventPublisher;
    private final SessionRepository sessionRepository;

    @PostMapping
    @Operation(
            summary = "Create a new session",
            description = "Creates a new interview session. User can have only one active session at a time."
    )
    public ResponseEntity<SessionResponse> createSession(
            Authentication authentication,
            @Valid @RequestBody CreateSessionRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        SessionResponse response = sessionService.createSession(userId, request);
        
        // Publish WebSocket event
        Session session = sessionRepository.findById(response.id())
                .orElse(null);
        if (session != null) {
            eventPublisher.publishSessionCreated(session, response);
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/join")
    @Operation(
            summary = "Join a session",
            description = "Join an existing session. Updates session status to ACTIVE if it was SCHEDULED."
    )
    public ResponseEntity<SessionResponse> joinSession(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(authentication.getName());
        SessionResponse response = sessionService.joinSession(id, userId);
        
        // Publish WebSocket event
        Session session = sessionRepository.findById(id).orElse(null);
        if (session != null) {
            session.getParticipants().stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst()
                    .ifPresent(participant -> 
                            eventPublisher.participantJoined(session, participant, response));
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/leave")
    @Operation(
            summary = "Leave a session",
            description = "Leave an active session. Updates participant's left_at timestamp."
    )
    public ResponseEntity<Void> leaveSession(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(authentication.getName());
        sessionService.leaveSession(id, userId);
        
        // Publish WebSocket event
        Session session = sessionRepository.findById(id).orElse(null);
        if (session != null) {
            SessionResponse response = sessionService.getSession(id, userId);
            eventPublisher.participantLeft(session, userId, response);
        }
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/end")
    @Operation(
            summary = "End a session",
            description = "End a session. Only session creator or participants can end the session."
    )
    public ResponseEntity<Void> endSession(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(authentication.getName());
        sessionService.endSession(id, userId);
        
        // Publish WebSocket event
        Session session = sessionRepository.findById(id).orElse(null);
        if (session != null) {
            SessionResponse response = sessionService.getSession(id, userId);
            eventPublisher.publishSessionEnded(session, response);
        }
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get session by ID",
            description = "Returns session details. User must be the creator or a participant."
    )
    public ResponseEntity<SessionResponse> getSession(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(authentication.getName());
        SessionResponse response = sessionService.getSession(id, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            summary = "List sessions",
            description = "Returns paginated list of sessions where user is creator or participant. Can be filtered by status."
    )
    public ResponseEntity<SessionListResponse> listSessions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) SessionStatus status) {
        UUID userId = UUID.fromString(authentication.getName());
        SessionListResponse response = sessionService.listSessions(userId, page, size, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/active")
    @Operation(
            summary = "Get active session",
            description = "Returns the current user's active session (SCHEDULED or ACTIVE), if any."
    )
    public ResponseEntity<SessionResponse> getActiveSession(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Optional<SessionResponse> response = sessionService.getActiveSession(userId);
        return response.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/token")
    @Operation(
            summary = "Get LiveKit token",
            description = "Generates a LiveKit access token for joining the WebRTC room."
    )
    public ResponseEntity<LiveKitTokenResponse> getLiveKitToken(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(authentication.getName());
        
        // Verify user has access to session
        sessionService.getSession(id, userId);
        
        // Get user display name from profile
        String displayName = sessionService.getUserDisplayName(userId);
        
        LiveKitTokenResponse response = liveKitService.generateToken(id, userId, displayName);
        return ResponseEntity.ok(response);
    }
}

