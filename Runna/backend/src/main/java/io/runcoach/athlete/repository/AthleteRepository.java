package io.runcoach.athlete.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.runcoach.athlete.entity.AthleteEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AthleteRepository implements PanacheRepositoryBase<AthleteEntity, UUID> {
    public Optional<AthleteEntity> findByAuthSubject(String subject) {
        return find("authSubject", subject).firstResultOptional();
    }

    public Optional<AthleteEntity> lockByAuthSubject(String subject) {
        return find("authSubject", subject).withLock(LockModeType.PESSIMISTIC_WRITE).firstResultOptional();
    }
}