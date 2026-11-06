package io.runcoach.athlete.resource;

import io.quarkus.security.Authenticated;
import io.runcoach.athlete.dto.AthleteResponse;
import io.runcoach.athlete.dto.AthleteSnapshot;
import io.runcoach.athlete.dto.CreateAthleteRequest;
import io.runcoach.athlete.dto.UpdateAthleteRequest;
import io.runcoach.athlete.service.AthleteService;
import io.runcoach.athlete.validator.AthleteValidator;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/athletes/me")
@Authenticated
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Athletes")
@SecurityRequirement(name = "bearerAuth")
@APIResponse(responseCode = "401", description = "A valid bearer token with a supported subject is required")
public class AthleteResource {
    private final AthleteService service;
    private final JsonWebToken principal;
    private final AthleteValidator validator;

    @Inject
    public AthleteResource(AthleteService service, JsonWebToken principal, AthleteValidator validator) {
        this.service = service;
        this.principal = principal;
    this.validator = validator;
    }

    @GET
    @Operation(summary = "Read the authenticated athlete profile")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Current profile",
            headers = @Header(name = "ETag", description = "Strong profile version tag for If-Match"),
            content = @Content(schema = @Schema(implementation = AthleteResponse.class))),
        @APIResponse(responseCode = "404", description = "Athlete profile has not been created")
    })
    public Response current() {
    return profileResponse(Response.ok(), service.findCurrentSnapshot(principal.getSubject()));
    }

    @POST
    @Operation(summary = "Create the authenticated athlete's profile",
        description = "Creates one profile per JWT subject. Contact email is self-reported, not identity verification.")
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Profile created",
            headers = {@Header(name = "Location", description = "Canonical self-profile URL"),
                @Header(name = "ETag", description = "Strong profile version tag")},
            content = @Content(schema = @Schema(implementation = AthleteResponse.class))),
        @APIResponse(responseCode = "400", description = "Missing or invalid profile fields"),
        @APIResponse(responseCode = "409", description = "A profile already exists for this identity")
    })
    public Response create(@NotNull @Valid CreateAthleteRequest request, @Context UriInfo uriInfo) {
    AthleteSnapshot snapshot = service.createCurrent(principal.getSubject(), request);
    return profileResponse(Response.created(uriInfo.getAbsolutePath()), snapshot);
    }

    @PUT
    @Operation(summary = "Replace the authenticated athlete's editable profile fields",
        description = "Send a single strong ETag from GET or POST in If-Match. Identity and email cannot be changed here.")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Profile updated",
            headers = @Header(name = "ETag", description = "Current strong profile version tag"),
            content = @Content(schema = @Schema(implementation = AthleteResponse.class))),
        @APIResponse(responseCode = "400", description = "Invalid profile fields or malformed If-Match"),
        @APIResponse(responseCode = "404", description = "Athlete profile has not been created"),
        @APIResponse(responseCode = "412", description = "Profile changed concurrently or a weak ETag was supplied"),
        @APIResponse(responseCode = "428", description = "If-Match is required")
    })
    public Response update(@NotNull @Valid UpdateAthleteRequest request,
        @HeaderParam(HttpHeaders.IF_MATCH)
        @Parameter(description = "One quoted version tag, for example \"0\"", required = true) String ifMatch) {
    long version = validator.requireVersion(ifMatch);
    return profileResponse(Response.ok(), service.updateCurrent(principal.getSubject(), request, version));
    }

    private Response profileResponse(Response.ResponseBuilder builder, AthleteSnapshot snapshot) {
    return builder.entity(snapshot.profile()).tag(Long.toString(snapshot.version()))
        .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
        .header(HttpHeaders.VARY, HttpHeaders.AUTHORIZATION).build();
    }
}