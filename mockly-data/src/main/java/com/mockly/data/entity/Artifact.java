package com.mockly.data.entity;

import com.mockly.data.enums.ArtifactType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "artifacts", indexes = {
    @Index(name = "idx_artifacts_session", columnList = "session_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Artifact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", insertable = false, updatable = false)
    private Session session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ArtifactType type;

    @Column(name = "storage_url", nullable = false, columnDefinition = "TEXT")
    private String storageUrl;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}

