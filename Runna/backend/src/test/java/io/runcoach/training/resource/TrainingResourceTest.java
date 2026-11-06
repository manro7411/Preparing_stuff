package io.runcoach.training.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.runcoach.athlete.dto.AthleteResponse;
import io.runcoach.athlete.resource.AthleteHttpTestProfile;
import io.runcoach.athlete.service.AthleteService;
import jakarta.ws.rs.NotFoundException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(AthleteHttpTestProfile.class)
class TrainingResourceTest {
    private static final String BASE = "/api/v1/training";
    private static final String GOAL_ID = "00000000-0000-0000-0000-000000000002";

    @InjectMock
    AthleteService athleteService;

    @Test
    @DisplayName("All generation routes require authentication")
    void shouldRejectAnonymousRequests() {
        given().contentType("application/json").body(planBody()).when().post(BASE + "/plans/generate")
                .then().statusCode(401);
        given().contentType("application/json").body(Map.of("goalId", GOAL_ID, "weekStart", "2026-11-03"))
                .when().post(BASE + "/weeks/generate").then().statusCode(401);
        given().contentType("application/json").body(Map.of("goalId", GOAL_ID, "scheduledDate", "2026-11-03"))
                .when().post(BASE + "/workouts/generate").then().statusCode(401);
        verifyNoInteractions(athleteService);
    }

    @Test
    @TestSecurity(user = "runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "subject-1"))
    @DisplayName("Valid plan requests return a stable unavailable response rather than fabricated data")
    void shouldReturnNotImplementedForPlan() {
        givenAthlete();

        given().contentType("application/json").body(planBody()).when().post(BASE + "/plans/generate")
                .then().statusCode(501).header("Cache-Control", containsString("no-store"))
                .body("status", equalTo(501))
                .body("code", equalTo("TRAINING_GENERATION_NOT_IMPLEMENTED"))
                .body("message", equalTo("Training generation is not available yet."))
                .body("requestId", notNullValue());
        verify(athleteService).findCurrent("subject-1");
    }

    @Test
    @TestSecurity(user = "runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "subject-1"))
    @DisplayName("Weekly requests also reach the real unimplemented planner")
    void shouldReturnNotImplementedForWeek() {
        givenAthlete();

        given().contentType("application/json").body(Map.of("goalId", GOAL_ID, "weekStart", "2026-11-03"))
                .when().post(BASE + "/weeks/generate").then().statusCode(501)
                .body("code", equalTo("TRAINING_GENERATION_NOT_IMPLEMENTED"));
        verify(athleteService).findCurrent("subject-1");
    }

    @Test
    @TestSecurity(user = "runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "subject-1"))
    @DisplayName("Workout requests do not create a prescription")
    void shouldReturnNotImplementedForWorkout() {
        givenAthlete();

        given().contentType("application/json").body(Map.of("goalId", GOAL_ID, "scheduledDate", "2026-11-03"))
                .when().post(BASE + "/workouts/generate").then().statusCode(501)
                .body("code", equalTo("TRAINING_GENERATION_NOT_IMPLEMENTED"));
        verify(athleteService).findCurrent("subject-1");
    }

    @Test
    @TestSecurity(user = "runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "subject-1"))
    @DisplayName("Required fields are validated before profile lookup")
    void shouldRejectMissingFields() {
        given().contentType("application/json").body(Map.of()).when().post(BASE + "/plans/generate")
                .then().statusCode(400);
        given().contentType("application/json").body(Map.of()).when().post(BASE + "/weeks/generate")
                .then().statusCode(400);
        given().contentType("application/json").body(Map.of()).when().post(BASE + "/workouts/generate")
                .then().statusCode(400);
        verifyNoInteractions(athleteService);
    }

    @Test
    @TestSecurity(user = "runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "subject-1"))
    @DisplayName("Inverted date windows return a client error without looking up an athlete")
    void shouldRejectInvalidPlanWindow() {
        given().contentType("application/json")
                .body(Map.of("goalId", GOAL_ID, "startDate", "2026-12-03", "endDate", "2026-11-03"))
                .when().post(BASE + "/plans/generate").then().statusCode(400);
        verifyNoInteractions(athleteService);
    }

    @Test
    @TestSecurity(user = "runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "subject-1"))
    @DisplayName("Malformed goal identifiers are rejected before orchestration")
    void shouldRejectInvalidGoalId() {
        given().contentType("application/json")
                .body(Map.of("goalId", "not-a-uuid", "scheduledDate", "2026-11-03"))
                .when().post(BASE + "/workouts/generate").then().statusCode(400);
        verifyNoInteractions(athleteService);
    }

    @Test
    @TestSecurity(user = "runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "subject-1"))
    @DisplayName("Null request bodies are rejected")
    void shouldRejectNullBody() {
        given().contentType("application/json").body("null").when().post(BASE + "/plans/generate")
                .then().statusCode(400);
        verifyNoInteractions(athleteService);
    }

    @Test
    @TestSecurity(user = "runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "subject-1"))
    @DisplayName("A valid identity without a profile cannot enter planning")
    void shouldRequireAthleteProfile() {
        when(athleteService.findCurrent("subject-1")).thenThrow(new NotFoundException());

        given().contentType("application/json").body(planBody()).when().post(BASE + "/plans/generate")
                .then().statusCode(404);
        verify(athleteService).findCurrent("subject-1");
    }

    private void givenAthlete() {
        when(athleteService.findCurrent("subject-1"))
                .thenReturn(new AthleteResponse(UUID.randomUUID(), "Runner", "UTC"));
    }

    private static Map<String, String> planBody() {
        return Map.of("goalId", GOAL_ID, "startDate", "2026-11-03", "endDate", "2026-12-03");
    }
}