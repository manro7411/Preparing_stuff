package io.runcoach.training.entity;

import io.runcoach.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "training_plans")
public class TrainingPlanEntity extends BaseEntity {
    @NotNull
    @Column(name = "user_id", nullable = false, updatable = false)
    public UUID userId;

    @Column(name = "goal_id")
    public UUID goalId;

    @NotNull
    @Column(name = "start_date", nullable = false)
    public LocalDate startDate;

    @NotNull
    @Column(name = "end_date", nullable = false)
    public LocalDate endDate;

    @NotBlank
    @Size(max = 64)
    @Column(name = "engine_version", nullable = false, length = 64)
    public String engineVersion;

    @AssertTrue(message = "endDate must be on or after startDate")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}