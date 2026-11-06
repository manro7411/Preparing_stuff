package io.runcoach.cache.service;

import java.util.Objects;
import java.util.UUID;

public final class CacheKeys {
    public enum Scope {
        DASHBOARD("dashboard"), PROFILE("profile"), READINESS("readiness"),
        NUTRITION("nutrition"), TRAINING_PLAN("training-plan");

        private final String segment;

        Scope(String segment) {
            this.segment = segment;
        }
    }

    private CacheKeys() {
    }

    public static String forAthlete(Scope scope, UUID athleteId) {
        return "runcoach:v1:" + Objects.requireNonNull(scope).segment
                + ":athlete:" + Objects.requireNonNull(athleteId);
    }
}