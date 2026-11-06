package io.runcoach.payment.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.runcoach.common.exception.BillingException;
import io.runcoach.payment.entity.PaymentTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PaymentTransactionRepository implements PanacheRepositoryBase<PaymentTransaction, UUID> {
    public Optional<PaymentTransaction> byKey(UUID userId, UUID key) {
        return find("userId = ?1 and idempotencyKey = ?2", userId, key).firstResultOptional();
    }

    public PaymentTransaction owned(UUID userId, UUID paymentId, boolean lock) {
        var query = find("userId = ?1 and id = ?2", userId, paymentId);
        if (lock) {
            query.withLock(LockModeType.PESSIMISTIC_WRITE);
        }
        return query.firstResultOptional().orElseThrow(() ->
                new BillingException(404, "PAYMENT_NOT_FOUND", "Payment not found."));
    }
}