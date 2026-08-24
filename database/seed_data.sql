-- Sustainable Farm Product Transformation - Seed Data
-- Purpose: Reference and test data for development
-- Environment: Development
-- Author: Abdoul Ben Fatao SANON
-- Date: 2026-08-24

-- ============================================================================
-- EQUIPMENT SEED DATA
-- ============================================================================

INSERT INTO equipment (equipment_id, equipment_name, equipment_type, capacity_kg_per_hour, energy_consumption_kwh_per_kg, location, maintenance_status, last_maintenance_date) VALUES
('EQ-001', 'Industrial Washing Station Alpha', 'WASHING', 500.00, 0.15, 'Processing Zone A', 'ACTIVE', '2026-08-01'),
('EQ-002', 'Industrial Washing Station Beta', 'WASHING', 450.00, 0.18, 'Processing Zone A', 'ACTIVE', '2026-08-05'),
('EQ-003', 'Testa Dryer Unit 1', 'TESTA_DRYER', 200.00, 2.50, 'Drying Zone B', 'ACTIVE', '2026-08-10'),
('EQ-004', 'Testa Dryer Unit 2', 'TESTA_DRYER', 200.00, 2.45, 'Drying Zone B', 'ACTIVE', '2026-08-12'),
('EQ-005', 'Solar Dryer Array A', 'SOLAR_DRYER', 150.00, 0.00, 'Solar Zone C', 'ACTIVE', '2026-08-15'),
('EQ-006', 'Tunnel Dryer System', 'TUNNEL_DRYER', 300.00, 1.80, 'Drying Zone B', 'MAINTENANCE', '2026-07-20'),
('EQ-007', 'Packaging Line 1', 'PACKAGING', 400.00, 0.50, 'Packaging Zone D', 'ACTIVE', '2026-08-18'),
('EQ-008', 'Packaging Line 2', 'PACKAGING', 350.00, 0.55, 'Packaging Zone D', 'ACTIVE', '2026-08-18');

-- ============================================================================
-- OPERATOR SEED DATA
-- ============================================================================

INSERT INTO operator (operator_id, operator_name, role, certifications, active_status, hire_date) VALUES
('OP-001', 'Jean Kaboré', 'WASHER', 'Food Safety Level 2, HACCP Basic', 'ACTIVE', '2026-01-15'),
('OP-002', 'Marie Sanou', 'WASHER', 'Food Safety Level 2', 'ACTIVE', '2026-02-01'),
('OP-003', 'Abdoulaye Ouédraogo', 'DRYER', 'Drying Technology Certificate, Energy Management', 'ACTIVE', '2026-01-20'),
('OP-004', 'Fatima Zongo', 'DRYER', 'Drying Technology Certificate', 'ACTIVE', '2026-03-10'),
('OP-005', 'Paulin Yaméogo', 'PACKAGER', 'Packaging Standards, EU Export Requirements', 'ACTIVE', '2026-02-15'),
('OP-006', 'Aminata Diallo', 'PACKAGER', 'Packaging Standards', 'ACTIVE', '2026-03-01'),
('OP-007', 'Dr. Henri Somda', 'QC_INSPECTOR', 'Quality Control Level 3, HACCP Advanced, EU Standards', 'ACTIVE', '2025-09-01'),
('OP-008', 'Clarisse Traoré', 'QC_INSPECTOR', 'Quality Control Level 2, HACCP Basic', 'ACTIVE', '2026-04-01'),
('OP-009', 'Michel Kabré', 'SUPERVISOR', 'Production Management, Food Safety Level 3, HACCP Lead', 'ACTIVE', '2025-08-15'),
('OP-010', 'Aïcha Compaoré', 'AUDITOR', 'HACCP Auditor Certification, ISO 22000 Lead Auditor', 'ACTIVE', '2025-07-01');

-- ============================================================================
-- HISTORICAL HARVEST SEED DATA (7 years for forecasting)
-- ============================================================================

INSERT INTO historical_harvest (year, month, week, mango_variety, harvest_quantity_kg, quality_grade_a_pct, quality_grade_b_pct, quality_grade_c_pct, weather_condition, rainfall_mm, temperature_avg_c) VALUES
-- 2019 Data
(2019, 4, 15, 'KEITT', 1250.00, 75.5, 18.0, 5.5, 'Sunny', 45.2, 32.5),
(2019, 4, 16, 'KEITT', 1180.00, 72.0, 20.5, 6.5, 'Partly Cloudy', 52.1, 31.8),
(2019, 5, 18, 'KENT', 980.00, 68.5, 22.0, 8.0, 'Rainy', 85.3, 29.5),
(2019, 5, 19, 'KENT', 1050.00, 70.0, 21.0, 7.5, 'Cloudy', 62.0, 30.2),
(2019, 6, 22, 'TOMMY', 850.00, 65.0, 24.0, 9.5, 'Sunny', 38.5, 33.1),
(2019, 6, 23, 'TOMMY', 920.00, 67.5, 22.5, 8.0, 'Partly Cloudy', 41.2, 32.8),

-- 2020 Data
(2020, 4, 14, 'KEITT', 1350.00, 78.0, 16.5, 4.5, 'Sunny', 42.8, 33.0),
(2020, 4, 15, 'KEITT', 1280.00, 76.5, 17.5, 5.0, 'Sunny', 39.5, 32.9),
(2020, 5, 17, 'KENT', 1100.00, 71.0, 20.0, 7.5, 'Partly Cloudy', 55.3, 30.8),
(2020, 5, 18, 'KENT', 1150.00, 72.5, 19.0, 7.0, 'Cloudy', 58.1, 30.5),
(2020, 6, 21, 'TOMMY', 920.00, 66.5, 23.0, 8.5, 'Sunny', 35.7, 33.5),
(2020, 6, 22, 'TOMMY', 980.00, 68.0, 22.0, 8.0, 'Partly Cloudy', 40.2, 33.2),

-- 2021 Data
(2021, 4, 13, 'KEITT', 1420.00, 79.5, 15.0, 4.5, 'Sunny', 38.2, 33.8),
(2021, 4, 14, 'KEITT', 1350.00, 77.0, 16.5, 5.5, 'Sunny', 41.5, 33.2),
(2021, 5, 16, 'KENT', 1180.00, 73.0, 19.0, 6.5, 'Partly Cloudy', 52.8, 31.2),
(2021, 5, 17, 'KENT', 1220.00, 74.5, 18.0, 6.0, 'Cloudy', 55.0, 30.9),
(2021, 6, 20, 'TOMMY', 980.00, 68.0, 21.5, 8.0, 'Sunny', 33.9, 34.0),
(2021, 6, 21, 'TOMMY', 1050.00, 69.5, 20.5, 7.5, 'Partly Cloudy', 38.5, 33.7),

-- 2022 Data
(2022, 4, 12, 'KEITT', 1500.00, 81.0, 14.0, 4.0, 'Sunny', 35.5, 34.2),
(2022, 4, 13, 'KEITT', 1420.00, 78.5, 15.5, 4.5, 'Sunny', 38.8, 33.9),
(2022, 5, 15, 'KENT', 1250.00, 74.5, 18.0, 6.0, 'Partly Cloudy', 48.5, 31.5),
(2022, 5, 16, 'KENT', 1300.00, 76.0, 17.0, 5.5, 'Cloudy', 51.2, 31.2),
(2022, 6, 19, 'TOMMY', 1050.00, 70.0, 20.5, 7.5, 'Sunny', 32.1, 34.5),
(2022, 6, 20, 'TOMMY', 1120.00, 71.5, 19.5, 7.0, 'Partly Cloudy', 36.8, 34.2),

-- 2023 Data
(2023, 4, 11, 'KEITT', 1580.00, 82.5, 13.0, 3.5, 'Sunny', 33.2, 34.8),
(2023, 4, 12, 'KEITT', 1500.00, 80.0, 14.5, 4.0, 'Sunny', 36.5, 34.5),
(2023, 5, 14, 'KENT', 1320.00, 76.0, 17.0, 5.5, 'Partly Cloudy', 45.2, 31.8),
(2023, 5, 15, 'KENT', 1380.00, 77.5, 16.0, 5.0, 'Cloudy', 48.0, 31.5),
(2023, 6, 18, 'TOMMY', 1120.00, 72.0, 19.5, 7.0, 'Sunny', 30.5, 35.0),
(2023, 6, 19, 'TOMMY', 1200.00, 73.5, 18.5, 6.5, 'Partly Cloudy', 35.2, 34.8),

-- 2024 Data
(2024, 4, 10, 'KEITT', 1650.00, 84.0, 12.0, 3.0, 'Sunny', 31.0, 35.2),
(2024, 4, 11, 'KEITT', 1580.00, 81.5, 13.5, 3.5, 'Sunny', 34.8, 34.9),
(2024, 5, 13, 'KENT', 1400.00, 77.5, 16.0, 5.0, 'Partly Cloudy', 42.5, 32.0),
(2024, 5, 14, 'KENT', 1450.00, 79.0, 15.0, 4.5, 'Cloudy', 45.8, 31.8),
(2024, 6, 17, 'TOMMY', 1200.00, 74.0, 18.5, 6.5, 'Sunny', 28.9, 35.5),
(2024, 6, 18, 'TOMMY', 1280.00, 75.5, 17.5, 6.0, 'Partly Cloudy', 33.5, 35.2),

-- 2025 Data
(2025, 4, 9, 'KEITT', 1720.00, 85.5, 11.0, 2.5, 'Sunny', 29.5, 35.8),
(2025, 4, 10, 'KEITT', 1650.00, 83.0, 12.5, 3.0, 'Sunny', 33.2, 35.5),
(2025, 5, 12, 'KENT', 1480.00, 79.0, 15.0, 4.5, 'Partly Cloudy', 40.8, 32.2),
(2025, 5, 13, 'KENT', 1520.00, 80.5, 14.0, 4.0, 'Cloudy', 43.5, 32.0),
(2025, 6, 16, 'TOMMY', 1280.00, 76.0, 17.5, 6.0, 'Sunny', 27.2, 36.0),
(2025, 6, 17, 'TOMMY', 1350.00, 77.5, 16.5, 5.5, 'Partly Cloudy', 31.8, 35.8);

-- ============================================================================
-- SAMPLE BATCH DATA (for testing)
-- ============================================================================

INSERT INTO batch (batch_id, harvest_date, mango_variety, harvest_quantity_kg, current_status, farm_id, block_id) VALUES
('BATCH-2024-001', '2024-04-15', 'KEITT', 1250.00, 'CREATED', 'FARM-001', 'BLOCK-A'),
('BATCH-2024-002', '2024-04-16', 'KEITT', 1180.00, 'INTAKE', 'FARM-001', 'BLOCK-B'),
('BATCH-2024-003', '2024-05-18', 'KENT', 980.00, 'WASHING', 'FARM-002', 'BLOCK-C'),
('BATCH-2024-004', '2024-05-19', 'KENT', 1050.00, 'DRYING', 'FARM-002', 'BLOCK-D'),
('BATCH-2024-005', '2024-06-22', 'TOMMY', 850.00, 'PACKAGING', 'FARM-003', 'BLOCK-E');

-- ============================================================================
-- SAMPLE RAW INTAKE DATA (for testing)
-- ============================================================================

INSERT INTO raw_intake (intake_id, batch_id, source_farm, source_block, intake_date, received_quantity_kg, received_variety, received_grade, intake_operator) VALUES
('INTAKE-2024-001', 'BATCH-2024-001', 'FARM-001', 'BLOCK-A', '2024-04-15', 1250.00, 'KEITT', 'A', 'OP-001'),
('INTAKE-2024-002', 'BATCH-2024-002', 'FARM-001', 'BLOCK-B', '2024-04-16', 1180.00, 'KEITT', 'A', 'OP-002'),
('INTAKE-2024-003', 'BATCH-2024-003', 'FARM-002', 'BLOCK-C', '2024-05-18', 980.00, 'KENT', 'B', 'OP-001'),
('INTAKE-2024-004', 'BATCH-2024-004', 'FARM-002', 'BLOCK-D', '2024-05-19', 1050.00, 'KENT', 'A', 'OP-002'),
('INTAKE-2024-005', 'BATCH-2024-005', 'FARM-003', 'BLOCK-E', '2024-06-22', 850.00, 'TOMMY', 'B', 'OP-001');

-- ============================================================================
-- SAMPLE HARVEST EVENT DATA (for testing)
-- ============================================================================

INSERT INTO harvest_event (harvest_id, batch_id, harvest_date, harvest_time, mango_variety, farm_id, block_id, harvest_quantity_kg, quality_grade, quality_grade_description, harvest_team_id, harvest_supervisor, weather_conditions, storage_location) VALUES
('HVST-2024-001', 'BATCH-2024-001', '2024-04-15', '07:30:00', 'KEITT', 'FARM-001', 'BLOCK-A', 1250.00, 'A', 'Premium quality, no defects', 'TEAM-ALPHA', 'Michel Kabré', 'Sunny', 'STORAGE-A'),
('HVST-2024-002', 'BATCH-2024-002', '2024-04-16', '08:00:00', 'KEITT', 'FARM-001', 'BLOCK-B', 1180.00, 'A', 'Premium quality, minimal defects', 'TEAM-ALPHA', 'Michel Kabré', 'Partly Cloudy', 'STORAGE-A'),
('HVST-2024-003', 'BATCH-2024-003', '2024-05-18', '07:45:00', 'KENT', 'FARM-002', 'BLOCK-C', 980.00, 'B', 'Standard quality, minor defects', 'TEAM-BETA', 'Michel Kabré', 'Rainy', 'STORAGE-B'),
('HVST-2024-004', 'BATCH-2024-004', '2024-05-19', '08:15:00', 'KENT', 'FARM-002', 'BLOCK-D', 1050.00, 'A', 'Premium quality, no defects', 'TEAM-BETA', 'Michel Kabré', 'Cloudy', 'STORAGE-B'),
('HVST-2024-005', 'BATCH-2024-005', '2024-06-22', '07:00:00', 'TOMMY', 'FARM-003', 'BLOCK-E', 850.00, 'B', 'Standard quality, some surface defects', 'TEAM-GAMMA', 'Michel Kabré', 'Sunny', 'STORAGE-C');

-- ============================================================================
-- SAMPLE WASH/SORT RECORD DATA (for testing)
-- ============================================================================

INSERT INTO wash_sort_record (record_id, batch_id, input_quantity_kg, output_quantity_kg, waste_quantity_kg, water_usage_liters, start_time, end_time, equipment_id, operator_id) VALUES
('WSH-2024-001', 'BATCH-2024-002', 1180.00, 1050.00, 130.00, 450.00, '2024-04-16 09:00:00', '2024-04-16 11:30:00', 'EQ-001', 'OP-001'),
('WSH-2024-002', 'BATCH-2024-003', 980.00, 880.00, 100.00, 380.00, '2024-05-18 10:00:00', '2024-05-18 12:15:00', 'EQ-002', 'OP-002'),
('WSH-2024-003', 'BATCH-2024-004', 1050.00, 950.00, 100.00, 400.00, '2024-05-19 09:30:00', '2024-05-19 12:00:00', 'EQ-001', 'OP-001');

-- ============================================================================
-- SAMPLE DRYING RUN DATA (for testing)
-- ============================================================================

INSERT INTO drying_run (run_id, batch_id, duration_hours, target_temperature_c, actual_temperature_c, start_moisture_pct, end_moisture_pct, energy_usage_kwh, start_time, end_time, equipment_id, operator_id) VALUES
('DRY-2024-001', 'BATCH-2024-003', 18.50, 55.00, 54.50, 82.00, 14.50, 2450.00, '2024-05-18 13:00:00', '2024-05-19 07:30:00', 'EQ-003', 'OP-003'),
('DRY-2024-002', 'BATCH-2024-004', 17.75, 55.00, 55.20, 80.00, 13.80, 2380.00, '2024-05-19 13:00:00', '2024-05-20 06:45:00', 'EQ-004', 'OP-004');

-- ============================================================================
-- SAMPLE PACKAGING RECORD DATA (for testing)
-- ============================================================================

INSERT INTO packaging_record (record_id, batch_id, package_type, package_quantity_kg, lot_code, export_ready, packaging_date, equipment_id, operator_id) VALUES
('PKG-2024-001', 'BATCH-2024-004', '1KG_BAG', 920.00, 'LOT-2024-05-20-001', true, '2024-05-20', 'EQ-007', 'OP-005'),
('PKG-2024-002', 'BATCH-2024-005', '2KG_BAG', 800.00, 'LOT-2024-06-25-001', false, '2024-06-25', 'EQ-008', 'OP-006');

-- ============================================================================
-- SAMPLE QC CHECKPOINT DATA (for testing)
-- ============================================================================

INSERT INTO qc_checkpoint (checkpoint_id, batch_id, stage, result, defects, defects_count, inspector_id, checkpoint_time, notes) VALUES
('QC-2024-001', 'BATCH-2024-002', 'WASHING', 'PASS', 'Minor surface blemishes on 5% of fruit', 5, 'OP-007', '2024-04-16 11:45:00', 'Mandatory washing checkpoint - passed'),
('QC-2024-002', 'BATCH-2024-003', 'WASHING', 'PASS', 'No significant defects detected', 0, 'OP-007', '2024-05-18 12:30:00', 'Mandatory washing checkpoint - passed'),
('QC-2024-003', 'BATCH-2024-003', 'DRYING', 'PASS', 'Moisture content within target range', 0, 'OP-007', '2024-05-19 08:00:00', 'Drying quality check - passed'),
('QC-2024-004', 'BATCH-2024-004', 'WASHING', 'PASS', 'No defects detected', 0, 'OP-008', '2024-05-19 12:15:00', 'Mandatory washing checkpoint - passed'),
('QC-2024-005', 'BATCH-2024-004', 'COOLING', 'PASS', 'Temperature and moisture acceptable', 0, 'OP-008', '2024-05-20 07:00:00', 'Mandatory cooling checkpoint - passed'),
('QC-2024-006', 'BATCH-2024-004', 'FINAL', 'PASS', 'Final quality inspection passed', 0, 'OP-007', '2024-05-20 14:00:00', 'Final product release - passed');

-- ============================================================================
-- SAMPLE COMPLIANCE RECORD DATA (for testing)
-- ============================================================================

INSERT INTO compliance_record (record_id, batch_id, compliance_type, requirement, result, evidence, auditor_id, audit_date, next_audit_date) VALUES
('COMP-2024-001', 'BATCH-2024-004', 'HACCP', 'Critical control points monitored and documented', 'COMPLIANT', 'CCP monitoring records complete and verified', 'OP-010', '2024-05-20', '2024-08-20'),
('COMP-2024-002', 'BATCH-2024-004', 'FOOD_SAFETY', 'Temperature control during drying process', 'COMPLIANT', 'Temperature logs show consistent control within acceptable range', 'OP-010', '2024-05-20', '2024-08-20'),
('COMP-2024-003', 'BATCH-2024-004', 'EU_EXPORT', 'Moisture content within EU requirements (6-18%)', 'COMPLIANT', 'Final moisture content: 13.8% (within 6-18% range)', 'OP-010', '2024-05-20', '2024-08-20'),
('COMP-2024-004', 'BATCH-2024-004', 'HYGIENE', 'Personnel hygiene and sanitation procedures', 'COMPLIANT', 'Hygiene checklist completed, all requirements met', 'OP-010', '2024-05-20', '2024-08-20'),
('COMP-2024-005', 'BATCH-2024-004', 'TRACEABILITY', 'Batch traceability from harvest to packaging', 'COMPLIANT', 'Full traceability documentation complete and verified', 'OP-010', '2024-05-20', '2024-08-20');

-- ============================================================================
-- SEED DATA SUMMARY
-- ============================================================================

-- Equipment: 8 records (various types and statuses)
-- Operators: 10 records (all roles covered)
-- Historical Harvest: 42 records (7 years of data)
-- Batches: 5 records (various statuses)
-- Raw Intake: 5 records (linked to batches)
-- Harvest Events: 5 records (linked to batches)
-- Wash/Sort Records: 3 records (linked to batches)
-- Drying Runs: 2 records (linked to batches)
-- Packaging Records: 2 records (linked to batches)
-- QC Checkpoints: 6 records (including mandatory checkpoints)
-- Compliance Records: 5 records (full HACCP compliance)

-- Total Records: 93 records for development and testing