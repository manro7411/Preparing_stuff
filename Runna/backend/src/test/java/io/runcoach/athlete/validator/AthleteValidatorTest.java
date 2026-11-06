package io.runcoach.athlete.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.ws.rs.WebApplicationException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AthleteValidatorTest {
    private final AthleteValidator validator = new AthleteValidator();

    @Test
    @DisplayName("Subject is required and must fit the existing database mapping")
    void shouldRejectMissingOrOversizedSubject() {
        for (String subject : Arrays.asList(null, "", " ", "a".repeat(256))) {
            var exception = assertThrows(WebApplicationException.class, () -> validator.requireSubject(subject));
            assertEquals(401, exception.getResponse().getStatus());
            assertEquals("Bearer", exception.getResponse().getHeaderString("WWW-Authenticate"));
        }
        assertDoesNotThrow(() -> validator.requireSubject("a".repeat(255)));
    }

    @Test
    @DisplayName("A single quoted non-negative version is accepted")
    void shouldParseStrongVersionTag() {
        assertEquals(0, validator.requireVersion("\"0\""));
        assertEquals(42, validator.requireVersion(" \"42\" "));
        assertEquals(Long.MAX_VALUE, validator.requireVersion("\"" + Long.MAX_VALUE + "\""));
    }

    @Test
    @DisplayName("Missing If-Match requires the caller to retrieve the profile first")
    void shouldRequireIfMatch() {
        for (String header : Arrays.asList(null, "", " ")) {
            var exception = assertThrows(WebApplicationException.class, () -> validator.requireVersion(header));
            assertEquals(428, exception.getResponse().getStatus());
        }
    }

    @Test
    @DisplayName("Malformed, wildcard, multi-value and overflowing tags are rejected")
    void shouldRejectInvalidVersionTag() {
        for (String header : List.of("0", "*", "\"-1\"", "\"abc\"", "\"01\"", "\"0\", \"1\"", "\"9223372036854775808\"")) {
            var exception = assertThrows(WebApplicationException.class, () -> validator.requireVersion(header));
            assertEquals(400, exception.getResponse().getStatus(), header);
        }
    }

    @Test
    @DisplayName("Weak validators cannot satisfy an update precondition")
    void shouldRejectWeakVersionTag() {
        var exception = assertThrows(WebApplicationException.class, () -> validator.requireVersion("W/\"0\""));

        assertEquals(412, exception.getResponse().getStatus());
    }
}