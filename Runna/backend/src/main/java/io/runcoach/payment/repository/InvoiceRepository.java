package io.runcoach.payment.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.runcoach.payment.entity.BillingInvoice;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class InvoiceRepository implements PanacheRepositoryBase<BillingInvoice, UUID> {
    public Optional<BillingInvoice> byPayment(UUID paymentId) {
        return find("paymentId", paymentId).firstResultOptional();
    }
}