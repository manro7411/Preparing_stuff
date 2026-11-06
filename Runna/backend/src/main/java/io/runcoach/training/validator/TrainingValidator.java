package io.runcoach.training.validator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import java.time.DateTimeException;
import java.time.LocalDate;

@ApplicationScoped
public class TrainingValidator {
    public void requireValidRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new BadRequestException("A start date and an end date on or after it are required");
        }
    }

    public LocalDate endOfWeek(LocalDate weekStart) {
        if (weekStart == null) {
            throw new BadRequestException("A week start date is required");
        }
        try {
            return weekStart.plusDays(6);
        } catch (DateTimeException exception) {
            throw new BadRequestException("The seven-day window exceeds the supported date range");
        }
    }
}