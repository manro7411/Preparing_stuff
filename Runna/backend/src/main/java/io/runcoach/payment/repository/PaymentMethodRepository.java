package io.runcoach.payment.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.runcoach.payment.entity.PaymentMethod;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class PaymentMethodRepository implements PanacheRepositoryBase<PaymentMethod, UUID> {
}