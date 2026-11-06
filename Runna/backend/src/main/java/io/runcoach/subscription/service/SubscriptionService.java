package io.runcoach.subscription.service;

import io.runcoach.athlete.service.AthleteService;
import io.runcoach.common.exception.BillingException;
import io.runcoach.subscription.dto.SubscriptionResponse;
import io.runcoach.subscription.entity.PlanCode;
import io.runcoach.subscription.entity.SubscriptionStatus;
import io.runcoach.subscription.entity.UserSubscription;
import io.runcoach.subscription.mapper.SubscriptionMapper;
import io.runcoach.subscription.repository.SubscriptionPlanRepository;
import io.runcoach.subscription.repository.UserSubscriptionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class SubscriptionService {
    private final AthleteService athletes;
    private final UserSubscriptionRepository subscriptions;
    private final SubscriptionPlanRepository plans;
    private final SubscriptionMapper mapper;
    private final BillingService billing;
    private final Clock clock;

    @Inject
    public SubscriptionService(AthleteService athletes, UserSubscriptionRepository subscriptions,
            SubscriptionPlanRepository plans, SubscriptionMapper mapper, BillingService billing, Clock clock) {
        this.athletes = athletes;
        this.subscriptions = subscriptions;
        this.plans = plans;
        this.mapper = mapper;
        this.billing = billing;
        this.clock = clock;
    }

    @Transactional
    public SubscriptionResponse current(String subject) {
        return response(currentLocked(subject));
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public UserSubscription currentLocked(String subject) {
        return forLockedAccount(athletes.lockForBilling(subject).id());
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public UserSubscription lockAccount(UUID accountId) {
        athletes.lockAccountForBilling(accountId);
        return forLockedAccount(accountId);
    }

    private UserSubscription forLockedAccount(UUID accountId) {
        UserSubscription subscription = subscriptions.findByUser(accountId).orElseGet(() -> {
            UserSubscription created = new UserSubscription();
            created.userId = accountId;
            created.planId = plans.require(PlanCode.FREE).id;
            created.status = SubscriptionStatus.ACTIVE;
            subscriptions.persistAndFlush(created);
            return created;
        });
        refresh(subscription);
        subscriptions.flush();
        return subscription;
    }

    public SubscriptionResponse response(UserSubscription subscription) {
        PlanCode code = plans.findById(subscription.planId).code;
        PlanCode pending = subscription.pendingPlanId == null ? null : plans.findById(subscription.pendingPlanId).code;
        return mapper.toResponse(subscription, code, pending);
    }

    public PlanCode planCode(UserSubscription subscription) {
        return plans.findById(subscription.planId).code;
    }

    private void refresh(UserSubscription subscription) {
        Instant now = clock.instant();
        if (subscription.periodEnd == null || now.isBefore(subscription.periodEnd)
                || subscription.status != SubscriptionStatus.TRIAL && subscription.status != SubscriptionStatus.ACTIVE) {
            return;
        }
        if (subscription.status == SubscriptionStatus.TRIAL) {
            subscription.status = SubscriptionStatus.EXPIRED;
        } else if (subscription.cancelAtPeriodEnd) {
            subscription.status = SubscriptionStatus.CANCELLED;
            clearPending(subscription);
        } else if (subscription.pendingPlanId != null) {
            subscription.planId = subscription.pendingPlanId;
            if (subscription.pendingPeriodEnd != null) {
                subscription.billingCycle = subscription.pendingBillingCycle;
                subscription.currency = subscription.pendingCurrency;
                subscription.periodStart = subscription.pendingPeriodStart;
                subscription.periodEnd = subscription.pendingPeriodEnd;
                subscription.status = now.isBefore(subscription.periodEnd) ? SubscriptionStatus.ACTIVE : SubscriptionStatus.EXPIRED;
            } else if (planCode(subscription) == PlanCode.FREE) {
                subscription.status = SubscriptionStatus.ACTIVE;
                subscription.periodStart = null;
                subscription.periodEnd = null;
                subscription.billingCycle = null;
                subscription.currency = null;
            } else {
                subscription.status = SubscriptionStatus.EXPIRED;
            }
            clearPending(subscription);
        } else {
            subscription.status = SubscriptionStatus.EXPIRED;
        }
    }

    @Transactional
    public SubscriptionResponse cancel(String subject) {
        billing.requirePolicy();
        UserSubscription subscription = currentLocked(subject);
        requireLivePaidPeriod(subscription);
        if (subscription.pendingPeriodEnd != null) {
            throw new BillingException(409, "PREPAID_CHANGE_EXISTS", "A prepaid renewal must be reconciled before cancellation.");
        }
        subscription.cancelAtPeriodEnd = true;
        clearPending(subscription);
        subscriptions.flush();
        return response(subscription);
    }

    @Transactional
    public SubscriptionResponse restore(String subject) {
        billing.requirePolicy();
        UserSubscription subscription = currentLocked(subject);
        requireLivePaidPeriod(subscription);
        if (!subscription.cancelAtPeriodEnd) {
            throw new BillingException(409, "NOT_PENDING_CANCELLATION", "There is no pending cancellation to restore.");
        }
        subscription.cancelAtPeriodEnd = false;
        subscriptions.flush();
        return response(subscription);
    }

    @Transactional
    public SubscriptionResponse downgrade(String subject, PlanCode target) {
        billing.requirePolicy();
        UserSubscription subscription = currentLocked(subject);
        requireLivePaidPeriod(subscription);
        if (rank(target) >= rank(planCode(subscription))) {
            throw new BillingException(400, "INVALID_DOWNGRADE", "Choose a lower subscription plan.");
        }
        if (subscription.pendingPeriodEnd != null || subscription.cancelAtPeriodEnd) {
            throw new BillingException(409, "CHANGE_ALREADY_PENDING", "Resolve the existing pending change first.");
        }
        subscription.pendingPlanId = plans.require(target).id;
        subscriptions.flush();
        return response(subscription);
    }

    public static int rank(PlanCode plan) {
        return switch (plan) { case FREE -> 0; case PRO -> 1; case ELITE -> 2; };
    }

    private void requireLivePaidPeriod(UserSubscription subscription) {
        if (subscription.status != SubscriptionStatus.ACTIVE || planCode(subscription) == PlanCode.FREE
                || subscription.periodEnd == null) {
            throw new BillingException(409, "NO_ACTIVE_PAID_SUBSCRIPTION", "An active paid subscription is required.");
        }
    }

    public static void clearPending(UserSubscription subscription) {
        subscription.pendingPlanId = null;
        subscription.pendingBillingCycle = null;
        subscription.pendingCurrency = null;
        subscription.pendingPeriodStart = null;
        subscription.pendingPeriodEnd = null;
    }

    @Transactional
    public List<UUID> dueAccounts() {
        return subscriptions.find("status in ?1 and periodEnd <= ?2 order by userId",
                List.of(SubscriptionStatus.TRIAL, SubscriptionStatus.ACTIVE), clock.instant())
                .page(0, 100).list().stream().map(subscription -> subscription.userId).toList();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void expireAccount(UUID accountId) {
        lockAccount(accountId);
    }
}