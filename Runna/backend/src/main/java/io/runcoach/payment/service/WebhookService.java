package io.runcoach.payment.service;

import io.runcoach.common.exception.BillingException;
import io.runcoach.payment.dto.PaymentResponse;
import io.runcoach.payment.dto.WebhookRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;

@ApplicationScoped
@Transactional(Transactional.TxType.NOT_SUPPORTED)
public class WebhookService {
    @Inject
    PaymentGatewayRegistry gateways;
    @Inject
    PaymentStore store;

    public PaymentResponse handle(@NotNull @Valid WebhookRequest request) {
        byte[] payload;
        try {
            payload = Base64.getDecoder().decode(request.payloadBase64());
        } catch (IllegalArgumentException exception) {
            throw new BillingException(400, "INVALID_WEBHOOK_ENCODING", "The webhook payload must be base64 encoded.");
        }
        if (payload.length > 262144) {
            throw new BillingException(413, "WEBHOOK_TOO_LARGE", "The webhook payload is too large.");
        }
        var gateway = gateways.require(request.provider());
        var event = PaymentService.call(() -> gateway.verifyWebhook(payload, request.signature()));
        if (event == null || event.eventId() == null || event.eventId().isBlank()
                || event.eventId().length() > 255 || event.paymentId() == null) {
            throw new BillingException(400, "INVALID_WEBHOOK", "The provider webhook could not be authenticated.");
        }
        var payment = store.webhookView(request.provider(), event.paymentId());
        if (payment.providerPaymentId() == null) {
            throw new BillingException(409, "CHECKOUT_NOT_READY", "Retry the webhook after checkout creation completes.");
        }
        var receipt = PaymentService.call(() -> gateway.verifyPayment(
                new PaymentGateway.VerifyCommand(event.paymentId(), payment.providerPaymentId())));
        return store.settle(payment.ownerId(), event.paymentId(), receipt, event.eventId(), hash(payload));
    }

    private static String hash(byte[] payload) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }
}