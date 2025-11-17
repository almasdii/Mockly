package com.mockly.core.service;

import com.mockly.core.dto.auth.LoginRequest;
import com.mockly.core.dto.auth.RegisterRequest;
import com.mockly.core.dto.auth.RefreshTokenRequest;
import com.mockly.core.dto.auth.TokenResponse;
import com.mockly.core.exception.EmailAlreadyExistsException;
import com.mockly.core.exception.InvalidCredentialsException;
import com.mockly.core.exception.TokenInvalidException;
import com.mockly.core.exception.UserNotFoundException;
import com.mockly.data.entity.Profile;
import com.mockly.data.entity.User;
import com.mockly.data.repository.ProfileRepository;
import com.mockly.data.repository.UserRepository;
import com.mockly.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private User testUser;
    private Profile testProfile;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        
        registerRequest = new RegisterRequest(
                "test@example.com",
                "password123",
                "Test User",
                Profile.ProfileRole.CANDIDATE
        );

        testUser = User.builder()
                .id(testUserId)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .build();

        testProfile = Profile.builder()
                .userId(testUserId)
                .user(testUser)
                .role(Profile.ProfileRole.CANDIDATE)
                .displayName("Test User")
                .skills(List.of())
                .build();

        testUser.setProfile(testProfile);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUserSuccessfully() {
        // Given
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.password())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(any(UUID.class), any(Profile.ProfileRole.class)))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(UUID.class)))
                .thenReturn("refresh-token");
        when(profileRepository.save(any(Profile.class))).thenReturn(testProfile);

        // When
        TokenResponse response = authService.register(registerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.userId()).isEqualTo(testUserId);

        verify(userRepository).existsByEmail(registerRequest.email());
        verify(userRepository).save(any(User.class));
        verify(profileRepository).save(any(Profile.class));
        verify(jwtTokenProvider).generateAccessToken(any(UUID.class), eq(Profile.ProfileRole.CANDIDATE));
        verify(jwtTokenProvider).generateRefreshToken(any(UUID.class));
        verify(valueOperations).set(anyString(), eq("refresh-token"), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when email already exists")
    void shouldThrowExceptionWhenEmailExists() {
        // Given
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining(registerRequest.email());

        verify(userRepository, never()).save(any());
        verify(profileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should login user successfully")
    void shouldLoginSuccessfully() {
        // Given
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.password(), testUser.getPasswordHash()))
                .thenReturn(true);
        when(profileRepository.findByUserId(testUserId)).thenReturn(Optional.of(testProfile));
        when(jwtTokenProvider.generateAccessToken(testUserId, testProfile.getRole()))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(testUserId))
                .thenReturn("refresh-token");

        // When
        TokenResponse response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.userId()).isEqualTo(testUserId);

        verify(userRepository).findByEmail(loginRequest.email());
        verify(passwordEncoder).matches(loginRequest.password(), testUser.getPasswordHash());
        verify(jwtTokenProvider).generateAccessToken(testUserId, testProfile.getRole());
        verify(jwtTokenProvider).generateRefreshToken(testUserId);
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException for invalid password")
    void shouldThrowExceptionForInvalidPassword() {
        // Given
        LoginRequest loginRequest = new LoginRequest("test@example.com", "wrongPassword");
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.password(), testUser.getPasswordHash()))
                .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtTokenProvider, never()).generateAccessToken(any(), any());
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void shouldRefreshTokenSuccessfully() {
        // Given
        String refreshToken = "valid-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        
        when(jwtTokenProvider.validateRefreshToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromRefreshToken(refreshToken)).thenReturn(testUserId);
        when(redisTemplate.opsForValue().get(anyString())).thenReturn(refreshToken);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(testUserId)).thenReturn(Optional.of(testProfile));
        when(jwtTokenProvider.generateAccessToken(testUserId, testProfile.getRole()))
                .thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(testUserId))
                .thenReturn("new-refresh-token");

        // When
        TokenResponse response = authService.refreshToken(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.userId()).isEqualTo(testUserId);

        verify(jwtTokenProvider).validateRefreshToken(refreshToken);
        verify(jwtTokenProvider).getUserIdFromRefreshToken(refreshToken);
        verify(jwtTokenProvider).generateAccessToken(testUserId, testProfile.getRole());
        verify(jwtTokenProvider).generateRefreshToken(testUserId);
    }

    @Test
    @DisplayName("Should throw TokenInvalidException for invalid refresh token")
    void shouldThrowExceptionForInvalidRefreshToken() {
        // Given
        String refreshToken = "invalid-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        
        when(jwtTokenProvider.validateRefreshToken(refreshToken)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(TokenInvalidException.class)
                .hasMessageContaining("Invalid refresh token");

        verify(jwtTokenProvider, never()).getUserIdFromRefreshToken(anyString());
    }

    @Test
    @DisplayName("Should throw TokenInvalidException when refresh token not found in Redis")
    void shouldThrowExceptionWhenRefreshTokenNotFoundInRedis() {
        // Given
        String refreshToken = "valid-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        
        when(jwtTokenProvider.validateRefreshToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromRefreshToken(refreshToken)).thenReturn(testUserId);
        when(redisTemplate.opsForValue().get(anyString())).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(TokenInvalidException.class)
                .hasMessageContaining("Refresh token not found or expired");
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user not found during refresh")
    void shouldThrowExceptionWhenUserNotFoundDuringRefresh() {
        // Given
        String refreshToken = "valid-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        
        when(jwtTokenProvider.validateRefreshToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromRefreshToken(refreshToken)).thenReturn(testUserId);
        when(redisTemplate.opsForValue().get(anyString())).thenReturn(refreshToken);
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Should logout successfully")
    void shouldLogoutSuccessfully() {
        // Given
        String refreshToken = "refresh-token";
        when(jwtTokenProvider.validateRefreshToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromRefreshToken(refreshToken)).thenReturn(testUserId);

        // When
        authService.logout(refreshToken);

        // Then
        verify(jwtTokenProvider).validateRefreshToken(refreshToken);
        verify(jwtTokenProvider).getUserIdFromRefreshToken(refreshToken);
        verify(redisTemplate).delete(anyString());
    }

    @Test
    @DisplayName("Should handle logout with invalid token gracefully")
    void shouldHandleLogoutWithInvalidTokenGracefully() {
        // Given
        String refreshToken = "invalid-refresh-token";
        when(jwtTokenProvider.validateRefreshToken(refreshToken)).thenReturn(false);

        // When
        authService.logout(refreshToken);

        // Then
        verify(jwtTokenProvider).validateRefreshToken(refreshToken);
        verify(jwtTokenProvider, never()).getUserIdFromRefreshToken(anyString());
        verify(redisTemplate, never()).delete(anyString());
    }
}

