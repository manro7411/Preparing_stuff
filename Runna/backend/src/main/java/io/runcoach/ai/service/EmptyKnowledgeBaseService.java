package io.runcoach.ai.service;

import io.runcoach.ai.dto.CoachContext;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class EmptyKnowledgeBaseService implements KnowledgeBaseService {
    @Override
    public List<KnowledgeSnippet> retrieve(CoachContext context) {
        return List.of();
    }
}