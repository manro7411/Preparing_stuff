package io.runcoach.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.runcoach.common.exception.BillingException;
import io.runcoach.payment.dto.PaymentRequest;
import io.runcoach.payment.dto.PaymentResponse;
import io.runcoach.payment.entity.PaymentProvider;
import io.runcoach.payment.entity.PaymentPurpose;
import io.runcoach.payment.entity.PaymentStatus;
import io.runcoach.subscription.service.BillingService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BillingOrchestrationTest {
    @Test
    @DisplayName("Unconfigured prices do not reserve or send a payment")
    void shouldNotCallGatewayBeforeConfiguredPrice() {
        PaymentService service = service();
        when(service.billing.quote(any())).thenThrow(new BillingException(503, "PRICE_UNCONFIGURED", "Set a price."));

        var exception = assertThrows(BillingException.class,
                () -> service.create("owner", mock(PaymentRequest.class), PaymentPurpose.SUBSCRIBE));

        assertEquals("PRICE_UNCONFIGURED", exception.code());
        verifyNoInteractions(service.gateways, service.store);
    }

    @Test
    @DisplayName("An already refunded payment does not reach gateway verification")
    void shouldRejectRefundedPayment() {
        PaymentService service = service();
        UUID id = UUID.randomUUID();
        var response = new PaymentResponse(id, PaymentProvider.STRIPE, PaymentStatus.REFUNDED, 1000,
                io.runcoach.common.dto.CurrencyCode.USD, null, null, false, false);
        when(service.store.owned("owner", id)).thenReturn(new PaymentStore.View(UUID.randomUUID(), null,
                "provider-1", PaymentProvider.STRIPE, response));

        var exception = assertThrows(BillingException.class, () -> service.verify("owner", id));

        assertEquals("PAYMENT_ALREADY_REFUNDED", exception.code());
        verifyNoInteractions(service.gateways);
    }

    private PaymentService service() {
        PaymentService service = new PaymentService();
        service.billing = mock(BillingService.class);
        service.gateways = mock(PaymentGatewayRegistry.class);
        service.store = mock(PaymentStore.class);
        return service;
    }
}