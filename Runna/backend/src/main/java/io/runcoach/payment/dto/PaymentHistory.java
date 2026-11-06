package io.runcoach.payment.dto;

import java.util.List;

public record PaymentHistory(List<PaymentResponse> items, int page, int size) {
    public PaymentHistory {
        items = List.copyOf(items);
    }
}