-- Sustainable Farm Product Transformation Database Schema
-- Based on Validated MERISE MCD (Week 5 Phase 3 - 10.0/10 Score)
-- Technology: PostgreSQL 15+
-- Author: Abdoul Ben Fatao SANON
-- Date: 2026-08-14

-- Drop existing tables (for clean setup)
DROP TABLE IF EXISTS compliance_record CASCADE;
DROP TABLE IF EXISTS packaging_record CASCADE;
DROP TABLE IF EXISTS qc_checkpoint CASCADE;
DROP TABLE IF EXISTS drying_run CASCADE;
DROP TABLE IF EXISTS wash_sort_record CASCADE;
DROP TABLE IF EXISTS batch CASCADE;
DROP TABLE IF EXISTS raw_intake CASCADE;
DROP TABLE IF EXISTS historical_harvest CASCADE;
DROP TABLE IF EXISTS harvest_event CASCADE;
DROP TABLE IF EXISTS operator CASCADE;
DROP TABLE IF EXISTS equipment CASCADE;

-- ============================================================================
-- SUPPORTING ENTITIES
-- ============================================================================

-- EQUIPMENT: Machinery and equipment data
CREATE TABLE equipment (
    equipment_id VARCHAR(50) PRIMARY KEY,
    equipment_name VARCHAR(100) NOT NULL,
    equipment_type VARCHAR(50) NOT NULL CHECK (equipment_type IN ('WASHING', 'DRYING', 'PACKAGING', 'TESTA_DRYER', 'SOLAR_DRYER', 'TUNNEL_DRYER')),
    capacity_kg_per_hour DECIMAL(10,2) NOT NULL,
    energy_consumption_kwh_per_kg DECIMAL(10,4) NOT NULL,
    location VARCHAR(100),
    maintenance_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (maintenance_status IN ('ACTIVE', 'MAINTENANCE', 'RETIRED')),
    last_maintenance_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- OPERATOR: Personnel data
CREATE TABLE operator (
    operator_id VARCHAR(50) PRIMARY KEY,
    operator_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('WASHER', 'DRYER', 'PACKAGER', 'QC_INSPECTOR', 'SUPERVISOR', 'AUDITOR')),
    certifications TEXT,
    active_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (active_status IN ('ACTIVE', 'INACTIVE')),
    hire_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- INTEGRATION ENTITIES
-- ============================================================================

-- HARVEST_EVENT: Harvest data from Plants workstream
CREATE TABLE harvest_event (
    harvest_id VARCHAR(50) PRIMARY KEY,
    batch_id VARCHAR(50) UNIQUE NOT NULL,
    harvest_date DATE NOT NULL,
    harvest_time TIME,
    mango_variety VARCHAR(20) NOT NULL CHECK (mango_variety IN ('KEITT', 'KENT', 'TOMMY', 'AMÉLIE', 'OTHER')),
    farm_id VARCHAR(50) NOT NULL,
    block_id VARCHAR(50) NOT NULL,
    harvest_quantity_kg DECIMAL(10,2) NOT NULL CHECK (harvest_quantity_kg > 0),
    quality_grade VARCHAR(1) NOT NULL CHECK (quality_grade IN ('A', 'B', 'C', 'D')),
    quality_grade_description VARCHAR(200),
    harvest_team_id VARCHAR(50),
    harvest_supervisor VARCHAR(100),
    weather_conditions VARCHAR(200),
    storage_location VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- HISTORICAL_HARVEST: Aggregated historical harvest data
CREATE TABLE historical_harvest (
    year INTEGER NOT NULL,
    month INTEGER NOT NULL CHECK (month BETWEEN 1 AND 12),
    week INTEGER NOT NULL CHECK (week BETWEEN 1 AND 53),
    mango_variety VARCHAR(20) NOT NULL CHECK (mango_variety IN ('KEITT', 'KENT', 'TOMMY', 'AMÉLIE', 'OTHER')),
    harvest_quantity_kg DECIMAL(10,2) NOT NULL,
    quality_grade_a_pct DECIMAL(5,2) CHECK (quality_grade_a_pct BETWEEN 0 AND 100),
    quality_grade_b_pct DECIMAL(5,2) CHECK (quality_grade_b_pct BETWEEN 0 AND 100),
    quality_grade_c_pct DECIMAL(5,2) CHECK (quality_grade_c_pct BETWEEN 0 AND 100),
    weather_condition VARCHAR(50),
    rainfall_mm DECIMAL(10,2),
    temperature_avg_c DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (year, month, week, mango_variety)
);

-- ============================================================================
-- CORE PROCESSING ENTITIES
-- ============================================================================

-- BATCH: Central traceability entity for mango processing
CREATE TABLE batch (
    batch_id VARCHAR(50) PRIMARY KEY,
    harvest_date DATE NOT NULL,
    mango_variety VARCHAR(20) NOT NULL CHECK (mango_variety IN ('KEITT', 'KENT', 'TOMMY', 'AMÉLIE', 'OTHER')),
    harvest_quantity_kg DECIMAL(10,2) NOT NULL CHECK (harvest_quantity_kg > 0),
    current_status VARCHAR(30) NOT NULL DEFAULT 'CREATED' CHECK (current_status IN ('CREATED', 'INTAKE', 'WASHING', 'DRYING', 'PACKAGING', 'COMPLETED', 'SHIPPED', 'REJECTED')),
    farm_id VARCHAR(50) NOT NULL,
    block_id VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- RAW_INTAKE: Raw material intake from Plants
CREATE TABLE raw_intake (
    intake_id VARCHAR(50) PRIMARY KEY,
    batch_id VARCHAR(50) UNIQUE NOT NULL REFERENCES batch(batch_id) ON DELETE CASCADE,
    source_farm VARCHAR(100) NOT NULL,
    source_block VARCHAR(50) NOT NULL,
    intake_date DATE NOT NULL,
    received_quantity_kg DECIMAL(10,2) NOT NULL CHECK (received_quantity_kg > 0),
    received_variety VARCHAR(20) NOT NULL CHECK (received_variety IN ('KEITT', 'KENT', 'TOMMY', 'AMÉLIE', 'OTHER')),
    received_grade VARCHAR(1) NOT NULL CHECK (received_grade IN ('A', 'B', 'C', 'D')),
    intake_operator VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- WASH_SORT_RECORD: Washing and sorting stage data
CREATE TABLE wash_sort_record (
    record_id VARCHAR(50) PRIMARY KEY,
    batch_id VARCHAR(50) NOT NULL REFERENCES batch(batch_id) ON DELETE CASCADE,
    input_quantity_kg DECIMAL(10,2) NOT NULL CHECK (input_quantity_kg > 0),
    output_quantity_kg DECIMAL(10,2) NOT NULL CHECK (output_quantity_kg > 0),
    waste_quantity_kg DECIMAL(10,2) NOT NULL CHECK (waste_quantity_kg >= 0),
    water_usage_liters DECIMAL(10,2) NOT NULL CHECK (water_usage_liters >= 0),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    equipment_id VARCHAR(50) REFERENCES equipment(equipment_id),
    operator_id VARCHAR(50) REFERENCES operator(operator_id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- DRYING_RUN: Drying process data (core transformation)
CREATE TABLE drying_run (
    run_id VARCHAR(50) PRIMARY KEY,
    batch_id VARCHAR(50) NOT NULL REFERENCES batch(batch_id) ON DELETE CASCADE,
    duration_hours DECIMAL(5,2) NOT NULL CHECK (duration_hours > 0),
    target_temperature_c DECIMAL(5,2) NOT NULL,
    actual_temperature_c DECIMAL(5,2) NOT NULL,
    start_moisture_pct DECIMAL(5,2) NOT NULL CHECK (start_moisture_pct > 0),
    end_moisture_pct DECIMAL(5,2) NOT NULL CHECK (end_moisture_pct >= 6 AND end_moisture_pct <= 18),
    energy_usage_kwh DECIMAL(10,2) NOT NULL CHECK (energy_usage_kwh >= 0),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    equipment_id VARCHAR(50) REFERENCES equipment(equipment_id),
    operator_id VARCHAR(50) REFERENCES operator(operator_id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- PACKAGING_RECORD: Packaging stage data
CREATE TABLE packaging_record (
    record_id VARCHAR(50) PRIMARY KEY,
    batch_id VARCHAR(50) NOT NULL REFERENCES batch(batch_id) ON DELETE CASCADE,
    package_type VARCHAR(20) NOT NULL CHECK (package_type IN ('1KG_BAG', '2KG_BAG', 'BULK')),
    package_quantity_kg DECIMAL(10,2) NOT NULL CHECK (package_quantity_kg > 0),
    lot_code VARCHAR(50) NOT NULL UNIQUE,
    export_ready BOOLEAN NOT NULL DEFAULT FALSE,
    packaging_date DATE NOT NULL,
    equipment_id VARCHAR(50) REFERENCES equipment(equipment_id),
    operator_id VARCHAR(50) REFERENCES operator(operator_id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- QC_CHECKPOINT: Quality control checkpoint data
CREATE TABLE qc_checkpoint (
    checkpoint_id VARCHAR(50) PRIMARY KEY,
    batch_id VARCHAR(50) NOT NULL REFERENCES batch(batch_id) ON DELETE CASCADE,
    stage VARCHAR(30) NOT NULL CHECK (stage IN ('INTAKE', 'WASHING', 'DRYING', 'COOLING', 'PACKAGING', 'FINAL')),
    result VARCHAR(10) NOT NULL CHECK (result IN ('PASS', 'FAIL', 'REWORK')),
    defects TEXT,
    defects_count INTEGER NOT NULL DEFAULT 0 CHECK (defects_count >= 0),
    inspector_id VARCHAR(50) REFERENCES operator(operator_id),
    checkpoint_time TIMESTAMP NOT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- COMPLIANCE_RECORD: HACCP compliance data
CREATE TABLE compliance_record (
    record_id VARCHAR(50) PRIMARY KEY,
    batch_id VARCHAR(50) NOT NULL REFERENCES batch(batch_id) ON DELETE CASCADE,
    compliance_type VARCHAR(30) NOT NULL CHECK (compliance_type IN ('HACCP', 'FOOD_SAFETY', 'EU_EXPORT', 'HYGIENE', 'TRACEABILITY')),
    requirement TEXT NOT NULL,
    result VARCHAR(10) NOT NULL CHECK (result IN ('COMPLIANT', 'NON_COMPLIANT', 'PENDING')),
    evidence TEXT,
    auditor_id VARCHAR(50) REFERENCES operator(operator_id),
    audit_date DATE NOT NULL,
    next_audit_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================================

-- Equipment indexes
CREATE INDEX idx_equipment_type ON equipment(equipment_type);
CREATE INDEX idx_equipment_status ON equipment(maintenance_status);

-- Operator indexes
CREATE INDEX idx_operator_role ON operator(role);
CREATE INDEX idx_operator_status ON operator(active_status);

-- Harvest event indexes
CREATE INDEX idx_harvest_date ON harvest_event(harvest_date);
CREATE INDEX idx_harvest_variety ON harvest_event(mango_variety);
CREATE INDEX idx_harvest_grade ON harvest_event(quality_grade);

-- Historical harvest indexes
CREATE INDEX idx_historical_date ON historical_harvest(year, month, week);
CREATE INDEX idx_historical_variety ON historical_harvest(mango_variety);

-- Batch indexes
CREATE INDEX idx_batch_status ON batch(current_status);
CREATE INDEX idx_batch_date ON batch(harvest_date);
CREATE INDEX idx_batch_variety ON batch(mango_variety);

-- Process record indexes
CREATE INDEX idx_wash_batch ON wash_sort_record(batch_id);
CREATE INDEX idx_drying_batch ON drying_run(batch_id);
CREATE INDEX idx_packaging_batch ON packaging_record(batch_id);
CREATE INDEX idx_qc_batch ON qc_checkpoint(batch_id);
CREATE INDEX idx_compliance_batch ON compliance_record(batch_id);

-- QC checkpoint indexes
CREATE INDEX idx_qc_stage ON qc_checkpoint(stage);
CREATE INDEX idx_qc_result ON qc_checkpoint(result);

-- Compliance record indexes
CREATE INDEX idx_compliance_type ON compliance_record(compliance_type);
CREATE INDEX idx_compliance_result ON compliance_record(result);

-- ============================================================================
-- FUNCTIONS FOR AUTOMATIC TIMESTAMP UPDATES
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply update triggers to all tables with updated_at
CREATE TRIGGER update_equipment_updated_at BEFORE UPDATE ON equipment
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_operator_updated_at BEFORE UPDATE ON operator
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_harvest_event_updated_at BEFORE UPDATE ON harvest_event
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_historical_harvest_updated_at BEFORE UPDATE ON historical_harvest
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_batch_updated_at BEFORE UPDATE ON batch
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_raw_intake_updated_at BEFORE UPDATE ON raw_intake
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_wash_sort_record_updated_at BEFORE UPDATE ON wash_sort_record
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_drying_run_updated_at BEFORE UPDATE ON drying_run
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_packaging_record_updated_at BEFORE UPDATE ON packaging_record
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_qc_checkpoint_updated_at BEFORE UPDATE ON qc_checkpoint
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_compliance_record_updated_at BEFORE UPDATE ON compliance_record
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================================================

COMMENT ON TABLE equipment IS 'Machinery and equipment data - Supporting Entity';
COMMENT ON TABLE operator IS 'Personnel data - Supporting Entity';
COMMENT ON TABLE harvest_event IS 'Harvest data from Plants workstream - Integration Entity';
COMMENT ON TABLE historical_harvest IS 'Aggregated historical harvest data for forecasting - Integration Entity';
COMMENT ON TABLE batch IS 'Central traceability entity for mango processing - Core Processing Entity';
COMMENT ON TABLE raw_intake IS 'Raw material intake from Plants - Supporting Entity';
COMMENT ON TABLE wash_sort_record IS 'Washing and sorting stage data - Core Processing Entity';
COMMENT ON TABLE drying_run IS 'Drying process data (core transformation) - Core Processing Entity';
COMMENT ON TABLE packaging_record IS 'Packaging stage data - Core Processing Entity';
COMMENT ON TABLE qc_checkpoint IS 'Quality control checkpoint data - Core Processing Entity';
COMMENT ON TABLE compliance_record IS 'HACCP compliance data - Core Processing Entity';

COMMENT ON COLUMN batch.current_status IS 'Business Rule: Status transitions from CREATED → INTAKE → WASHING → DRYING → PACKAGING → COMPLETED → SHIPPED';
COMMENT ON COLUMN drying_run.end_moisture_pct IS 'Business Rule: Target moisture content 12-18% (range: 6-17.44%) for EU compliance';
COMMENT ON COLUMN qc_checkpoint.stage IS 'Business Rule: Mandatory at washing (Phase 2) and cooling (Phase 5) stages';
COMMENT ON COLUMN packaging_record.lot_code IS 'Business Rule: Lot codes mandatory for traceability compliance';
COMMENT ON COLUMN historical_harvest IS 'Business Rule: Minimum 7 years required for forecasting';