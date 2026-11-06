package io.runcoach.ai.service;

import io.runcoach.ai.dto.CoachContext;
import java.util.List;

public interface KnowledgeBaseService {
    List<KnowledgeSnippet> retrieve(CoachContext context);

    record KnowledgeSnippet(String sourceId, String text) {
    }
}