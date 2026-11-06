package io.runcoach.training.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PlannedTrainingPlan(
        UUID goalId, LocalDate startDate, LocalDate endDate, String engineVersion, List<PlannedWorkout> workouts) {
    public PlannedTrainingPlan {
        workouts = List.copyOf(workouts);
    }
}