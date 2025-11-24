package com.mockly.core.dto.artifact;

import com.mockly.data.enums.ArtifactType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request to generate a pre-signed URL for artifact upload.
 */
public record RequestUploadRequest(
        @NotNull(message = "Artifact type is required")
        ArtifactType type,

        @NotNull(message = "File name is required")
        @Size(min = 1, max = 255, message = "File name must be between 1 and 255 characters")
        String fileName,

        @NotNull(message = "File size is required")
        @Positive(message = "File size must be positive")
        Long fileSizeBytes,

        String contentType
) {}

