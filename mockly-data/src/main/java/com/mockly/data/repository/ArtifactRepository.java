package com.mockly.data.repository;

import com.mockly.data.entity.Artifact;
import com.mockly.data.enums.ArtifactType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArtifactRepository extends JpaRepository<Artifact, UUID> {

    /**
     * Find all artifacts for a session.
     */
    List<Artifact> findBySessionId(UUID sessionId);

    /**
     * Find all artifacts of a specific type for a session.
     */
    List<Artifact> findBySessionIdAndType(UUID sessionId, ArtifactType type);

    /**
     * Find a specific artifact by session ID and type.
     * Returns the first match if multiple exist.
     */
    Optional<Artifact> findFirstBySessionIdAndType(UUID sessionId, ArtifactType type);

    /**
     * Count artifacts for a session.
     */
    long countBySessionId(UUID sessionId);

    /**
     * Check if an artifact of a specific type exists for a session.
     */
    boolean existsBySessionIdAndType(UUID sessionId, ArtifactType type);
}

