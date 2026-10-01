-- 1. Table de suivi des capteurs IoT (P7) : device_id garanti unique, last_seen mis à jour à chaque
--    ingestion, battery et rssi conservés au passage. L'indicateur sensor_availability se base sur
--    ce registre désormais (fin du placeholder statique).
--
-- 2. Table d'historique d'humidité du sol (P3) : chaque mesure de sol est stockée ici pour l'alimenter
--    les règles de pilotage (report automatique, alerte stress hydrique, graphique sparkline).

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
