package io.runcoach.goal.entity;

import io.runcoach.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "goals")
public class GoalEntity extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    public UUID userId;

    @Column(nullable = false, length = 160)
    public String name;

    @Column(name = "target_date")
    public LocalDate targetDate;

    @Column(name = "distance_meters", precision = 12, scale = 2)
    public BigDecimal distanceMeters;

    @Column(name = "target_duration_seconds")
    public Integer targetDurationSeconds;
}