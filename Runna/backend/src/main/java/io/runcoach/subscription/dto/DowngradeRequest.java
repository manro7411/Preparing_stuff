package io.runcoach.subscription.dto;

import io.runcoach.subscription.entity.PlanCode;
import jakarta.validation.constraints.NotNull;

public record DowngradeRequest(@NotNull PlanCode plan) {
}