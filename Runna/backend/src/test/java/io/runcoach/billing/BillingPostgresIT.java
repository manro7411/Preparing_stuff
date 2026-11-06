package io.runcoach.billing;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.runcoach.athlete.entity.AthleteEntity;
import io.runcoach.billing.support.BillingDatabaseProfile;
import io.runcoach.billing.support.BillingPostgresResource;
import io.runcoach.billing.support.BillingTestClock;
import io.runcoach.billing.support.BillingTestGateway;
import io.runcoach.common.dto.CurrencyCode;
import io.runcoach.common.exception.BillingException;
import io.runcoach.payment.dto.PaymentRequest;
import io.runcoach.payment.entity.PaymentProvider;
import io.runcoach.payment.entity.PaymentPurpose;
import io.runcoach.payment.repository.InvoiceRepository;
import io.runcoach.payment.repository.PaymentTransactionRepository;
import io.runcoach.payment.service.PaymentService;
import io.runcoach.subscription.entity.BillingCycle;
import io.runcoach.subscription.entity.PlanCode;
import io.runcoach.subscription.service.TrialService;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(BillingDatabaseProfile.class)
@QuarkusTestResource(value = BillingPostgresResource.class, restrictToAnnotatedClass = true)
@TestSecurity(user = "db-runner")
@JwtSecurity(claims = @Claim(key = "sub", value = "billing-db-owner"))
@SuppressWarnings("unused")
class BillingPostgresIT {
    private static final Instant START = Instant.parse("2026-11-03T10:00:00Z");
    @Inject EntityManager entities;
    @Inject DataSource database;
    @Inject BillingTestClock clock;
    @Inject BillingTestGateway gateway;
    @Inject TrialService trials;
    @Inject PaymentService payments;
    @Inject PaymentTransactionRepository transactions;
    @Inject InvoiceRepository invoices;
    UUID ownerId;

    @BeforeEach
    void reset() {
        clock.set(START);
        gateway.reset();
        QuarkusTransaction.requiringNew().run(() -> {
            entities.createNativeQuery("TRUNCATE billing_invoice, payment_webhook_log, payment_transaction, payment_method, user_subscription, users CASCADE")
                    .executeUpdate();
            ownerId = createAccount("billing-db-owner");
        });
    }

    @Test
    @DisplayName("Trial creation, duplicate prevention and exact expiry use real PostgreSQL")
    void shouldExpireTrialAndRemovePaidFeatures() {
        given().contentType("application/json").when().post("/api/v1/subscriptions/trial").then()
                .statusCode(200).body("plan", equalTo("PRO")).body("status", equalTo("TRIAL"))
                .body("trialEndsAt", equalTo("2026-11-17T10:00:00Z"));
        given().contentType("application/json").when().post("/api/v1/subscriptions/trial").then().statusCode(409);
        given().when().get("/api/v1/subscriptions/features").then().statusCode(200)
                .body("features.size()", equalTo(6)).body("features", hasItem("AI_COACH"));

        clock.set(START.plus(Duration.ofDays(14)));

        given().when().get("/api/v1/subscriptions/me").then().statusCode(200).body("status", equalTo("EXPIRED"));
        given().when().get("/api/v1/subscriptions/features").then().statusCode(200)
                .body("effectivePlan", equalTo("FREE")).body("features.size()", equalTo(2))
                .body("features", not(hasItem("AI_COACH")));
    }

    @Test
    @DisplayName("Concurrent first-trial requests serialize on the account row")
    void shouldPreventConcurrentTrials() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var requests = new ArrayList<java.util.concurrent.Future<Integer>>();
            for (int attempt = 0; attempt < 2; attempt++) {
                requests.add(executor.submit(() -> {
                    barrier.await(10, TimeUnit.SECONDS);
                    try {
                        trials.start("billing-db-owner");
                        return 200;
                    } catch (BillingException exception) {
                        return exception.status();
                    }
                }));
            }
            List<Integer> statuses = new ArrayList<>();
            for (var request : requests) {
                statuses.add(request.get(20, TimeUnit.SECONDS));
            }
            assertEquals(List.of(200, 409), statuses.stream().sorted().toList());
        }
    }

    @Test
    @DisplayName("Monthly payment stays pending until verified, then activates and invoices exactly once")
    void shouldActivateVerifiedMonthlyPaymentOnlyOnce() {
        UUID paymentId = checkout("/api/v1/payments/create", PlanCode.PRO, BillingCycle.MONTHLY, UUID.randomUUID());
        verifyPayment(paymentId).then().statusCode(200).body("status", equalTo("PENDING"))
                .body("subscriptionApplied", equalTo(false));
        given().when().get("/api/v1/subscriptions/me").then().body("plan", equalTo("FREE"));

        gateway.succeed(paymentId);
        verifyPayment(paymentId).then().statusCode(200).body("status", equalTo("SUCCESS"))
                .body("subscriptionApplied", equalTo(true)).body("reconciliationRequired", equalTo(false));
        verifyPayment(paymentId).then().statusCode(200).body("subscriptionApplied", equalTo(true));

        given().when().get("/api/v1/subscriptions/me").then().body("plan", equalTo("PRO"))
                .body("periodEnd", equalTo("2026-12-03T10:00:00Z"));
        assertEquals(1L, QuarkusTransaction.requiringNew().call(() -> invoices.count("paymentId", paymentId)));
    }

    @Test
    @DisplayName("Yearly renewal extends the existing paid end date only after payment")
    void shouldExtendPaidPeriodWithYearlyRenewal() {
        paid(PlanCode.PRO);
        UUID renewal = checkout("/api/v1/subscriptions/renew", PlanCode.PRO, BillingCycle.YEARLY, UUID.randomUUID());
        gateway.succeed(renewal);

        verifyPayment(renewal).then().statusCode(200).body("subscriptionApplied", equalTo(true));

        given().when().get("/api/v1/subscriptions/me").then().body("periodEnd", equalTo("2027-12-03T10:00:00Z"));
    }

    @Test
    @DisplayName("Trial upgrade preserves trial history and grants inherited ELITE features")
    void shouldUpgradeTrialWithoutResettingEligibility() {
        given().contentType("application/json").when().post("/api/v1/subscriptions/trial").then().statusCode(200);
        UUID upgrade = checkout("/api/v1/subscriptions/upgrade", PlanCode.ELITE, BillingCycle.MONTHLY, UUID.randomUUID());
        gateway.succeed(upgrade);
        verifyPayment(upgrade).then().statusCode(200).body("subscriptionApplied", equalTo(true));

        given().when().get("/api/v1/subscriptions/me").then().body("plan", equalTo("ELITE"))
                .body("trialEligible", equalTo(false)).body("trialStartedAt", equalTo("2026-11-03T10:00:00Z"));
        given().when().get("/api/v1/subscriptions/features").then().body("features.size()", equalTo(9))
                .body("features", hasItem("ADVANCED_ANALYTICS")).body("features", hasItem("TRAINING_PLANS"));
    }

    @Test
    @DisplayName("Idempotency keys preserve one checkout and reject changed request payloads")
    void shouldReuseCheckoutWithoutCreatingAnotherPayment() {
        UUID key = UUID.randomUUID();
        UUID first = checkout("/api/v1/payments/create", PlanCode.PRO, BillingCycle.MONTHLY, key);
        UUID second = checkout("/api/v1/payments/create", PlanCode.PRO, BillingCycle.MONTHLY, key);

        assertEquals(first, second);
        assertEquals(1L, QuarkusTransaction.requiringNew().call(() -> transactions.count()));
        given().contentType("application/json").body(body(PlanCode.ELITE, BillingCycle.MONTHLY, key))
                .when().post("/api/v1/payments/create").then().statusCode(409).body("code", equalTo("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    @TestSecurity(user = "other-runner")
    @JwtSecurity(claims = @Claim(key = "sub", value = "other-billing-account"))
    @DisplayName("Another account cannot verify or list the owner's payment")
    void shouldEnforcePaymentOwnership() {
        QuarkusTransaction.requiringNew().run(() -> createAccount("other-billing-account"));
        var payment = payments.create("billing-db-owner", new PaymentRequest(PlanCode.PRO, BillingCycle.MONTHLY,
                CurrencyCode.USD, PaymentProvider.STRIPE, UUID.randomUUID()), PaymentPurpose.SUBSCRIBE);

        verifyPayment(payment.id()).then().statusCode(404);
        given().when().get("/api/v1/payments/history").then().statusCode(200).body("items.size()", equalTo(0));
    }

    @Test
    @TestSecurity(user = "relay", roles = "ADMIN")
    @JwtSecurity(claims = @Claim(key = "sub", value = "billing-db-owner"))
    @DisplayName("Provider-signed webhooks are idempotent and changed replay content is rejected")
    void shouldVerifyAndDeduplicateWebhooks() {
        UUID paymentId = checkout("/api/v1/payments/create", PlanCode.PRO, BillingCycle.MONTHLY, UUID.randomUUID());
        gateway.succeed(paymentId);
        String payload = "{\"eventId\":\"evt-1\",\"paymentId\":\"" + paymentId + "\"}";
        Map<String, String> envelope = webhook(payload);

        given().contentType("application/json").body(envelope).when().post("/api/v1/payments/webhook").then().statusCode(200);
        given().contentType("application/json").body(envelope).when().post("/api/v1/payments/webhook").then().statusCode(200);
        given().contentType("application/json").body(webhook(payload + " ")).when().post("/api/v1/payments/webhook").then()
                .statusCode(409).body("code", equalTo("WEBHOOK_REPLAY_CONFLICT"));
        assertEquals(1L, QuarkusTransaction.requiringNew().call(() -> invoices.count("paymentId", paymentId)));
    }

    @Test
    @DisplayName("The database trigger prevents clearing a consumed trial marker")
    void shouldPreserveTrialHistoryInDatabase() throws SQLException {
        trials.start("billing-db-owner");
        try (var connection = database.getConnection();
                var statement = connection.prepareStatement("UPDATE user_subscription SET trial_started_at = NULL, trial_ends_at = NULL WHERE user_id = ?")) {
            statement.setObject(1, ownerId);

            SQLException exception = assertThrows(SQLException.class, statement::executeUpdate);

            assertEquals("23514", exception.getSQLState());
        }
    }

    @Test
    @DisplayName("Cancel/restore and unpaid downgrade honor the explicitly enabled preset")
    void shouldRestoreThenExpireUnpaidDowngrade() {
        paid(PlanCode.ELITE);
        given().contentType("application/json").when().post("/api/v1/subscriptions/cancel").then()
                .statusCode(200).body("cancelAtPeriodEnd", equalTo(true));
        given().contentType("application/json").when().post("/api/v1/subscriptions/restore").then()
                .statusCode(200).body("cancelAtPeriodEnd", equalTo(false));
        given().contentType("application/json").body(Map.of("plan", "PRO")).when().post("/api/v1/subscriptions/downgrade")
                .then().statusCode(200).body("plan", equalTo("ELITE")).body("pendingPlan", equalTo("PRO"));
        clock.set(Instant.parse("2026-12-03T10:00:00Z"));

        given().when().get("/api/v1/subscriptions/me").then().body("plan", equalTo("PRO")).body("status", equalTo("EXPIRED"));
        given().when().get("/api/v1/subscriptions/features").then().body("effectivePlan", equalTo("FREE"));
    }

    private UUID createAccount(String subject) {
        AthleteEntity athlete = new AthleteEntity();
        athlete.authSubject = subject;
        athlete.email = subject + "@example.test";
        athlete.displayName = "Test Runner";
        athlete.timeZone = "UTC";
        entities.persist(athlete);
        entities.flush();
        return athlete.id;
    }

    private UUID checkout(String path, PlanCode plan, BillingCycle cycle, UUID key) {
        String id = given().contentType("application/json").body(body(plan, cycle, key)).when().post(path).then()
                .statusCode(202).extract().path("id");
        return UUID.fromString(id);
    }

    private void paid(PlanCode plan) {
        UUID paymentId = checkout("/api/v1/payments/create", plan, BillingCycle.MONTHLY, UUID.randomUUID());
        gateway.succeed(paymentId);
        verifyPayment(paymentId).then().statusCode(200).body("subscriptionApplied", equalTo(true));
    }

    private io.restassured.response.Response verifyPayment(UUID paymentId) {
        return given().contentType("application/json").body(Map.of("paymentId", paymentId))
                .when().post("/api/v1/payments/verify");
    }

    private Map<String, Object> body(PlanCode plan, BillingCycle cycle, UUID key) {
        return Map.of("plan", plan, "billingCycle", cycle, "currency", "USD", "provider", "STRIPE", "idempotencyKey", key);
    }

    private Map<String, String> webhook(String payload) {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        return Map.of("provider", "STRIPE", "payloadBase64", Base64.getEncoder().encodeToString(bytes), "signature", gateway.sign(bytes));
    }
}