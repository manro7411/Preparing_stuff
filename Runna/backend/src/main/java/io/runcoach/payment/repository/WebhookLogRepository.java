package io.runcoach.payment.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.runcoach.payment.entity.PaymentProvider;
import io.runcoach.payment.entity.WebhookLog;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class WebhookLogRepository implements PanacheRepositoryBase<WebhookLog, UUID> {
    public Optional<WebhookLog> byEvent(PaymentProvider provider, String eventId) {
        return find("provider = ?1 and eventId = ?2", provider, eventId).firstResultOptional();
    }
}