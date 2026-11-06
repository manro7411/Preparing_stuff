package io.runcoach.training.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Request one goal-based workout on the supplied date")
public record GenerateWorkoutRequest(@NotNull UUID goalId, @NotNull LocalDate scheduledDate) {
}