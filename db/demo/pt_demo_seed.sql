-- ============================================================================
-- PRODUCT TRANSFORMATION — JEU DE DONNEES DE DEMONSTRATION
--
-- ATTENTION : donnees SYNTHETIQUES. Elles ne proviennent PAS du module Plants
-- et ne doivent jamais etre presentees comme des recoltes reelles.
-- Tous les identifiants sont prefixes par DEMO pour rester identifiables.
--
-- Objectif : rendre le dashboard PT lisible (KPI + 5 graphiques) sur une base
-- dont les tables PT existent mais sont vides.
--
-- PREREQUIS (non execute par ce script) :
--   La colonne harvest_event.plants_harvest_id est mappee par l'entite
--   HarvestEvent mais absente de la base de dev. Pour demarrer le backend avec
--   la validation Hibernate active, il faut d'abord :
--
--       ALTER TABLE harvest_event ADD COLUMN plants_harvest_id bigint;
--
--   Ce script n'ajoute volontairement aucune colonne, ne cree aucune table,
--   ne modifie aucune table existante et ne supprime aucune donnee.
--
-- UTILISATION (a executer uniquement sur une base de demonstration) :
--   docker exec -i sustainable-farm-db \
--     psql -U postgres -d sustainable_farm < db/demo/pt_demo_seed.sql
--
-- Les dates sont relatives a CURRENT_DATE : le jeu de donnees tombe toujours
-- dans la periode par defaut du dashboard (7 derniers jours).
-- Re-executable sans erreur (ON CONFLICT DO NOTHING).
-- ============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- 1) Operateurs (drying et controle qualite)
-- ---------------------------------------------------------------------------
INSERT INTO operator (operator_id, active_status, certifications, created_at,
                      hire_date, operator_name, role, updated_at)
VALUES
    ('OP-DEMO-01', 'ACTIVE', 'HACCP', now(), CURRENT_DATE - 400, 'Awa DEMO (QC)', 'QC_INSPECTOR', now()),
    ('OP-DEMO-02', 'ACTIVE', 'Sechage solaire', now(), CURRENT_DATE - 300, 'Issa DEMO (Sechage)', 'DRYER', now())
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2) Equipements (2 actifs, 1 en maintenance -> utilisation = 67 %)
-- ---------------------------------------------------------------------------
INSERT INTO pt_equipment (equipment_id, capacity_kg_per_hour, created_at,
                       energy_consumption_kwh_per_kg, equipment_name, equipment_type,
                       last_maintenance_date, location, maintenance_status, updated_at)
VALUES
    ('EQ-DEMO-01', 120.00, now(), 0.8500, 'Sechoir solaire DEMO 1', 'SOLAR_DRYER',
     CURRENT_DATE - 20, 'Unite PT', 'ACTIVE', now()),
    ('EQ-DEMO-02', 80.00, now(), 1.1000, 'Laveuse DEMO 1', 'WASHING',
     CURRENT_DATE - 35, 'Unite PT', 'ACTIVE', now()),
    ('EQ-DEMO-03', 60.00, now(), 0.9500, 'Conditionneuse DEMO 1', 'PACKAGING',
     CURRENT_DATE - 5, 'Unite PT', 'MAINTENANCE', now())
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 3) Lots (3 actifs : WASHING / DRYING / PACKAGING, 1 termine, 1 expedie)
--    Les lots portent le prefixe LOT-DEMO pour etre identifiables.
-- ---------------------------------------------------------------------------
INSERT INTO batch (batch_id, block_id, created_at, current_status, farm_id,
                   harvest_date, harvest_quantity_kg, mango_variety, updated_at)
VALUES
    ('LOT-DEMO-001', 'BLOC-A', now(), 'WASHING',   'FERME-DEMO', CURRENT_DATE - 1, 520.00, 'KEITT',   now()),
    ('LOT-DEMO-002', 'BLOC-A', now(), 'DRYING',    'FERME-DEMO', CURRENT_DATE - 2, 480.50, 'KEITT',   now()),
    ('LOT-DEMO-003', 'BLOC-B', now(), 'PACKAGING', 'FERME-DEMO', CURRENT_DATE - 3, 610.25, 'AMÉLIE',  now()),
    ('LOT-DEMO-004', 'BLOC-B', now(), 'COMPLETED', 'FERME-DEMO', CURRENT_DATE - 5, 395.00, 'KENT',    now()),
    ('LOT-DEMO-005', 'BLOC-C', now(), 'SHIPPED',   'FERME-DEMO', CURRENT_DATE - 6, 275.75, 'TOMMY',   now())
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 4) Recoltes (1 evenement par lot, conserve le numero de lot d'origine)
--    plants_harvest_id est volontairement omis : la colonne peut ne pas exister.
-- ---------------------------------------------------------------------------
INSERT INTO harvest_event (harvest_id, batch_id, block_id, created_at, farm_id,
                           harvest_date, harvest_quantity_kg, harvest_time,
                           mango_variety, quality_grade, updated_at)
VALUES
    ('HE-DEMO-001', 'LOT-DEMO-001', 'BLOC-A', now(), 'FERME-DEMO', CURRENT_DATE - 1, 520.00, time '07:30', 'KEITT',  'A', now()),
    ('HE-DEMO-002', 'LOT-DEMO-002', 'BLOC-A', now(), 'FERME-DEMO', CURRENT_DATE - 2, 480.50, time '08:10', 'KEITT',  'A', now()),
    ('HE-DEMO-003', 'LOT-DEMO-003', 'BLOC-B', now(), 'FERME-DEMO', CURRENT_DATE - 3, 610.25, time '07:45', 'AMÉLIE', 'B', now()),
    ('HE-DEMO-004', 'LOT-DEMO-004', 'BLOC-B', now(), 'FERME-DEMO', CURRENT_DATE - 5, 395.00, time '09:00', 'KENT',   'A', now()),
    ('HE-DEMO-005', 'LOT-DEMO-005', 'BLOC-C', now(), 'FERME-DEMO', CURRENT_DATE - 6, 275.75, time '08:25', 'TOMMY',  'A', now())
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 5) Controles qualite (4 PASS, 1 FAIL, 1 REWORK -> taux ~67 %)
-- ---------------------------------------------------------------------------
INSERT INTO qc_checkpoint (checkpoint_id, checkpoint_time, created_at, defects,
                           defects_count, notes, result, stage, updated_at,
                           batch_id, inspector_id)
VALUES
    ('QC-DEMO-001', (CURRENT_DATE - 1) + time '10:00', now(), NULL, 0, 'Conforme', 'PASS',   'INTAKE',    now(), 'LOT-DEMO-001', 'OP-DEMO-01'),
    ('QC-DEMO-002', (CURRENT_DATE - 2) + time '11:00', now(), '2 fruits meurtris', 2, 'Ecart mineur', 'PASS', 'WASHING', now(), 'LOT-DEMO-002', 'OP-DEMO-01'),
    ('QC-DEMO-003', (CURRENT_DATE - 3) + time '09:30', now(), 'Humidite trop elevee', 5, 'Retour sechoir', 'REWORK', 'DRYING', now(), 'LOT-DEMO-003', 'OP-DEMO-01'),
    ('QC-DEMO-004', (CURRENT_DATE - 4) + time '14:00', now(), NULL, 0, 'Conforme', 'PASS',   'DRYING',    now(), 'LOT-DEMO-003', 'OP-DEMO-01'),
    ('QC-DEMO-005', (CURRENT_DATE - 5) + time '15:30', now(), 'Moisissure', 3, 'Lot ecarte', 'FAIL',  'FINAL',     now(), 'LOT-DEMO-004', 'OP-DEMO-01'),
    ('QC-DEMO-006', (CURRENT_DATE - 6) + time '16:00', now(), NULL, 0, 'Conforme', 'PASS',   'PACKAGING', now(), 'LOT-DEMO-005', 'OP-DEMO-01')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 6) Sechages (alimente l'energie, la production et le graphique hebdomadaire)
-- ---------------------------------------------------------------------------
INSERT INTO drying_run (run_id, actual_temperature_c, created_at, duration_hours,
                        end_moisture_pct, end_time, energy_usage_kwh,
                        start_moisture_pct, start_time, target_temperature_c,
                        updated_at, batch_id, equipment_id, operator_id)
VALUES
    ('DR-DEMO-001', 54.50, now(), 6.50, 12.00, (CURRENT_DATE - 1) + time '14:30', 118.40,
     78.00, (CURRENT_DATE - 1) + time '08:00', 55.00, now(), 'LOT-DEMO-001', 'EQ-DEMO-01', 'OP-DEMO-02'),
    ('DR-DEMO-002', 55.20, now(), 5.00, 11.50, (CURRENT_DATE - 2) + time '13:00', 96.75,
     76.00, (CURRENT_DATE - 2) + time '08:00', 55.00, now(), 'LOT-DEMO-002', 'EQ-DEMO-01', 'OP-DEMO-02'),
    ('DR-DEMO-003', 54.80, now(), 7.00, 13.00, (CURRENT_DATE - 3) + time '15:00', 142.10,
     80.00, (CURRENT_DATE - 3) + time '08:00', 55.00, now(), 'LOT-DEMO-003', 'EQ-DEMO-01', 'OP-DEMO-02')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 7) Lavage / tri (alimente la consommation d'eau : 1 380 L / 575 kg = 2.40)
-- ---------------------------------------------------------------------------
INSERT INTO wash_sort_record (record_id, created_at, end_time, input_quantity_kg,
                              output_quantity_kg, start_time, updated_at,
                              waste_quantity_kg, water_usage_liters, batch_id,
                              equipment_id, operator_id)
VALUES
    ('WS-DEMO-001', now(), (CURRENT_DATE - 1) + time '09:30', 520.00, 480.00,
     (CURRENT_DATE - 1) + time '07:45', now(), 40.00, 1250.00, 'LOT-DEMO-001', 'EQ-DEMO-02', 'OP-DEMO-02'),
    ('WS-DEMO-002', now(), (CURRENT_DATE - 3) + time '10:15', 610.25, 565.00,
     (CURRENT_DATE - 3) + time '08:15', now(), 45.25, 1380.00, 'LOT-DEMO-003', 'EQ-DEMO-02', 'OP-DEMO-02')
ON CONFLICT DO NOTHING;

COMMIT;

-- ---------------------------------------------------------------------------
-- Verification attendue apres execution (information seulement) :
--   harvest_event   : 5 lignes
--   batch           : 5 lots (3 actifs)
--   qc_checkpoint   : 6 controles (4 PASS)
--   drying_run      : 3 sechages
--   wash_sort_record: 2 lavages
-- ---------------------------------------------------------------------------
