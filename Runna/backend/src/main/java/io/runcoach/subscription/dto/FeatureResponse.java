package io.runcoach.subscription.dto;

import io.runcoach.subscription.entity.Feature;
import io.runcoach.subscription.entity.PlanCode;
import java.util.Set;

public record FeatureResponse(PlanCode effectivePlan, Set<Feature> features) {
    public FeatureResponse {
        features = Set.copyOf(features);
    }
}