package io.runcoach.billing.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.runcoach.common.exception.BillingException;
import io.runcoach.payment.entity.PaymentProvider;
import io.runcoach.payment.entity.PaymentStatus;
import io.runcoach.payment.service.PaymentGateway;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Alternative
@ApplicationScoped
public class BillingTestGateway implements PaymentGateway {
    private final Map<UUID, CreateCommand> commands = new ConcurrentHashMap<>();
    private final Map<UUID, PaymentStatus> statuses = new ConcurrentHashMap<>();
    @Inject TransactionManager transactions;
    @Inject ObjectMapper json;

    public void reset() { commands.clear(); statuses.clear(); }
    public void succeed(UUID paymentId) { statuses.put(paymentId, PaymentStatus.SUCCESS); }
    @Override public PaymentProvider provider() { return PaymentProvider.STRIPE; }

    @Override
    public Checkout createPayment(CreateCommand command) {
        requireNoTransaction();
        CreateCommand existing = commands.putIfAbsent(command.paymentId(), command);
        if (existing != null && !existing.equals(command)) {
            throw new IllegalStateException("Test checkout idempotency mismatch");
        }
        statuses.putIfAbsent(command.paymentId(), PaymentStatus.PENDING);
        return new Checkout("test-" + command.paymentId(), "https://checkout.example.test/" + command.paymentId());
    }

    @Override
    public Receipt verifyPayment(VerifyCommand command) {
        requireNoTransaction();
        CreateCommand original = commands.get(command.paymentId());
        if (original == null || !("test-" + command.paymentId()).equals(command.providerPaymentId())) {
            throw new IllegalStateException("Test payment not found");
        }
        return new Receipt(command.paymentId(), command.providerPaymentId(), original.amountMinor(),
                original.currency(), statuses.get(command.paymentId()));
    }

    @Override
    public Receipt refundPayment(RefundCommand command) {
        requireNoTransaction();
        CreateCommand original = commands.get(command.paymentId());
        return new Receipt(command.paymentId(), command.providerPaymentId(), command.amountMinor(), original.currency(), PaymentStatus.REFUNDED);
    }

    @Override
    public void cancelPayment(VerifyCommand command) {
        requireNoTransaction();
        statuses.put(command.paymentId(), PaymentStatus.FAILED);
    }

    @Override
    public VerifiedWebhook verifyWebhook(byte[] originalPayload, String signature) {
        requireNoTransaction();
        try {
            if (!MessageDigest.isEqual(HexFormat.of().parseHex(signature), HexFormat.of().parseHex(sign(originalPayload)))) {
                throw new IllegalArgumentException("Signature mismatch");
            }
            return json.readValue(originalPayload, VerifiedWebhook.class);
        } catch (java.io.IOException | IllegalArgumentException exception) {
            throw new BillingException(400, "INVALID_SIGNATURE", "Provider signature is invalid.");
        }
    }

    public String sign(byte[] payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec("test-only-signing-key-not-for-production".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void requireNoTransaction() {
        try {
            if (transactions.getStatus() != Status.STATUS_NO_TRANSACTION) {
                throw new IllegalStateException("Gateway calls must not hold database transactions");
            }
        } catch (jakarta.transaction.SystemException exception) {
            throw new IllegalStateException(exception);
        }
    }
}