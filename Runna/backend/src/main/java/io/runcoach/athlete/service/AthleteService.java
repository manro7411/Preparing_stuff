package io.runcoach.athlete.service;

import io.runcoach.athlete.dto.AthleteResponse;
import io.runcoach.athlete.dto.AthleteSnapshot;
import io.runcoach.athlete.dto.CreateAthleteRequest;
import io.runcoach.athlete.dto.UpdateAthleteRequest;
import io.runcoach.athlete.entity.AthleteEntity;
import io.runcoach.athlete.mapper.AthleteMapper;
import io.runcoach.athlete.repository.AthleteRepository;
import io.runcoach.athlete.validator.AthleteValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.hibernate.exception.ConstraintViolationException;

@ApplicationScoped
public class AthleteService {
    private final AthleteRepository repository;
    private final AthleteMapper mapper;
    private final AthleteValidator validator;

    @Inject
    public AthleteService(AthleteRepository repository, AthleteMapper mapper, AthleteValidator validator) {
        this.repository = repository;
        this.mapper = mapper;
        this.validator = validator;
    }

    @Transactional
    public AthleteResponse findCurrent(String subject) {
        return mapper.toResponse(requireCurrent(subject));
    }

    @Transactional
    public AthleteSnapshot findCurrentSnapshot(String subject) {
        return mapper.toSnapshot(requireCurrent(subject));
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public AthleteResponse lockForBilling(String subject) {
        validator.requireSubject(subject);
        return repository.lockByAuthSubject(subject).map(mapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Athlete profile not provisioned"));
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void lockAccountForBilling(java.util.UUID accountId) {
        if (repository.findById(accountId, LockModeType.PESSIMISTIC_WRITE) == null) {
            throw new NotFoundException("Athlete profile not provisioned");
        }
    }

    @Transactional
    public AthleteSnapshot createCurrent(String subject, @NotNull @Valid CreateAthleteRequest request) {
        validator.requireSubject(subject);
        if (repository.findByAuthSubject(subject).isPresent()) {
            throw new WebApplicationException("Athlete profile already exists", 409);
        }
        AthleteEntity entity = mapper.toEntity(subject, request);
        try {
            repository.persistAndFlush(entity);
        } catch (PersistenceException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException constraint
                        && "23505".equals(constraint.getSQLState())
                        && "users_auth_subject_key".equals(constraint.getConstraintName())) {
                    throw new WebApplicationException("Athlete profile already exists", 409);
                }
            }
            throw exception;
        }
        return mapper.toSnapshot(entity);
    }

    @Transactional
    public AthleteSnapshot updateCurrent(String subject, @NotNull @Valid UpdateAthleteRequest request,
            @PositiveOrZero long expectedVersion) {
        AthleteEntity entity = requireCurrent(subject);
        if (entity.version != expectedVersion) {
            throw new WebApplicationException("Profile has changed; retrieve it again before updating", 412);
        }
        mapper.updateEntity(entity, request);
        try {
            repository.flush();
        } catch (OptimisticLockException exception) {
            throw new WebApplicationException("Profile has changed; retrieve it again before updating", 412);
        }
        return mapper.toSnapshot(entity);
    }

    private AthleteEntity requireCurrent(String subject) {
        validator.requireSubject(subject);
        return repository.findByAuthSubject(subject)
                .orElseThrow(() -> new NotFoundException("Athlete profile not provisioned"));
    }
}