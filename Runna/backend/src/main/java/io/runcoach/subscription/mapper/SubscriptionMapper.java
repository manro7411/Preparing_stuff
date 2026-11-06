package io.runcoach.subscription.mapper;

import io.runcoach.subscription.dto.SubscriptionResponse;
import io.runcoach.subscription.entity.PlanCode;
import io.runcoach.subscription.entity.SubscriptionStatus;
import io.runcoach.subscription.entity.UserSubscription;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SubscriptionMapper {
    public SubscriptionResponse toResponse(UserSubscription entity, PlanCode plan, PlanCode pending) {
        boolean eligible = entity.trialStartedAt == null && entity.status != SubscriptionStatus.TRIAL
                && (entity.status != SubscriptionStatus.ACTIVE || plan == PlanCode.FREE);
        return new SubscriptionResponse(entity.id, plan, entity.status, entity.billingCycle, entity.currency,
                entity.periodStart, entity.periodEnd, entity.trialStartedAt, entity.trialEndsAt,
                eligible, entity.cancelAtPeriodEnd, pending, entity.version);
    }
}