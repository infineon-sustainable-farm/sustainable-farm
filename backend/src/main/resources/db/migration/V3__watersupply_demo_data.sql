-- SWMS - Donnees de demonstration controlees (V3)
-- Jeu minimal, relationnellement valide et idempotent pour les environnements demo/dev.

INSERT INTO users (id, created_at, first_name, last_name, email, password_hash, status)
VALUES (
    '10000000-0000-0000-0000-000000000001',
    '2026-01-01T08:00:00Z',
    'Demo',
    'Admin',
    'demo.admin@sustainable-farm.local',
    '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiWQd8RrSn2qHh9Vb50LRyK8T1fQ8uO',
    true
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO farms (id, created_at, name, description, address, latitude, longitude, area_hectares)
VALUES (
    '20000000-0000-0000-0000-000000000001',
    '2026-01-01T08:05:00Z',
    'Ferme Demo Nord',
    'Ferme pilote pour le module water supply.',
    'Route agricole 1',
    48.8566,
    2.3522,
    24.5
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO fields (id, created_at, farm_id, name, area_hectares, crop_type, soil_type, coordinates)
VALUES (
    '30000000-0000-0000-0000-000000000001',
    '2026-01-01T08:10:00Z',
    '20000000-0000-0000-0000-000000000001',
    'Champ Maraicher A',
    6.8,
    'legumes',
    'limon',
    '48.8566,2.3522'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO zones (id, created_at, field_id, name, area_hectares, irrigation_method)
VALUES (
    '40000000-0000-0000-0000-000000000001',
    '2026-01-01T08:15:00Z',
    '30000000-0000-0000-0000-000000000001',
    'Zone Goutte A',
    2.4,
    'drip'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO water_sources (id, created_at, farm_id, name, type, capacity_liters, current_level_liters, latitude, longitude)
VALUES (
    '50000000-0000-0000-0000-000000000001',
    '2026-01-01T08:20:00Z',
    '20000000-0000-0000-0000-000000000001',
    'Citerne principale',
    'rain_tank',
    50000,
    31250,
    48.8569,
    2.3525
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO water_consumption (id, created_at, farm_id, source_id, consumption_liters, consumption_date, irrigation_id)
VALUES (
    '60000000-0000-0000-0000-000000000001',
    '2026-01-01T08:25:00Z',
    '20000000-0000-0000-0000-000000000001',
    '50000000-0000-0000-0000-000000000001',
    1250,
    '2026-01-01T09:00:00Z',
    null
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO water_quality_tests (id, created_at, source_id, ph, temperature_celsius, turbidity_ntu, conductivity_us_cm, salinity_ppt, test_date)
VALUES (
    '70000000-0000-0000-0000-000000000001',
    '2026-01-01T08:30:00Z',
    '50000000-0000-0000-0000-000000000001',
    7.1,
    18.5,
    2.2,
    450,
    0.2,
    '2026-01-01T09:10:00Z'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO irrigation_schedules (id, created_at, zone_id, start_time, duration_minutes, water_quantity_liters, status, created_by)
VALUES (
    '80000000-0000-0000-0000-000000000001',
    '2026-01-01T08:35:00Z',
    '40000000-0000-0000-0000-000000000001',
    '2026-01-02T06:00:00Z',
    45,
    1500,
    'scheduled',
    '10000000-0000-0000-0000-000000000001'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO irrigation_logs (id, created_at, schedule_id, actual_start_time, actual_end_time, water_used_liters, status)
VALUES (
    '90000000-0000-0000-0000-000000000001',
    '2026-01-01T08:40:00Z',
    '80000000-0000-0000-0000-000000000001',
    '2026-01-02T06:00:00Z',
    '2026-01-02T06:45:00Z',
    1500,
    'completed'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO notifications (id, created_at, user_id, title, message, type, is_read, action_url)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    '2026-01-01T08:45:00Z',
    '10000000-0000-0000-0000-000000000001',
    'Demo pret',
    'Les donnees de demonstration water supply sont disponibles.',
    'info',
    false,
    '/api/farms'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO rainwater_harvests (id, created_at, source_id, catchment_area_m2, rainfall_mm, runoff_coefficient, harvested_liters, capture_date)
VALUES (
    'b0000000-0000-0000-0000-000000000001',
    '2026-01-01T08:50:00Z',
    '50000000-0000-0000-0000-000000000001',
    180,
    12,
    0.82,
    1771.2,
    '2026-01-01T10:00:00Z'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO drip_maintenance_logs (
    id,
    created_at,
    zone_id,
    maintenance_date,
    maintenance_type,
    filter_cleaned,
    clogging_detected,
    clogging_severity,
    emitter_replaced_count,
    notes,
    performed_by
)
VALUES (
    'c0000000-0000-0000-0000-000000000001',
    '2026-01-01T08:55:00Z',
    '40000000-0000-0000-0000-000000000001',
    '2026-01-03T07:30:00Z',
    'inspection',
    true,
    false,
    null,
    0,
    'Controle demo sans anomalie.',
    'Technicien Demo'
)
ON CONFLICT (id) DO NOTHING;
