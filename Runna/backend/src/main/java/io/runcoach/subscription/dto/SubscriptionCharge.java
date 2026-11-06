package io.runcoach.subscription.dto;

import io.runcoach.common.dto.CurrencyCode;
import io.runcoach.payment.entity.PaymentPurpose;
import io.runcoach.subscription.entity.BillingCycle;
import io.runcoach.subscription.entity.PlanCode;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionCharge(UUID accountId, UUID subscriptionId, long expectedVersion, UUID targetPlanId,
        PlanCode targetPlan, BillingCycle billingCycle, CurrencyCode currency, PaymentPurpose purpose, Instant verifiedAt) {
}