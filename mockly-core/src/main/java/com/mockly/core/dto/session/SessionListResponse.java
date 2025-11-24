package com.mockly.core.dto.session;

import java.util.List;

/**
 * Response containing paginated list of sessions.
 */
public record SessionListResponse(
        List<SessionResponse> sessions,
        long total,
        int page,
        int size
) {}

