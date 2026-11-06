package io.runcoach.payment.mapper;

import io.runcoach.payment.dto.PaymentResponse;
import io.runcoach.payment.entity.PaymentTransaction;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PaymentMapper {
    public PaymentResponse toResponse(PaymentTransaction entity) {
        return new PaymentResponse(entity.id, entity.provider, entity.status, entity.amountMinor, entity.currency,
                entity.checkoutUrl, entity.paidAt, entity.subscriptionApplied, entity.reconciliationRequired);
    }
}