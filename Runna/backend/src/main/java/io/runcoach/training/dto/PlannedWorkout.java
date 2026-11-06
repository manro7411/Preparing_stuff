package io.runcoach.training.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PlannedWorkout(
        LocalDate scheduledDate, String workoutType, BigDecimal distanceMeters, Integer durationSeconds) {
}