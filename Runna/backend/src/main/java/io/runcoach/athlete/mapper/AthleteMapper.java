package io.runcoach.athlete.mapper;

import io.runcoach.athlete.dto.AthleteResponse;
import io.runcoach.athlete.dto.AthleteSnapshot;
import io.runcoach.athlete.dto.CreateAthleteRequest;
import io.runcoach.athlete.dto.UpdateAthleteRequest;
import io.runcoach.athlete.entity.AthleteEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AthleteMapper {
    public AthleteResponse toResponse(AthleteEntity entity) {
        return new AthleteResponse(entity.id, entity.displayName, entity.timeZone);
    }

    public AthleteSnapshot toSnapshot(AthleteEntity entity) {
        return new AthleteSnapshot(toResponse(entity), entity.version);
    }

    public AthleteEntity toEntity(String subject, CreateAthleteRequest request) {
        AthleteEntity entity = new AthleteEntity();
        entity.authSubject = subject;
        entity.email = request.email();
        entity.displayName = request.displayName().strip();
        entity.timeZone = request.timeZone();
        return entity;
    }

    public void updateEntity(AthleteEntity entity, UpdateAthleteRequest request) {
        entity.displayName = request.displayName().strip();
        entity.timeZone = request.timeZone();
    }
}