package io.runcoach.subscription.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.runcoach.subscription.entity.UserSubscription;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserSubscriptionRepository implements PanacheRepositoryBase<UserSubscription, UUID> {
    public Optional<UserSubscription> findByUser(UUID userId) {
        return find("userId", userId).firstResultOptional();
    }
}