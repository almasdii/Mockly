package com.mockly.api.controller;

import com.mockly.core.dto.artifact.CompleteUploadRequest;
import com.mockly.core.dto.artifact.RequestUploadRequest;
import com.mockly.core.dto.artifact.RequestUploadResponse;
import com.mockly.core.dto.session.ArtifactResponse;
import com.mockly.core.service.ArtifactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions/{sessionId}/artifacts")
@RequiredArgsConstructor
@Tag(name = "Artifacts", description = "Artifact upload and management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ArtifactController {

    private final ArtifactService artifactService;

    @PostMapping("/request-upload")
    @Operation(
            summary = "Request upload URL",
            description = "Generates a pre-signed URL for uploading an artifact. Validates file type and size (max 500MB)."
    )
    public ResponseEntity<RequestUploadResponse> requestUpload(
            Authentication authentication,
            @PathVariable UUID sessionId,
            @Valid @RequestBody RequestUploadRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        RequestUploadResponse response = artifactService.requestUpload(sessionId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{artifactId}/complete")
    @Operation(
            summary = "Complete upload",
            description = "Marks artifact upload as complete and saves metadata. Verifies file was uploaded to storage."
    )
    public ResponseEntity<ArtifactResponse> completeUpload(
            Authentication authentication,
            @PathVariable UUID sessionId,
            @PathVariable UUID artifactId,
            @Valid @RequestBody CompleteUploadRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        ArtifactResponse response = artifactService.completeUpload(sessionId, artifactId, userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{artifactId}")
    @Operation(
            summary = "Get artifact",
            description = "Returns artifact details by ID."
    )
    public ResponseEntity<ArtifactResponse> getArtifact(
            Authentication authentication,
            @PathVariable UUID sessionId,
            @PathVariable UUID artifactId) {
        UUID userId = UUID.fromString(authentication.getName());
        ArtifactResponse response = artifactService.getArtifact(sessionId, artifactId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            summary = "List artifacts",
            description = "Returns all artifacts for a session."
    )
    public ResponseEntity<List<ArtifactResponse>> listArtifacts(
            Authentication authentication,
            @PathVariable UUID sessionId) {
        UUID userId = UUID.fromString(authentication.getName());
        List<ArtifactResponse> response = artifactService.listArtifacts(sessionId, userId);
        return ResponseEntity.ok(response);
    }
}

