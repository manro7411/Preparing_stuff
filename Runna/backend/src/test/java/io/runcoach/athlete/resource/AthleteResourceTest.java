package io.runcoach.athlete.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import io.runcoach.athlete.dto.AthleteSnapshot;
import io.runcoach.athlete.dto.CreateAthleteRequest;
import io.runcoach.athlete.dto.UpdateAthleteRequest;
import io.runcoach.athlete.service.AthleteService;
import jakarta.ws.rs.WebApplicationException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(AthleteHttpTestProfile.class)
class AthleteResourceTest {
    private static final String PATH = "/api/v1/athletes/me";
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @InjectMock
    AthleteService service;

    @Test
    @DisplayName("Anonymous callers cannot access athlete profiles")
    void shouldRejectUnauthenticatedRequest() {
        given().when().get(PATH).then().statusCode(401);
        given().contentType("application/json").body(createBody()).when().post(PATH).then().statusCode(401);
        given().contentType("application/json").header("If-Match", "\"0\"")
                .body(Map.of("displayName", "Runner", "timeZone", "UTC"))
                .when().put(PATH).then().statusCode(401);
        verifyNoInteractions(service);
    }

    @Test
    @TestSecurity(user = "runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "subject-1"))
    @DisplayName("GET preserves the profile response and returns private concurrency metadata")
    void shouldReturnCurrentAthleteWithEtag() {
        when(service.findCurrentSnapshot("subject-1")).thenReturn(snapshot(2));

        given().when().get(PATH).then().statusCode(200)
                .header("ETag", "\"2\"")
                .header("Cache-Control", containsString("no-store"))
                .header("Vary", containsString("Authorization"))
                .body("id", equalTo(ID.toString()))
                .body("displayName", equalTo("Runner"))
                .body("timeZone", equalTo("UTC"))
                .body("$", not(hasKey("email")))
                .body("$", not(hasKey("authSubject")))
                .body("$", not(hasKey("version")));
        verify(service).findCurrentSnapshot("subject-1");
    }

    @Test
    @TestSecurity(user = "runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "subject-1"))
    @DisplayName("POST returns 201, canonical Location and the initial version tag")
    void shouldCreateAthleteUsingAuthenticatedSubject() {
        when(service.createCurrent(eq("subject-1"), any(CreateAthleteRequest.class))).thenReturn(snapshot(0));

        given().contentType("application/json").body(createBody()).when().post(PATH).then()
                .statusCode(201).header("Location", endsWith(PATH)).header("ETag", "\"0\"")
                .body("id", equalTo(ID.toString()));
        verify(service).createCurrent("subject-1", new CreateAthleteRequest("runner@example.com", "Runner", "UTC"));
    }

    @Test
    @TestSecurity(user = "runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "subject-1"))
    @DisplayName("Invalid create fields are rejected before the service is invoked")
    void shouldRejectInvalidCreateRequest() {
        given().contentType("application/json")
                .body(Map.of("email", "invalid", "displayName", " ", "timeZone", "Unknown/Zone"))
                .when().post(PATH).then().statusCode(400);
        verifyNoInteractions(service);
    }

    @Test
    @TestSecurity(user = "runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "subject-1"))
    @DisplayName("A null JSON request body is rejected")
    void shouldRejectNullCreateBody() {
        given().contentType("application/json").body("null").when().post(PATH).then().statusCode(400);
        verifyNoInteractions(service);
    }

    @Test
    @TestSecurity(user = "runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "subject-1"))
    @DisplayName("PUT forwards the strong version and returns the version produced by persistence")
    void shouldUpdateAthleteWithIfMatch() {
        when(service.updateCurrent(eq("subject-1"), any(UpdateAthleteRequest.class), eq(2L))).thenReturn(snapshot(3));

        given().contentType("application/json").header("If-Match", "\"2\"")
                .body(Map.of("displayName", "Runner", "timeZone", "UTC"))
                .when().put(PATH).then().statusCode(200).header("ETag", "\"3\"");
        verify(service).updateCurrent("subject-1", new UpdateAthleteRequest("Runner", "UTC"), 2);
    }

    @Test
    @TestSecurity(user = "runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "subject-1"))
    @DisplayName("PUT without If-Match is rejected before mutation")
    void shouldRequireUpdatePrecondition() {
        given().contentType("application/json").body(Map.of("displayName", "Runner", "timeZone", "UTC"))
                .when().put(PATH).then().statusCode(428);
        verifyNoInteractions(service);
    }

    @Test
    @TestSecurity(user = "runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "subject-1"))
    @DisplayName("Invalid update fields are rejected before the service is invoked")
    void shouldRejectInvalidUpdateRequest() {
        given().contentType("application/json").header("If-Match", "\"0\"")
                .body(Map.of("displayName", " ", "timeZone", "Unknown/Zone"))
                .when().put(PATH).then().statusCode(400);
        verifyNoInteractions(service);
    }

    @Test
    @TestSecurity(user = "runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "subject-1"))
    @DisplayName("A stale update is returned as HTTP 412")
    void shouldReturnPreconditionFailedWhenVersionStale() {
        when(service.updateCurrent(eq("subject-1"), any(UpdateAthleteRequest.class), eq(0L)))
                .thenThrow(new WebApplicationException("Stale profile", 412));

        given().contentType("application/json").header("If-Match", "\"0\"")
                .body(Map.of("displayName", "Runner", "timeZone", "UTC"))
                .when().put(PATH).then().statusCode(412);
    }

    @Test
    @TestSecurity(user = "runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "subject-1"))
    @DisplayName("Duplicate creation is returned as HTTP 409")
    void shouldReturnConflictWhenProfileExists() {
        when(service.createCurrent(eq("subject-1"), any(CreateAthleteRequest.class)))
                .thenThrow(new WebApplicationException("Already exists", 409));

        given().contentType("application/json").body(createBody()).when().post(PATH).then().statusCode(409);
    }

    private static Map<String, String> createBody() {
        return Map.of("email", "runner@example.com", "displayName", "Runner", "timeZone", "UTC");
    }

    private static AthleteSnapshot snapshot(long version) {
        return new AthleteSnapshot(new AthleteResponse(ID, "Runner", "UTC"), version);
    }
}