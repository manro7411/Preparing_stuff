DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM subscriptions) OR EXISTS (SELECT 1 FROM payments) THEN
        RAISE EXCEPTION 'Legacy billing rows require an explicit reconciliation migration before V4';
    END IF;
END $$;

CREATE TABLE subscription_plan (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(16) NOT NULL UNIQUE CHECK (code IN ('FREE','PRO','ELITE')),
    display_name VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE subscription_feature (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES subscription_plan(id),
    feature VARCHAR(64) NOT NULL CHECK (feature IN ('BASIC_DASHBOARD','ACTIVITY_TRACKING','TRAINING_PLANS',
        'NUTRITION_COACH','AI_COACH','RACE_PREDICTION','ADAPTIVE_TRAINING','UNLIMITED_AI_COACH','ADVANCED_ANALYTICS')),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (plan_id, feature)
);

CREATE TABLE user_subscription (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    plan_id UUID NOT NULL REFERENCES subscription_plan(id),
    status VARCHAR(16) NOT NULL CHECK (status IN ('TRIAL','ACTIVE','EXPIRED','CANCELLED','PAST_DUE')),
    billing_cycle VARCHAR(16) CHECK (billing_cycle IN ('MONTHLY','YEARLY')),
    currency VARCHAR(3) CHECK (currency IN ('USD','THB')),
    trial_started_at TIMESTAMPTZ,
    trial_ends_at TIMESTAMPTZ,
    period_start TIMESTAMPTZ,
    period_end TIMESTAMPTZ,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    pending_plan_id UUID REFERENCES subscription_plan(id),
    pending_billing_cycle VARCHAR(16) CHECK (pending_billing_cycle IN ('MONTHLY','YEARLY')),
    pending_currency VARCHAR(3) CHECK (pending_currency IN ('USD','THB')),
    pending_period_start TIMESTAMPTZ,
    pending_period_end TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id, user_id),
    CHECK ((trial_started_at IS NULL AND trial_ends_at IS NULL) OR
        (trial_started_at IS NOT NULL AND trial_ends_at IS NOT NULL AND trial_ends_at = trial_started_at + INTERVAL '336 hours')),
    CHECK (status <> 'TRIAL' OR (trial_started_at IS NOT NULL AND period_end IS NOT NULL
        AND period_start IS NOT NULL AND period_start = trial_started_at AND period_end = trial_ends_at)),
    CHECK ((period_start IS NULL AND period_end IS NULL) OR
        (period_start IS NOT NULL AND period_end IS NOT NULL AND period_end > period_start)),
    CHECK ((pending_period_start IS NULL AND pending_period_end IS NULL) OR
        (pending_plan_id IS NOT NULL AND pending_billing_cycle IS NOT NULL AND pending_currency IS NOT NULL
         AND pending_period_start IS NOT NULL AND pending_period_end IS NOT NULL AND pending_period_end > pending_period_start))
);
CREATE INDEX idx_user_subscription_expiration ON user_subscription(period_end) WHERE status IN ('TRIAL','ACTIVE');

CREATE FUNCTION preserve_trial_history() RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN
    IF OLD.trial_started_at IS NOT NULL AND
       (NEW.trial_started_at IS DISTINCT FROM OLD.trial_started_at OR NEW.trial_ends_at IS DISTINCT FROM OLD.trial_ends_at) THEN
        RAISE EXCEPTION 'Trial history cannot be changed' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER immutable_trial_history BEFORE UPDATE ON user_subscription
    FOR EACH ROW EXECUTE FUNCTION preserve_trial_history();

CREATE FUNCTION prevent_trial_history_deletion() RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN
    IF OLD.trial_started_at IS NOT NULL THEN
        RAISE EXCEPTION 'Consumed trial history cannot be deleted' USING ERRCODE = '23514';
    END IF;
    RETURN OLD;
END $$;
CREATE TRIGGER permanent_trial_history BEFORE DELETE ON user_subscription
    FOR EACH ROW EXECUTE FUNCTION prevent_trial_history_deletion();

CREATE TABLE payment_method (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    provider VARCHAR(16) NOT NULL CHECK (provider IN ('STRIPE','OMISE','PROMPTPAY','APPLE','GOOGLE')),
    provider_method_id VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, provider, provider_method_id)
);

CREATE TABLE payment_transaction (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    subscription_id UUID NOT NULL,
    target_plan_id UUID NOT NULL REFERENCES subscription_plan(id),
    idempotency_key UUID NOT NULL,
    subscription_version BIGINT NOT NULL,
    purpose VARCHAR(16) NOT NULL CHECK (purpose IN ('SUBSCRIBE','UPGRADE','RENEW')),
    provider VARCHAR(16) NOT NULL CHECK (provider IN ('STRIPE','OMISE','PROMPTPAY','APPLE','GOOGLE')),
    provider_payment_id VARCHAR(255),
    checkout_url VARCHAR(2048),
    billing_cycle VARCHAR(16) NOT NULL CHECK (billing_cycle IN ('MONTHLY','YEARLY')),
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency IN ('USD','THB')),
    status VARCHAR(16) NOT NULL CHECK (status IN ('CREATED','PENDING','SUCCESS','FAILED','REFUNDED')),
    paid_at TIMESTAMPTZ,
    subscription_applied BOOLEAN NOT NULL DEFAULT FALSE,
    reconciliation_required BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subscription_id, user_id) REFERENCES user_subscription(id, user_id),
    UNIQUE (user_id, idempotency_key),
    UNIQUE (provider, provider_payment_id),
    UNIQUE (id, user_id),
    CHECK (status <> 'SUCCESS' OR paid_at IS NOT NULL),
    CHECK (NOT subscription_applied OR status IN ('SUCCESS','REFUNDED'))
);
CREATE INDEX idx_payment_transaction_history ON payment_transaction(user_id, created_at DESC, id);
CREATE UNIQUE INDEX uq_payment_open_checkout ON payment_transaction(user_id) WHERE status IN ('CREATED','PENDING');

CREATE TABLE payment_webhook_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(16) NOT NULL CHECK (provider IN ('STRIPE','OMISE','PROMPTPAY','APPLE','GOOGLE')),
    event_id VARCHAR(255) NOT NULL,
    payment_id UUID NOT NULL REFERENCES payment_transaction(id),
    payload_hash VARCHAR(64) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (provider, event_id)
);
CREATE INDEX idx_webhook_payment ON payment_webhook_log(payment_id);

CREATE TABLE billing_invoice (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    subscription_id UUID NOT NULL,
    payment_id UUID NOT NULL UNIQUE,
    invoice_number VARCHAR(48) NOT NULL UNIQUE,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency IN ('USD','THB')),
    issued_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subscription_id, user_id) REFERENCES user_subscription(id, user_id),
    FOREIGN KEY (payment_id, user_id) REFERENCES payment_transaction(id, user_id)
);
CREATE INDEX idx_invoice_user ON billing_invoice(user_id, issued_at DESC);

INSERT INTO subscription_plan(code, display_name) VALUES ('FREE','Free'),('PRO','Pro'),('ELITE','Elite');
INSERT INTO subscription_feature(plan_id, feature)
SELECT plan.id, features.feature FROM subscription_plan plan CROSS JOIN
    (VALUES ('BASIC_DASHBOARD'),('ACTIVITY_TRACKING')) AS features(feature);
INSERT INTO subscription_feature(plan_id, feature)
SELECT plan.id, features.feature FROM subscription_plan plan CROSS JOIN
    (VALUES ('TRAINING_PLANS'),('NUTRITION_COACH'),('AI_COACH'),('RACE_PREDICTION')) AS features(feature)
    WHERE plan.code IN ('PRO','ELITE');
INSERT INTO subscription_feature(plan_id, feature)
SELECT plan.id, features.feature FROM subscription_plan plan CROSS JOIN
    (VALUES ('ADAPTIVE_TRAINING'),('UNLIMITED_AI_COACH'),('ADVANCED_ANALYTICS')) AS features(feature)
    WHERE plan.code = 'ELITE';