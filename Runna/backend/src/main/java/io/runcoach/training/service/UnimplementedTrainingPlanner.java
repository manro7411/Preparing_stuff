package io.runcoach.training.service;

import io.runcoach.training.dto.PlannedTrainingPlan;
import io.runcoach.training.dto.PlannedWeek;
import io.runcoach.training.dto.PlannedWorkout;
import io.runcoach.training.dto.TrainingAdaptationContext;
import io.runcoach.training.dto.TrainingPlanningContext;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UnimplementedTrainingPlanner implements TrainingPlanEngine {
    @Override
    public PlannedTrainingPlan generatePlan(TrainingPlanningContext context) {
        throw new TrainingPlanningUnavailableException();
    }

    @Override
    public PlannedWeek generateWeek(TrainingPlanningContext context) {
        throw new TrainingPlanningUnavailableException();
    }

    @Override
    public PlannedWorkout generateWorkout(TrainingPlanningContext context) {
        throw new TrainingPlanningUnavailableException();
    }

    @Override
    public PlannedTrainingPlan adaptPlan(TrainingPlanningContext context,
            PlannedTrainingPlan currentPlan, TrainingAdaptationContext adaptation) {
        throw new TrainingPlanningUnavailableException();
    }
}