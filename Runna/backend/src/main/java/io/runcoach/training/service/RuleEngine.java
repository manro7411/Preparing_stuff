package io.runcoach.training.service;

import io.runcoach.training.dto.PlannedTrainingPlan;
import io.runcoach.training.dto.TrainingPlanningContext;
import java.util.List;

public interface RuleEngine {
    RuleEvaluation evaluate(TrainingPlanningContext context, PlannedTrainingPlan candidate);

    record RuleEvaluation(List<RuleViolation> violations) {
        public RuleEvaluation {
            violations = List.copyOf(violations);
        }
    }

    record RuleViolation(String code, String message) {
    }
}