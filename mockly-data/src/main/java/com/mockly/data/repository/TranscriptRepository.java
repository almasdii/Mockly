package com.mockly.data.repository;

import com.mockly.data.entity.Transcript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TranscriptRepository extends JpaRepository<Transcript, UUID> {

    /**
     * Find all transcripts for a session.
     */
    List<Transcript> findBySessionId(UUID sessionId);

    /**
     * Find transcripts by session ID and source.
     */
    List<Transcript> findBySessionIdAndSource(UUID sessionId, Transcript.TranscriptSource source);
}

