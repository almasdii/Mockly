package com.mockly.core.dto.artifact;

import java.util.UUID;

/**
 * Response containing pre-signed URL and artifact ID for upload.
 */
public record RequestUploadResponse(
        UUID artifactId,
        String uploadUrl,
        String objectName,
        Integer expiresInSeconds
) {}

