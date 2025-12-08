package com.mockly.core.service;

import com.mockly.core.dto.artifact.CompleteUploadRequest;
import com.mockly.core.dto.artifact.RequestUploadRequest;
import com.mockly.core.dto.artifact.RequestUploadResponse;
import com.mockly.core.dto.session.ArtifactResponse;
import com.mockly.core.exception.ArtifactUploadException;
import com.mockly.core.exception.BadRequestException;
import com.mockly.core.exception.ResourceNotFoundException;
import com.mockly.data.entity.Artifact;
import com.mockly.data.enums.ArtifactType;
import com.mockly.data.repository.ArtifactRepository;
import com.mockly.data.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for managing artifacts (audio files, recordings, etc.).
 * Handles upload requests, validation, and metadata storage.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactService {

    private static final long MAX_FILE_SIZE_BYTES = 500 * 1024 * 1024;
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "audio/mpeg", "audio/mp3", "audio/wav", "audio/wave", "audio/x-wav",
            "audio/webm", "audio/ogg", "audio/mp4", "audio/x-m4a",
            "application/octet-stream"
    );
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            ".mp3", ".wav", ".webm", ".ogg", ".m4a", ".mp4", ".bin", ".raw"
    );

    private final ArtifactRepository artifactRepository;
    private final SessionRepository sessionRepository;
    private final MinIOService minIOService;
    private final ReportService reportService;

    /**
     * Request upload URL for an artifact.
     * Validates file type and size, creates artifact record, generates pre-signed URL.
     *
     * @param sessionId Session ID
     * @param userId User ID (for authorization)
     * @param request Upload request with file metadata
     * @return Pre-signed URL and artifact ID
     */
    @Transactional
    public RequestUploadResponse requestUpload(UUID sessionId, UUID userId, RequestUploadRequest request) {
        log.info("Requesting upload URL for session: {}, type: {}, fileName: {}", 
                sessionId, request.type(), request.fileName());

        // Validate session exists and user has access
        sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        // Validate file size
        if (request.fileSizeBytes() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException(
                    String.format("File size exceeds maximum allowed size of %d MB", MAX_FILE_SIZE_BYTES / (1024 * 1024))
            );
        }

        // Validate file type
        validateFileType(request.fileName(), request.contentType());

        // Generate object name: sessions/{sessionId}/artifacts/{artifactId}/{fileName}
        UUID artifactId = UUID.randomUUID();
        String objectName = String.format("sessions/%s/artifacts/%s/%s", 
                sessionId, artifactId, sanitizeFileName(request.fileName()));

        // Create artifact record with PENDING status (will be updated on complete)
        Artifact artifact = Artifact.builder()
                .id(artifactId)
                .sessionId(sessionId)
                .type(request.type())
                .storageUrl(objectName) // Will be full URL after upload
                .sizeBytes(request.fileSizeBytes())
                .build();

        artifact = artifactRepository.save(artifact);
        log.info("Created artifact record: {}", artifactId);

        // Generate pre-signed upload URL (valid for 1 hour)
        int expirySeconds = 3600;
        String uploadUrl = minIOService.generatePresignedUploadUrl(objectName, expirySeconds);

        return new RequestUploadResponse(
                artifactId,
                uploadUrl,
                objectName,
                expirySeconds
        );
    }

    /**
     * Complete artifact upload.
     * Verifies file was uploaded, validates file size, and updates artifact metadata.
     * If artifact type is AUDIO_MIXED, automatically triggers ML pipeline.
     *
     * @param sessionId Session ID
     * @param artifactId Artifact ID
     * @param userId User ID (for authorization)
     * @param request Completion request with final metadata
     * @return Updated artifact response
     * @throws ArtifactUploadException if file size mismatch or upload verification fails
     */
    @Transactional
    public ArtifactResponse completeUpload(UUID sessionId, UUID artifactId, UUID userId, CompleteUploadRequest request) {
        try {
            log.info("=== COMPLETE UPLOAD START ===");
            log.info("SessionId: {}, ArtifactId: {}, UserId: {}", sessionId, artifactId, userId);
            log.info("Request: {}", request);
            log.info("FileSizeBytes: {}, DurationSec: {}", request.fileSizeBytes(), request.durationSec());

            // Validate session exists
            log.info("Validating session exists...");
            sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
            log.info("Session validated");

            // Get artifact
            log.info("Getting artifact...");
            Artifact artifact = artifactRepository.findById(artifactId)
                    .orElseThrow(() -> new ResourceNotFoundException("Artifact not found: " + artifactId));
            log.info("Artifact found: {}", artifact);

            // Verify artifact belongs to session
            log.info("Verifying artifact belongs to session...");
            if (!artifact.getSessionId().equals(sessionId)) {
                throw new BadRequestException("Artifact does not belong to this session");
            }
            log.info("Artifact belongs to session");

            // Verify file was uploaded to MinIO using statObject
            log.info("Checking if object exists in MinIO: {}", artifact.getStorageUrl());
            if (!minIOService.objectExists(artifact.getStorageUrl())) {
                throw new ArtifactUploadException("File was not uploaded to storage. Please upload the file first.");
            }
            log.info("Object exists in MinIO");

            // Get actual file size from MinIO using statObject
            log.info("Getting object metadata from MinIO...");
            var metadata = minIOService.getObjectMetadata(artifact.getStorageUrl());
            long actualSize = metadata.size();
            log.info("Actual file size from MinIO: {} bytes", actualSize);

            long expectedSize = request.fileSizeBytes() != null ? request.fileSizeBytes() : artifact.getSizeBytes();
            log.info("Expected file size: {} bytes", expectedSize);

            // Verify file size matches
            if (expectedSize > 0 && actualSize != expectedSize) {
                log.error("File size mismatch for artifact {}: expected {} bytes, actual {} bytes",
                        artifactId, expectedSize, actualSize);
                throw new ArtifactUploadException(
                        String.format("File size mismatch: expected %d bytes, but actual size is %d bytes. " +
                                "Please re-upload the file.", expectedSize, actualSize));
            }

            log.info("File size verification passed for artifact {}: {} bytes", artifactId, actualSize);

            // Save artifact metadata
            log.info("Updating artifact metadata...");
            artifact.setSizeBytes(actualSize);
            artifact.setDurationSec(request.durationSec());

            // Update storage URL to include full path
            String fullStorageUrl = String.format("%s/%s", minIOService.getBucketName(), artifact.getStorageUrl());
            artifact.setStorageUrl(fullStorageUrl);
            log.info("New storage URL: {}", fullStorageUrl);

            log.info("Saving artifact to database...");
            artifact = artifactRepository.save(artifact);
            log.info("Artifact saved successfully");

            // If artifact.type == AUDIO_MIXED → trigger ML pipeline automatically
//            if (artifact.getType() == ArtifactType.AUDIO_MIXED) {
//                log.info("AUDIO_MIXED artifact detected, triggering ML pipeline for session: {}", sessionId);
//                triggerMLPipelineAsync(sessionId);
//            }

            log.info("=== COMPLETE UPLOAD SUCCESS ===");
            return toResponse(artifact);

        } catch (Exception e) {
            log.error("=== COMPLETE UPLOAD ERROR ===", e);
            log.error("Error message: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Trigger ML pipeline asynchronously for session.
     */
    @Async("reportProcessingExecutor")
    public CompletableFuture<Void> triggerMLPipelineAsync(UUID sessionId) {
        try {
            log.info("Triggering ML pipeline for session: {}", sessionId);
            reportService.generateReport(sessionId);
            log.info("ML pipeline triggered successfully for session: {}", sessionId);
        } catch (Exception e) {
            log.error("Failed to trigger ML pipeline for session: {}", sessionId, e);
            // Don't throw - this is async and shouldn't block the upload completion
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Get artifact by ID.
     */
    @Transactional(readOnly = true)
    public ArtifactResponse getArtifact(UUID sessionId, UUID artifactId, UUID userId) {
        Artifact artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> new ResourceNotFoundException("Artifact not found: " + artifactId));

        if (!artifact.getSessionId().equals(sessionId)) {
            throw new BadRequestException("Artifact does not belong to this session");
        }

        return toResponse(artifact);
    }

    /**
     * List artifacts for a session.
     */
    @Transactional(readOnly = true)
    public List<ArtifactResponse> listArtifacts(UUID sessionId, UUID userId) {
        // Validate session exists
        sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        List<Artifact> artifacts = artifactRepository.findBySessionId(sessionId);
        return artifacts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Validate file type by extension and content type.
     */
    private void validateFileType(String fileName, String contentType) {
        // Check extension
        String lowerFileName = fileName.toLowerCase();
        boolean hasValidExtension = ALLOWED_EXTENSIONS.stream()
                .anyMatch(lowerFileName::endsWith);

        if (!hasValidExtension) {
            throw new BadRequestException(
                    String.format("File type not allowed. Allowed extensions: %s", ALLOWED_EXTENSIONS)
            );
        }

        // Check content type if provided
        if (contentType != null && !contentType.isBlank()) {
            String lowerContentType = contentType.toLowerCase();
            boolean hasValidContentType = ALLOWED_CONTENT_TYPES.stream()
                    .anyMatch(lowerContentType::startsWith);

            if (!hasValidContentType) {
                log.warn("Content type {} not in allowed list, but extension is valid. Proceeding.", contentType);
            }
        }
    }

    /**
     * Sanitize file name to prevent path traversal and special characters.
     */
    private String sanitizeFileName(String fileName) {
        // Remove path separators and dangerous characters
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Convert entity to response DTO.
     */
    private ArtifactResponse toResponse(Artifact artifact) {
        return new ArtifactResponse(
                artifact.getId(),
                artifact.getSessionId(),
                artifact.getType(),
                artifact.getStorageUrl(),
                artifact.getDurationSec(),
                artifact.getSizeBytes(),
                artifact.getCreatedAt()
        );
    }
}

