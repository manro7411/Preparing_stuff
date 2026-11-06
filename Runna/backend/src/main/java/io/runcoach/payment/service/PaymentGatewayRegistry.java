package io.runcoach.payment.service;

import io.runcoach.common.exception.BillingException;
import io.runcoach.payment.entity.PaymentProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class PaymentGatewayRegistry {
    private final Instance<PaymentGateway> gateways;

    @Inject
    public PaymentGatewayRegistry(Instance<PaymentGateway> gateways) {
        this.gateways = gateways;
    }

    public PaymentGateway require(PaymentProvider provider) {
        var matches = gateways.stream().filter(gateway -> gateway.provider() == provider).toList();
        if (matches.size() != 1) {
            throw new BillingException(503, "PAYMENT_PROVIDER_UNAVAILABLE", "This payment provider is not configured.");
        }
        return matches.getFirst();
    }
}