-- 1. IoT sensor tracking table (P7): device_id guaranteed unique, last_seen updated on each
--    ingestion, battery and rssi kept along the way. The sensor_availability indicator relies on
--    this registry from now on (end of the static placeholder).
--
-- 2. Soil moisture history table (P3): each soil measurement is stored here to feed
--    the control rules (automatic postpone, water stress alert, sparkline chart).

CREATE TABLE IF NOT EXISTS iot_devices (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id     VARCHAR(64) NOT NULL UNIQUE,
    type          VARCHAR(32) NOT NULL,
    last_seen     TIMESTAMP,
    battery_percent INTEGER,
    rssi          INTEGER,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS soil_moisture_readings (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    zone_id          UUID NOT NULL,
    depth_cm         INTEGER NOT NULL DEFAULT 10,
    moisture_percent DOUBLE PRECISION NOT NULL,
    measured_at      TIMESTAMP NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE soil_moisture_readings ADD CONSTRAINT fk_soil_zone
    FOREIGN KEY (zone_id) REFERENCES zones(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_soil_zone_measured ON soil_moisture_readings (zone_id, measured_at);
CREATE INDEX IF NOT EXISTS idx_iot_device_uid ON iot_devices (device_id);
CREATE INDEX IF NOT EXISTS idx_iot_last_seen ON iot_devices (last_seen);
