package io.runcoach.subscription.entity;

import io.runcoach.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "subscription_plan")
public class SubscriptionPlan extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 16)
    public PlanCode code;

    @Column(name = "display_name", nullable = false, length = 64)
    public String displayName;
}