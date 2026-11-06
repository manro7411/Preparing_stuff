package io.runcoach.athlete.resource;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class AthleteHttpTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.hibernate-orm.enabled", "false",
                "quarkus.flyway.enabled", "false",
                "quarkus.datasource.jdbc", "false",
                "quarkus.http.test-port", "0");
    }
}