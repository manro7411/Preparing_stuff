package io.runcoach.athlete.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.runcoach.athlete.dto.CreateAthleteRequest;
import io.runcoach.athlete.dto.UpdateAthleteRequest;
import io.runcoach.athlete.entity.AthleteEntity;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AthleteMapperTest {
    private final AthleteMapper mapper = new AthleteMapper();

    @Test
    @DisplayName("Creation maps the server-provided subject and leaves generated fields to persistence")
    void shouldMapCreateRequestWithoutGeneratedFields() {
        var entity = mapper.toEntity("jwt-subject", new CreateAthleteRequest("runner@example.com", " Runner ", "UTC"));

        assertEquals("jwt-subject", entity.authSubject);
        assertEquals("runner@example.com", entity.email);
        assertEquals("Runner", entity.displayName);
        assertEquals("UTC", entity.timeZone);
        assertNull(entity.id);
        assertNull(entity.createdAt);
        assertEquals(0, entity.version);
    }

    @Test
    @DisplayName("Updating editable fields cannot overwrite ownership, identity, version or audit fields")
    void shouldPreserveProtectedFieldsWhenUpdating() {
        AthleteEntity entity = mapper.toEntity("jwt-subject", new CreateAthleteRequest("runner@example.com", "Runner", "UTC"));
        entity.id = UUID.randomUUID();
        entity.version = 3;
        entity.createdAt = Instant.parse("2026-01-01T00:00:00Z");
        entity.updatedAt = entity.createdAt;
        UUID id = entity.id;

        mapper.updateEntity(entity, new UpdateAthleteRequest(" New Name ", "Asia/Bangkok"));

        assertEquals(id, entity.id);
        assertEquals("jwt-subject", entity.authSubject);
        assertEquals("runner@example.com", entity.email);
        assertEquals(3, entity.version);
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), entity.createdAt);
        assertEquals(entity.createdAt, entity.updatedAt);
        assertEquals("New Name", entity.displayName);
        assertEquals("Asia/Bangkok", entity.timeZone);
    }

    @Test
    @DisplayName("Snapshot carries a separate persistence version while preserving the original profile response")
    void shouldMapVersionedSnapshot() {
        AthleteEntity entity = mapper.toEntity("jwt-subject", new CreateAthleteRequest("runner@example.com", "Runner", "UTC"));
        entity.id = UUID.randomUUID();
        entity.version = 4;

        var snapshot = mapper.toSnapshot(entity);

        assertEquals(entity.id, snapshot.profile().id());
        assertEquals("Runner", snapshot.profile().displayName());
        assertEquals("UTC", snapshot.profile().timeZone());
        assertEquals(4, snapshot.version());
    }
}