-- SWMS - Schema hardening (V2)
-- Keeps V1 immutable while aligning production schema with the current JPA model.

-- The technical system account is no longer inserted here: a migration must carry neither data
-- nor accounts (PR-33). It is created at startup by SystemUserSeeder (profile "!test"),
-- idempotently and without login credentials, since authentication is delegated to the
-- global software. As this migration may run on a pre-Flyway database whose
-- schedules and alerts already point to that account missing from the users table, both
-- constraints toward users are added NOT VALID: existing rows are not
-- checked at this point, new ones are checked as soon as the account is created at startup.

ALTER TABLE IF EXISTS water_consumptions RENAME TO water_consumption;

-- The renames below only concern a historical V1 schema (task_type / log_date /
-- technician). A database created by Hibernate already carries the final names: without this guard,
-- the ALTER TABLE ... RENAME COLUMN would fail with "column task_type does not exist" and block
-- the whole application startup.
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
    FOREIGN KEY (created_by) REFERENCES users(id) NOT VALID;

ALTER TABLE irrigation_logs
    ADD CONSTRAINT fk_irrigation_logs_schedule
    FOREIGN KEY (schedule_id) REFERENCES irrigation_schedules(id);

ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_user
    FOREIGN KEY (user_id) REFERENCES users(id) NOT VALID;

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
