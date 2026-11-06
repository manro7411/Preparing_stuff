package io.runcoach.athlete.dto;

import io.runcoach.athlete.validator.IanaTimeZone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CreateAthleteRequest", description = "The authenticated athlete's initial profile")
public record CreateAthleteRequest(
        @NotBlank @Email @Size(max = 320)
        @Schema(description = "Contact email; not a verified identity claim", examples = {"runner@example.com"}, maxLength = 320)
        String email,
        @NotBlank @Size(max = 120)
        @Schema(examples = {"Alex Runner"}, maxLength = 120)
        String displayName,
        @NotBlank @Size(max = 64) @IanaTimeZone
        @Schema(description = "Named time zone supported by the server's TZDB", examples = {"Asia/Bangkok"}, maxLength = 64)
        String timeZone) {
}