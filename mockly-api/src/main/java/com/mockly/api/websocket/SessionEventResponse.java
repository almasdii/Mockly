package com.mockly.api.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mockly.core.dto.report.ReportResponse;
import com.mockly.core.dto.session.ArtifactResponse;
import com.mockly.core.dto.session.SessionResponse;
import com.mockly.core.dto.session.TranscriptResponse;

/**
 * Unified event response for WebSocket notifications.
 * Contains event type and relevant data based on event type.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionEventResponse(
        SessionEventType type,
        SessionResponse session,
        ArtifactResponse artifact,
        TranscriptResponse transcript,
        ReportResponse report
) {
    /**
     * Create event response for session-related events.
     */
    public static SessionEventResponse sessionEvent(SessionEventType type, SessionResponse session) {
        return new SessionEventResponse(type, session, null, null, null);
    }

    /**
     * Create event response for artifact upload.
     */
    public static SessionEventResponse artifactEvent(SessionEventType type, SessionResponse session, ArtifactResponse artifact) {
        return new SessionEventResponse(type, session, artifact, null, null);
    }

    /**
     * Create event response for transcript addition.
     */
    public static SessionEventResponse transcriptEvent(SessionEventType type, SessionResponse session, TranscriptResponse transcript) {
        return new SessionEventResponse(type, session, null, transcript, null);
    }

    /**
     * Create event response for report ready.
     */
    public static SessionEventResponse reportEvent(SessionEventType type, SessionResponse session, ReportResponse report) {
        return new SessionEventResponse(type, session, null, null, report);
    }
}

