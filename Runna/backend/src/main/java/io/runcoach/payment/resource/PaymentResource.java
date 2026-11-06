package io.runcoach.payment.resource;

import io.quarkus.security.Authenticated;
import io.runcoach.common.dto.ApiError;
import io.runcoach.common.service.CurrentAccount;
import io.runcoach.payment.dto.PaymentHistory;
import io.runcoach.payment.dto.PaymentRequest;
import io.runcoach.payment.dto.PaymentResponse;
import io.runcoach.payment.dto.VerifyPaymentRequest;
import io.runcoach.payment.dto.WebhookRequest;
import io.runcoach.payment.entity.PaymentPurpose;
import io.runcoach.payment.service.PaymentService;
import io.runcoach.payment.service.WebhookService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/payments")
@Authenticated
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Payments")
@SecurityRequirement(name = "bearerAuth")
@APIResponse(responseCode = "400", description = "Invalid input")
@APIResponse(responseCode = "401", description = "JWT authentication required")
@APIResponse(responseCode = "404", description = "Owned payment or athlete profile not found")
@APIResponse(responseCode = "409", description = "Idempotency or payment state conflict")
@APIResponse(responseCode = "502", description = "Provider unavailable or receipt mismatch")
@APIResponse(responseCode = "503", description = "Price, policy or provider not configured",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
public class PaymentResource {
    @Inject CurrentAccount account;
    @Inject PaymentService payments;
    @Inject WebhookService webhooks;

    @POST
    @Path("/create")
    @Operation(summary = "Create an idempotent checkout using a server-side price")
    @APIResponse(responseCode = "202", description = "Checkout prepared, not proof of payment",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class)))
    public Response create(@NotNull @Valid PaymentRequest request) {
        return reply(payments.create(account.subject(), request, PaymentPurpose.SUBSCRIBE), 202);
    }

    @POST
    @Path("/verify")
    @Operation(summary = "Verify the current account's payment with its provider and atomically settle it")
    @APIResponse(responseCode = "200", description = "Provider-confirmed current payment state",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class)))
    public Response verify(@NotNull @Valid VerifyPaymentRequest request) {
        return reply(payments.verify(account.subject(), request.paymentId()), 200);
    }

    @POST
    @Path("/webhook")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Receive an ADMIN-authenticated relay of original provider-signed webhook bytes",
            description = "JWT does not replace provider signature/replay validation. Direct unauthenticated provider callbacks are not exposed.")
    @APIResponse(responseCode = "200", description = "Verified webhook processed or replayed idempotently",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class)))
    @APIResponse(responseCode = "403", description = "ADMIN role required")
    @APIResponse(responseCode = "413", description = "Webhook payload exceeds the size limit")
    public Response webhook(@NotNull @Valid WebhookRequest request) {
        return reply(webhooks.handle(request), 200);
    }

    @GET
    @Path("/history")
    @Operation(summary = "Read a bounded page of the current account's payments")
    @APIResponse(responseCode = "200", description = "Owned payment history",
            content = @Content(schema = @Schema(implementation = PaymentHistory.class)))
    public Response history(@QueryParam("page") @DefaultValue("0") @Min(0) @Max(10000) int page,
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size) {
        return reply(payments.history(account.subject(), page, size), 200);
    }

    private Response reply(Object body, int status) {
        return Response.status(status).entity(body).header("Cache-Control", "private, no-store")
                .header("Vary", "Authorization").build();
    }
}