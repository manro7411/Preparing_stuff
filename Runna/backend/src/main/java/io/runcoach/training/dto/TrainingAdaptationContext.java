package io.runcoach.training.dto;

import java.time.LocalDate;
import java.util.UUID;

public record TrainingAdaptationContext(UUID trainingPlanId, long expectedVersion, LocalDate effectiveFrom) {
}