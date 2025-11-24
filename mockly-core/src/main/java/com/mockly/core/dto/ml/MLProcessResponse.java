package com.mockly.core.dto.ml;

import java.util.Map;

/**
 * Response from ML service after processing.
 */
public record MLProcessResponse(
        Map<String, Object> metrics,
        String summary,
        String recommendations,
        Map<String, Object> transcript
) {}

