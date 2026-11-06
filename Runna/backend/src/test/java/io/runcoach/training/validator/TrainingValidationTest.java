package io.runcoach.training.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import io.runcoach.training.dto.GenerateTrainingPlanRequest;
import io.runcoach.training.dto.GenerateWeeklyPlanRequest;
import io.runcoach.training.dto.GenerateWorkoutRequest;
import io.runcoach.training.entity.TrainingPlanEntity;
import io.runcoach.training.entity.WorkoutEntity;
import io.runcoach.training.service.TrainingService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unused")
class TrainingValidationTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.byProvider(HibernateValidator.class).configure()
                .messageInterpolator(new ParameterMessageInterpolator()).buildValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    @DisplayName("All plan-generation fields are mandatory")
    void shouldRequirePlanInputs() {
        assertEquals(3, validator.validate(new GenerateTrainingPlanRequest(null, null, null)).size());
    }

    @Test
    @DisplayName("Week and workout requests require a goal and date")
    void shouldRequireWeekAndWorkoutInputs() {
        assertEquals(2, validator.validate(new GenerateWeeklyPlanRequest(null, null)).size());
        assertEquals(2, validator.validate(new GenerateWorkoutRequest(null, null)).size());
    }

    @Test
    @DisplayName("Interface-level service constraints remain available to non-HTTP callers")
    void shouldValidateServiceContractParameters() throws NoSuchMethodException {
        TrainingService service = mock(TrainingService.class);
        var method = TrainingService.class.getMethod("generatePlan", String.class, GenerateTrainingPlanRequest.class);

        var missingBody = validator.forExecutables().validateParameters(service, method, new Object[]{"subject-1", null});
        var invalidBody = validator.forExecutables().validateParameters(service, method,
                new Object[]{"subject-1", new GenerateTrainingPlanRequest(null, null, null)});

        assertEquals(1, missingBody.size());
        assertEquals(3, invalidBody.size());
    }

    @Test
    @DisplayName("Plan entities require ownership, dates and an engine version")
    void shouldRequirePlanEntityFields() {
        assertEquals(4, validator.validate(new TrainingPlanEntity()).size());
    }

    @Test
    @DisplayName("Plan entity date ranges match the database check constraint")
    void shouldRejectInvertedEntityDates() {
        TrainingPlanEntity entity = plan();
        entity.endDate = entity.startDate.minusDays(1);
        assertFalse(validator.validate(entity).isEmpty());

        entity.endDate = entity.startDate;
        assertTrue(validator.validate(entity).isEmpty());
    }

    @Test
    @DisplayName("Goal association remains nullable for existing persisted plans")
    void shouldPreserveNullableGoalMapping() {
        TrainingPlanEntity entity = plan();

        assertTrue(validator.validate(entity).isEmpty());
    }

    @Test
    @DisplayName("Workout entities require owner, plan, date and type")
    void shouldRequireWorkoutEntityFields() {
        assertEquals(4, validator.validate(new WorkoutEntity()).size());
    }

    @Test
    @DisplayName("Workout distances and durations cannot be negative")
    void shouldRejectNegativeWorkoutMetrics() {
        WorkoutEntity entity = workout();
        entity.distanceMeters = new BigDecimal("-0.01");
        entity.durationSeconds = -1;

        assertEquals(2, validator.validate(entity).size());
    }

    @Test
    @DisplayName("Optional workout metrics preserve existing nullable schema semantics")
    void shouldAllowAbsentOrZeroWorkoutMetrics() {
        WorkoutEntity entity = workout();
        assertTrue(validator.validate(entity).isEmpty());

        entity.distanceMeters = BigDecimal.ZERO;
        entity.durationSeconds = 0;
        assertTrue(validator.validate(entity).isEmpty());
    }

    @Test
    @DisplayName("Workout distance precision matches NUMERIC(12,2)")
    void shouldRejectExcessiveDistancePrecision() {
        WorkoutEntity entity = workout();
        entity.distanceMeters = new BigDecimal("10000000000.00");
        assertFalse(validator.validate(entity).isEmpty());

        entity.distanceMeters = new BigDecimal("1.001");
        assertFalse(validator.validate(entity).isEmpty());
    }

    @Test
    @DisplayName("Engine versions and workout types respect database string lengths")
    void shouldRejectOversizedMetadata() {
        TrainingPlanEntity plan = plan();
        plan.engineVersion = "a".repeat(65);
        WorkoutEntity workout = workout();
        workout.workoutType = "a".repeat(65);

        assertFalse(validator.validate(plan).isEmpty());
        assertFalse(validator.validate(workout).isEmpty());
    }

    private static TrainingPlanEntity plan() {
        TrainingPlanEntity entity = new TrainingPlanEntity();
        entity.userId = UUID.randomUUID();
        entity.startDate = LocalDate.of(2026, 11, 3);
        entity.endDate = entity.startDate.plusDays(7);
        entity.engineVersion = "test-engine";
        return entity;
    }

    private static WorkoutEntity workout() {
        WorkoutEntity entity = new WorkoutEntity();
        entity.userId = UUID.randomUUID();
        entity.trainingPlanId = UUID.randomUUID();
        entity.scheduledDate = LocalDate.of(2026, 11, 3);
        entity.workoutType = "test-type";
        return entity;
    }
}