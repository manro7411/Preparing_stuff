package io.runcoach.athlete.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IanaTimeZoneValidator.class)
public @interface IanaTimeZone {
    String message() default "must be a supported named time zone";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}