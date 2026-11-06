package io.runcoach.payment.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record VerifyPaymentRequest(@NotNull UUID paymentId) {
}