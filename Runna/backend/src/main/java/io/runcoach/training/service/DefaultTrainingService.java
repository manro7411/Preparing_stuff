package io.runcoach.training.service;

import io.runcoach.athlete.dto.AthleteResponse;
import io.runcoach.athlete.service.AthleteService;
import io.runcoach.training.dto.GenerateTrainingPlanRequest;
import io.runcoach.training.dto.GenerateWeeklyPlanRequest;
import io.runcoach.training.dto.GenerateWorkoutRequest;
import io.runcoach.training.dto.PlannedTrainingPlan;
import io.runcoach.training.dto.PlannedWeek;
import io.runcoach.training.dto.PlannedWorkout;
import io.runcoach.training.dto.TrainingPlanningContext;
import io.runcoach.training.validator.TrainingValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.UUID;

@ApplicationScoped
public class DefaultTrainingService implements TrainingService {
    private final AthleteService athleteService;
    private final TrainingPlanner planner;
    private final TrainingValidator validator;

    @Inject
    public DefaultTrainingService(AthleteService athleteService, TrainingPlanner planner, TrainingValidator validator) {
        this.athleteService = athleteService;
        this.planner = planner;
        this.validator = validator;
    }

    @Override
    public PlannedTrainingPlan generatePlan(String subject, GenerateTrainingPlanRequest request) {
        validator.requireValidRange(request.startDate(), request.endDate());
        return planner.generatePlan(context(subject, request.goalId(), request.startDate(), request.endDate()));
    }

    @Override
    public PlannedWeek generateWeek(String subject, GenerateWeeklyPlanRequest request) {
        LocalDate endDate = validator.endOfWeek(request.weekStart());
        return planner.generateWeek(context(subject, request.goalId(), request.weekStart(), endDate));
    }

    @Override
    public PlannedWorkout generateWorkout(String subject, GenerateWorkoutRequest request) {
        validator.requireValidRange(request.scheduledDate(), request.scheduledDate());
        return planner.generateWorkout(context(subject, request.goalId(), request.scheduledDate(), request.scheduledDate()));
    }

    private TrainingPlanningContext context(String subject, UUID goalId, LocalDate startDate, LocalDate endDate) {
        AthleteResponse athlete = athleteService.findCurrent(subject);
        return new TrainingPlanningContext(athlete.id(), goalId, athlete.timeZone(), startDate, endDate);
    }
}