package io.runcoach.payment.entity;

import io.runcoach.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_webhook_log")
public class WebhookLog extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, updatable = false)
    public PaymentProvider provider;
    @Column(name = "event_id", nullable = false, length = 255, updatable = false)
    public String eventId;
    @Column(name = "payment_id", nullable = false, updatable = false)
    public UUID paymentId;
    @Column(name = "payload_hash", nullable = false, length = 64, updatable = false)
    public String payloadHash;
    @Column(name = "processed_at", nullable = false, updatable = false)
    public Instant processedAt;
}