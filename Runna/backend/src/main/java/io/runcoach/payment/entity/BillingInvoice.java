package io.runcoach.payment.entity;

import io.runcoach.common.dto.CurrencyCode;
import io.runcoach.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "billing_invoice")
public class BillingInvoice extends BaseEntity {
    @Column(name = "user_id", nullable = false, updatable = false)
    public UUID userId;
    @Column(name = "subscription_id", nullable = false, updatable = false)
    public UUID subscriptionId;
    @Column(name = "payment_id", nullable = false, unique = true, updatable = false)
    public UUID paymentId;
    @Column(name = "invoice_number", nullable = false, unique = true, length = 48, updatable = false)
    public String invoiceNumber;
    @Column(name = "amount_minor", nullable = false, updatable = false)
    public long amountMinor;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3, updatable = false)
    public CurrencyCode currency;
    @Column(name = "issued_at", nullable = false, updatable = false)
    public Instant issuedAt;
}