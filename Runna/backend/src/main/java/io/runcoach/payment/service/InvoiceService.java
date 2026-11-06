package io.runcoach.payment.service;

import io.runcoach.payment.entity.BillingInvoice;
import io.runcoach.payment.entity.PaymentTransaction;
import io.runcoach.payment.repository.InvoiceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.UUID;

@ApplicationScoped
public class InvoiceService {
    @Inject
    InvoiceRepository invoices;
    @Inject
    Clock clock;

    @Transactional(Transactional.TxType.MANDATORY)
    public void issue(PaymentTransaction payment) {
        if (invoices.byPayment(payment.id).isPresent()) {
            return;
        }
        BillingInvoice invoice = new BillingInvoice();
        invoice.userId = payment.userId;
        invoice.subscriptionId = payment.subscriptionId;
        invoice.paymentId = payment.id;
        invoice.invoiceNumber = "RC-" + UUID.randomUUID();
        invoice.amountMinor = payment.amountMinor;
        invoice.currency = payment.currency;
        invoice.issuedAt = clock.instant();
        invoices.persistAndFlush(invoice);
    }
}