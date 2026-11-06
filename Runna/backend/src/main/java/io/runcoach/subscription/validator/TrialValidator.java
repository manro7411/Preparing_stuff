package io.runcoach.subscription.validator;

import io.runcoach.common.exception.BillingException;
import io.runcoach.subscription.entity.PlanCode;
import io.runcoach.subscription.entity.SubscriptionStatus;
import io.runcoach.subscription.entity.UserSubscription;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;

@ApplicationScoped
public class TrialValidator {
    public Instant trialEnd(Instant startedAt) {
        return startedAt.plus(Duration.ofDays(14));
    }

    public void requireEligible(UserSubscription subscription, PlanCode currentPlan) {
        if (subscription.trialStartedAt != null) {
            throw new BillingException(409, "TRIAL_ALREADY_USED", "This account has already used its trial.");
        }
        if (subscription.status == SubscriptionStatus.TRIAL
                || subscription.status == SubscriptionStatus.ACTIVE && currentPlan != PlanCode.FREE) {
            throw new BillingException(409, "TRIAL_NOT_ELIGIBLE", "An active paid subscription cannot start a trial.");
        }
    }
}