package io.runcoach.ai.dto;

public record OpenAiRequest(String model, String instructions, String input) {
}