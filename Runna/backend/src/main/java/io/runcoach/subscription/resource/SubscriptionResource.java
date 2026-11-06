package io.runcoach.subscription.resource;

import io.quarkus.security.Authenticated;
import io.runcoach.common.dto.ApiError;
import io.runcoach.common.service.CurrentAccount;
import io.runcoach.payment.dto.PaymentRequest;
import io.runcoach.payment.dto.PaymentResponse;
import io.runcoach.payment.entity.PaymentPurpose;
import io.runcoach.payment.service.PaymentService;
import io.runcoach.subscription.dto.DowngradeRequest;
import io.runcoach.subscription.dto.FeatureResponse;
import io.runcoach.subscription.dto.SubscriptionResponse;
import io.runcoach.subscription.service.FeatureAccessService;
import io.runcoach.subscription.service.SubscriptionService;
import io.runcoach.subscription.service.TrialService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/subscriptions")
@Authenticated
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Subscriptions")
@SecurityRequirement(name = "bearerAuth")
@APIResponse(responseCode = "400", description = "Invalid request or transition")
@APIResponse(responseCode = "401", description = "JWT authentication required")
@APIResponse(responseCode = "404", description = "Athlete profile is not provisioned")
@APIResponse(responseCode = "409", description = "Subscription state conflicts with the requested action")
@APIResponse(responseCode = "503", description = "Billing configuration or provider is unavailable",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
public class SubscriptionResource {
    @Inject CurrentAccount account;
    @Inject SubscriptionService subscriptions;
    @Inject TrialService trials;
    @Inject FeatureAccessService features;
    @Inject PaymentService payments;

    @POST
    @Path("/trial")
    @Operation(summary = "Start the account's one-time fourteen-day PRO trial")
    @APIResponse(responseCode = "200", description = "Trial started",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class)))
    public Response trial() {
        return reply(trials.start(account.subject()), 200);
    }

    @GET
    @Path("/me")
    @Operation(summary = "Read the current account's subscription, refreshing expiration")
    @APIResponse(responseCode = "200", description = "Current subscription",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class)))
    public Response current() {
        return reply(subscriptions.current(account.subject()), 200);
    }

    @GET
    @Path("/features")
    @Operation(summary = "Read effective features, without granting expired entitlements")
    @APIResponse(responseCode = "200", description = "Effective feature access",
            content = @Content(schema = @Schema(implementation = FeatureResponse.class)))
    public Response features() {
        return reply(features.features(), 200);
    }

    @POST
    @Path("/subscribe")
    @Operation(summary = "Create a monthly or yearly checkout; activation requires verified payment")
    @APIResponse(responseCode = "202", description = "Checkout prepared",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class)))
    public Response subscribe(@NotNull @Valid PaymentRequest request) {
        return reply(payments.create(account.subject(), request, PaymentPurpose.SUBSCRIBE), 202);
    }

    @POST
    @Path("/upgrade")
    @Operation(summary = "Create an upgrade checkout, including conversion of an unexpired trial")
    @APIResponse(responseCode = "202", description = "Upgrade checkout prepared",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class)))
    public Response upgrade(@NotNull @Valid PaymentRequest request) {
        return reply(payments.create(account.subject(), request, PaymentPurpose.UPGRADE), 202);
    }

    @POST
    @Path("/renew")
    @Operation(summary = "Create a renewal checkout; no entitlement is extended before verified payment")
    @APIResponse(responseCode = "202", description = "Renewal checkout prepared",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class)))
    public Response renew(@NotNull @Valid PaymentRequest request) {
        return reply(payments.create(account.subject(), request, PaymentPurpose.RENEW), 202);
    }

    @POST
    @Path("/downgrade")
    @Operation(summary = "Schedule a lower plan at period end under the configured lifecycle policy")
    @APIResponse(responseCode = "200", description = "Downgrade scheduled",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class)))
    public Response downgrade(@NotNull @Valid DowngradeRequest request) {
        return reply(subscriptions.downgrade(account.subject(), request.plan()), 200);
    }

    @POST
    @Path("/cancel")
    @Operation(summary = "Cancel at period end under the configured lifecycle policy")
    @APIResponse(responseCode = "200", description = "Cancellation scheduled",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class)))
    public Response cancel() {
        return reply(subscriptions.cancel(account.subject()), 200);
    }

    @POST
    @Path("/restore")
    @Operation(summary = "Reverse an unexpired pending cancellation, not restore an app-store purchase")
    @APIResponse(responseCode = "200", description = "Pending cancellation reversed",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class)))
    public Response restore() {
        return reply(subscriptions.restore(account.subject()), 200);
    }

    private Response reply(Object body, int status) {
        return Response.status(status).entity(body).header("Cache-Control", "private, no-store")
                .header("Vary", "Authorization").build();
    }
}