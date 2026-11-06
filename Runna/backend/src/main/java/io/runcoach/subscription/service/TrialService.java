package io.runcoach.subscription.service;

import io.runcoach.subscription.dto.SubscriptionResponse;
import io.runcoach.subscription.entity.PlanCode;
import io.runcoach.subscription.entity.SubscriptionStatus;
import io.runcoach.subscription.repository.SubscriptionPlanRepository;
import io.runcoach.subscription.repository.UserSubscriptionRepository;
import io.runcoach.subscription.validator.TrialValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Clock;

@ApplicationScoped
public class TrialService {
    private final SubscriptionService service;
    private final SubscriptionPlanRepository plans;
    private final UserSubscriptionRepository subscriptions;
    private final TrialValidator validator;
    private final Clock clock;

    @Inject
    public TrialService(SubscriptionService service, SubscriptionPlanRepository plans,
            UserSubscriptionRepository subscriptions, TrialValidator validator, Clock clock) {
        this.service = service;
        this.plans = plans;
        this.subscriptions = subscriptions;
        this.validator = validator;
        this.clock = clock;
    }

    @Transactional
    public SubscriptionResponse start(String subject) {
        var subscription = service.currentLocked(subject);
        validator.requireEligible(subscription, service.planCode(subscription));
        subscription.planId = plans.require(PlanCode.PRO).id;
        subscription.status = SubscriptionStatus.TRIAL;
        subscription.trialStartedAt = clock.instant();
        subscription.trialEndsAt = validator.trialEnd(subscription.trialStartedAt);
        subscription.periodStart = subscription.trialStartedAt;
        subscription.periodEnd = subscription.trialEndsAt;
        subscription.billingCycle = null;
        subscription.currency = null;
        subscription.cancelAtPeriodEnd = false;
        SubscriptionService.clearPending(subscription);
        subscriptions.flush();
        return service.response(subscription);
    }
}