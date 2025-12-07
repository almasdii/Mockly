package com.mockly.core.service;

import com.mockly.core.dto.report.ReportResponse;
import com.mockly.core.exception.BadRequestException;
import com.mockly.core.exception.ResourceNotFoundException;
import com.mockly.data.entity.Artifact;
import com.mockly.data.entity.Report;
import com.mockly.data.entity.Transcript;
import com.mockly.data.enums.ArtifactType;
import com.mockly.data.repository.ArtifactRepository;
import com.mockly.data.repository.ReportRepository;
import com.mockly.data.repository.SessionRepository;
import com.mockly.data.repository.TranscriptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing reports.
 * Handles report generation, ML processing, and status management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final SessionRepository sessionRepository;
    private final ArtifactRepository artifactRepository;
    private final TranscriptRepository transcriptRepository;
    private final MLServiceClient mlServiceClient;
    private final MinIOService minIOService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Trigger report generation for a session.
     * Creates a PENDING report and starts async processing.
     *
     * @param sessionId Session ID
     * @param userId User ID (for authorization)
     * @return Report response
     */
    @Transactional
    public ReportResponse triggerReportGeneration(UUID sessionId, UUID userId) {
        log.info("Triggering report generation for session: {}", sessionId);


        sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));


        Optional<Report> existingReport = reportRepository.findBySessionId(sessionId);
        if (existingReport.isPresent()) {
            Report report = existingReport.get();
            if (report.getStatus() == Report.ReportStatus.PROCESSING || 
                report.getStatus() == Report.ReportStatus.READY) {
                log.info("Report already exists and is {} for session: {}", report.getStatus(), sessionId);
                return toResponse(report);
            }

            if (report.getStatus() == Report.ReportStatus.FAILED) {
                report.setStatus(Report.ReportStatus.PENDING);
                report.setErrorMessage(null);
                report = reportRepository.save(report);
            }
        } else {

            Report report = Report.builder()
                    .sessionId(sessionId)
                    .status(Report.ReportStatus.PENDING)
                    .build();
            report = reportRepository.save(report);
        }


        Artifact artifact = artifactRepository.findFirstBySessionIdAndType(sessionId, ArtifactType.AUDIO_MIXED)
                .orElseGet(() -> artifactRepository.findBySessionId(sessionId).stream()
                        .filter(a -> a.getType() == ArtifactType.AUDIO_LEFT || 
                                   a.getType() == ArtifactType.AUDIO_RIGHT)
                        .findFirst()
                        .orElseThrow(() -> new BadRequestException(
                                "No audio artifact found for session. Please upload an audio file first.")));


        processReportAsync(sessionId, artifact.getId());

        // Return current report status
        Report report = reportRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found after creation"));
        return toResponse(report);
    }

    /**
     * Process report asynchronously.
     * Fetches artifact, sends to ML service, saves results.
     */
    @Async("reportProcessingExecutor")
    @Transactional
    public CompletableFuture<Void> processReportAsync(UUID sessionId, UUID artifactId) {
        log.info("Starting async report processing for session: {}, artifact: {}", sessionId, artifactId);

        Report report = reportRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + sessionId));

        try {

            report.setStatus(Report.ReportStatus.PROCESSING);
            report = reportRepository.save(report);


            Artifact artifact = artifactRepository.findById(artifactId)
                    .orElseThrow(() -> new ResourceNotFoundException("Artifact not found: " + artifactId));


            String artifactUrl = minIOService.generatePresignedDownloadUrl(artifact.getStorageUrl(), 3600);


            var mlRequest = new com.mockly.core.dto.ml.MLProcessRequest(
                    sessionId,
                    artifactId,
                    artifactUrl,
                    artifact.getType().name()
            );

            var mlResponse = mlServiceClient.process(mlRequest);


            if (mlResponse.transcript() != null && !mlResponse.transcript().isEmpty()) {
                Transcript transcript = Transcript.builder()
                        .sessionId(sessionId)
                        .source(Transcript.TranscriptSource.MIXED)
                        .text(mlResponse.transcript())
                        .build();
                transcriptRepository.save(transcript);
                log.info("Saved transcript for session: {}", sessionId);
            }


            report.setMetrics(mlResponse.metrics());
            report.setSummary(mlResponse.summary());
            report.setRecommendations(mlResponse.recommendations());
            report.setStatus(Report.ReportStatus.READY);
            report.setErrorMessage(null);
            report = reportRepository.save(report);

            log.info("Report processing completed successfully for session: {}", sessionId);


            eventPublisher.publishEvent(new ReportReadyEvent(sessionId, toResponse(report)));

        } catch (Exception e) {
            log.error("Report processing failed for session: {}", sessionId, e);
            report.setStatus(Report.ReportStatus.FAILED);
            report.setErrorMessage(e.getMessage());
            reportRepository.save(report);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Generate report for a session synchronously.
     * Collects all artifacts, calls ML service, saves results, and triggers WebSocket event.
     *
     * @param sessionId Session ID
     * @return Report response with READY status
     */
    @Transactional
    public ReportResponse generateReport(UUID sessionId) {
        log.info("Generating report for session: {}", sessionId);


        sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));


        Report report = reportRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Report newReport = Report.builder()
                            .sessionId(sessionId)
                            .status(Report.ReportStatus.PENDING)
                            .build();
                    return reportRepository.save(newReport);
                });

        try {

            report.setStatus(Report.ReportStatus.PROCESSING);
            report.setErrorMessage(null);
            report = reportRepository.save(report);


            var allArtifacts = artifactRepository.findBySessionId(sessionId);
            if (allArtifacts.isEmpty()) {
                throw new BadRequestException(
                        "No artifacts found for session. Please upload at least one audio artifact first.");
            }

            log.info("Found {} artifacts for session: {}", allArtifacts.size(), sessionId);


            Artifact primaryArtifact = allArtifacts.stream()
                    .filter(a -> a.getType() == ArtifactType.AUDIO_MIXED)
                    .findFirst()
                    .orElseGet(() -> allArtifacts.stream()
                            .filter(a -> a.getType() == ArtifactType.AUDIO_LEFT || 
                                       a.getType() == ArtifactType.AUDIO_RIGHT)
                            .findFirst()
                            .orElseThrow(() -> new BadRequestException(
                                    "No audio artifact found for session. Please upload an audio file first.")));

            log.info("Using primary artifact: {} (type: {}) for ML processing", 
                    primaryArtifact.getId(), primaryArtifact.getType());

            String artifactUrl = minIOService.generatePresignedDownloadUrl(
                    primaryArtifact.getStorageUrl(), 3600);

            var mlRequest = new com.mockly.core.dto.ml.MLProcessRequest(
                    sessionId,
                    primaryArtifact.getId(),
                    artifactUrl,
                    primaryArtifact.getType().name()
            );

            log.info("Calling ML service for session: {}, artifact: {}", sessionId, primaryArtifact.getId());


            var mlResponse = mlServiceClient.process(mlRequest);

            log.info("ML service processing completed for session: {}", sessionId);


            if (mlResponse.transcript() != null && !mlResponse.transcript().isEmpty()) {
                Transcript transcript = Transcript.builder()
                        .sessionId(sessionId)
                        .source(Transcript.TranscriptSource.MIXED)
                        .text(mlResponse.transcript())
                        .build();
                transcriptRepository.save(transcript);
                log.info("Saved transcript for session: {}", sessionId);
            }


            report.setMetrics(mlResponse.metrics());
            report.setSummary(mlResponse.summary());
            report.setRecommendations(mlResponse.recommendations());
            report.setStatus(Report.ReportStatus.READY);
            report.setErrorMessage(null);
            report = reportRepository.save(report);

            log.info("Report saved successfully for session: {}", sessionId);


            ReportResponse reportResponse = toResponse(report);

            eventPublisher.publishEvent(new ReportReadyEvent(sessionId, reportResponse));

            log.info("Report generation completed successfully for session: {}", sessionId);

            return reportResponse;

        } catch (BadRequestException | ResourceNotFoundException e) {

            log.error("Business error during report generation for session: {}", sessionId, e);
            report.setStatus(Report.ReportStatus.FAILED);
            report.setErrorMessage(e.getMessage());
            reportRepository.save(report);
            throw e;
        } catch (Exception e) {

            log.error("Unexpected error during report generation for session: {}", sessionId, e);
            report.setStatus(Report.ReportStatus.FAILED);
            report.setErrorMessage("Report generation failed: " + 
                    (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            reportRepository.save(report);
            throw new RuntimeException("Failed to generate report: " + e.getMessage(), e);
        }
    }

    /**
     * Get report for a session.
     *
     * @param sessionId Session ID
     * @param userId User ID (for authorization)
     * @return Report response
     */
    @Transactional(readOnly = true)
    public ReportResponse getReport(UUID sessionId, UUID userId) {
        Report report = reportRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found for session: " + sessionId));

        return toResponse(report);
    }

    /**
     * Convert entity to response DTO.
     */
    private ReportResponse toResponse(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getSessionId(),
                report.getMetrics(),
                report.getSummary(),
                report.getRecommendations(),
                report.getStatus(),
                report.getErrorMessage(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }

    /**
     * Event published when report processing is complete.
     */
    public record ReportReadyEvent(UUID sessionId, ReportResponse report) {}
}

