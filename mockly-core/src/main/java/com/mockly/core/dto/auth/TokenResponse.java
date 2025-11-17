package com.mockly.core.dto.auth;

import java.util.UUID;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        UUID userId
) {}

