-- V10: automatic irrigation control (F1), clogging detection via flow (F3),
-- rain source level tracking (F2) and real-time level (F5).
--
-- The migration number lives in a folder shared between modules: announce it to
-- the team before the pull request, to avoid two competing V10s.

-- F1: irrigation can now be created by the soil moisture rule, not only
-- by hand. The column distinguishes both origins (values: manual, auto).
ALTER TABLE irrigation_schedules
    ADD COLUMN IF NOT EXISTS trigger_source VARCHAR(20) NOT NULL DEFAULT 'manual';

-- F3: the theoretical flow of a zone is computed from its drip network:
-- theoretical_flow (L/h) = emitter count x nominal flow of one emitter (L/h).
ALTER TABLE zones
    ADD COLUMN IF NOT EXISTS emitter_count INTEGER,
    ADD COLUMN IF NOT EXISTS emitter_nominal_flow_lh DOUBLE PRECISION;

-- F3: attaching a flow measurement to a zone allows comparing the actually
-- measured volume (meter) with the theoretical network volume over the same watering duration.
ALTER TABLE water_consumption
    ADD COLUMN IF NOT EXISTS zone_id UUID;

ALTER TABLE water_consumption
    ADD CONSTRAINT fk_water_consumption_zone
    FOREIGN KEY (zone_id) REFERENCES zones(id);

CREATE INDEX IF NOT EXISTS idx_water_consumption_zone_date
    ON water_consumption(zone_id, consumption_date);
