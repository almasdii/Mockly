package com.mockly.core.dto.user;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 50, message = "Name must not exceed 50 characters")
        String name,

        @Size(max = 50, message = "Surname must not exceed 50 characters")
        String surname,

        @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
        String avatarUrl,

        @Size(max = 50, message = "Level must not exceed 50 characters")
        String level
) {}

