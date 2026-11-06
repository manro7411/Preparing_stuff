package io.runcoach.cache.service;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.SetArgs;
import io.quarkus.redis.datasource.value.ValueCommands;
import java.time.Duration;
import java.util.Optional;
import org.jboss.logging.Logger;

public class RedisCacheService implements CacheService {
    private static final Logger LOG = Logger.getLogger(RedisCacheService.class);
    private final RedisDataSource dataSource;
    private final ValueCommands<String, String> values;

    public RedisCacheService(RedisDataSource dataSource) {
        this.dataSource = dataSource;
        this.values = dataSource.value(String.class);
    }

    @Override
    public Optional<String> get(String key) {
        try {
            return Optional.ofNullable(values.get(key));
        } catch (RuntimeException exception) {
            LOG.debug("Cache read unavailable; use the source of truth");
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Cache TTL must be positive");
        }
        try {
            values.set(key, value, new SetArgs().px(Math.max(1L, ttl.toMillis())));
        } catch (RuntimeException exception) {
            LOG.debug("Cache write unavailable; continue without caching");
        }
    }

    @Override
    public void evict(String key) {
        try {
            dataSource.key().del(key);
        } catch (RuntimeException exception) {
            LOG.debug("Cache eviction unavailable; cached entries expire by TTL");
        }
    }
}