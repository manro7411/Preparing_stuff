package io.runcoach.billing.support;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

@Alternative
@ApplicationScoped
public class BillingTestClock extends Clock {
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-03T10:00:00Z"));

    public void set(Instant instant) { now.set(instant); }
    @Override public Instant instant() { return now.get(); }
    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
}