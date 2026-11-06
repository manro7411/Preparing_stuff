package io.runcoach.exception;

import io.runcoach.common.dto.ApiError;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.UUID;
import org.jboss.logging.Logger;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger LOG = Logger.getLogger(ApiExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        if (exception == null) {
            throw new IllegalArgumentException("Exception must not be null");
        }
        Exception failure = exception;
        String requestId = UUID.randomUUID().toString();
        int status = failure instanceof WebApplicationException webException
                ? webException.getResponse().getStatus() : 500;
        String message = status >= 500 ? "Service unavailable. Try again later." : "Request could not be completed.";
        if (status >= 500) {
            LOG.errorf("Request %s failed (%s)", requestId, failure.getClass());
        }
        Response.ResponseBuilder response = failure instanceof WebApplicationException webException
                ? Response.fromResponse(webException.getResponse()) : Response.status(status);
        return response.type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ApiError(status, "HTTP_" + status, message, requestId)).build();
    }
}