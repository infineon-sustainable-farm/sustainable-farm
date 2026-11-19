-- ============================================================================
-- MACHINERY - DEMO MACHINES OF THE EQUIPMENT REGISTER
--
-- Synthetic data, dev profile only (MachineryDemoDataSeeder). Every code carries DEMO.
--
-- Machinery owns the single equipment register (table equipment, ADR 0004): it is the only
-- module that creates its machines, whatever their stage. These are the processing and drying
-- machines of the product transformation demo data (2 operational, 1 under maintenance); their
-- product transformation attributes (pt_equipment_spec) are added by
-- seed/producttransformation-demo-seed.sql, which runs after this script.
--
-- Re-runnable without error (ON CONFLICT DO NOTHING).
-- ============================================================================

INSERT INTO equipment (code, name, category, stage, status, created_at, updated_at)
VALUES
    ('EQ-DEMO-01', 'Sechoir solaire DEMO 1', 'PROCESSING_MACHINERY', 'DRYING',     'OPERATIONAL',       now(), now()),
    ('EQ-DEMO-02', 'Laveuse DEMO 1',         'PROCESSING_MACHINERY', 'PROCESSING', 'OPERATIONAL',       now(), now()),
    ('EQ-DEMO-03', 'Conditionneuse DEMO 1',  'PROCESSING_MACHINERY', 'PROCESSING', 'UNDER_MAINTENANCE', now(), now())
ON CONFLICT DO NOTHING;
