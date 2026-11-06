package io.runcoach.payment.dto;

import io.runcoach.common.dto.CurrencyCode;
import io.runcoach.payment.entity.PaymentProvider;
import io.runcoach.subscription.entity.BillingCycle;
import io.runcoach.subscription.entity.PlanCode;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Select a plan; the server determines the charge. Never send a payment amount or user ID.")
public record PaymentRequest(@NotNull PlanCode plan, @NotNull BillingCycle billingCycle,
        @NotNull CurrencyCode currency, @NotNull PaymentProvider provider, @NotNull UUID idempotencyKey) {
}