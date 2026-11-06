package io.runcoach.subscription.service;

import io.runcoach.common.exception.BillingException;
import io.runcoach.payment.dto.PaymentRequest;
import io.runcoach.subscription.entity.BillingCycle;
import io.runcoach.subscription.entity.PlanCode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import org.eclipse.microprofile.config.Config;

@ApplicationScoped
public class BillingService {
    private final Config config;

    @Inject
    public BillingService(Config config) {
        this.config = config;
    }

    public void requirePolicy() {
        if (!"PERIOD_END_NO_PRORATION".equals(config.getOptionalValue("runcoach.billing.policy", String.class).orElse("UNCONFIGURED"))) {
            throw new BillingException(503, "BILLING_POLICY_UNCONFIGURED", "Billing lifecycle policy has not been configured.");
        }
    }

    public long quote(PaymentRequest request) {
        requirePolicy();
        if (request.plan() == PlanCode.FREE) {
            throw new BillingException(400, "FREE_PLAN_NOT_BILLABLE", "The FREE plan does not require payment.");
        }
        String key = "runcoach.billing.prices." + request.plan().name().toLowerCase(Locale.ROOT) + "."
                + request.billingCycle().name().toLowerCase(Locale.ROOT) + "." + request.currency().name().toLowerCase(Locale.ROOT);
        long amount = config.getOptionalValue(key, Long.class).orElse(0L);
        if (amount <= 0) {
            throw new BillingException(503, "PRICE_UNCONFIGURED", "A server-side price is required for this plan and currency.");
        }
        return amount;
    }

    public Instant periodEnd(Instant start, BillingCycle cycle) {
        var date = start.atZone(ZoneOffset.UTC);
        return (cycle == BillingCycle.MONTHLY ? date.plusMonths(1) : date.plusYears(1)).toInstant();
    }
}