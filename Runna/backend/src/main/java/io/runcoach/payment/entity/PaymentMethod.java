package io.runcoach.payment.entity;

import io.runcoach.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "payment_method")
public class PaymentMethod extends BaseEntity {
    @Column(name = "user_id", nullable = false, updatable = false)
    public UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, updatable = false)
    public PaymentProvider provider;
    @Column(name = "provider_method_id", nullable = false, length = 255, updatable = false)
    public String providerMethodId;
    @Column(nullable = false)
    public boolean active;
}