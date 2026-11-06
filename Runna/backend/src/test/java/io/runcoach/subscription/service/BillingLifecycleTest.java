package io.runcoach.subscription.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.runcoach.athlete.dto.AthleteResponse;
import io.runcoach.athlete.service.AthleteService;
import io.runcoach.common.exception.BillingException;
import io.runcoach.subscription.entity.BillingCycle;
import io.runcoach.subscription.entity.PlanCode;
import io.runcoach.subscription.entity.SubscriptionPlan;
import io.runcoach.subscription.entity.SubscriptionStatus;
import io.runcoach.subscription.entity.UserSubscription;
import io.runcoach.subscription.mapper.SubscriptionMapper;
import io.runcoach.subscription.repository.SubscriptionPlanRepository;
import io.runcoach.subscription.repository.UserSubscriptionRepository;
import io.runcoach.subscription.validator.TrialValidator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unused")
class BillingLifecycleTest {
    private static final Instant NOW = Instant.parse("2026-11-03T10:00:00Z");
    AthleteService athletes;
    UserSubscriptionRepository repository;
    SubscriptionPlanRepository plans;
    BillingService billing;
    UserSubscription subscription;
    EnumMap<PlanCode, SubscriptionPlan> catalog;

    @BeforeEach
    void setUp() {
        athletes = mock(AthleteService.class);
        repository = mock(UserSubscriptionRepository.class);
        plans = mock(SubscriptionPlanRepository.class);
        billing = mock(BillingService.class);
        catalog = new EnumMap<>(PlanCode.class);
        for (PlanCode code : PlanCode.values()) {
            SubscriptionPlan plan = new SubscriptionPlan();
            plan.id = UUID.randomUUID();
            plan.code = code;
            catalog.put(code, plan);
        }
        when(plans.require(any())).thenAnswer(invocation -> catalog.get(invocation.getArgument(0, PlanCode.class)));
        when(plans.findById(any(UUID.class))).thenAnswer(invocation -> catalog.values().stream()
                .filter(plan -> plan.id.equals(invocation.getArgument(0))).findFirst().orElseThrow());
        subscription = new UserSubscription();
        subscription.id = UUID.randomUUID();
        subscription.userId = UUID.randomUUID();
        subscription.planId = catalog.get(PlanCode.FREE).id;
        subscription.status = SubscriptionStatus.ACTIVE;
        when(repository.findByUser(subscription.userId)).thenReturn(Optional.of(subscription));
        when(athletes.lockForBilling("owner")).thenReturn(new AthleteResponse(subscription.userId, "Runner", "UTC"));
    }

    @Test
    @DisplayName("Trial activation grants PRO for fourteen days and is not repeatable")
    void shouldActivateTrialOnlyOnce() {
        SubscriptionService service = service(NOW);
        TrialService trials = new TrialService(service, plans, repository, new TrialValidator(), Clock.fixed(NOW, ZoneOffset.UTC));

        var result = trials.start("owner");

        assertEquals(PlanCode.PRO, result.plan());
        assertEquals(SubscriptionStatus.TRIAL, result.status());
        assertEquals(NOW.plus(Duration.ofDays(14)), result.trialEndsAt());
        var exception = assertThrows(BillingException.class, () -> trials.start("owner"));
        assertEquals("TRIAL_ALREADY_USED", exception.code());
    }

    @Test
    @DisplayName("At the exact trial boundary access expires and history remains")
    void shouldExpireTrialAtBoundary() {
        subscription.planId = catalog.get(PlanCode.PRO).id;
        subscription.status = SubscriptionStatus.TRIAL;
        subscription.trialStartedAt = NOW.minus(Duration.ofDays(14));
        subscription.trialEndsAt = NOW;
        subscription.periodStart = subscription.trialStartedAt;
        subscription.periodEnd = NOW;

        var result = service(NOW).current("owner");

        assertEquals(SubscriptionStatus.EXPIRED, result.status());
        assertFalse(result.trialEligible());
        assertEquals(NOW.minus(Duration.ofDays(14)), result.trialStartedAt());
    }

    @Test
    @DisplayName("Pending cancellation can be restored while the paid period remains live")
    void shouldRestorePendingCancellation() {
        paid();
        SubscriptionService service = service(NOW);

        assertTrue(service.cancel("owner").cancelAtPeriodEnd());
        assertFalse(service.restore("owner").cancelAtPeriodEnd());
        assertEquals(SubscriptionStatus.ACTIVE, subscription.status);
    }

    @Test
    @DisplayName("Cancellation takes effect at period end under the explicit preset")
    void shouldCancelAtPeriodEnd() {
        paid();
        service(NOW).cancel("owner");

        var result = service(subscription.periodEnd).current("owner");

        assertEquals(SubscriptionStatus.CANCELLED, result.status());
    }

    @Test
    @DisplayName("A scheduled FREE downgrade retains access until the paid period ends")
    void shouldApplyFreeDowngradeAtBoundary() {
        paid();
        var pending = service(NOW).downgrade("owner", PlanCode.FREE);
        assertEquals(PlanCode.ELITE, pending.plan());
        assertEquals(PlanCode.FREE, pending.pendingPlan());

        var result = service(subscription.periodEnd).current("owner");

        assertEquals(PlanCode.FREE, result.plan());
        assertEquals(SubscriptionStatus.ACTIVE, result.status());
        assertEquals(null, result.periodEnd());
    }

    @Test
    @DisplayName("An unpaid paid-tier downgrade never grants a free renewal")
    void shouldRequirePaymentForPaidDowngrade() {
        paid();
        service(NOW).downgrade("owner", PlanCode.PRO);

        var result = service(subscription.periodEnd).current("owner");

        assertEquals(PlanCode.PRO, result.plan());
        assertEquals(SubscriptionStatus.EXPIRED, result.status());
    }

    private void paid() {
        subscription.planId = catalog.get(PlanCode.ELITE).id;
        subscription.billingCycle = BillingCycle.MONTHLY;
        subscription.periodStart = NOW.minus(Duration.ofDays(1));
        subscription.periodEnd = NOW.plus(Duration.ofDays(20));
    }

    private SubscriptionService service(Instant now) {
        return new SubscriptionService(athletes, repository, plans, new SubscriptionMapper(), billing, Clock.fixed(now, ZoneOffset.UTC));
    }
}