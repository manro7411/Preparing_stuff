package io.runcoach.billing;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.runcoach.common.dto.CurrencyCode;
import io.runcoach.common.exception.BillingException;
import io.runcoach.payment.entity.PaymentStatus;
import io.runcoach.payment.entity.PaymentTransaction;
import io.runcoach.payment.service.PaymentGateway;
import io.runcoach.payment.validator.PaymentValidator;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BillingReceiptTest {
    private final PaymentValidator validator = new PaymentValidator();

    @Test
    @DisplayName("A provider receipt must bind payment identity, amount and currency")
    void shouldRejectMismatchedReceipts() {
        PaymentTransaction payment = payment();
        for (var receipt : java.util.List.of(
                new PaymentGateway.Receipt(UUID.randomUUID(), "provider-1", 1000, CurrencyCode.USD, PaymentStatus.SUCCESS),
                new PaymentGateway.Receipt(payment.id, "another", 1000, CurrencyCode.USD, PaymentStatus.SUCCESS),
                new PaymentGateway.Receipt(payment.id, "provider-1", 1, CurrencyCode.USD, PaymentStatus.SUCCESS),
                new PaymentGateway.Receipt(payment.id, "provider-1", 1000, CurrencyCode.THB, PaymentStatus.SUCCESS))) {
            var exception = assertThrows(BillingException.class, () -> validator.receipt(payment, receipt));
            assertEquals("GATEWAY_RECEIPT_MISMATCH", exception.code());
        }
    }

    @Test
    @DisplayName("Matching verified success can proceed to settlement")
    void shouldAcceptMatchingReceipt() {
        PaymentTransaction payment = payment();
        assertDoesNotThrow(() -> validator.receipt(payment,
                new PaymentGateway.Receipt(payment.id, "provider-1", 1000, CurrencyCode.USD, PaymentStatus.SUCCESS)));
    }

    @Test
    @DisplayName("Provider URLs must not use executable or insecure schemes")
    void shouldRejectUnsafeCheckoutUrl() {
        for (String url : java.util.List.of("javascript:alert(1)", "http://checkout.example.com", "https://user:pass@example.com")) {
            var exception = assertThrows(BillingException.class,
                    () -> validator.checkout(new PaymentGateway.Checkout("provider-1", url)));
            assertEquals("INVALID_CHECKOUT_URL", exception.code());
        }
        assertDoesNotThrow(() -> validator.checkout(new PaymentGateway.Checkout("provider-1", "https://checkout.example.com/session")));
    }

    @Test
    @DisplayName("Refund receipts do not silently apply an undefined entitlement policy")
    void shouldRequireRefundReconciliation() {
        PaymentTransaction payment = payment();
        var exception = assertThrows(BillingException.class, () -> validator.receipt(payment,
                new PaymentGateway.Receipt(payment.id, "provider-1", 1000, CurrencyCode.USD, PaymentStatus.REFUNDED)));
        assertEquals("REFUND_RECONCILIATION_REQUIRED", exception.code());
    }

    private static PaymentTransaction payment() {
        PaymentTransaction payment = new PaymentTransaction();
        payment.id = UUID.randomUUID();
        payment.providerPaymentId = "provider-1";
        payment.amountMinor = 1000;
        payment.currency = CurrencyCode.USD;
        return payment;
    }
}