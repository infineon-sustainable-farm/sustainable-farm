-- SWMS - Schema hardening (V2)
-- Keeps V1 immutable while aligning production schema with the current JPA model.

-- Reparation (adoption Flyway sur une base existante) : les contraintes ajoutees plus bas
-- exigent que leurs parents existent. Or les plannings et alertes crees par l'application avant
-- Flyway pointent tous vers le compte technique systeme, qui n'etait insere que par V4 -- donc
-- APRES ces contraintes. Sans ce seed, la contrainte fk_irrigation_schedules_created_by
-- echouait ("insert or update on table irrigation_schedules violates foreign key constraint")
-- et l'application ne demarrait plus. V4 conserve la definition de reference : son insert est
-- idempotent (ON CONFLICT (id) DO NOTHING) et devient un no-op.
INSERT INTO users (id, created_at, first_name, last_name, email, password_hash, status)
VALUES (
    '00000000-0000-0000-0000-000000000000',
    NOW(),
    'Systeme',
    'IoT',
    'systeme.iot@watersupply.local',
    'NO_LOGIN_SYSTEM_ACCOUNT',
    FALSE
)
ON CONFLICT (id) DO NOTHING;

ALTER TABLE IF EXISTS water_consumptions RENAME TO water_consumption;

-- Les renommages ci-dessous ne concernent qu'un schema V1 historique (task_type / log_date /
-- technician). Une base creee par Hibernate porte deja les noms finaux : sans ce garde-fou,
-- l'ALTER TABLE ... RENAME COLUMN echouait avec "column task_type does not exist" et bloquait
-- tout le demarrage de l'application.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'drip_maintenance_logs' AND column_name = 'task_type') THEN
        ALTER TABLE drip_maintenance_logs RENAME COLUMN task_type TO maintenance_type;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'drip_maintenance_logs' AND column_name = 'log_date') THEN
        ALTER TABLE drip_maintenance_logs RENAME COLUMN log_date TO maintenance_date;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'drip_maintenance_logs' AND column_name = 'technician') THEN
        ALTER TABLE drip_maintenance_logs RENAME COLUMN technician TO performed_by;
    END IF;
END
$$;

ALTER TABLE IF EXISTS drip_maintenance_logs
    ADD COLUMN IF NOT EXISTS filter_cleaned BOOLEAN,
    ADD COLUMN IF NOT EXISTS clogging_detected BOOLEAN,
    ADD COLUMN IF NOT EXISTS clogging_severity VARCHAR(255),
    ADD COLUMN IF NOT EXISTS emitter_replaced_count INTEGER,
    ADD COLUMN IF NOT EXISTS notes VARCHAR(1000);

ALTER TABLE IF EXISTS drip_maintenance_logs
    DROP COLUMN IF EXISTS issue_found,
    DROP COLUMN IF EXISTS action_taken,
    DROP COLUMN IF EXISTS next_check_date;

ALTER TABLE fields
    ADD CONSTRAINT fk_fields_farm
    FOREIGN KEY (farm_id) REFERENCES farms(id);

ALTER TABLE zones
    ADD CONSTRAINT fk_zones_field
    FOREIGN KEY (field_id) REFERENCES fields(id);

ALTER TABLE water_sources
    ADD CONSTRAINT fk_water_sources_farm
    FOREIGN KEY (farm_id) REFERENCES farms(id);

ALTER TABLE water_consumption
    ADD CONSTRAINT fk_water_consumption_farm
    FOREIGN KEY (farm_id) REFERENCES farms(id);

ALTER TABLE water_consumption
    ADD CONSTRAINT fk_water_consumption_source
    FOREIGN KEY (source_id) REFERENCES water_sources(id);

ALTER TABLE water_quality_tests
    ADD CONSTRAINT fk_water_quality_tests_source
    FOREIGN KEY (source_id) REFERENCES water_sources(id);

ALTER TABLE irrigation_schedules
    ADD CONSTRAINT fk_irrigation_schedules_zone
    FOREIGN KEY (zone_id) REFERENCES zones(id);

ALTER TABLE irrigation_schedules
    ADD CONSTRAINT fk_irrigation_schedules_created_by
    FOREIGN KEY (created_by) REFERENCES users(id);

ALTER TABLE irrigation_logs
    ADD CONSTRAINT fk_irrigation_logs_schedule
    FOREIGN KEY (schedule_id) REFERENCES irrigation_schedules(id);

ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_user
    FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE rainwater_harvests
    ADD CONSTRAINT fk_rainwater_harvests_source
    FOREIGN KEY (source_id) REFERENCES water_sources(id);

ALTER TABLE drip_maintenance_logs
    ADD CONSTRAINT fk_drip_maintenance_logs_zone
    FOREIGN KEY (zone_id) REFERENCES zones(id);

CREATE INDEX IF NOT EXISTS idx_fields_farm_id ON fields(farm_id);
CREATE INDEX IF NOT EXISTS idx_zones_field_id ON zones(field_id);
CREATE INDEX IF NOT EXISTS idx_water_sources_farm_id ON water_sources(farm_id);
CREATE INDEX IF NOT EXISTS idx_water_consumption_farm_id ON water_consumption(farm_id);
CREATE INDEX IF NOT EXISTS idx_water_consumption_source_id ON water_consumption(source_id);
CREATE INDEX IF NOT EXISTS idx_water_consumption_date ON water_consumption(consumption_date);
CREATE INDEX IF NOT EXISTS idx_water_quality_tests_source_id ON water_quality_tests(source_id);
CREATE INDEX IF NOT EXISTS idx_water_quality_tests_test_date ON water_quality_tests(test_date);
CREATE INDEX IF NOT EXISTS idx_irrigation_schedules_zone_id ON irrigation_schedules(zone_id);
CREATE INDEX IF NOT EXISTS idx_irrigation_schedules_start_time ON irrigation_schedules(start_time);
CREATE INDEX IF NOT EXISTS idx_irrigation_logs_schedule_id ON irrigation_logs(schedule_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(type);
CREATE INDEX IF NOT EXISTS idx_rainwater_harvests_source_id ON rainwater_harvests(source_id);
CREATE INDEX IF NOT EXISTS idx_rainwater_harvests_capture_date ON rainwater_harvests(capture_date);
CREATE INDEX IF NOT EXISTS idx_drip_maintenance_logs_zone_id ON drip_maintenance_logs(zone_id);
CREATE INDEX IF NOT EXISTS idx_drip_maintenance_logs_maintenance_date ON drip_maintenance_logs(maintenance_date);
