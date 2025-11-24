package com.mockly.data.repository;

import com.mockly.data.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    /**
     * Find report by session ID.
     */
    Optional<Report> findBySessionId(UUID sessionId);

    /**
     * Check if report exists for a session.
     */
    boolean existsBySessionId(UUID sessionId);
}

