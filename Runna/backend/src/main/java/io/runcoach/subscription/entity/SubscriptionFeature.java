package io.runcoach.subscription.entity;

import io.runcoach.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "subscription_feature")
public class SubscriptionFeature extends BaseEntity {
    @Column(name = "plan_id", nullable = false)
    public UUID planId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    public Feature feature;
}