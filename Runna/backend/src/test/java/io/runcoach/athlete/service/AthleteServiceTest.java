package io.runcoach.athlete.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.runcoach.athlete.entity.AthleteEntity;
import io.runcoach.athlete.dto.CreateAthleteRequest;
import io.runcoach.athlete.dto.UpdateAthleteRequest;
import io.runcoach.athlete.mapper.AthleteMapper;
import io.runcoach.athlete.repository.AthleteRepository;
import io.runcoach.athlete.validator.AthleteValidator;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.hibernate.exception.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
class AthleteServiceTest {
    @Mock
    AthleteRepository repository;
    AthleteService service;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        service = new AthleteService(repository, new AthleteMapper(), new AthleteValidator());
    }

    @Test
    @DisplayName("Profile lookup uses the authenticated subject")
    void shouldReturnDataWhenRequestValid() {
        AthleteEntity athlete = new AthleteEntity();
        athlete.id = UUID.randomUUID();
        athlete.displayName = "Runner";
        athlete.timeZone = "UTC";
        when(repository.findByAuthSubject("subject-1")).thenReturn(Optional.of(athlete));

        var response = service.findCurrent("subject-1");

        assertEquals(athlete.id, response.id());
        assertEquals("Runner", response.displayName());
    }

    @Test
    @DisplayName("Missing subject is rejected before repository access")
    void shouldThrowExceptionWhenRequestMissing() {
        var exception = assertThrows(NotAuthorizedException.class, () -> service.findCurrent(" "));
        assertEquals(401, exception.getResponse().getStatus());
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Missing profile does not create an implicit account")
    void shouldThrowExceptionWhenProfileMissing() {
        when(repository.findByAuthSubject("subject-1")).thenReturn(Optional.empty());

        var exception = assertThrows(NotFoundException.class, () -> service.findCurrent("subject-1"));
        assertEquals(404, exception.getResponse().getStatus());
    }

    @Test
    @DisplayName("Versioned lookup preserves the public profile shape")
    void shouldReturnCurrentVersionedProfile() {
        AthleteEntity athlete = athlete();
        athlete.version = 5;
        when(repository.findByAuthSubject("subject-1")).thenReturn(Optional.of(athlete));

        var snapshot = service.findCurrentSnapshot("subject-1");

        assertEquals(athlete.id, snapshot.profile().id());
        assertEquals(5, snapshot.version());
        verify(repository).findByAuthSubject("subject-1");
    }

    @Test
    @DisplayName("Create assigns ownership from the authenticated subject and flushes before returning")
    void shouldCreateAthleteSuccessfully() {
        when(repository.findByAuthSubject("subject-1")).thenReturn(Optional.empty());
        UUID id = UUID.randomUUID();
        doAnswer(invocation -> {
            AthleteEntity entity = invocation.getArgument(0);
            entity.id = id;
            return null;
        }).when(repository).persistAndFlush(any(AthleteEntity.class));

        var snapshot = service.createCurrent("subject-1", createRequest());

        ArgumentCaptor<AthleteEntity> captor = ArgumentCaptor.forClass(AthleteEntity.class);
        verify(repository).persistAndFlush(captor.capture());
        assertEquals("subject-1", captor.getValue().authSubject);
        assertEquals("runner@example.com", captor.getValue().email);
        assertEquals(id, snapshot.profile().id());
        assertEquals("Runner", snapshot.profile().displayName());
        assertEquals(0, snapshot.version());
    }

    @Test
    @DisplayName("An existing identity cannot create a second profile")
    void shouldRejectDuplicateProfile() {
        when(repository.findByAuthSubject("subject-1")).thenReturn(Optional.of(athlete()));

        var exception = assertThrows(WebApplicationException.class, () -> service.createCurrent("subject-1", createRequest()));

        assertEquals(409, exception.getResponse().getStatus());
        verify(repository, never()).persistAndFlush(any(AthleteEntity.class));
    }

    @Test
    @DisplayName("A unique-subject race is translated to conflict rather than an internal error")
    void shouldRejectConcurrentDuplicateCreation() {
        when(repository.findByAuthSubject("subject-1")).thenReturn(Optional.empty());
        var duplicate = new ConstraintViolationException("Duplicate subject", new SQLException("Duplicate", "23505"),
                "users_auth_subject_key");
        doThrow(new PersistenceException(duplicate)).when(repository).persistAndFlush(any(AthleteEntity.class));

        var exception = assertThrows(WebApplicationException.class, () -> service.createCurrent("subject-1", createRequest()));

        assertEquals(409, exception.getResponse().getStatus());
    }

    @Test
    @DisplayName("An unrelated constraint violation is not mislabeled as duplicate identity")
    void shouldPropagateUnrelatedPersistenceFailure() {
        when(repository.findByAuthSubject("subject-1")).thenReturn(Optional.empty());
        var failure = new ConstraintViolationException("Other constraint", new SQLException("Constraint", "23505"),
                "other_constraint");
        doThrow(failure).when(repository).persistAndFlush(any(AthleteEntity.class));

        var exception = assertThrows(PersistenceException.class, () -> service.createCurrent("subject-1", createRequest()));

        assertSame(failure, exception);
    }

    @Test
    @DisplayName("Create requires an authenticated subject before persistence")
    void shouldRejectUnauthenticatedCreation() {
        var exception = assertThrows(WebApplicationException.class, () -> service.createCurrent(null, createRequest()));

        assertEquals(401, exception.getResponse().getStatus());
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Update preserves identity fields and returns the version after flush")
    void shouldUpdateCurrentAthleteSuccessfully() {
        AthleteEntity athlete = athlete();
        athlete.version = 2;
        when(repository.findByAuthSubject("subject-1")).thenReturn(Optional.of(athlete));
        doAnswer(invocation -> {
            athlete.version = 3;
            return null;
        }).when(repository).flush();

        var snapshot = service.updateCurrent("subject-1", new UpdateAthleteRequest("Updated Runner", "Asia/Bangkok"), 2);

        assertEquals("Updated Runner", snapshot.profile().displayName());
        assertEquals("Asia/Bangkok", snapshot.profile().timeZone());
        assertEquals(3, snapshot.version());
        assertEquals("subject-1", athlete.authSubject);
        assertEquals("runner@example.com", athlete.email);
        verify(repository).flush();
        verify(repository, never()).persistAndFlush(any(AthleteEntity.class));
    }

    @Test
    @DisplayName("A stale version cannot mutate or flush a profile")
    void shouldRejectStaleProfileUpdate() {
        AthleteEntity athlete = athlete();
        athlete.version = 2;
        when(repository.findByAuthSubject("subject-1")).thenReturn(Optional.of(athlete));

        var exception = assertThrows(WebApplicationException.class,
                () -> service.updateCurrent("subject-1", new UpdateAthleteRequest("Changed", "UTC"), 1));

        assertEquals(412, exception.getResponse().getStatus());
        assertEquals("Runner", athlete.displayName);
        verify(repository, never()).flush();
    }

    @Test
    @DisplayName("A concurrent write after lookup is rejected by optimistic locking")
    void shouldRejectOptimisticLockConflict() {
        when(repository.findByAuthSubject("subject-1")).thenReturn(Optional.of(athlete()));
        doThrow(new OptimisticLockException("Concurrent update")).when(repository).flush();

        var exception = assertThrows(WebApplicationException.class,
                () -> service.updateCurrent("subject-1", new UpdateAthleteRequest("Changed", "UTC"), 0));

        assertEquals(412, exception.getResponse().getStatus());
    }

    @Test
    @DisplayName("Update never creates a missing profile")
    void shouldRejectUpdateWhenProfileMissing() {
        when(repository.findByAuthSubject("subject-1")).thenReturn(Optional.empty());

        var exception = assertThrows(WebApplicationException.class,
                () -> service.updateCurrent("subject-1", new UpdateAthleteRequest("Changed", "UTC"), 0));

        assertEquals(404, exception.getResponse().getStatus());
        verify(repository, never()).flush();
    }

    @Test
    @DisplayName("Update requires an authenticated subject before repository access")
    void shouldRejectUnauthenticatedUpdate() {
        var exception = assertThrows(WebApplicationException.class,
                () -> service.updateCurrent(" ", new UpdateAthleteRequest("Changed", "UTC"), 0));

        assertEquals(401, exception.getResponse().getStatus());
        verifyNoInteractions(repository);
    }

    private static AthleteEntity athlete() {
        AthleteEntity entity = new AthleteMapper().toEntity("subject-1", createRequest());
        entity.id = UUID.randomUUID();
        return entity;
    }

    private static CreateAthleteRequest createRequest() {
        return new CreateAthleteRequest("runner@example.com", "Runner", "UTC");
    }
}