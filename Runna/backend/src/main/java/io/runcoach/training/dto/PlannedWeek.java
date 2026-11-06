package io.runcoach.training.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PlannedWeek(UUID goalId, LocalDate weekStart, String engineVersion, List<PlannedWorkout> workouts) {
    public PlannedWeek {
        workouts = List.copyOf(workouts);
    }
}