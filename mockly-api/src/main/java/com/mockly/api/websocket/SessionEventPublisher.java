package com.mockly.api.websocket;

import com.mockly.core.dto.session.SessionResponse;
import com.mockly.data.entity.Session;
import com.mockly.data.entity.SessionParticipant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publishes session-related events to WebSocket clients.
 * Uses STOMP messaging to send real-time updates.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Publish session created event.
     * Sends to: /topic/sessions/{sessionId} and /topic/users/{userId}/sessions
     */
    public void publishSessionCreated(Session session, SessionResponse sessionResponse) {
        String sessionTopic = "/topic/sessions/" + session.getId();
        String userTopic = "/topic/users/" + session.getCreatedBy() + "/sessions";

        SessionEvent event = new SessionEvent("SESSION_CREATED", sessionResponse);

        messagingTemplate.convertAndSend(sessionTopic, event);
        messagingTemplate.convertAndSend(userTopic, event);

        log.info("Published SESSION_CREATED event for session: {}", session.getId());
    }

    /**
     * Publish session updated event.
     * Sends to: /topic/sessions/{sessionId} and /topic/users/{userId}/sessions for all participants
     */
    public void publishSessionUpdated(Session session, SessionResponse sessionResponse) {
        String sessionTopic = "/topic/sessions/" + session.getId();

        SessionEvent event = new SessionEvent("SESSION_UPDATED", sessionResponse);

        messagingTemplate.convertAndSend(sessionTopic, event);

        // Notify all participants
        if (session.getParticipants() != null) {
            for (SessionParticipant participant : session.getParticipants()) {
                String userTopic = "/topic/users/" + participant.getUserId() + "/sessions";
                messagingTemplate.convertAndSend(userTopic, event);
            }
        }

        log.info("Published SESSION_UPDATED event for session: {}", session.getId());
    }

    /**
     * Publish participant joined event.
     * Sends to: /topic/sessions/{sessionId} and /topic/users/{userId}/sessions
     */
    public void publishParticipantJoined(Session session, SessionParticipant participant, SessionResponse sessionResponse) {
        String sessionTopic = "/topic/sessions/" + session.getId();
        String userTopic = "/topic/users/" + participant.getUserId() + "/sessions";

        SessionEvent event = new SessionEvent("PARTICIPANT_JOINED", sessionResponse);

        messagingTemplate.convertAndSend(sessionTopic, event);
        messagingTemplate.convertAndSend(userTopic, event);

        log.info("Published PARTICIPANT_JOINED event for session: {}, participant: {}", 
                session.getId(), participant.getUserId());
    }

    /**
     * Publish participant left event.
     * Sends to: /topic/sessions/{sessionId} and /topic/users/{userId}/sessions
     */
    public void publishParticipantLeft(Session session, UUID userId, SessionResponse sessionResponse) {
        String sessionTopic = "/topic/sessions/" + session.getId();
        String userTopic = "/topic/users/" + userId + "/sessions";

        SessionEvent event = new SessionEvent("PARTICIPANT_LEFT", sessionResponse);

        messagingTemplate.convertAndSend(sessionTopic, event);
        messagingTemplate.convertAndSend(userTopic, event);

        log.info("Published PARTICIPANT_LEFT event for session: {}, participant: {}", 
                session.getId(), userId);
    }

    /**
     * Publish session ended event.
     * Sends to: /topic/sessions/{sessionId} and /topic/users/{userId}/sessions for all participants
     */
    public void publishSessionEnded(Session session, SessionResponse sessionResponse) {
        String sessionTopic = "/topic/sessions/" + session.getId();

        SessionEvent event = new SessionEvent("SESSION_ENDED", sessionResponse);

        messagingTemplate.convertAndSend(sessionTopic, event);

        // Notify all participants
        if (session.getParticipants() != null) {
            for (SessionParticipant participant : session.getParticipants()) {
                String userTopic = "/topic/users/" + participant.getUserId() + "/sessions";
                messagingTemplate.convertAndSend(userTopic, event);
            }
        }

        log.info("Published SESSION_ENDED event for session: {}", session.getId());
    }

    /**
     * WebSocket event wrapper.
     */
    public record SessionEvent(
            String type,
            SessionResponse data
    ) {}
}

