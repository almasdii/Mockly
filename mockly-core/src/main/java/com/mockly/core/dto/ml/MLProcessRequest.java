package com.mockly.core.dto.ml;

import java.util.UUID;

/**
 * Request to ML service for processing artifact.
 */
public record MLProcessRequest(
        UUID sessionId,
        UUID artifactId,
        String artifactUrl,
        String artifactType
) {}

