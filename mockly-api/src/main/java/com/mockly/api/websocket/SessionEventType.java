package com.mockly.api.websocket;

/**
 * Enumeration of session event types for WebSocket notifications.
 */
public enum SessionEventType {
    SESSION_CREATED,
    SESSION_UPDATED,
    SESSION_ENDED,
    PARTICIPANT_JOINED,
    PARTICIPANT_LEFT,
    ARTIFACT_UPLOADED,
    TRANSCRIPT_ADDED,
    REPORT_READY
}

