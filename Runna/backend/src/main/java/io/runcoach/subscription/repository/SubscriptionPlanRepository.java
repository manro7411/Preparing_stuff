package io.runcoach.subscription.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.runcoach.common.exception.BillingException;
import io.runcoach.subscription.entity.PlanCode;
import io.runcoach.subscription.entity.SubscriptionPlan;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class SubscriptionPlanRepository implements PanacheRepositoryBase<SubscriptionPlan, UUID> {
    public SubscriptionPlan require(PlanCode code) {
        return find("code", code).firstResultOptional()
                .orElseThrow(() -> new BillingException(503, "CATALOG_UNAVAILABLE", "The subscription catalog is unavailable."));
    }
}