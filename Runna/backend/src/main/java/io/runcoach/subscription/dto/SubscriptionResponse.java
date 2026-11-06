package io.runcoach.subscription.dto;

import io.runcoach.common.dto.CurrencyCode;
import io.runcoach.subscription.entity.BillingCycle;
import io.runcoach.subscription.entity.PlanCode;
import io.runcoach.subscription.entity.SubscriptionStatus;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(UUID id, PlanCode plan, SubscriptionStatus status, BillingCycle billingCycle,
        CurrencyCode currency, Instant periodStart, Instant periodEnd, Instant trialStartedAt, Instant trialEndsAt,
        boolean trialEligible, boolean cancelAtPeriodEnd, PlanCode pendingPlan, long version) {
}