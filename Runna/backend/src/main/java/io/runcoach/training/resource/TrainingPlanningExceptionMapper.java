package io.runcoach.training.resource;

import io.runcoach.common.dto.ApiError;
import io.runcoach.training.service.TrainingPlanningUnavailableException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.UUID;

@Provider
public class TrainingPlanningExceptionMapper implements ExceptionMapper<TrainingPlanningUnavailableException> {
    @Override
    public Response toResponse(TrainingPlanningUnavailableException exception) {
        return Response.status(501).type(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .entity(new ApiError(501, "TRAINING_GENERATION_NOT_IMPLEMENTED",
                        "Training generation is not available yet.", UUID.randomUUID().toString())).build();
    }
}