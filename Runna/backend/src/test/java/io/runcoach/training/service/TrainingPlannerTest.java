package io.runcoach.training.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.runcoach.training.dto.TrainingPlanningContext;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class TrainingPlannerTest {
    private final TrainingPlanner planner = new UnimplementedTrainingPlanner();
    private final TrainingPlanningContext context = new TrainingPlanningContext(
            UUID.randomUUID(), UUID.randomUUID(), "UTC", LocalDate.of(2026, 11, 3), LocalDate.of(2026, 12, 3));

    @Test
    @DisplayName("Plan generation does not invent an algorithm")
    void shouldRejectPlanGenerationUntilImplemented() {
        assertUnavailable(() -> planner.generatePlan(context));
    }

    @Test
    @DisplayName("Weekly generation does not invent a schedule")
    void shouldRejectWeeklyGenerationUntilImplemented() {
        assertUnavailable(() -> planner.generateWeek(context));
    }

    @Test
    @DisplayName("Workout generation does not invent a prescription")
    void shouldRejectWorkoutGenerationUntilImplemented() {
        assertUnavailable(() -> planner.generateWorkout(context));
    }

    @Test
    @DisplayName("Adaptive planning remains an unimplemented contract")
    void shouldRejectAdaptivePlanningUntilImplemented() {
        assertUnavailable(() -> planner.adaptPlan(context, null, null));
    }

    private static void assertUnavailable(Executable operation) {
        var exception = assertThrows(TrainingPlanningUnavailableException.class, operation);
        assertEquals("Training generation is not implemented", exception.getMessage());
    }
}