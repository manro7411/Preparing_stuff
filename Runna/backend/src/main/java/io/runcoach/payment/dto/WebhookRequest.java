package io.runcoach.payment.dto;

import io.runcoach.payment.entity.PaymentProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "ADMIN-authenticated relay envelope preserving original signed provider bytes")
public record WebhookRequest(@NotNull PaymentProvider provider,
        @NotBlank @Size(max = 350000) String payloadBase64,
        @NotBlank @Size(max = 4096) String signature) {
}