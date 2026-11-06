CREATE TABLE sleep_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ NOT NULL,
    source VARCHAR(64) NOT NULL,
    external_id VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (ended_at > started_at),
    UNIQUE (user_id, source, external_id)
);
CREATE INDEX idx_sleep_records_user_started ON sleep_records(user_id, started_at);

CREATE TABLE heart_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    measured_at TIMESTAMPTZ NOT NULL,
    resting_heart_rate_bpm NUMERIC(6, 2) CHECK (resting_heart_rate_bpm > 0),
    hrv_rmssd_ms NUMERIC(10, 2) CHECK (hrv_rmssd_ms >= 0),
    source VARCHAR(64) NOT NULL,
    external_id VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, source, external_id)
);
CREATE INDEX idx_heart_metrics_user_measured ON heart_metrics(user_id, measured_at);

CREATE TABLE recovery_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    score_date DATE NOT NULL,
    score NUMERIC(5, 2) NOT NULL CHECK (score BETWEEN 0 AND 100),
    engine_version VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, score_date)
);

CREATE TABLE readiness_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    score_date DATE NOT NULL,
    score NUMERIC(5, 2) NOT NULL CHECK (score BETWEEN 0 AND 100),
    engine_version VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, score_date)
);

CREATE TABLE nutrition_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    plan_date DATE NOT NULL,
    energy_kcal NUMERIC(10, 2) CHECK (energy_kcal >= 0),
    protein_grams NUMERIC(10, 2) CHECK (protein_grams >= 0),
    carbohydrate_grams NUMERIC(10, 2) CHECK (carbohydrate_grams >= 0),
    fat_grams NUMERIC(10, 2) CHECK (fat_grams >= 0),
    hydration_ml INTEGER CHECK (hydration_ml >= 0),
    engine_version VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, plan_date)
);