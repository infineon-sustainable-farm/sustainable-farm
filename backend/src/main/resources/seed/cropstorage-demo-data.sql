-- cropstorage-demo-data.sql: FICTIONAL demo data of the Crop Storage module, profile dev only.
--
-- Loaded by modules.cropstorage.config.CropStorageDemoDataSeeder when storage_zone is empty, so
-- that every Crop Storage screen and table has rows on a fresh dev database. None of these rows
-- is real operational data: codes, quantities, values and readings are invented for the demo.
-- Climate ranges follow the dried-mango storage range used by the sensor alerts (10 to 25 C,
-- 20 to 60 %). Varieties are keys of the Plants catalogue, loaded at startup before this file.
-- PostgreSQL SQL.

INSERT INTO storage_zone (created_at, updated_at, zone_code, name, description, capacity_kg,
                          temperature_min_celsius, temperature_max_celsius, humidity_min_percent,
                          humidity_max_percent, cooling_available, status)
VALUES (now(), now(), 'DEMO-A1', 'Dry store A1', 'Demo zone: packaged dried mango', 5000.00, 10.00, 25.00, 20.00, 60.00, false, 'ACTIVE'),
       (now(), now(), 'DEMO-C1', 'Cold room C1', 'Demo zone: refrigerated', 2000.00, 10.00, 18.00, 20.00, 55.00, true, 'ACTIVE'),
       (now(), now(), 'DEMO-B2', 'Dry store B2', 'Demo zone: under maintenance', 3000.00, 10.00, 25.00, 20.00, 60.00, false, 'MAINTENANCE');

INSERT INTO supplier (created_at, updated_at, supplier_code, name, region, contact_phone)
VALUES (now(), now(), 'DEMO-SUP-1', 'Demo cooperative', 'Hauts-Bassins', NULL),
       (now(), now(), 'DEMO-SUP-2', 'Demo grower group', 'Cascades', NULL);

INSERT INTO crop_storage_batch (created_at, updated_at, version, batch_code, product_type, product_stage,
                                origin, supplier_id, variety_key, packaging_date, storage_entry_date,
                                initial_quantity_kg, current_quantity_kg, moisture_percent, quality_grade,
                                status, storage_zone_id, expected_dispatch_date, expiry_date,
                                customer_priority, value_per_kg_amount, value_per_kg_currency)
VALUES (now(), now(), 0, 'DEMO-B-001', 'DRIED_MANGO', 'PACKAGED', 'Farm', NULL, 'KENT',
        current_date - 20, current_date - 19, 800.00, 650.00, 14.00, 'A', 'STORED',
        (SELECT id FROM storage_zone WHERE zone_code = 'DEMO-A1'), current_date + 10, current_date + 160, 2, 600.00, 'XOF'),
       (now(), now(), 0, 'DEMO-B-002', 'DRIED_MANGO', 'PACKAGED', 'Farm', NULL, 'KEITT',
        current_date - 12, current_date - 11, 500.00, 500.00, 15.00, 'A', 'STORED',
        (SELECT id FROM storage_zone WHERE zone_code = 'DEMO-A1'), current_date + 20, current_date + 170, 0, 600.00, 'XOF'),
       (now(), now(), 0, 'DEMO-B-003', 'DRIED_MANGO', 'DRIED', 'Supplier',
        (SELECT id FROM supplier WHERE supplier_code = 'DEMO-SUP-1'), 'AMELIE',
        NULL, current_date - 8, 400.00, 380.00, 16.00, 'B', 'STORED',
        (SELECT id FROM storage_zone WHERE zone_code = 'DEMO-C1'), current_date + 30, current_date + 120, 1, 550.00, 'XOF'),
       (now(), now(), 0, 'DEMO-B-004', 'FRESH_MANGO', 'FRESH', 'Supplier',
        (SELECT id FROM supplier WHERE supplier_code = 'DEMO-SUP-2'), 'KENT',
        NULL, current_date - 2, 300.00, 300.00, NULL, 'A', 'STORED',
        (SELECT id FROM storage_zone WHERE zone_code = 'DEMO-C1'), current_date + 5, current_date + 12, 3, 250.00, 'XOF'),
       (now(), now(), 0, 'DEMO-B-005', 'DRIED_MANGO', 'PACKAGED', 'Farm', NULL, 'KENT',
        current_date - 40, current_date - 39, 200.00, 0.00, 14.00, 'A', 'DISPATCHED',
        NULL, current_date - 5, current_date + 140, 0, 600.00, 'XOF');

INSERT INTO stock_movement (created_at, updated_at, type, quantity_kg, batch_id, from_zone_id, to_zone_id, reason, moved_at)
VALUES (now(), now(), 'IN', 800.00, (SELECT id FROM crop_storage_batch WHERE batch_code = 'DEMO-B-001'), NULL,
        (SELECT id FROM storage_zone WHERE zone_code = 'DEMO-A1'), 'Demo entry', now() - interval '19 days'),
       (now(), now(), 'OUT', 150.00, (SELECT id FROM crop_storage_batch WHERE batch_code = 'DEMO-B-001'),
        (SELECT id FROM storage_zone WHERE zone_code = 'DEMO-A1'), NULL, 'Demo dispatch', now() - interval '3 days'),
       (now(), now(), 'IN', 400.00, (SELECT id FROM crop_storage_batch WHERE batch_code = 'DEMO-B-003'), NULL,
        (SELECT id FROM storage_zone WHERE zone_code = 'DEMO-C1'), 'Demo entry', now() - interval '8 days'),
       (now(), now(), 'OUT', 200.00, (SELECT id FROM crop_storage_batch WHERE batch_code = 'DEMO-B-005'),
        (SELECT id FROM storage_zone WHERE zone_code = 'DEMO-A1'), NULL, 'Demo dispatch', now() - interval '6 days');

INSERT INTO sensor (created_at, updated_at, sensor_code, sensor_type, name, storage_zone_id, status, installation_date)
VALUES (now(), now(), 'DEMO-S-A1-T', 'TEMPERATURE_HUMIDITY', 'A1 climate', (SELECT id FROM storage_zone WHERE zone_code = 'DEMO-A1'), 'ACTIVE', current_date - 90),
       (now(), now(), 'DEMO-S-C1-T', 'TEMPERATURE_HUMIDITY', 'C1 climate', (SELECT id FROM storage_zone WHERE zone_code = 'DEMO-C1'), 'ACTIVE', current_date - 90),
       (now(), now(), 'DEMO-S-B2-T', 'TEMPERATURE_HUMIDITY', 'B2 climate', (SELECT id FROM storage_zone WHERE zone_code = 'DEMO-B2'), 'MAINTENANCE', current_date - 200);

INSERT INTO sensor_reading (created_at, updated_at, sensor_id, temperature, humidity, measured_at)
SELECT now(), now(), s.id, r.temperature, r.humidity, now() - (r.hours_ago || ' hours')::interval
FROM sensor s
JOIN (VALUES ('DEMO-S-A1-T', 22.50, 48.00, 3), ('DEMO-S-A1-T', 23.10, 50.00, 2), ('DEMO-S-A1-T', 23.80, 51.00, 1),
             ('DEMO-S-C1-T', 14.20, 45.00, 3), ('DEMO-S-C1-T', 15.00, 47.00, 2), ('DEMO-S-C1-T', 19.40, 52.00, 1))
     AS r(sensor_code, temperature, humidity, hours_ago) ON r.sensor_code = s.sensor_code;

INSERT INTO quality_check (created_at, updated_at, batch_id, moisture_percent, quality_grade, passed, inspector_person_id, notes, checked_at)
VALUES (now(), now(), (SELECT id FROM crop_storage_batch WHERE batch_code = 'DEMO-B-001'), 14.00, 'A', true, NULL, 'Demo check', now() - interval '10 days'),
       (now(), now(), (SELECT id FROM crop_storage_batch WHERE batch_code = 'DEMO-B-003'), 17.50, 'B', false, NULL, 'Demo check: moisture above target', now() - interval '4 days');

INSERT INTO loss_record (created_at, updated_at, batch_id, quantity_kg, cause, value_amount, value_currency, recorded_at)
VALUES (now(), now(), (SELECT id FROM crop_storage_batch WHERE batch_code = 'DEMO-B-003'), 20.00, 'Mould or moisture', 11000.00, 'XOF', now() - interval '4 days');

INSERT INTO crop_storage_alert (created_at, type, severity, message, status, sensor_id, storage_zone_id, batch_id, corrective_action)
VALUES (now() - interval '1 hours', 'TEMPERATURE', 'HIGH', 'Demo alert: temperature 19.4 C above max 18 C in zone DEMO-C1', 'ACTIVE',
        (SELECT id FROM sensor WHERE sensor_code = 'DEMO-S-C1-T'), (SELECT id FROM storage_zone WHERE zone_code = 'DEMO-C1'), NULL, NULL),
       (now() - interval '4 days', 'HUMIDITY', 'HIGH', 'Demo alert: moisture above target on batch DEMO-B-003', 'RESOLVED',
        NULL, (SELECT id FROM storage_zone WHERE zone_code = 'DEMO-C1'), (SELECT id FROM crop_storage_batch WHERE batch_code = 'DEMO-B-003'), 'Batch re-dried');
