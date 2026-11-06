package io.runcoach.training.service;

import io.runcoach.training.dto.GenerateTrainingPlanRequest;
import io.runcoach.training.dto.GenerateWeeklyPlanRequest;
import io.runcoach.training.dto.GenerateWorkoutRequest;
import io.runcoach.training.dto.PlannedTrainingPlan;
import io.runcoach.training.dto.PlannedWeek;
import io.runcoach.training.dto.PlannedWorkout;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface TrainingService {
    PlannedTrainingPlan generatePlan(String subject, @NotNull @Valid GenerateTrainingPlanRequest request);

    PlannedWeek generateWeek(String subject, @NotNull @Valid GenerateWeeklyPlanRequest request);

    PlannedWorkout generateWorkout(String subject, @NotNull @Valid GenerateWorkoutRequest request);
}