package io.runcoach.training.service;

import io.runcoach.training.dto.PlannedTrainingPlan;
import io.runcoach.training.dto.PlannedWeek;
import io.runcoach.training.dto.PlannedWorkout;
import io.runcoach.training.dto.TrainingAdaptationContext;
import io.runcoach.training.dto.TrainingPlanningContext;

public interface TrainingPlanner {
    PlannedTrainingPlan generatePlan(TrainingPlanningContext context);

    PlannedWeek generateWeek(TrainingPlanningContext context);

    PlannedWorkout generateWorkout(TrainingPlanningContext context);

    PlannedTrainingPlan adaptPlan(TrainingPlanningContext context,
            PlannedTrainingPlan currentPlan, TrainingAdaptationContext adaptation);
}