package io.runcoach.training.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Request a seven-day planning window starting on the supplied date; no weekday is assumed")
public record GenerateWeeklyPlanRequest(@NotNull UUID goalId, @NotNull LocalDate weekStart) {
}