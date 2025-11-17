package com.mockly.core.service;

import com.mockly.core.dto.auth.LoginRequest;
import com.mockly.core.dto.auth.RefreshTokenRequest;
import com.mockly.core.dto.auth.RegisterRequest;
import com.mockly.core.dto.auth.TokenResponse;
import com.mockly.core.exception.BadRequestException;
import com.mockly.core.exception.EmailAlreadyExistsException;
import com.mockly.core.exception.InvalidCredentialsException;
import com.mockly.core.exception.TokenInvalidException;
import com.mockly.core.exception.UserNotFoundException;
import com.mockly.data.entity.Profile;
import com.mockly.data.entity.User;
import com.mockly.data.repository.ProfileRepository;
import com.mockly.data.repository.UserRepository;
import com.mockly.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 86400000L; // 24 hours

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        user = userRepository.save(user);

        Profile profile = Profile.builder()
                .userId(user.getId())
                .user(user)
                .role(request.role())
                .displayName(request.displayName())
                .skills(List.of())
                .build();

        profileRepository.save(profile);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), request.role());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        storeRefreshToken(refreshToken, user.getId());

        return new TokenResponse(accessToken, refreshToken, user.getId());
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException());

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Profile not found for user"));

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), profile.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        storeRefreshToken(refreshToken, user.getId());

        return new TokenResponse(accessToken, refreshToken, user.getId());
    }

    public TokenResponse refreshToken(RefreshTokenRequest request) {
        if (!jwtTokenProvider.validateRefreshToken(request.refreshToken())) {
            throw new TokenInvalidException("Invalid refresh token");
        }

        UUID userId;
        try {
            userId = jwtTokenProvider.getUserIdFromRefreshToken(request.refreshToken());
        } catch (Exception e) {
            throw new TokenInvalidException("Invalid refresh token", e);
        }

        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
        if (storedToken == null || !storedToken.equals(request.refreshToken())) {
            throw new TokenInvalidException("Refresh token not found or expired");
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Profile not found for user"));

        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, profile.getRole());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        storeRefreshToken(newRefreshToken, userId);

        return new TokenResponse(newAccessToken, newRefreshToken, userId);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (jwtTokenProvider.validateRefreshToken(refreshToken)) {
            UUID userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);
            redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
        }
    }

    private void storeRefreshToken(String refreshToken, UUID userId) {
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userId,
                refreshToken,
                REFRESH_TOKEN_EXPIRATION_MS,
                TimeUnit.MILLISECONDS
        );
    }
}

