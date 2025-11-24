package com.mockly.core.mapper;

import com.mockly.core.dto.session.ArtifactResponse;
import com.mockly.core.dto.session.SessionParticipantResponse;
import com.mockly.core.dto.session.SessionResponse;
import com.mockly.data.entity.Artifact;
import com.mockly.data.entity.Session;
import com.mockly.data.entity.SessionParticipant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for converting between Session entities and DTOs.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SessionMapper {

    /**
     * Convert Session entity to SessionResponse DTO.
     * Maps nested participants and artifacts.
     */
    @Mapping(target = "creatorDisplayName", expression = "java(session.getCreator() != null && session.getCreator().getProfile() != null ? session.getCreator().getProfile().getDisplayName() : null)")
    @Mapping(target = "participants", source = "participants")
    @Mapping(target = "artifacts", source = "artifacts")
    SessionResponse toResponse(Session session);

    /**
     * Convert SessionParticipant entity to SessionParticipantResponse DTO.
     */
    @Mapping(target = "userDisplayName", expression = "java(participant.getUser() != null && participant.getUser().getProfile() != null ? participant.getUser().getProfile().getDisplayName() : null)")
    @Mapping(target = "userEmail", expression = "java(participant.getUser() != null ? participant.getUser().getEmail() : null)")
    SessionParticipantResponse toResponse(SessionParticipant participant);

    /**
     * Convert Artifact entity to ArtifactResponse DTO.
     */
    ArtifactResponse toResponse(Artifact artifact);

    /**
     * Convert list of Session entities to list of SessionResponse DTOs.
     */
    List<SessionResponse> toResponseList(List<Session> sessions);

    /**
     * Convert list of SessionParticipant entities to list of SessionParticipantResponse DTOs.
     */
    List<SessionParticipantResponse> toParticipantResponseList(List<SessionParticipant> participants);

    /**
     * Convert list of Artifact entities to list of ArtifactResponse DTOs.
     */
    List<ArtifactResponse> toArtifactResponseList(List<Artifact> artifacts);
}

