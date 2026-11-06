package io.runcoach.subscription.service;

import io.runcoach.common.service.CurrentAccount;
import io.runcoach.subscription.dto.FeatureResponse;
import io.runcoach.subscription.entity.Feature;
import io.runcoach.subscription.entity.PlanCode;
import io.runcoach.subscription.entity.SubscriptionStatus;
import io.runcoach.subscription.repository.SubscriptionFeatureRepository;
import io.runcoach.subscription.repository.SubscriptionPlanRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Clock;

@ApplicationScoped
public class FeatureAccessService {
    @Inject
    CurrentAccount account;
    @Inject
    SubscriptionService subscriptions;
    @Inject
    SubscriptionPlanRepository plans;
    @Inject
    SubscriptionFeatureRepository features;
    @Inject
    Clock clock;

    @Transactional
    public FeatureResponse features() {
        var subscription = subscriptions.current(account.subject());
        boolean paidAccess = (subscription.status() == SubscriptionStatus.ACTIVE || subscription.status() == SubscriptionStatus.TRIAL)
                && subscription.periodStart() != null && !clock.instant().isBefore(subscription.periodStart())
                && subscription.periodEnd() != null && clock.instant().isBefore(subscription.periodEnd());
        PlanCode effective = paidAccess ? subscription.plan() : PlanCode.FREE;
        return new FeatureResponse(effective, features.features(plans.require(effective).id));
    }

    public boolean canUseAiCoach() { return features().features().contains(Feature.AI_COACH); }
    public boolean canUseTrainingPlan() { return features().features().contains(Feature.TRAINING_PLANS); }
    public boolean canUseAdvancedAnalytics() { return features().features().contains(Feature.ADVANCED_ANALYTICS); }
    public boolean canUseNutritionCoach() { return features().features().contains(Feature.NUTRITION_COACH); }
}