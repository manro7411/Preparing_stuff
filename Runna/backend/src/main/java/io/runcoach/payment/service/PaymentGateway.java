package io.runcoach.payment.service;

import io.runcoach.common.dto.CurrencyCode;
import io.runcoach.payment.entity.PaymentProvider;
import io.runcoach.payment.entity.PaymentPurpose;
import io.runcoach.payment.entity.PaymentStatus;
import io.runcoach.subscription.entity.BillingCycle;
import io.runcoach.subscription.entity.PlanCode;
import java.util.UUID;

public interface PaymentGateway {
    PaymentProvider provider();

    Checkout createPayment(CreateCommand command);

    Receipt verifyPayment(VerifyCommand command);

    Receipt refundPayment(RefundCommand command);

    void cancelPayment(VerifyCommand command);

    VerifiedWebhook verifyWebhook(byte[] originalPayload, String signature);

    record CreateCommand(UUID paymentId, UUID accountId, PlanCode plan, BillingCycle billingCycle,
            long amountMinor, CurrencyCode currency, PaymentPurpose purpose) {
    }

    record VerifyCommand(UUID paymentId, String providerPaymentId) {
    }

    record RefundCommand(UUID paymentId, String providerPaymentId, long amountMinor, UUID idempotencyKey) {
    }

    record Checkout(String providerPaymentId, String checkoutUrl) {
    }

    record Receipt(UUID paymentId, String providerPaymentId, long amountMinor,
            CurrencyCode currency, PaymentStatus status) {
    }

    record VerifiedWebhook(String eventId, UUID paymentId) {
    }
}