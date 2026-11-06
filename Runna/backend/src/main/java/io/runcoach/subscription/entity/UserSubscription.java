package io.runcoach.subscription.entity;

import io.runcoach.common.dto.CurrencyCode;
import io.runcoach.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_subscription")
public class UserSubscription extends BaseEntity {
    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    public UUID userId;

    @Column(name = "plan_id", nullable = false)
    public UUID planId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public SubscriptionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", length = 16)
    public BillingCycle billingCycle;

    @Enumerated(EnumType.STRING)
    @Column(length = 3)
    public CurrencyCode currency;

    @Column(name = "trial_started_at")
    public Instant trialStartedAt;

    @Column(name = "trial_ends_at")
    public Instant trialEndsAt;

    @Column(name = "period_start")
    public Instant periodStart;

    @Column(name = "period_end")
    public Instant periodEnd;

    @Column(name = "cancel_at_period_end", nullable = false)
    public boolean cancelAtPeriodEnd;

    @Column(name = "pending_plan_id")
    public UUID pendingPlanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "pending_billing_cycle", length = 16)
    public BillingCycle pendingBillingCycle;

    @Enumerated(EnumType.STRING)
    @Column(name = "pending_currency", length = 3)
    public CurrencyCode pendingCurrency;

    @Column(name = "pending_period_start")
    public Instant pendingPeriodStart;

    @Column(name = "pending_period_end")
    public Instant pendingPeriodEnd;
}