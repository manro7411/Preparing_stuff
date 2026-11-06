package io.runcoach.common.exception;

public class BillingException extends RuntimeException {
    private final int status;
    private final String code;

    public BillingException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }
}