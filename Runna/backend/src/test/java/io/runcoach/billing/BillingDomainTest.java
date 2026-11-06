package io.runcoach.billing;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.runcoach.common.exception.BillingException;
import io.runcoach.subscription.entity.PlanCode;
import io.runcoach.subscription.entity.SubscriptionStatus;
import io.runcoach.subscription.entity.UserSubscription;
import io.runcoach.subscription.validator.TrialValidator;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BillingDomainTest {
    private final TrialValidator validator = new TrialValidator();

    @Test
    @DisplayName("A trial lasts exactly fourteen 24-hour days")
    void shouldCalculateFourteenDayTrial() {
        assertEquals(Instant.parse("2026-11-17T10:00:00Z"),
                validator.trialEnd(Instant.parse("2026-11-03T10:00:00Z")));
    }

    @Test
    @DisplayName("A free account without trial history is eligible")
    void shouldAllowFirstTrial() {
        UserSubscription subscription = new UserSubscription();
        subscription.status = SubscriptionStatus.ACTIVE;

        assertDoesNotThrow(() -> validator.requireEligible(subscription, PlanCode.FREE));
    }

    @Test
    @DisplayName("Expired or cancelled trials cannot be restarted")
    void shouldRejectPreviouslyUsedTrial() {
        for (SubscriptionStatus status : SubscriptionStatus.values()) {
            UserSubscription subscription = new UserSubscription();
            subscription.status = status;
            subscription.trialStartedAt = Instant.parse("2026-01-01T00:00:00Z");

            var exception = assertThrows(BillingException.class,
                    () -> validator.requireEligible(subscription, PlanCode.FREE));

            assertEquals(409, exception.status());
            assertEquals("TRIAL_ALREADY_USED", exception.code());
        }
    }

    @Test
    @DisplayName("An active paid account cannot start a trial")
    void shouldRejectActivePaidAccount() {
        UserSubscription subscription = new UserSubscription();
        subscription.status = SubscriptionStatus.ACTIVE;

        var exception = assertThrows(BillingException.class,
                () -> validator.requireEligible(subscription, PlanCode.PRO));

        assertEquals("TRIAL_NOT_ELIGIBLE", exception.code());
    }
}