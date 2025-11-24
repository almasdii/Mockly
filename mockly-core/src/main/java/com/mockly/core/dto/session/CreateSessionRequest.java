package com.mockly.core.dto.session;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Request to create a new interview session.
 */
public record CreateSessionRequest(
        /**
         * ID of the interviewer who will conduct the interview.
         */
        @NotNull(message = "Interviewer ID is required")
        UUID interviewerId,

        /**
         * Scheduled start time for the session (optional).
         * If not provided, session will be created as SCHEDULED without a specific time.
         */
        OffsetDateTime scheduledAt
) {}

