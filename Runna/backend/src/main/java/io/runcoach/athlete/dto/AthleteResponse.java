package io.runcoach.athlete.dto;

import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "AthleteResponse", description = "Public fields of the authenticated athlete's profile")
public record AthleteResponse(
	@Schema(readOnly = true) UUID id,
	@Schema(maxLength = 120) String displayName,
	@Schema(maxLength = 64) String timeZone) {
}