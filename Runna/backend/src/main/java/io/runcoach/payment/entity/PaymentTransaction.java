package io.runcoach.payment.entity;

import io.runcoach.common.dto.CurrencyCode;
import io.runcoach.common.entity.BaseEntity;
import io.runcoach.subscription.entity.BillingCycle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_transaction")
public class PaymentTransaction extends BaseEntity {
    @Column(name = "user_id", nullable = false, updatable = false)
    public UUID userId;
    @Column(name = "subscription_id", nullable = false, updatable = false)
    public UUID subscriptionId;
    @Column(name = "target_plan_id", nullable = false, updatable = false)
    public UUID targetPlanId;
    @Column(name = "idempotency_key", nullable = false, updatable = false)
    public UUID idempotencyKey;
    @Column(name = "subscription_version", nullable = false, updatable = false)
    public long subscriptionVersion;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, updatable = false)
    public PaymentPurpose purpose;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, updatable = false)
    public PaymentProvider provider;
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 16, updatable = false)
    public BillingCycle billingCycle;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3, updatable = false)
    public CurrencyCode currency;
    @Column(name = "amount_minor", nullable = false, updatable = false)
    public long amountMinor;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public PaymentStatus status;
    @Column(name = "provider_payment_id", length = 255)
    public String providerPaymentId;
    @Column(name = "checkout_url", length = 2048)
    public String checkoutUrl;
    @Column(name = "paid_at")
    public Instant paidAt;
    @Column(name = "subscription_applied", nullable = false)
    public boolean subscriptionApplied;
    @Column(name = "reconciliation_required", nullable = false)
    public boolean reconciliationRequired;
}