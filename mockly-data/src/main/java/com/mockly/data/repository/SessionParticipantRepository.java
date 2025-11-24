package com.mockly.data.repository;

import com.mockly.data.entity.SessionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionParticipantRepository extends JpaRepository<SessionParticipant, UUID> {

    /**
     * Find a participant by session ID and user ID.
     */
    Optional<SessionParticipant> findBySessionIdAndUserId(UUID sessionId, UUID userId);

    /**
     * Find all participants in a session.
     */
    List<SessionParticipant> findBySessionId(UUID sessionId);

    /**
     * Find all sessions where a user is a participant.
     */
    List<SessionParticipant> findByUserId(UUID userId);

    /**
     * Check if a user is already a participant in a session.
     */
    boolean existsBySessionIdAndUserId(UUID sessionId, UUID userId);

    /**
     * Count participants in a session.
     */
    long countBySessionId(UUID sessionId);

    /**
     * Find all active participants (not left) in a session.
     */
    List<SessionParticipant> findBySessionIdAndLeftAtIsNull(UUID sessionId);
}

