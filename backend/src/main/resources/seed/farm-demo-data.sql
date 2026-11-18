-- Site referential demo data (farm module): one farm and its blocks A, B and C.
-- Loaded only at startup under the "dev" profile by FarmDemoDataSeeder, before the seeds of the
-- modules that reference these rows (Water Supply, Plants, Product Transformation).
-- Block A is the Plants block of the Keitt variety (PlantsDataSeeder) and the field of the Water
-- Supply zone "Zone Goutte A"; blocks A, B and C are the blocks of the Product Transformation lots.
-- Explicit ids keep the references of the Water Supply seed valid; the identity sequences are moved
-- past them at the end. Idempotent: ON CONFLICT DO NOTHING covers the id and the unique block code.

INSERT INTO farm (id, created_at, updated_at, name, description, address, latitude, longitude, area_hectares)
VALUES (
    1,
    '2026-01-01T08:05:00Z',
    '2026-01-01T08:05:00Z',
    'Ferme Demo Nord',
    'Ferme pilote de la demonstration.',
    'Route agricole 1',
    48.8566,
    2.3522,
    24.5
)
ON CONFLICT DO NOTHING;

INSERT INTO block (id, created_at, updated_at, farm_id, code, name, area_hectares, crop_type, soil_type, coordinates)
VALUES
    (1, '2026-01-01T08:10:00Z', '2026-01-01T08:10:00Z', 1, 'A', 'Bloc A', 6.8, 'mangue', 'limon', '48.8566,2.3522'),
    (2, '2026-01-01T08:10:00Z', '2026-01-01T08:10:00Z', 1, 'B', 'Bloc B', 5.2, 'mangue', 'limon', NULL),
    (3, '2026-01-01T08:10:00Z', '2026-01-01T08:10:00Z', 1, 'C', 'Bloc C', 4.1, 'mangue', 'sableux', NULL)
ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('farm', 'id'), GREATEST((SELECT MAX(id) FROM farm), 1));
SELECT setval(pg_get_serial_sequence('block', 'id'), GREATEST((SELECT MAX(id) FROM block), 1));
