package com.mockly.core.dto.session;

import com.mockly.data.enums.ArtifactType;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response containing artifact information.
 */
public record ArtifactResponse(
        UUID id,
        UUID sessionId,
        ArtifactType type,
        String storageUrl,
        Integer durationSec,
        Long sizeBytes,
        OffsetDateTime createdAt
) {}

