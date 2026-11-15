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
-- PREREQUIS :
--   Les tables PT doivent exister. Depuis V14__producttransformation_schema.sql
--   (migration Flyway possedee par le module) elles sont creees automatiquement,
--   y compris harvest_event.plants_harvest_id.
--
--   Sur une base construite AVANT cette migration, appliquer d'abord :
--
--       ALTER TABLE harvest_event ADD COLUMN plants_harvest_id bigint;
--
--   Ce script n'ajoute volontairement aucune colonne, ne cree aucune table,
--   ne modifie aucune table existante et ne supprime aucune donnee.
--
-- UTILISATION (a executer uniquement sur une base de demonstration) :
--   docker exec -i sustainable-farm-db \
--     psql -U postgres -d sustainable_farm < backend/src/main/resources/seed/producttransformation-demo-seed.sql
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

-- ---------------------------------------------------------------------------
-- 8) Receptions (une par lot, alimente l'ecran de reception)
-- ---------------------------------------------------------------------------
INSERT INTO raw_intake (intake_id, created_at, intake_date, intake_operator,
                        received_grade, received_quantity_kg, received_variety,
                        source_block, source_farm, updated_at, batch_id)
VALUES
    ('RI-DEMO-001', now(), CURRENT_DATE - 1, 'Moussa DEMO (Lavage)', 'A', 520.00, 'KEITT',   'BLOC-A', 'FERME-DEMO', now(), 'LOT-DEMO-001'),
    ('RI-DEMO-002', now(), CURRENT_DATE - 2, 'Moussa DEMO (Lavage)', 'A', 480.50, 'KEITT',   'BLOC-A', 'FERME-DEMO', now(), 'LOT-DEMO-002'),
    ('RI-DEMO-003', now(), CURRENT_DATE - 3, 'Moussa DEMO (Lavage)', 'B', 610.25, 'AMÉLIE',  'BLOC-B', 'FERME-DEMO', now(), 'LOT-DEMO-003'),
    ('RI-DEMO-004', now(), CURRENT_DATE - 5, 'Moussa DEMO (Lavage)', 'A', 395.00, 'KENT',    'BLOC-B', 'FERME-DEMO', now(), 'LOT-DEMO-004'),
    ('RI-DEMO-005', now(), CURRENT_DATE - 6, 'Moussa DEMO (Lavage)', 'A', 275.75, 'TOMMY',   'BLOC-C', 'FERME-DEMO', now(), 'LOT-DEMO-005')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 9) Conditionnement (1 lot export-ready, 1 lot en attente)
-- ---------------------------------------------------------------------------
INSERT INTO packaging_record (record_id, created_at, export_ready, lot_code,
                              package_quantity_kg, package_type, packaging_date,
                              updated_at, batch_id, equipment_id, operator_id)
VALUES
    ('PKG-DEMO-001', now(), TRUE,  'LOT-DEMO-003-PKG', 565.00, '2KG_BAG', CURRENT_DATE - 3, now(), 'LOT-DEMO-003', 'EQ-DEMO-03', 'OP-DEMO-02'),
    ('PKG-DEMO-002', now(), FALSE, 'LOT-DEMO-004-PKG', 360.00, 'BULK',    CURRENT_DATE - 5, now(), 'LOT-DEMO-004', 'EQ-DEMO-03', 'OP-DEMO-02')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 10) Conformite (HACCP / export UE / tracabilite)
-- ---------------------------------------------------------------------------
INSERT INTO compliance_record (record_id, audit_date, compliance_type, created_at,
                               evidence, next_audit_date, requirement, result,
                               updated_at, auditor_id, batch_id)
VALUES
    ('CR-DEMO-001', CURRENT_DATE - 5, 'HACCP',        now(), 'Plan HACCP 2026', CURRENT_DATE + 85,  'Maitrise des points critiques', 'COMPLIANT',     now(), 'OP-DEMO-01', 'LOT-DEMO-004'),
    ('CR-DEMO-002', CURRENT_DATE - 6, 'EU_EXPORT',    now(), 'Certificat phytosanitaire', CURRENT_DATE + 55, 'Exigences export UE', 'COMPLIANT',     now(), 'OP-DEMO-01', 'LOT-DEMO-005'),
    ('CR-DEMO-003', CURRENT_DATE - 3, 'TRACEABILITY', now(), NULL, NULL, 'Tracabilite amont/aval', 'PENDING', now(), 'OP-DEMO-01', 'LOT-DEMO-003')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 11) Historique de recolte (12 derniers mois, serie du graphique annuel)
-- ---------------------------------------------------------------------------
INSERT INTO historical_harvest (mango_variety, month, week, year, created_at,
                                harvest_quantity_kg, quality_grade_a_pct,
                                quality_grade_b_pct, quality_grade_c_pct,
                                rainfall_mm, temperature_avg_c,
                                updated_at, weather_condition)
SELECT
    'KEITT',
    EXTRACT(MONTH FROM month_start)::int,
    EXTRACT(WEEK  FROM month_start)::int,
    EXTRACT(YEAR  FROM month_start)::int,
    now(),
    ROUND((1800 + (EXTRACT(MONTH FROM month_start)::int * 95))::numeric, 2),
    ROUND((68 + (EXTRACT(MONTH FROM month_start)::int % 5) * 2.5)::numeric, 2),
    ROUND((22 - (EXTRACT(MONTH FROM month_start)::int % 4) * 1.5)::numeric, 2),
    ROUND((10 - (EXTRACT(MONTH FROM month_start)::int % 3))::numeric, 2),
    ROUND((40 + (EXTRACT(MONTH FROM month_start)::int * 9))::numeric, 2),
    ROUND((24.5 + (EXTRACT(MONTH FROM month_start)::int % 4) * 0.8)::numeric, 2),
    now(),
    'DEMO'
FROM generate_series(
    date_trunc('month', CURRENT_DATE) - interval '11 months',
    date_trunc('month', CURRENT_DATE),
    interval '1 month'
) AS month_start
ON CONFLICT DO NOTHING;

COMMIT;

-- ---------------------------------------------------------------------------
-- Verification attendue apres execution (information seulement) :
--   harvest_event   : 5 lignes
--   batch           : 5 lots (3 actifs)
--   raw_intake      : 5 receptions
--   qc_checkpoint   : 6 controles (4 PASS)
--   drying_run      : 3 sechages
--   wash_sort_record: 2 lavages
--   packaging_record: 2 conditionnements (1 export-ready)
--   compliance_record: 3 enregistrements (2 conformes, 1 en attente)
--   historical_harvest: 12 mois
-- ---------------------------------------------------------------------------
