package io.runcoach.payment.dto;

import io.runcoach.common.dto.CurrencyCode;
import io.runcoach.payment.entity.PaymentProvider;
import io.runcoach.payment.entity.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(UUID id, PaymentProvider provider, PaymentStatus status, long amountMinor,
        CurrencyCode currency, String checkoutUrl, Instant paidAt, boolean subscriptionApplied,
        boolean reconciliationRequired) {
}