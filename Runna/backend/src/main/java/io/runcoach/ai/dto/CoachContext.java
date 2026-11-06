package io.runcoach.ai.dto;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CoachContext(UUID athleteId, String locale, List<EngineRecommendation> recommendations) {
    public CoachContext {
        Objects.requireNonNull(athleteId);
        Objects.requireNonNull(locale);
        recommendations = List.copyOf(recommendations);
    }

    public record EngineRecommendation(String engine, String engineVersion, String recommendation) {
    }
}