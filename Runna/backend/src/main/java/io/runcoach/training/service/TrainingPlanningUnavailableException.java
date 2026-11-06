package io.runcoach.training.service;

public class TrainingPlanningUnavailableException extends RuntimeException {
    public TrainingPlanningUnavailableException() {
        super("Training generation is not implemented");
    }
}