package com.mockly.core.service;

import com.mockly.core.dto.user.UpdateProfileRequest;
import com.mockly.core.dto.user.UserResponse;
import com.mockly.core.exception.ResourceNotFoundException;
import com.mockly.core.exception.UserNotFoundException;
import com.mockly.data.entity.Profile;
import com.mockly.data.entity.User;
import com.mockly.data.repository.ProfileRepository;
import com.mockly.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                profile.getName(),
                profile.getSurname(),
                profile.getRole(),
                profile.getAvatarUrl(),
                profile.getLevel(),
                profile.getSkills()
        );
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + user.getId()));

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                profile.getName(),
                profile.getSurname(),
                profile.getRole(),
                profile.getAvatarUrl(),
                profile.getLevel(),
                profile.getSkills()
        );
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));

        if (request.name() != null) {
            profile.setName(request.name());
        }
        if (request.surname() != null) {
            profile.setSurname(request.surname());
        }
        if (request.avatarUrl() != null) {
            profile.setAvatarUrl(request.avatarUrl());
        }
        if (request.level() != null) {
            profile.setLevel(request.level());
        }

        profile = profileRepository.save(profile);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                profile.getName(),
                profile.getSurname(),
                profile.getRole(),
                profile.getAvatarUrl(),
                profile.getLevel(),
                profile.getSkills()
        );
    }
}

