package com.mockly.core.dto.report;

import com.mockly.data.entity.Report;
import java.util.Map;
import java.util.UUID;

/**
 * Response containing report data.
 */
public record ReportResponse(
        UUID id,
        UUID sessionId,
        Map<String, Object> metrics,
        String summary,
        String recommendations,
        Report.ReportStatus status,
        String errorMessage,
        java.time.OffsetDateTime createdAt,
        java.time.OffsetDateTime updatedAt
) {}

