package io.runcoach.athlete.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.ZoneId;
import java.util.Set;

public class IanaTimeZoneValidator implements ConstraintValidator<IanaTimeZone, String> {
    private static final Set<String> ZONE_IDS = ZoneId.getAvailableZoneIds();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.isBlank() || ZONE_IDS.contains(value);
    }
}