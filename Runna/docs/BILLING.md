# Subscription And Payment Modules

The implementation is in `backend/src/main/java/io/runcoach/subscription`,
`payment`, and shared `common` packages. REST resources delegate to application
services; repositories use Panache; DTOs isolate transport from JPA entities.
No provider credentials, card numbers, security codes, or raw webhook payloads
are stored or logged.

## Implemented Behavior

- FREE, PRO, ELITE catalog with inherited features seeded by Flyway.
- One 14-day PRO trial per account. Trial lasts exactly 336 hours from start.
- Trial eligibility survives upgrades, expiry and cancellation; database triggers
  prevent clearing or deleting consumed trial history.
- JWT subjects resolve accounts through the athlete module. Client requests
  cannot set account IDs, payment amounts, provider success, or entitlements.
- Account-row locks serialize subscription mutations and initial trial creation.
- A 60-second job persists expiry in bounded batches; reads and feature access
  also refresh expiry. Access ends at the exact boundary even before a job runs.
- Only current TRIAL/ACTIVE paid periods grant paid features. EXPIRED,
  CANCELLED and PAST_DUE accounts fall back to FREE features.
- Checkout preparation and settlement have their own database transactions;
  provider calls execute outside them. Payment IDs are stable provider
  idempotency keys. Only one open checkout per account is allowed.
- Verified receipt must match internal ID, provider ID, amount and currency.
- SUCCESS settlement records payment, activates/renews and creates one invoice
  atomically. Repeated verification cannot extend access or duplicate invoices.
- Changed subscription versions do not overwrite newer state: captured money
  is recorded with `reconciliationRequired=true` for operational resolution.

`trialEligible` is informational; eligibility is checked again under the account
lock. The schema uses one durable subscription aggregate per account rather than
deleting/recreating it for each billing period. Subscription version checks may
reject a late receipt after cancellation, expiration, downgrade or another change;
an approved reconciliation process must handle the captured funds.

## Features

| Plan | Features |
| --- | --- |
| FREE | BASIC_DASHBOARD, ACTIVITY_TRACKING |
| PRO | FREE + TRAINING_PLANS, NUTRITION_COACH, AI_COACH, RACE_PREDICTION |
| ELITE | PRO + ADAPTIVE_TRAINING, UNLIMITED_AI_COACH, ADVANCED_ANALYTICS |

`FeatureAccessService` provides `canUseAiCoach()`, `canUseTrainingPlan()`,
`canUseAdvancedAnalytics()` and `canUseNutritionCoach()` for the authenticated
request. Existing unimplemented training/AI engines remain unavailable; billing
does not magically enable their algorithms. Call these guards at the owning
feature's application boundary when those features are implemented. The current
catalog is authorization, not quota metering; AI usage limits are future work.

## Configuration And Policy

Trial and FREE features do not require checkout configuration. Paid checkout,
cancellation, restore and downgrade default to **503** until a lifecycle policy
is explicitly approved. No custom policy was supplied, so it is not guessed.

The available opt-in preset is:

```properties
runcoach.billing.policy=PERIOD_END_NO_PRORATION
```

This preset means cancellation/downgrade at paid-period end, restore reverses an
unexpired pending cancellation, a verified upgrade starts a new paid period,
and verified renewal extends from the later of payment time and current end.
No proration, refunds, unused-time credits, grace periods or automatic charges
are assumed. A paid-tier downgrade needs verified renewal payment; otherwise
it expires at the old boundary. A FREE downgrade needs no payment. A prepaid
pending tier change must be reconciled before cancellation. Calendar billing
uses UTC months/years, including month-end/leap-day normalization.

Confirm this preset before enabling it. If it does not match the product's
custom policy, replace its policy logic and associated lifecycle tests before
any live billing. It is not presented as an approved production business rule.

Configure positive integer minor-unit prices in application configuration:

```properties
runcoach.billing.prices.pro.monthly.usd=<integer cents>
runcoach.billing.prices.pro.yearly.usd=<integer cents>
runcoach.billing.prices.elite.monthly.usd=<integer cents>
runcoach.billing.prices.elite.yearly.usd=<integer cents>
runcoach.billing.prices.pro.monthly.thb=<integer satang>
runcoach.billing.prices.pro.yearly.thb=<integer satang>
runcoach.billing.prices.elite.monthly.thb=<integer satang>
runcoach.billing.prices.elite.yearly.thb=<integer satang>
```

The environment equivalents are `RUNCOACH_BILLING_PRICES_PRO_MONTHLY_USD`, etc.
`BILLING_POLICY` configures the policy. Zero/missing prices disable the affected
checkout rather than silently selling paid plans for free. Environment examples
and Compose pass-through are included. Kubernetes/Helm can use the same values
in environment-specific ConfigMaps; gateway secrets belong in secret management.

## Gateway Boundary

`PaymentGateway` defines `createPayment`, `verifyPayment`, `refundPayment`,
`cancelPayment`, and provider-signed webhook verification. `PaymentProvider`
supports STRIPE, OMISE, PROMPTPAY, APPLE and GOOGLE. The registry requires exactly
one configured CDI adapter for the requested provider and otherwise returns 503.
There are **no live provider adapters** and no production fake-success gateway.

Implement provider timeouts, idempotent create/refund operations, account/mode
binding, signed callbacks and timestamp/replay validation inside each adapter.
Treat gateway receipts as authoritative only after verifying these properties.
Mobile stores require their own receipt/transaction verification and restoration
flows; the generic restore endpoint reverses cancellation, not app-store imports.
The test gateway exists only in test sources and is activated only by the
database test profile. Refund/cancel gateway ports reserve future integration;
there is no live refund API or automatic entitlement-refund policy. REFUNDED
receipts fail closed pending an approved reconciliation process.

Retries after a provider/network failure use the original client idempotency
key and persisted payment ID. Do not retry checkout with a new key. Orphaned
checkouts, captured-but-unapplied payments and reconciliation flags require an
operator runbook/provider reconciliation worker before production rollout.

## APIs

All routes require verified JWT authentication. All ownership is server-derived.

| Method | Route | Result |
| --- | --- | --- |
| POST | /api/v1/subscriptions/trial | Start one-time PRO trial |
| POST | /api/v1/subscriptions/subscribe | Create monthly/yearly checkout |
| POST | /api/v1/subscriptions/upgrade | Create upgrade checkout |
| POST | /api/v1/subscriptions/downgrade | Schedule a lower plan |
| POST | /api/v1/subscriptions/cancel | Schedule cancellation |
| POST | /api/v1/subscriptions/restore | Reverse pending cancellation |
| POST | /api/v1/subscriptions/renew | Create renewal checkout |
| GET | /api/v1/subscriptions/me | Current subscription and trial history |
| GET | /api/v1/subscriptions/features | Effective plan/features |
| POST | /api/v1/payments/create | Create idempotent checkout |
| POST | /api/v1/payments/verify | Verify owned payment and settle |
| POST | /api/v1/payments/webhook | ADMIN-authenticated signed relay |
| GET | /api/v1/payments/history | Owned paginated payment history |

```json
{
  "plan": "PRO",
  "billingCycle": "MONTHLY",
  "currency": "USD",
  "provider": "STRIPE",
  "idempotencyKey": "00000000-0000-0000-0000-000000000001"
}
```

Use this body for create/subscribe/upgrade/renew. Verify accepts `paymentId` only;
downgrade accepts `plan`. Checkout returns 202, not payment success. Trial,
lifecycle actions, verification and reads return 200. Validation errors use
Quarkus' native validation response. Application errors use `ApiError`, with
409 for trial/idempotency/state conflicts and 503 for missing configuration.
History uses `page=0&size=20`, bounded to size 100 and page 10000.

## Webhook Security

The requirement to protect **every** endpoint with JWT is retained. Real provider
servers normally do not possess RunCoach user JWTs, so a trusted relay with an
ADMIN service identity must preserve the original signed payload bytes and
provider signature:

```json
{"provider":"STRIPE","payloadBase64":"<original bytes as base64>","signature":"<provider signature>"}
```

ADMIN authentication does not replace provider authentication. The gateway
extracts event/payment IDs only after signature/replay verification; the service
then queries the provider's current payment state. Payload hashes and unique
provider/event IDs prevent changed replays and duplicate settlement. Raw payloads
are not persisted; decoded payload is capped at 256 KiB. Reject invalid provider
signatures with a safe `BillingException`, not details containing secrets.
If direct public provider callbacks are required later, approve a separate
signature-authenticated endpoint rather than weakening JWT enforcement silently.

## Database And Tests

V4 creates all seven requested tables, catalog data, foreign keys, uniqueness
constraints and immutable trial-history triggers. V1-V3 are preserved. Populated
legacy `subscriptions`/`payments` block V4 until an explicit reconciliation and
backfill migration has been designed. Use restricted runtime DB permissions;
never allow runtime code to truncate trial history or rewrite captured money.

```bash
mvn -f backend/pom.xml verify
mvn -f backend/pom.xml '-Dtest=Billing*Test' test
# Docker + PostgreSQL Testcontainers, or isolated external PostgreSQL:
mvn -f backend/pom.xml -Ppostgres-it verify
```

For an existing test server, set `RUNCOACH_TEST_DB_URL`,
`RUNCOACH_TEST_DB_USER`, and `RUNCOACH_TEST_DB_PASSWORD`. Its database name must
end in `_test`. **The integration suite truncates this database's user/billing
data between tests. Never point it at development, staging or production data.**
The PostgreSQL CI job uses an isolated `runcoach_test` service.

Unit/HTTP checks cover trial rules, expiry boundaries, explicit pricing/policy,
receipt mismatches, single settlement, reconciliation and JWT/ADMIN constraints.
Ten PostgreSQL integration tests cover concurrent trials, history immutability,
monthly/yearly billing, trial upgrade, ownership and signed-webhook replays.
The integration suite compiled but local execution was blocked by unavailable
Docker. Migration execution, locking and real-database assertions remain
unverified locally; the CI job is declared, not claimed to have run.

OpenAPI schemas are generated during Maven packaging in
`backend/target/openapi`; resource annotations document contracts/status codes.

## Production Gates

Approve prices/custom lifecycle semantics; implement and certify real gateway
adapters; run PostgreSQL and provider sandbox suites; establish refund,
reconciliation and orphan-checkout runbooks; add rate limits, audit/metrics and
provider failure alerts. BillingInvoice is a basic payment invoice record, not
a jurisdiction-compliant tax invoice/PDF: merchant details, taxes, tax numbering,
customer billing addresses and statutory retention require explicit requirements.
There is no recurring debit scheduler or PAST_DUE collection/grace workflow.
The enums and ports reserve these states but do not invent their financial rules.