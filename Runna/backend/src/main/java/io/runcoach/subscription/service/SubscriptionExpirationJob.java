package io.runcoach.subscription.service;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SubscriptionExpirationJob {
    private static final Logger LOG = Logger.getLogger(SubscriptionExpirationJob.class);
    @Inject
    SubscriptionService subscriptions;

    @Scheduled(every = "60s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @SuppressWarnings("unused")
    void expire() {
        for (var accountId : subscriptions.dueAccounts()) {
            try {
                subscriptions.expireAccount(accountId);
            } catch (RuntimeException exception) {
                LOG.error("Subscription expiration failed; the next scheduled run will retry");
            }
        }
    }
}