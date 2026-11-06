package io.runcoach.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import io.runcoach.ai.dto.CoachContext;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiCoachServiceTest {
    @Test
    @DisplayName("Scaffold does not call AI or calculate a training plan")
    void shouldRejectExplanationUntilPipelineImplemented() {
        OpenAiClient client = mock(OpenAiClient.class);
        AiCoachService service = new AiCoachService(client, new PromptBuilder(), new EmptyKnowledgeBaseService());
        CoachContext context = new CoachContext(UUID.randomUUID(), "en", List.of());

        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> service.explain(context));

        assertEquals(501, exception.getResponse().getStatus());
        verifyNoInteractions(client);
    }
}