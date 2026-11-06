package io.runcoach.common.resource;

import io.runcoach.common.dto.ApiError;
import io.runcoach.common.exception.BillingException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.UUID;

@Provider
public class BillingExceptionMapper implements ExceptionMapper<BillingException> {
    @Override
    public Response toResponse(BillingException exception) {
        return Response.status(exception.status()).type(MediaType.APPLICATION_JSON_TYPE)
                .header("Cache-Control", "private, no-store")
                .entity(new ApiError(exception.status(), exception.code(), exception.getMessage(), UUID.randomUUID().toString()))
                .build();
    }
}