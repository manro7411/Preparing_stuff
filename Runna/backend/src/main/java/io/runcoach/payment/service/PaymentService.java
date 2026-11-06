package io.runcoach.payment.service;

import io.runcoach.common.exception.BillingException;
import io.runcoach.payment.dto.PaymentHistory;
import io.runcoach.payment.dto.PaymentRequest;
import io.runcoach.payment.dto.PaymentResponse;
import io.runcoach.payment.entity.PaymentPurpose;
import io.runcoach.payment.entity.PaymentStatus;
import io.runcoach.subscription.service.BillingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import java.util.function.Supplier;

@ApplicationScoped
@Transactional(Transactional.TxType.NOT_SUPPORTED)
public class PaymentService {
    @Inject
    PaymentGatewayRegistry gateways;
    @Inject
    PaymentStore store;
    @Inject
    BillingService billing;

    public PaymentResponse create(String subject, @NotNull @Valid PaymentRequest request, PaymentPurpose purpose) {
        long quote = billing.quote(request);
        PaymentGateway gateway = gateways.require(request.provider());
        PaymentStore.View prepared = store.prepare(subject, request, purpose, quote);
        if (prepared.providerPaymentId() != null || prepared.response().status() != PaymentStatus.CREATED) {
            return prepared.response();
        }
        var checkout = call(() -> gateway.createPayment(prepared.command()));
        return store.checkout(prepared.ownerId(), prepared.command().paymentId(), checkout);
    }

    public PaymentResponse verify(String subject, UUID paymentId) {
        PaymentStore.View payment = store.owned(subject, paymentId);
        if (payment.response().status() == PaymentStatus.SUCCESS) {
            return payment.response();
        }
        if (payment.response().status() == PaymentStatus.REFUNDED) {
            throw new BillingException(409, "PAYMENT_ALREADY_REFUNDED", "This payment has already been refunded.");
        }
        if (payment.providerPaymentId() == null) {
            throw new BillingException(409, "CHECKOUT_NOT_READY", "Retry creation with the original idempotency key first.");
        }
        var gateway = gateways.require(payment.provider());
        var receipt = call(() -> gateway.verifyPayment(new PaymentGateway.VerifyCommand(paymentId, payment.providerPaymentId())));
        return store.settle(payment.ownerId(), paymentId, receipt, null, null);
    }

    public PaymentHistory history(String subject, int page, int size) {
        if (page < 0 || page > 10000 || size < 1 || size > 100) {
            throw new BillingException(400, "INVALID_PAGE", "Use page 0-10000 and size 1-100.");
        }
        return new PaymentHistory(store.history(subject, page, size), page, size);
    }

    public static <Result> Result call(Supplier<Result> operation) {
        try {
            return operation.get();
        } catch (BillingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BillingException(502, "PAYMENT_GATEWAY_UNAVAILABLE", "The provider could not complete this request. Retry safely.");
        }
    }
}