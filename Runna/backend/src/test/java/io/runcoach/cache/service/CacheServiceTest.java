package io.runcoach.cache.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CacheServiceTest {
    @Test
    @DisplayName("Disabled caching always misses and accepts writes")
    void shouldReturnMissWhenCacheDisabled() {
        CacheService cache = new NoOpCacheService();

        assertDoesNotThrow(() -> cache.put("key", "value", Duration.ofMinutes(1)));
        assertDoesNotThrow(() -> cache.evict("key"));
        assertTrue(cache.get("key").isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Redis outage does not prevent reads, writes or eviction")
    void shouldContinueWhenRedisUnavailable() {
        RedisDataSource dataSource = mock(RedisDataSource.class);
        ValueCommands<String, String> values = mock(ValueCommands.class);
        org.mockito.Mockito.doReturn(values).when(dataSource).value(String.class);
        when(values.get("key")).thenThrow(new IllegalStateException("Unavailable"));
        org.mockito.Mockito.doThrow(new IllegalStateException("Unavailable"))
                .when(values).set(org.mockito.ArgumentMatchers.eq("key"),
                        org.mockito.ArgumentMatchers.eq("value"), org.mockito.ArgumentMatchers.any());
        when(dataSource.key()).thenThrow(new IllegalStateException("Unavailable"));
        CacheService cache = new RedisCacheService(dataSource);

        assertTrue(cache.get("key").isEmpty());
        assertDoesNotThrow(() -> cache.put("key", "value", Duration.ofMinutes(1)));
        assertDoesNotThrow(() -> cache.evict("key"));
    }

    @Test
    @DisplayName("Cache keys include version, scope and athlete ownership")
    void shouldBuildAthleteScopedKey() {
        UUID athleteId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        String key = CacheKeys.forAthlete(CacheKeys.Scope.TRAINING_PLAN, athleteId);

        assertEquals("runcoach:v1:training-plan:athlete:" + athleteId, key);
    }
}