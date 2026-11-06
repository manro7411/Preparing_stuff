package io.runcoach.training.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Request goal-based planning; ownership and goal details must be resolved server-side")
public record GenerateTrainingPlanRequest(
        @NotNull UUID goalId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate) {
}