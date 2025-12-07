package com.mockly.core.service;

import com.mockly.core.dto.session.CreateSessionRequest;
import com.mockly.core.dto.session.SessionListResponse;
import com.mockly.core.dto.session.SessionResponse;
import com.mockly.core.exception.BadRequestException;
import com.mockly.core.exception.ResourceNotFoundException;
import com.mockly.core.mapper.SessionMapper;
import com.mockly.data.entity.Session;
import com.mockly.data.entity.SessionParticipant;
import com.mockly.data.enums.ParticipantRole;
import com.mockly.data.enums.SessionStatus;
import com.mockly.data.repository.ProfileRepository;
import com.mockly.data.repository.SessionParticipantRepository;
import com.mockly.data.repository.SessionRepository;
import com.mockly.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing interview sessions.
 * Handles session lifecycle, participant management, and validation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final SessionRepository sessionRepository;
    private final SessionParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final SessionMapper sessionMapper;
    private final LiveKitService liveKitService;

    /**
     * Create a new interview session.
     * Validates that the user doesn't have an active session.
     *
     * @param userId ID of the user creating the session (candidate)
     * @param request Create session request
     * @return Created session response
     */
    @Transactional
    public SessionResponse createSession(UUID userId, CreateSessionRequest request) {
        log.info("Creating session for user: {} with interviewer: {}", userId, request.interviewerId());

        // Validate users exist
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }

        if (!userRepository.existsById(request.interviewerId())) {
            throw new ResourceNotFoundException("Interviewer not found: " + request.interviewerId());
        }

        Optional<Session> activeSession = sessionRepository
                .findFirstByCreatedByAndStatusInOrderByCreatedAtDesc(
                        userId,
                        List.of(SessionStatus.SCHEDULED, SessionStatus.ACTIVE)
                );

        if (activeSession.isPresent()) {
            throw new BadRequestException(
                    "User already has an active session. Please end the current session before creating a new one."
            );
        }


        Session session = Session.builder()
                .createdBy(userId)
                .status(SessionStatus.SCHEDULED)
                .startsAt(request.scheduledAt())
                .roomProvider("livekit")
                .build();

        session = sessionRepository.save(session);

        String roomId = liveKitService.createRoom(session.getId());
        session.setRoomId(roomId);
        session = sessionRepository.save(session);

        SessionParticipant candidateParticipant = SessionParticipant.builder()
                .sessionId(session.getId())
                .userId(userId)
                .roleInSession(ParticipantRole.CANDIDATE)
                .build();

        participantRepository.save(candidateParticipant);

        SessionParticipant interviewerParticipant = SessionParticipant.builder()
                .sessionId(session.getId())
                .userId(request.interviewerId())
                .roleInSession(ParticipantRole.INTERVIEWER)
                .build();

        participantRepository.save(interviewerParticipant);

        log.info("Session created successfully: {}", session.getId());

        session = sessionRepository.findById(session.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found after creation"));

        return sessionMapper.toResponse(session);
    }

    /**
     * Join an existing session.
     * Updates session status to ACTIVE if it was SCHEDULED.
     *
     * @param sessionId Session ID
     * @param userId User ID joining the session
     * @return Updated session response
     */
    @Transactional
    public SessionResponse joinSession(UUID sessionId, UUID userId) {
        log.info("User {} joining session {}", userId, sessionId);

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));


        if (session.getStatus() == SessionStatus.ENDED || session.getStatus() == SessionStatus.CANCELED) {
            throw new BadRequestException("Cannot join a session that has ended or been canceled");
        }


        Optional<SessionParticipant> existingParticipant = participantRepository
                .findBySessionIdAndUserId(sessionId, userId);

        if (existingParticipant.isPresent()) {
            SessionParticipant participant = existingParticipant.get();
            if (participant.getLeftAt() != null) {
                // Re-joining: update leftAt to null
                participant.setLeftAt(null);
                participant.setJoinedAt(OffsetDateTime.now());
                participantRepository.save(participant);
            }

        } else {

            log.warn("User {} joining session {} but not in participant list", userId, sessionId);
            throw new BadRequestException("User is not authorized to join this session");
        }


        if (session.getStatus() == SessionStatus.SCHEDULED) {
            session.setStatus(SessionStatus.ACTIVE);
            session.setStartsAt(OffsetDateTime.now());
            sessionRepository.save(session);
        }


        SessionParticipant participant = existingParticipant.get();
        if (participant.getJoinedAt() == null) {
            participant.setJoinedAt(OffsetDateTime.now());
            participantRepository.save(participant);
        }

        log.info("User {} successfully joined session {}", userId, sessionId);


        session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        return sessionMapper.toResponse(session);
    }

    /**
     * Leave a session.
     * Updates participant's left_at timestamp.
     *
     * @param sessionId Session ID
     * @param userId User ID leaving the session
     */
    @Transactional
    public void leaveSession(UUID sessionId, UUID userId) {
        log.info("User {} leaving session {}", userId, sessionId);

        SessionParticipant participant = participantRepository
                .findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found in session"));

        if (participant.getLeftAt() == null) {
            participant.setLeftAt(OffsetDateTime.now());
            participantRepository.save(participant);
            log.info("User {} left session {}", userId, sessionId);
        } else {
            log.info("User {} already left session {}", userId, sessionId);
        }
    }

    /**
     * End a session.
     * Only the session creator or a participant can end the session.
     *
     * @param sessionId Session ID
     * @param userId User ID ending the session
     */
    @Transactional
    public void endSession(UUID sessionId, UUID userId) {
        log.info("User {} ending session {}", userId, sessionId);

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        // Validate user can end session (creator or participant)
        boolean isCreator = session.getCreatedBy().equals(userId);
        boolean isParticipant = participantRepository.existsBySessionIdAndUserId(sessionId, userId);

        if (!isCreator && !isParticipant) {
            throw new BadRequestException("Only session creator or participants can end the session");
        }

        if (session.getStatus() == SessionStatus.ENDED) {
            log.info("Session {} already ended", sessionId);
            return;
        }

        session.setStatus(SessionStatus.ENDED);
        session.setEndsAt(OffsetDateTime.now());
        sessionRepository.save(session);

        // Mark all participants as left
        List<SessionParticipant> participants = participantRepository.findBySessionId(sessionId);
        for (SessionParticipant participant : participants) {
            if (participant.getLeftAt() == null) {
                participant.setLeftAt(OffsetDateTime.now());
                participantRepository.save(participant);
            }
        }

        log.info("Session {} ended successfully", sessionId);
    }

    /**
     * Get session by ID.
     * User must be the creator or a participant.
     *
     * @param sessionId Session ID
     * @param userId User ID requesting the session
     * @return Session response
     */
    @Transactional(readOnly = true)
    public SessionResponse getSession(UUID sessionId, UUID userId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        // Validate user has access (creator or participant)
        boolean isCreator = session.getCreatedBy().equals(userId);
        boolean isParticipant = participantRepository.existsBySessionIdAndUserId(sessionId, userId);

        if (!isCreator && !isParticipant) {
            throw new BadRequestException("You don't have access to this session");
        }

        return sessionMapper.toResponse(session);
    }

    /**
     * List sessions for a user.
     * Returns sessions where user is creator or participant.
     *
     * @param userId User ID
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param status Optional status filter
     * @return Paginated session list
     */
    @Transactional(readOnly = true)
    public SessionListResponse listSessions(UUID userId, int page, int size, SessionStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Session> sessionsPage;
        if (status != null) {
            // Filter by status - get sessions where user is creator or participant
            List<Session> allCreatedSessions = sessionRepository
                    .findByCreatedByOrderByCreatedAtDesc(userId);
            List<Session> createdSessions = allCreatedSessions.stream()
                    .filter(s -> s.getStatus() == status)
                    .toList();
            List<Session> participantSessions = sessionRepository
                    .findByParticipantUserIdAndStatus(userId, status);

            // Combine and paginate manually (simplified - in production, use proper query)
            List<Session> allSessions = createdSessions;
            for (Session session : participantSessions) {
                if (!allSessions.contains(session)) {
                    allSessions.add(session);
                }
            }
            allSessions.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

            int start = page * size;
            int end = Math.min(start + size, allSessions.size());
            List<Session> pagedSessions = start < allSessions.size()
                    ? allSessions.subList(start, end)
                    : List.of();

            sessionsPage = new org.springframework.data.domain.PageImpl<>(
                    pagedSessions,
                    pageable,
                    allSessions.size()
            );
        } else {
            // Get all sessions where user is creator or participant
            List<Session> createdSessions = sessionRepository
                    .findByCreatedByOrderByCreatedAtDesc(userId);
            List<Session> participantSessions = sessionRepository
                    .findByParticipantUserId(userId);

            // Combine and paginate
            List<Session> allSessions = createdSessions;
            for (Session session : participantSessions) {
                if (!allSessions.contains(session)) {
                    allSessions.add(session);
                }
            }
            allSessions.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

            int start = page * size;
            int end = Math.min(start + size, allSessions.size());
            List<Session> pagedSessions = start < allSessions.size()
                    ? allSessions.subList(start, end)
                    : List.of();

            sessionsPage = new org.springframework.data.domain.PageImpl<>(
                    pagedSessions,
                    pageable,
                    allSessions.size()
            );
        }

        List<SessionResponse> sessionResponses = sessionMapper.toResponseList(sessionsPage.getContent());

        return new SessionListResponse(
                sessionResponses,
                sessionsPage.getTotalElements(),
                sessionsPage.getNumber(),
                sessionsPage.getSize()
        );
    }

    /**
     * Get active session for a user.
     *
     * @param userId User ID
     * @return Optional session response (empty if no active session)
     */
    @Transactional(readOnly = true)
    public Optional<SessionResponse> getActiveSession(UUID userId) {
        Optional<Session> activeSession = sessionRepository
                .findFirstByCreatedByAndStatusInOrderByCreatedAtDesc(
                        userId,
                        List.of(SessionStatus.SCHEDULED, SessionStatus.ACTIVE)
                );

        return activeSession.map(sessionMapper::toResponse);
    }

    /**
     * Get user display name from profile.
     * Combines name and surname, falls back to email if not set.
     *
     * @param userId User ID
     * @return Display name (name + surname) or email
     */
    @Transactional(readOnly = true)
    public String getUserDisplayName(UUID userId) {
        return profileRepository.findByUserId(userId)
                .map(profile -> {
                    String name = profile.getName();
                    String surname = profile.getSurname();
                    
                    // Combine name and surname if both are set
                    if (name != null && !name.isBlank() && surname != null && !surname.isBlank()) {
                        return name + " " + surname;
                    }
                    // Use only name if surname is not set
                    if (name != null && !name.isBlank()) {
                        return name;
                    }
                    // Use only surname if name is not set
                    if (surname != null && !surname.isBlank()) {
                        return surname;
                    }
                    // Fallback to email if neither name nor surname is set
                    return userRepository.findById(userId)
                            .map(user -> user.getEmail())
                            .orElse("User");
                })
                .orElseGet(() -> {
                    // If profile doesn't exist, use email
                    return userRepository.findById(userId)
                            .map(user -> user.getEmail())
                            .orElse("User");
                });
    }
}

