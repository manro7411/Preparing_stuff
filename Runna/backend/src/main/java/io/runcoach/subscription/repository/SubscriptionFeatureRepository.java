package io.runcoach.subscription.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.runcoach.subscription.entity.Feature;
import io.runcoach.subscription.entity.SubscriptionFeature;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class SubscriptionFeatureRepository implements PanacheRepositoryBase<SubscriptionFeature, UUID> {
    public Set<Feature> features(UUID planId) {
        return find("planId", planId).list().stream().map(entity -> entity.feature).collect(Collectors.toUnmodifiableSet());
    }
}