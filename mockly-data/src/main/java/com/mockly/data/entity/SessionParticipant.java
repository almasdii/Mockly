package com.mockly.data.entity;

import com.mockly.data.enums.ParticipantRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "session_participants", 
    indexes = {
        @Index(name = "idx_session_participants_session", columnList = "session_id"),
        @Index(name = "idx_session_participants_user", columnList = "user_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_session_participants_session_user", columnNames = {"session_id", "user_id"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", insertable = false, updatable = false)
    private Session session;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_in_session", nullable = false, length = 20)
    private ParticipantRole roleInSession;

    @Column(name = "joined_at")
    private OffsetDateTime joinedAt;

    @Column(name = "left_at")
    private OffsetDateTime leftAt;
}

