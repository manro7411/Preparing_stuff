package io.runcoach.training.resource;

import io.quarkus.security.Authenticated;
import io.runcoach.training.dto.GenerateTrainingPlanRequest;
import io.runcoach.training.dto.GenerateWeeklyPlanRequest;
import io.runcoach.training.dto.GenerateWorkoutRequest;
import io.runcoach.training.dto.PlannedTrainingPlan;
import io.runcoach.training.dto.PlannedWeek;
import io.runcoach.training.dto.PlannedWorkout;
import io.runcoach.training.service.TrainingService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/training")
@Authenticated
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Training")
@SecurityRequirement(name = "bearerAuth")
@APIResponse(responseCode = "400", description = "Missing or invalid planning inputs")
@APIResponse(responseCode = "401", description = "A valid bearer token is required")
@APIResponse(responseCode = "404", description = "Athlete profile has not been created")
@APIResponse(responseCode = "501", description = "Training generation is intentionally not implemented")
public class TrainingResource {
    private final TrainingService service;
    private final JsonWebToken principal;

    @Inject
    public TrainingResource(TrainingService service, JsonWebToken principal) {
        this.service = service;
        this.principal = principal;
    }

    @POST
    @Path("/plans/generate")
    @Operation(summary = "Request a goal-based training plan draft",
            description = "Contract only. The default planner returns 501 and does not persist a plan.")
    @APIResponse(responseCode = "200", description = "Reserved response for a future implemented planner",
            content = @Content(schema = @Schema(implementation = PlannedTrainingPlan.class)))
    public Response generatePlan(@NotNull @Valid GenerateTrainingPlanRequest request) {
        return draftResponse(service.generatePlan(principal.getSubject(), request));
    }

    @POST
    @Path("/weeks/generate")
    @Operation(summary = "Request a seven-day training draft",
            description = "Contract only. The default planner returns 501 without generating workouts.")
    @APIResponse(responseCode = "200", description = "Reserved response for a future implemented planner",
            content = @Content(schema = @Schema(implementation = PlannedWeek.class)))
    public Response generateWeek(@NotNull @Valid GenerateWeeklyPlanRequest request) {
        return draftResponse(service.generateWeek(principal.getSubject(), request));
    }

    @POST
    @Path("/workouts/generate")
    @Operation(summary = "Request a goal-based workout draft",
            description = "Contract only. The default planner returns 501 without prescribing a workout.")
    @APIResponse(responseCode = "200", description = "Reserved response for a future implemented planner",
            content = @Content(schema = @Schema(implementation = PlannedWorkout.class)))
    public Response generateWorkout(@NotNull @Valid GenerateWorkoutRequest request) {
        return draftResponse(service.generateWorkout(principal.getSubject(), request));
    }

    private Response draftResponse(Object draft) {
        return Response.ok(draft).header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header(HttpHeaders.VARY, HttpHeaders.AUTHORIZATION).build();
    }
}