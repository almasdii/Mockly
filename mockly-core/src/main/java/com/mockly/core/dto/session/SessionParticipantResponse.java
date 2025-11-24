package com.mockly.core.dto.session;

import com.mockly.data.enums.ParticipantRole;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response containing participant information in a session.
 */
public record SessionParticipantResponse(
        UUID id,
        UUID userId,
        String userDisplayName,
        String userEmail,
        ParticipantRole roleInSession,
        OffsetDateTime joinedAt,
        OffsetDateTime leftAt
) {}

