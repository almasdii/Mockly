package com.mockly.core.dto.user;

import com.mockly.data.entity.Profile;
import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String displayName,
        Profile.ProfileRole role,
        String avatarUrl,
        String level,
        List<String> skills
) {}

