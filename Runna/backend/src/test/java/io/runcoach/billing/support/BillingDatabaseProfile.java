package io.runcoach.billing.support;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;
import java.util.Set;

public class BillingDatabaseProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.hibernate-orm.enabled", "true", "quarkus.datasource.jdbc", "true",
                "quarkus.flyway.enabled", "true", "quarkus.http.test-port", "0",
                "runcoach.billing.policy", "PERIOD_END_NO_PRORATION",
                "runcoach.billing.prices.pro.monthly.usd", "1000",
                "runcoach.billing.prices.pro.yearly.usd", "10000",
                "runcoach.billing.prices.elite.monthly.usd", "2000",
                "runcoach.billing.prices.elite.yearly.usd", "20000");
    }

    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(BillingTestGateway.class, BillingTestClock.class);
    }
}