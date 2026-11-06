package io.runcoach.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.runcoach.athlete.service.AthleteService;
import io.runcoach.common.dto.CurrencyCode;
import io.runcoach.payment.entity.PaymentPurpose;
import io.runcoach.payment.entity.PaymentStatus;
import io.runcoach.payment.entity.PaymentTransaction;
import io.runcoach.payment.mapper.PaymentMapper;
import io.runcoach.payment.repository.PaymentTransactionRepository;
import io.runcoach.payment.repository.WebhookLogRepository;
import io.runcoach.payment.validator.PaymentValidator;
import io.runcoach.subscription.entity.BillingCycle;
import io.runcoach.subscription.entity.PlanCode;
import io.runcoach.subscription.service.SubscriptionBillingService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unused")
class BillingSettlementTest {
    PaymentStore store;
    PaymentTransaction payment;

    @BeforeEach
    void setUp() {
        store = new PaymentStore();
        store.athletes = mock(AthleteService.class);
        store.payments = mock(PaymentTransactionRepository.class);
        store.subscriptions = mock(SubscriptionBillingService.class);
        store.invoices = mock(InvoiceService.class);
        store.webhooks = mock(WebhookLogRepository.class);
        store.mapper = new PaymentMapper();
        store.validator = new PaymentValidator();
        store.clock = Clock.fixed(Instant.parse("2026-11-03T10:00:00Z"), ZoneOffset.UTC);
        payment = new PaymentTransaction();
        payment.id = UUID.randomUUID();
        payment.userId = UUID.randomUUID();
        payment.subscriptionId = UUID.randomUUID();
        payment.targetPlanId = UUID.randomUUID();
        payment.billingCycle = BillingCycle.MONTHLY;
        payment.purpose = PaymentPurpose.SUBSCRIBE;
        payment.amountMinor = 1000;
        payment.currency = CurrencyCode.USD;
        payment.providerPaymentId = "provider-1";
        payment.status = PaymentStatus.PENDING;
        when(store.payments.owned(payment.userId, payment.id, true)).thenReturn(payment);
    }

    @Test
    @DisplayName("Pending provider receipts never activate subscriptions")
    void shouldNotActivatePendingPayment() {
        var result = store.settle(payment.userId, payment.id, receipt(PaymentStatus.PENDING), null, null);

        assertEquals(PaymentStatus.PENDING, result.status());
        assertFalse(result.subscriptionApplied());
        verify(store.subscriptions, never()).activate(any());
        verify(store.invoices, never()).issue(any());
    }

    @Test
    @DisplayName("Repeated success does not extend subscriptions or issue a second invoice")
    void shouldSettleOnlyOnce() {
        when(store.subscriptions.code(payment.targetPlanId)).thenReturn(PlanCode.PRO);
        when(store.subscriptions.activate(any())).thenReturn(true);

        var first = store.settle(payment.userId, payment.id, receipt(PaymentStatus.SUCCESS), null, null);
        var second = store.settle(payment.userId, payment.id, receipt(PaymentStatus.SUCCESS), null, null);

        assertTrue(first.subscriptionApplied());
        assertEquals(first, second);
        verify(store.subscriptions, times(1)).activate(any());
        verify(store.invoices, times(1)).issue(payment);
    }

    @Test
    @DisplayName("A stale subscription records captured funds for reconciliation instead of overwriting entitlements")
    void shouldRecordReconciliationWhenEntitlementVersionChanged() {
        when(store.subscriptions.code(payment.targetPlanId)).thenReturn(PlanCode.PRO);
        when(store.subscriptions.activate(any())).thenReturn(false);

        var result = store.settle(payment.userId, payment.id, receipt(PaymentStatus.SUCCESS), null, null);

        assertEquals(PaymentStatus.SUCCESS, result.status());
        assertTrue(result.reconciliationRequired());
        assertFalse(result.subscriptionApplied());
        verify(store.invoices).issue(payment);
    }

    private PaymentGateway.Receipt receipt(PaymentStatus status) {
        return new PaymentGateway.Receipt(payment.id, payment.providerPaymentId, payment.amountMinor, payment.currency, status);
    }
}