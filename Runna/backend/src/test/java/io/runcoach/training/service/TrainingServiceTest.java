package io.runcoach.training.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class TrainingServiceTest {
    private static final UUID ATHLETE_ID = UUID.randomUUID();
    private static final UUID GOAL_ID = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2026, 11, 3);
    private static final LocalDate END = LocalDate.of(2026, 12, 3);

    @Mock
    AthleteService athleteService;
    @Mock
    TrainingPlanner planner;
    TrainingService service;

    @BeforeEach
    void setUp() {
        service = new DefaultTrainingService(athleteService, planner, new TrainingValidator());
    }

    @Test
    @DisplayName("Planning uses the authenticated athlete ID and stored time zone")
    void shouldBuildOwnedPlanningContext() {
        when(athleteService.findCurrent("subject-1")).thenReturn(athlete());
        var draft = new PlannedTrainingPlan(GOAL_ID, START, END, "test-engine", List.of());
        when(planner.generatePlan(any())).thenReturn(draft);

        var result = service.generatePlan("subject-1", new GenerateTrainingPlanRequest(GOAL_ID, START, END));

        assertSame(draft, result);
        verify(planner).generatePlan(new TrainingPlanningContext(ATHLETE_ID, GOAL_ID, "Asia/Bangkok", START, END));
    }

    @Test
    @DisplayName("Weekly context spans seven inclusive dates without choosing workouts")
    void shouldBuildSevenDayWindow() {
        when(athleteService.findCurrent("subject-1")).thenReturn(athlete());
        var draft = new PlannedWeek(GOAL_ID, START, "test-engine", List.of());
        when(planner.generateWeek(any())).thenReturn(draft);

        var result = service.generateWeek("subject-1", new GenerateWeeklyPlanRequest(GOAL_ID, START));

        assertSame(draft, result);
        verify(planner).generateWeek(new TrainingPlanningContext(ATHLETE_ID, GOAL_ID, "Asia/Bangkok", START, START.plusDays(6)));
    }

    @Test
    @DisplayName("Single-workout context covers only the requested date")
    void shouldBuildWorkoutWindow() {
        when(athleteService.findCurrent("subject-1")).thenReturn(athlete());
        var draft = new PlannedWorkout(START, "test-type", null, null);
        when(planner.generateWorkout(any())).thenReturn(draft);

        var result = service.generateWorkout("subject-1", new GenerateWorkoutRequest(GOAL_ID, START));

        assertSame(draft, result);
        verify(planner).generateWorkout(new TrainingPlanningContext(ATHLETE_ID, GOAL_ID, "Asia/Bangkok", START, START));
    }

    @Test
    @DisplayName("Inverted date windows are rejected before profile lookup or planning")
    void shouldRejectInvalidWindow() {
        var exception = assertThrows(BadRequestException.class,
                () -> service.generatePlan("subject-1", new GenerateTrainingPlanRequest(GOAL_ID, END, START)));

        assertEquals(400, exception.getResponse().getStatus());
        verifyNoInteractions(athleteService, planner);
    }

    @Test
    @DisplayName("Overflowing weekly dates are rejected before profile lookup or planning")
    void shouldRejectOverflowingWeek() {
        var exception = assertThrows(BadRequestException.class,
                () -> service.generateWeek("subject-1", new GenerateWeeklyPlanRequest(GOAL_ID, LocalDate.MAX)));

        assertEquals(400, exception.getResponse().getStatus());
        verifyNoInteractions(athleteService, planner);
    }

    @Test
    @DisplayName("A missing athlete cannot reach the planner")
    void shouldRequireAthleteProfile() {
        when(athleteService.findCurrent("subject-1")).thenThrow(new NotFoundException());

        var exception = assertThrows(NotFoundException.class,
                () -> service.generatePlan("subject-1", new GenerateTrainingPlanRequest(GOAL_ID, START, END)));

        assertEquals(404, exception.getResponse().getStatus());
        verifyNoInteractions(planner);
    }

    @Test
    @DisplayName("The unavailable planner is not replaced with fabricated output")
    void shouldPropagateUnavailablePlanner() {
        when(athleteService.findCurrent("subject-1")).thenReturn(athlete());
        var unavailable = new TrainingPlanningUnavailableException();
        when(planner.generatePlan(any())).thenThrow(unavailable);

        var exception = assertThrows(TrainingPlanningUnavailableException.class,
                () -> service.generatePlan("subject-1", new GenerateTrainingPlanRequest(GOAL_ID, START, END)));

        assertSame(unavailable, exception);
    }

    private static AthleteResponse athlete() {
        return new AthleteResponse(ATHLETE_ID, "Runner", "Asia/Bangkok");
    }
}