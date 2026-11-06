package io.runcoach.training.dto;

import java.time.LocalDate;
import java.util.UUID;

public record TrainingPlanningContext(
        UUID athleteId, UUID goalId, String timeZone, LocalDate startDate, LocalDate endDate) {
}