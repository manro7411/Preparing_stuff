package io.runcoach.billing.support;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.net.URI;
import java.util.Map;
import org.testcontainers.containers.PostgreSQLContainer;

public class BillingPostgresResource implements QuarkusTestResourceLifecycleManager {
    private PostgreSQLContainer<?> database;

    @Override
    public Map<String, String> start() {
        String external = System.getenv("RUNCOACH_TEST_DB_URL");
        if (external != null && !external.isBlank()) {
            if (!URI.create(external.substring("jdbc:".length())).getPath().endsWith("_test")) {
                throw new IllegalArgumentException("Billing integration tests require a dedicated database ending in _test");
            }
            return properties(external, System.getenv("RUNCOACH_TEST_DB_USER"), System.getenv("RUNCOACH_TEST_DB_PASSWORD"));
        }
        database = new PostgreSQLContainer<>("postgres:17-alpine");
        database.withDatabaseName("runcoach_test");
        database.start();
        return properties(database.getJdbcUrl(), database.getUsername(), database.getPassword());
    }

    private Map<String, String> properties(String url, String username, String password) {
        return Map.of("quarkus.datasource.jdbc.url", url, "quarkus.datasource.username", username,
                "quarkus.datasource.password", password);
    }

    @Override
    public void stop() {
        if (database != null) {
            database.close();
        }
    }
}