package io.runcoach.payment.validator;

import io.runcoach.common.exception.BillingException;
import io.runcoach.payment.entity.PaymentStatus;
import io.runcoach.payment.entity.PaymentTransaction;
import io.runcoach.payment.service.PaymentGateway;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.util.Objects;

@ApplicationScoped
public class PaymentValidator {
    public void receipt(PaymentTransaction payment, PaymentGateway.Receipt receipt) {
        if (receipt == null || !Objects.equals(payment.id, receipt.paymentId())
                || payment.providerPaymentId == null || !Objects.equals(payment.providerPaymentId, receipt.providerPaymentId())
                || payment.amountMinor != receipt.amountMinor() || payment.currency != receipt.currency()
                || receipt.status() == null) {
            throw new BillingException(502, "GATEWAY_RECEIPT_MISMATCH", "The provider receipt does not match this payment.");
        }
        if (receipt.status() == PaymentStatus.REFUNDED) {
            throw new BillingException(409, "REFUND_RECONCILIATION_REQUIRED", "Refunds require an approved entitlement reconciliation policy.");
        }
    }

    public void checkout(PaymentGateway.Checkout checkout) {
        if (checkout == null || checkout.providerPaymentId() == null || checkout.providerPaymentId().isBlank()
                || checkout.providerPaymentId().length() > 255) {
            throw new BillingException(502, "INVALID_PROVIDER_CHECKOUT", "The provider returned an invalid checkout.");
        }
        if (checkout.checkoutUrl() != null) {
            try {
                URI uri = URI.create(checkout.checkoutUrl());
                if (checkout.checkoutUrl().length() > 2048 || !"https".equalsIgnoreCase(uri.getScheme())
                        || uri.getHost() == null || uri.getUserInfo() != null) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException exception) {
                throw new BillingException(502, "INVALID_CHECKOUT_URL", "The provider checkout URL is not valid HTTPS.");
            }
        }
    }
}