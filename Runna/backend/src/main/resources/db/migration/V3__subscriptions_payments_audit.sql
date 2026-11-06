CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    provider VARCHAR(64) NOT NULL,
    provider_subscription_id VARCHAR(255) NOT NULL,
    product_code VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    period_start TIMESTAMPTZ,
    period_end TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (period_end IS NULL OR period_start IS NULL OR period_end >= period_start),
    UNIQUE (provider, provider_subscription_id),
    UNIQUE (id, user_id)
);
CREATE INDEX idx_subscriptions_user ON subscriptions(user_id);

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    subscription_id UUID,
    provider VARCHAR(64) NOT NULL,
    provider_payment_id VARCHAR(255) NOT NULL,
    amount_minor BIGINT NOT NULL CHECK (amount_minor >= 0),
    currency CHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    paid_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subscription_id, user_id) REFERENCES subscriptions(id, user_id),
    UNIQUE (provider, provider_payment_id)
);
CREATE INDEX idx_payments_user_created ON payments(user_id, created_at);
CREATE INDEX idx_payments_subscription ON payments(subscription_id, user_id);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID REFERENCES users(id),
    action VARCHAR(128) NOT NULL,
    resource_type VARCHAR(128) NOT NULL,
    resource_id UUID,
    request_id VARCHAR(128),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_audit_logs_actor_time ON audit_logs(actor_user_id, occurred_at);
CREATE INDEX idx_audit_logs_resource ON audit_logs(resource_type, resource_id);