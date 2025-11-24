package com.mockly.core.dto.session;

import com.mockly.data.enums.SessionStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response containing complete session information.
 */
public record SessionResponse(
        UUID id,
        UUID createdBy,
        String creatorDisplayName,
        SessionStatus status,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String roomProvider,
        String roomId,
        String recordingId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<SessionParticipantResponse> participants,
        List<ArtifactResponse> artifacts
) {}

