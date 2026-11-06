CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auth_subject VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    time_zone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(160) NOT NULL,
    target_date DATE,
    distance_meters NUMERIC(12, 2) CHECK (distance_meters > 0),
    target_duration_seconds INTEGER CHECK (target_duration_seconds > 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id, user_id)
);
CREATE INDEX idx_goals_user ON goals(user_id);

CREATE TABLE training_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    goal_id UUID,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    engine_version VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (end_date >= start_date),
    FOREIGN KEY (goal_id, user_id) REFERENCES goals(id, user_id),
    UNIQUE (id, user_id)
);
CREATE INDEX idx_training_plans_user ON training_plans(user_id);
CREATE INDEX idx_training_plans_goal ON training_plans(goal_id, user_id);

CREATE TABLE workouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    training_plan_id UUID NOT NULL,
    scheduled_date DATE NOT NULL,
    workout_type VARCHAR(64) NOT NULL,
    distance_meters NUMERIC(12, 2) CHECK (distance_meters >= 0),
    duration_seconds INTEGER CHECK (duration_seconds >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (training_plan_id, user_id) REFERENCES training_plans(id, user_id),
    UNIQUE (id, user_id)
);
CREATE INDEX idx_workouts_user_date ON workouts(user_id, scheduled_date);
CREATE INDEX idx_workouts_plan ON workouts(training_plan_id, user_id);

CREATE TABLE activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    workout_id UUID,
    started_at TIMESTAMPTZ NOT NULL,
    duration_seconds INTEGER NOT NULL CHECK (duration_seconds >= 0),
    distance_meters NUMERIC(12, 2) CHECK (distance_meters >= 0),
    source VARCHAR(64) NOT NULL,
    external_id VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (workout_id, user_id) REFERENCES workouts(id, user_id),
    UNIQUE (user_id, source, external_id)
);
CREATE INDEX idx_activities_user_started ON activities(user_id, started_at);
CREATE INDEX idx_activities_workout ON activities(workout_id, user_id);