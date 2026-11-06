package io.runcoach.training.entity;

import io.runcoach.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "workouts")
public class WorkoutEntity extends BaseEntity {
    @NotNull
    @Column(name = "user_id", nullable = false, updatable = false)
    public UUID userId;

    @NotNull
    @Column(name = "training_plan_id", nullable = false, updatable = false)
    public UUID trainingPlanId;

    @NotNull
    @Column(name = "scheduled_date", nullable = false)
    public LocalDate scheduledDate;

    @NotBlank
    @Size(max = 64)
    @Column(name = "workout_type", nullable = false, length = 64)
    public String workoutType;

    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "distance_meters", precision = 12, scale = 2)
    public BigDecimal distanceMeters;

    @PositiveOrZero
    @Column(name = "duration_seconds")
    public Integer durationSeconds;
}