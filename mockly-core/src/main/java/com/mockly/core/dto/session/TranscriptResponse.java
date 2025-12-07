package com.mockly.core.dto.session;

import com.mockly.data.entity.Transcript;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response containing transcript information.
 */
public record TranscriptResponse(
        UUID id,
        UUID sessionId,
        Transcript.TranscriptSource source,
        Map<String, Object> text,
        Map<String, Object> words,
        OffsetDateTime createdAt
) {}

