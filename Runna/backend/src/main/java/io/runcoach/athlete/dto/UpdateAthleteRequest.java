package io.runcoach.athlete.dto;

import io.runcoach.athlete.validator.IanaTimeZone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "UpdateAthleteRequest", description = "Replace editable profile fields; identity and contact email are unchanged")
public record UpdateAthleteRequest(
        @NotBlank @Size(max = 120)
        @Schema(examples = {"Alex Runner"}, maxLength = 120)
        String displayName,
        @NotBlank @Size(max = 64) @IanaTimeZone
        @Schema(examples = {"Asia/Bangkok"}, maxLength = 64)
        String timeZone) {
}