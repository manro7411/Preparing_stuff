package io.runcoach.subscription.service;

import io.runcoach.common.exception.BillingException;
import io.runcoach.payment.dto.PaymentRequest;
import io.runcoach.payment.entity.PaymentPurpose;
import io.runcoach.subscription.dto.SubscriptionCharge;
import io.runcoach.subscription.entity.PlanCode;
import io.runcoach.subscription.entity.SubscriptionStatus;
import io.runcoach.subscription.repository.SubscriptionPlanRepository;
import io.runcoach.subscription.repository.UserSubscriptionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class SubscriptionBillingService {
    @Inject
    SubscriptionService subscriptions;
    @Inject
    SubscriptionPlanRepository plans;
    @Inject
    UserSubscriptionRepository repository;
    @Inject
    BillingService billing;
    @Inject
    Clock clock;

    @Transactional(Transactional.TxType.MANDATORY)
    public SubscriptionCharge prepare(UUID accountId, PaymentRequest request, PaymentPurpose purpose) {
        var current = subscriptions.lockAccount(accountId);
        PlanCode currentPlan = subscriptions.planCode(current);
        boolean paidActive = current.status == SubscriptionStatus.ACTIVE && currentPlan != PlanCode.FREE;
        if (current.cancelAtPeriodEnd || current.pendingPeriodEnd != null) {
            throw new BillingException(409, "SUBSCRIPTION_CHANGE_PENDING", "Resolve the pending cancellation or prepaid change first.");
        }
        if (purpose == PaymentPurpose.SUBSCRIBE && paidActive) {
            throw new BillingException(409, "ALREADY_SUBSCRIBED", "Use upgrade or renew for an active paid subscription.");
        }
        if (purpose == PaymentPurpose.UPGRADE && current.status != SubscriptionStatus.TRIAL
                && (!paidActive || SubscriptionService.rank(request.plan()) <= SubscriptionService.rank(currentPlan))) {
            throw new BillingException(400, "INVALID_UPGRADE", "Choose a higher plan for an active subscription or upgrade a trial.");
        }
        if (purpose == PaymentPurpose.RENEW) {
            PlanCode renewalPlan = current.pendingPlanId == null ? currentPlan : plans.findById(current.pendingPlanId).code;
            if (current.billingCycle == null || renewalPlan == PlanCode.FREE || renewalPlan != request.plan()) {
                throw new BillingException(400, "INVALID_RENEWAL", "Renew the existing paid plan or its scheduled downgrade.");
            }
        }
        return new SubscriptionCharge(accountId, current.id, current.version, plans.require(request.plan()).id,
                request.plan(), request.billingCycle(), request.currency(), purpose, null);
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public boolean activate(SubscriptionCharge charge) {
        var current = subscriptions.lockAccount(charge.accountId());
        if (!current.id.equals(charge.subscriptionId()) || current.version != charge.expectedVersion()) {
            return false;
        }
        try {
            billing.requirePolicy();
        } catch (BillingException exception) {
            return false;
        }
        Instant start = charge.verifiedAt();
        if (charge.purpose() == PaymentPurpose.RENEW && current.periodEnd != null && start.isBefore(current.periodEnd)) {
            start = current.periodEnd;
        }
        Instant end = billing.periodEnd(start, charge.billingCycle());
        if (charge.purpose() == PaymentPurpose.RENEW && !current.planId.equals(charge.targetPlanId())
                && clock.instant().isBefore(start)) {
            current.pendingPlanId = charge.targetPlanId();
            current.pendingBillingCycle = charge.billingCycle();
            current.pendingCurrency = charge.currency();
            current.pendingPeriodStart = start;
            current.pendingPeriodEnd = end;
        } else {
            boolean extending = charge.purpose() == PaymentPurpose.RENEW && current.planId.equals(charge.targetPlanId())
                    && current.periodEnd != null && !charge.verifiedAt().isAfter(current.periodEnd);
            current.planId = charge.targetPlanId();
            current.status = SubscriptionStatus.ACTIVE;
            current.billingCycle = charge.billingCycle();
            current.currency = charge.currency();
            current.periodStart = extending ? current.periodStart : start;
            current.periodEnd = end;
            current.cancelAtPeriodEnd = false;
            SubscriptionService.clearPending(current);
        }
        repository.flush();
        return true;
    }

    public PlanCode code(UUID planId) {
        return plans.findById(planId).code;
    }
}