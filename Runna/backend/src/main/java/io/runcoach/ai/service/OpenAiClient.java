package io.runcoach.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.runcoach.ai.dto.OpenAiRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/responses")
@RegisterRestClient(configKey = "openai")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface OpenAiClient {
    @POST
    JsonNode explain(OpenAiRequest request, @HeaderParam("Authorization") String authorization);
}