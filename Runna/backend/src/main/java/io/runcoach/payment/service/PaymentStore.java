package io.runcoach.payment.service;

import io.runcoach.athlete.service.AthleteService;
import io.runcoach.common.exception.BillingException;
import io.runcoach.payment.dto.PaymentRequest;
import io.runcoach.payment.dto.PaymentResponse;
import io.runcoach.payment.entity.PaymentProvider;
import io.runcoach.payment.entity.PaymentPurpose;
import io.runcoach.payment.entity.PaymentStatus;
import io.runcoach.payment.entity.PaymentTransaction;
import io.runcoach.payment.entity.WebhookLog;
import io.runcoach.payment.mapper.PaymentMapper;
import io.runcoach.payment.repository.PaymentTransactionRepository;
import io.runcoach.payment.repository.WebhookLogRepository;
import io.runcoach.payment.validator.PaymentValidator;
import io.runcoach.subscription.dto.SubscriptionCharge;
import io.runcoach.subscription.service.SubscriptionBillingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PaymentStore {
    @Inject
    AthleteService athletes;
    @Inject
    PaymentTransactionRepository payments;
    @Inject
    SubscriptionBillingService subscriptions;
    @Inject
    PaymentMapper mapper;
    @Inject
    PaymentValidator validator;
    @Inject
    InvoiceService invoices;
    @Inject
    WebhookLogRepository webhooks;
    @Inject
    Clock clock;

    public record View(UUID ownerId, PaymentGateway.CreateCommand command, String providerPaymentId,
            PaymentProvider provider, PaymentResponse response) {
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public View prepare(String subject, PaymentRequest request, PaymentPurpose purpose, long quote) {
        UUID accountId = athletes.lockForBilling(subject).id();
        var existing = payments.byKey(accountId, request.idempotencyKey());
        if (existing.isPresent()) {
            PaymentTransaction payment = existing.get();
            if (subscriptions.code(payment.targetPlanId) != request.plan() || payment.billingCycle != request.billingCycle()
                    || payment.currency != request.currency() || payment.provider != request.provider() || payment.purpose != purpose) {
                throw new BillingException(409, "IDEMPOTENCY_CONFLICT", "This idempotency key was used for a different request.");
            }
            return view(payment);
        }
        if (payments.count("userId = ?1 and status in ?2", accountId, List.of(PaymentStatus.CREATED, PaymentStatus.PENDING)) > 0) {
            throw new BillingException(409, "CHECKOUT_PENDING", "Finish or reconcile the account's existing checkout first.");
        }
        SubscriptionCharge charge = subscriptions.prepare(accountId, request, purpose);
        PaymentTransaction payment = new PaymentTransaction();
        payment.userId = accountId;
        payment.subscriptionId = charge.subscriptionId();
        payment.subscriptionVersion = charge.expectedVersion();
        payment.targetPlanId = charge.targetPlanId();
        payment.idempotencyKey = request.idempotencyKey();
        payment.provider = request.provider();
        payment.billingCycle = request.billingCycle();
        payment.currency = request.currency();
        payment.amountMinor = quote;
        payment.purpose = purpose;
        payment.status = PaymentStatus.CREATED;
        payments.persistAndFlush(payment);
        return view(payment);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public PaymentResponse checkout(UUID accountId, UUID paymentId, PaymentGateway.Checkout checkout) {
        athletes.lockAccountForBilling(accountId);
        var payment = payments.owned(accountId, paymentId, true);
        validator.checkout(checkout);
        if (payment.providerPaymentId != null && !payment.providerPaymentId.equals(checkout.providerPaymentId())) {
            throw new BillingException(502, "NON_IDEMPOTENT_PROVIDER", "The provider did not preserve checkout idempotency.");
        }
        payment.providerPaymentId = checkout.providerPaymentId();
        payment.checkoutUrl = checkout.checkoutUrl();
        if (payment.status == PaymentStatus.CREATED) {
            payment.status = PaymentStatus.PENDING;
        }
        payments.flush();
        return mapper.toResponse(payment);
    }

    @Transactional
    public View owned(String subject, UUID paymentId) {
        return view(payments.owned(athletes.findCurrent(subject).id(), paymentId, false));
    }

    @Transactional
    public View webhookView(PaymentProvider provider, UUID paymentId) {
        var payment = payments.findById(paymentId);
        if (payment == null || payment.provider != provider) {
            throw new BillingException(404, "PAYMENT_NOT_FOUND", "Payment not found.");
        }
        return view(payment);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public PaymentResponse settle(UUID accountId, UUID paymentId, PaymentGateway.Receipt receipt,
            String eventId, String payloadHash) {
        athletes.lockAccountForBilling(accountId);
        var payment = payments.owned(accountId, paymentId, true);
        validator.receipt(payment, receipt);
        if (eventId != null) {
            var previous = webhooks.byEvent(payment.provider, eventId);
            if (previous.isPresent()) {
                if (!previous.get().paymentId.equals(paymentId) || !previous.get().payloadHash.equals(payloadHash)) {
                    throw new BillingException(409, "WEBHOOK_REPLAY_CONFLICT", "The webhook event does not match its original content.");
                }
                return mapper.toResponse(payment);
            }
        }
        if (payment.status != PaymentStatus.SUCCESS && payment.status != PaymentStatus.REFUNDED) {
            if (receipt.status() == PaymentStatus.SUCCESS) {
                payment.status = PaymentStatus.SUCCESS;
                payment.paidAt = clock.instant();
                payment.subscriptionApplied = subscriptions.activate(new SubscriptionCharge(payment.userId, payment.subscriptionId,
                        payment.subscriptionVersion, payment.targetPlanId, subscriptions.code(payment.targetPlanId),
                        payment.billingCycle, payment.currency, payment.purpose, payment.paidAt));
                payment.reconciliationRequired = !payment.subscriptionApplied;
                invoices.issue(payment);
            } else if (receipt.status() == PaymentStatus.FAILED || payment.status != PaymentStatus.FAILED) {
                payment.status = receipt.status() == PaymentStatus.CREATED ? PaymentStatus.PENDING : receipt.status();
            }
        }
        if (eventId != null) {
            WebhookLog log = new WebhookLog();
            log.provider = payment.provider;
            log.eventId = eventId;
            log.paymentId = payment.id;
            log.payloadHash = payloadHash;
            log.processedAt = clock.instant();
            webhooks.persist(log);
        }
        payments.flush();
        return mapper.toResponse(payment);
    }

    @Transactional
    public List<PaymentResponse> history(String subject, int page, int size) {
        UUID owner = athletes.findCurrent(subject).id();
        return payments.find("userId = ?1 order by createdAt desc, id", owner).page(page, size).list()
                .stream().map(mapper::toResponse).toList();
    }

    private View view(PaymentTransaction payment) {
        return new View(payment.userId, new PaymentGateway.CreateCommand(payment.id, payment.userId,
                subscriptions.code(payment.targetPlanId), payment.billingCycle, payment.amountMinor,
                payment.currency, payment.purpose), payment.providerPaymentId, payment.provider, mapper.toResponse(payment));
    }
}