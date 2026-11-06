package io.runcoach.billing;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
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
import io.runcoach.athlete.resource.AthleteHttpTestProfile;
import io.runcoach.common.exception.BillingException;
import io.runcoach.payment.dto.PaymentHistory;
import io.runcoach.payment.entity.PaymentPurpose;
import io.runcoach.payment.service.PaymentService;
import io.runcoach.payment.service.WebhookService;
import io.runcoach.subscription.service.TrialService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(AthleteHttpTestProfile.class)
class BillingResourceTest {
    @InjectMock PaymentService payments;
    @InjectMock WebhookService webhooks;
    @InjectMock TrialService trials;

    @Test
    @DisplayName("Billing routes cannot be called anonymously")
    void shouldRequireJwt() {
        given().when().get("/api/v1/subscriptions/me").then().statusCode(401);
        given().when().get("/api/v1/payments/history").then().statusCode(401);
        given().contentType("application/json").body(paymentBody()).when().post("/api/v1/payments/create").then().statusCode(401);
        verifyNoInteractions(payments, webhooks, trials);
    }

    @Test
    @TestSecurity(user = "runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "billing-subject"))
    @DisplayName("A normal account cannot impersonate the privileged webhook relay")
    void shouldRequireAdminForWebhook() {
        given().contentType("application/json").body(webhookBody()).when().post("/api/v1/payments/webhook").then().statusCode(403);
        verifyNoInteractions(webhooks);
    }

    @Test
    @TestSecurity(user = "relay", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "sub", value = "relay-subject"))
    @DisplayName("ADMIN authentication does not bypass provider verification")
    void shouldStillVerifyAdminWebhook() {
        when(webhooks.handle(any())).thenThrow(new BillingException(400, "INVALID_SIGNATURE", "Provider signature is invalid."));

        given().contentType("application/json").body(webhookBody()).when().post("/api/v1/payments/webhook").then()
                .statusCode(400).body("code", equalTo("INVALID_SIGNATURE"));
        verify(webhooks).handle(any());
    }

    @Test
    @TestSecurity(user = "runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "billing-subject"))
    @DisplayName("Payment history is scoped to the JWT subject")
    void shouldScopePaymentHistory() {
        when(payments.history("billing-subject", 0, 20)).thenReturn(new PaymentHistory(List.of(), 0, 20));

        given().when().get("/api/v1/payments/history").then().statusCode(200).body("items.size()", equalTo(0));
        verify(payments).history("billing-subject", 0, 20);
    }

    @Test
    @TestSecurity(user = "runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "billing-subject"))
    @DisplayName("Required payment fields are validated before service execution")
    void shouldRejectInvalidPaymentInput() {
        given().contentType("application/json").body(Map.of()).when().post("/api/v1/payments/create").then().statusCode(400);
        given().contentType("application/json").body("null").when().post("/api/v1/payments/verify").then().statusCode(400);
        given().when().get("/api/v1/payments/history?size=101").then().statusCode(400);
        verifyNoInteractions(payments);
    }

    @Test
    @TestSecurity(user = "runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "billing-subject"))
    @DisplayName("Unconfigured checkout returns a clear service error, not simulated success")
    void shouldExposeDisabledCheckout() {
        when(payments.create(eq("billing-subject"), any(), eq(PaymentPurpose.SUBSCRIBE)))
                .thenThrow(new BillingException(503, "PRICE_UNCONFIGURED", "Configure the price first."));

        given().contentType("application/json").body(paymentBody()).when().post("/api/v1/payments/create").then()
                .statusCode(503).body("code", equalTo("PRICE_UNCONFIGURED"));
    }

    @Test
    @TestSecurity(user = "runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "billing-subject"))
    @DisplayName("Duplicate trial conflicts retain an actionable application error code")
    void shouldExposeDuplicateTrial() {
        when(trials.start("billing-subject")).thenThrow(new BillingException(409, "TRIAL_ALREADY_USED", "Trial already used."));

        given().contentType("application/json").when().post("/api/v1/subscriptions/trial").then()
                .statusCode(409).body("code", equalTo("TRIAL_ALREADY_USED"));
    }

    private static Map<String, String> paymentBody() {
        return Map.of("plan", "PRO", "billingCycle", "MONTHLY", "currency", "USD", "provider", "STRIPE",
                "idempotencyKey", UUID.randomUUID().toString());
    }

    private static Map<String, String> webhookBody() {
        return Map.of("provider", "STRIPE", "payloadBase64", "e30=", "signature", "invalid-test-signature");
    }
}