package io.runcoach.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.runcoach.common.dto.CurrencyCode;
import io.runcoach.common.exception.BillingException;
import io.runcoach.payment.dto.PaymentRequest;
import io.runcoach.payment.entity.PaymentProvider;
import io.runcoach.subscription.entity.BillingCycle;
import io.runcoach.subscription.entity.PlanCode;
import io.runcoach.subscription.service.BillingService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BillingConfigurationTest {
    @Test
    @DisplayName("Unspecified lifecycle policy fails closed")
    void shouldRequireExplicitBillingPolicy() {
        Config config = mock(Config.class);
        when(config.getOptionalValue("runcoach.billing.policy", String.class)).thenReturn(Optional.empty());

        var exception = assertThrows(BillingException.class, () -> new BillingService(config).requirePolicy());

        assertEquals(503, exception.status());
    }

    @Test
    @DisplayName("Missing prices do not create free paid subscriptions")
    void shouldRequireServerSidePrice() {
        Config config = mock(Config.class);
        when(config.getOptionalValue("runcoach.billing.policy", String.class)).thenReturn(Optional.of("PERIOD_END_NO_PRORATION"));
        when(config.getOptionalValue("runcoach.billing.prices.pro.monthly.thb", Long.class)).thenReturn(Optional.empty());
        PaymentRequest request = new PaymentRequest(PlanCode.PRO, BillingCycle.MONTHLY, CurrencyCode.THB,
                PaymentProvider.OMISE, UUID.randomUUID());

        var exception = assertThrows(BillingException.class, () -> new BillingService(config).quote(request));

        assertEquals("PRICE_UNCONFIGURED", exception.code());
    }

    @Test
    @DisplayName("Billing periods use UTC calendar months and years rather than fixed-day guesses")
    void shouldUseCalendarPeriods() {
        BillingService service = new BillingService(mock(Config.class));

        assertEquals(Instant.parse("2027-02-28T10:00:00Z"),
                service.periodEnd(Instant.parse("2027-01-31T10:00:00Z"), BillingCycle.MONTHLY));
        assertEquals(Instant.parse("2029-02-28T10:00:00Z"),
                service.periodEnd(Instant.parse("2028-02-29T10:00:00Z"), BillingCycle.YEARLY));
    }
}