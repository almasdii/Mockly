package com.mockly.api.controller;

import com.mockly.api.websocket.SessionEventPublisher;
import com.mockly.core.dto.report.ReportResponse;
import com.mockly.core.service.ReportService;
import com.mockly.data.entity.Report;
import com.mockly.data.entity.Session;
import com.mockly.data.repository.SessionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/sessions/{sessionId}/report")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Report generation and retrieval endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;
    private final SessionRepository sessionRepository;
    private final SessionEventPublisher eventPublisher;

    @PostMapping("/trigger")
    @Operation(
            summary = "Trigger report generation",
            description = "Starts async report generation for a session. Requires an uploaded audio artifact. " +
                         "Returns immediately with PENDING status. Report will be processed asynchronously."
    )
    public ResponseEntity<ReportResponse> triggerReport(
            Authentication authentication,
            @PathVariable UUID sessionId) {
        UUID userId = UUID.fromString(authentication.getName());
        ReportResponse response = reportService.triggerReportGeneration(sessionId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            summary = "Get report",
            description = "Returns report status and data for a session. Status can be PENDING, PROCESSING, READY, or FAILED."
    )
    public ResponseEntity<ReportResponse> getReport(
            Authentication authentication,
            @PathVariable UUID sessionId) {
        UUID userId = UUID.fromString(authentication.getName());
        ReportResponse response = reportService.getReport(sessionId, userId);

        // Publish WebSocket event if report is ready
        if (response.status() == Report.ReportStatus.READY) {
            Session session = sessionRepository.findById(sessionId).orElse(null);
            if (session != null) {
                eventPublisher.reportReady(session, response);
            }
        }

        return ResponseEntity.ok(response);
    }
}

