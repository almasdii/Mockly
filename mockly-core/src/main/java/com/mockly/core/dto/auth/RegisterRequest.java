package com.mockly.core.dto.auth;

import com.mockly.data.entity.Profile;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email(message = "Invalid email format")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @NotBlank(message = "Name is required")
        @Size(max = 50, message = "Name must not exceed 50 characters")
        String name,

        @NotBlank(message = "Surname is required")
        @Size(max = 50, message = "Surname must not exceed 50 characters")
        String surname,

        @NotNull(message = "Role is required")
        Profile.ProfileRole role
) {}
