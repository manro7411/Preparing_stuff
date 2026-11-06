package io.runcoach.common.dto;

public record ApiError(int status, String code, String message, String requestId) {
}