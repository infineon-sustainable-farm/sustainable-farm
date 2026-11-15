-- V12: inter-module integrations of the watersupply module (see docs/integrations/).
--
-- Timestamps use TIMESTAMP like V1-V11 (instants stored in UTC, hibernate.jdbc.time_zone=UTC).

-- Product Transformation (T1): a consumption can be attributed to a lot and an activity
-- (WASHING, CUTTING, DRYER_CLEANING, IRRIGATION, OTHER, validated by the API).
ALTER TABLE water_consumption
    ADD COLUMN IF NOT EXISTS lot_id VARCHAR(128),
    ADD COLUMN IF NOT EXISTS activity VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_water_consumption_lot_id ON water_consumption(lot_id);

-- Visitor Management (V6) and internal logic: planned water unavailability on a farm, a source or
-- a zone. Distinct from drip_maintenance_logs, which records interventions already done.
CREATE TABLE IF NOT EXISTS water_unavailability (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    scope VARCHAR(16) NOT NULL,
    scope_id UUID NOT NULL,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    reason VARCHAR(255),
    status VARCHAR(16) NOT NULL,
    CONSTRAINT chk_water_unavailability_scope CHECK (scope IN ('farm', 'source', 'zone')),
    CONSTRAINT chk_water_unavailability_status CHECK (status IN ('planned', 'active', 'cancelled')),
    CONSTRAINT chk_water_unavailability_period CHECK (end_at > start_at)
);

CREATE INDEX IF NOT EXISTS idx_water_unavailability_scope ON water_unavailability(scope, scope_id);
CREATE INDEX IF NOT EXISTS idx_water_unavailability_period ON water_unavailability(start_at, end_at);

-- Visitor Management (V7): visit announcements, idempotent on visit_id.
CREATE TABLE IF NOT EXISTS water_visitor_events (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    visit_id VARCHAR(128) NOT NULL,
    group_name VARCHAR(255),
    expected_at TIMESTAMP NOT NULL,
    zones TEXT,
    conflict BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_water_visitor_events_visit_id UNIQUE (visit_id)
);

-- Visitor Management (V8): versioned safety briefing, one row per published version.
CREATE TABLE IF NOT EXISTS water_safety_briefing (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    version VARCHAR(64) NOT NULL,
    allowed_zones TEXT,
    forbidden_zones TEXT,
    risks TEXT,
    briefing_text TEXT,
    max_capacity INTEGER,
    guide_required BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_water_safety_briefing_created_at ON water_safety_briefing(created_at);

-- Internal logic (5.2): reservoir level history (water_sources only keeps the last value).
CREATE TABLE IF NOT EXISTS water_level_history (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    source_id UUID NOT NULL,
    level_liters DOUBLE PRECISION NOT NULL,
    measured_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_water_level_history_source FOREIGN KEY (source_id) REFERENCES water_sources(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_water_level_history_source_date ON water_level_history(source_id, measured_at);

-- IoT (6.1): journal of outbound device commands (audit, including the rejected ones).
CREATE TABLE IF NOT EXISTS iot_commands (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    device_id VARCHAR(64) NOT NULL,
    command_type VARCHAR(64) NOT NULL,
    payload TEXT,
    status VARCHAR(32) NOT NULL,
    requested_by UUID,
    requested_at TIMESTAMP NOT NULL,
    acknowledged_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_iot_commands_device_requested ON iot_commands(device_id, requested_at);
