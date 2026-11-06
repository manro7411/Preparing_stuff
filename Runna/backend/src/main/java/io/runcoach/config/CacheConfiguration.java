package io.runcoach.config;

import io.quarkus.redis.datasource.RedisDataSource;
import io.runcoach.cache.service.CacheService;
import io.runcoach.cache.service.NoOpCacheService;
import io.runcoach.cache.service.RedisCacheService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CacheConfiguration {
    @Produces
    @Singleton
    public CacheService cacheService(RedisDataSource dataSource,
            @ConfigProperty(name = "runcoach.cache.enabled", defaultValue = "false") boolean enabled) {
        return enabled ? new RedisCacheService(dataSource) : new NoOpCacheService();
    }
}