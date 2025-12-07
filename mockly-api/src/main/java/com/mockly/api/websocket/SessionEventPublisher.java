package com.mockly.api.websocket;

import com.mockly.core.dto.report.ReportResponse;
import com.mockly.core.dto.session.ArtifactResponse;
import com.mockly.core.dto.session.SessionResponse;
import com.mockly.core.dto.session.TranscriptResponse;
import com.mockly.core.mapper.SessionMapper;
import com.mockly.data.entity.Artifact;
import com.mockly.data.entity.Session;
import com.mockly.data.entity.SessionParticipant;
import com.mockly.data.entity.Transcript;
import com.mockly.data.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publishes session-related events to WebSocket clients.
 * Uses STOMP messaging to send real-time updates.
 * All events are sent to: /topic/sessions/{sessionId}
 * Payload: SessionEventResponse
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final SessionMapper sessionMapper;
    private final SessionRepository sessionRepository;

    private static final String SESSION_TOPIC_PREFIX = "/topic/sessions/";

    /**
     * Publish participant joined event.
     * Sends to: /topic/sessions/{sessionId}
     */
    public void participantJoined(Session session, SessionParticipant participant, SessionResponse sessionResponse) {
        String sessionTopic = SESSION_TOPIC_PREFIX + session.getId();
        SessionEventResponse event = SessionEventResponse.sessionEvent(
                SessionEventType.PARTICIPANT_JOINED, sessionResponse);
        
        publishToSessionTopic(sessionTopic, event);
        log.info("Published PARTICIPANT_JOINED event for session: {}, participant: {}", 
                session.getId(), participant.getUserId());
    }

    /**
     * Publish participant left event.
     * Sends to: /topic/sessions/{sessionId}
     */
    public void participantLeft(Session session, UUID userId, SessionResponse sessionResponse) {
        String sessionTopic = SESSION_TOPIC_PREFIX + session.getId();
        SessionEventResponse event = SessionEventResponse.sessionEvent(
                SessionEventType.PARTICIPANT_LEFT, sessionResponse);
        
        publishToSessionTopic(sessionTopic, event);
        log.info("Published PARTICIPANT_LEFT event for session: {}, participant: {}", 
                session.getId(), userId);
    }

    /**
     * Publish artifact uploaded event.
     * Sends to: /topic/sessions/{sessionId}
     */
    public void artifactUploaded(Session session, Artifact artifact) {
        SessionResponse sessionResponse = loadAndMapSession(session.getId());
        ArtifactResponse artifactResponse = sessionMapper.toResponse(artifact);
        
        String sessionTopic = SESSION_TOPIC_PREFIX + session.getId();
        SessionEventResponse event = SessionEventResponse.artifactEvent(
                SessionEventType.ARTIFACT_UPLOADED, sessionResponse, artifactResponse);
        
        publishToSessionTopic(sessionTopic, event);
        log.info("Published ARTIFACT_UPLOADED event for session: {}, artifact: {}", 
                session.getId(), artifact.getId());
    }

    /**
     * Publish transcript added event.
     * Sends to: /topic/sessions/{sessionId}
     */
    public void transcriptAdded(Session session, Transcript transcript) {
        SessionResponse sessionResponse = loadAndMapSession(session.getId());
        TranscriptResponse transcriptResponse = toTranscriptResponse(transcript);
        
        String sessionTopic = SESSION_TOPIC_PREFIX + session.getId();
        SessionEventResponse event = SessionEventResponse.transcriptEvent(
                SessionEventType.TRANSCRIPT_ADDED, sessionResponse, transcriptResponse);
        
        publishToSessionTopic(sessionTopic, event);
        log.info("Published TRANSCRIPT_ADDED event for session: {}, transcript: {}", 
                session.getId(), transcript.getId());
    }

    /**
     * Publish report ready event.
     * Sends to: /topic/sessions/{sessionId}
     */
    public void reportReady(Session session, ReportResponse reportResponse) {
        SessionResponse sessionResponse = loadAndMapSession(session.getId());
        
        String sessionTopic = SESSION_TOPIC_PREFIX + session.getId();
        SessionEventResponse event = SessionEventResponse.reportEvent(
                SessionEventType.REPORT_READY, sessionResponse, reportResponse);
        
        publishToSessionTopic(sessionTopic, event);
        log.info("Published REPORT_READY event for session: {}", session.getId());
    }

    // Legacy methods for backward compatibility

    /**
     * @deprecated Use {@link #participantJoined(Session, SessionParticipant, SessionResponse)} instead
     */
    @Deprecated
    public void publishParticipantJoined(Session session, SessionParticipant participant, SessionResponse sessionResponse) {
        participantJoined(session, participant, sessionResponse);
    }

    /**
     * @deprecated Use {@link #participantLeft(Session, UUID, SessionResponse)} instead
     */
    @Deprecated
    public void publishParticipantLeft(Session session, UUID userId, SessionResponse sessionResponse) {
        participantLeft(session, userId, sessionResponse);
    }

    /**
     * @deprecated Use {@link #reportReady(Session, ReportResponse)} instead
     */
    @Deprecated
    public void publishReportReady(Session session, ReportResponse reportResponse) {
        reportReady(session, reportResponse);
    }

    /**
     * Publish session created event (legacy).
     */
    public void publishSessionCreated(Session session, SessionResponse sessionResponse) {
        String sessionTopic = SESSION_TOPIC_PREFIX + session.getId();
        SessionEventResponse event = SessionEventResponse.sessionEvent(
                SessionEventType.SESSION_CREATED, sessionResponse);
        
        publishToSessionTopic(sessionTopic, event);
        log.info("Published SESSION_CREATED event for session: {}", session.getId());
    }

    /**
     * Publish session updated event (legacy).
     */
    public void publishSessionUpdated(Session session, SessionResponse sessionResponse) {
        String sessionTopic = SESSION_TOPIC_PREFIX + session.getId();
        SessionEventResponse event = SessionEventResponse.sessionEvent(
                SessionEventType.SESSION_UPDATED, sessionResponse);
        
        publishToSessionTopic(sessionTopic, event);
        log.info("Published SESSION_UPDATED event for session: {}", session.getId());
    }

    /**
     * Publish session ended event (legacy).
     */
    public void publishSessionEnded(Session session, SessionResponse sessionResponse) {
        String sessionTopic = SESSION_TOPIC_PREFIX + session.getId();
        SessionEventResponse event = SessionEventResponse.sessionEvent(
                SessionEventType.SESSION_ENDED, sessionResponse);
        
        publishToSessionTopic(sessionTopic, event);
        log.info("Published SESSION_ENDED event for session: {}", session.getId());
    }

    // Private helper methods

    /**
     * Publish event to session topic.
     * DRY method to avoid code duplication.
     */
    private void publishToSessionTopic(String sessionTopic, SessionEventResponse event) {
        messagingTemplate.convertAndSend(sessionTopic, event);
    }

    /**
     * Load session from repository and map to SessionResponse.
     */
    private SessionResponse loadAndMapSession(UUID sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        return sessionMapper.toResponse(session);
    }

    /**
     * Convert Transcript entity to TranscriptResponse DTO.
     */
    private TranscriptResponse toTranscriptResponse(Transcript transcript) {
        return new TranscriptResponse(
                transcript.getId(),
                transcript.getSessionId(),
                transcript.getSource(),
                transcript.getText(),
                transcript.getWords(),
                transcript.getCreatedAt()
        );
    }
}
