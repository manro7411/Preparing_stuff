package io.runcoach.athlete.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.runcoach.athlete.dto.CreateAthleteRequest;
import io.runcoach.athlete.dto.UpdateAthleteRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unused")
class AthleteValidationTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.byProvider(HibernateValidator.class).configure()
                .messageInterpolator(new ParameterMessageInterpolator()).buildValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    @DisplayName("A valid profile accepts named time zones and UTC")
    void shouldAcceptValidProfile() {
        for (String zone : List.of("UTC", "Asia/Bangkok", "America/New_York")) {
            assertTrue(validator.validate(new CreateAthleteRequest("runner@example.com", "Runner", zone)).isEmpty());
        }
    }

    @Test
    @DisplayName("All create fields are mandatory")
    void shouldRejectMissingCreateFields() {
        var violations = validator.validate(new CreateAthleteRequest(null, null, null));

        Set<String> paths = violations.stream().map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
        assertEquals(Set.of("email", "displayName", "timeZone"), paths);
    }

    @Test
    @DisplayName("Blank profile values are invalid")
    void shouldRejectBlankFields() {
        assertEquals(3, validator.validate(new CreateAthleteRequest("", " ", " ")).size());
    }

    @Test
    @DisplayName("Contact email must have a valid address shape")
    void shouldRejectMalformedEmail() {
        assertFalse(validator.validate(new CreateAthleteRequest("not-an-email", "Runner", "UTC")).isEmpty());
    }

    @Test
    @DisplayName("Field lengths match database column limits")
    void shouldRejectOversizedFields() {
        assertFalse(validator.validate(new CreateAthleteRequest("a".repeat(310) + "@example.com", "Runner", "UTC")).isEmpty());
        assertFalse(validator.validate(new UpdateAthleteRequest("a".repeat(121), "UTC")).isEmpty());
        assertFalse(validator.validate(new UpdateAthleteRequest("Runner", "a".repeat(65))).isEmpty());
    }

    @Test
    @DisplayName("Unknown zones and fixed offsets are not accepted as named time zones")
    void shouldRejectUnsupportedTimeZones() {
        for (String zone : List.of("Mars/Olympus", "+07:00", " Asia/Bangkok ")) {
            assertFalse(validator.validate(new UpdateAthleteRequest("Runner", zone)).isEmpty());
        }
    }

    @Test
    @DisplayName("Updating a profile requires both editable fields")
    void shouldRejectMissingUpdateFields() {
        assertEquals(2, validator.validate(new UpdateAthleteRequest(null, null)).size());
    }
}