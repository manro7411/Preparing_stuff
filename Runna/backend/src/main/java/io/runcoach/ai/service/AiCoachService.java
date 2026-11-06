package io.runcoach.ai.service;

import io.runcoach.ai.dto.CoachContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
@SuppressWarnings("unused")
public class AiCoachService {
    private final OpenAiClient client;
    private final PromptBuilder promptBuilder;
    private final KnowledgeBaseService knowledgeBase;

    @Inject
    public AiCoachService(@RestClient OpenAiClient client, PromptBuilder promptBuilder,
            KnowledgeBaseService knowledgeBase) {
        this.client = client;
        this.promptBuilder = promptBuilder;
        this.knowledgeBase = knowledgeBase;
    }

    public String explain(CoachContext context) {
        throw new WebApplicationException("AI explanation pipeline is not implemented", 501);
    }
}