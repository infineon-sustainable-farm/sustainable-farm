-- ============================================================================
-- Product Transformation — schema owner migration
--
-- Until now the 11 Product Transformation tables were created by Hibernate
-- (ddl-auto=update) and no Flyway migration knew about them: a fresh database
-- could not be started, and `harvest_event.plants_harvest_id` was missing on
-- databases built before the Plants integration was added.
--
-- This migration is owned by the Product Transformation module. It creates the
-- tables with `IF NOT EXISTS`, so it is safe on a database where Hibernate has
-- already created them, and it is the only place where the PT schema is
-- declared for new environments.
--
-- Convention: every constraint is added inside a `duplicate_object` guard so a
-- re-run (or an already-populated database) does not abort the migration.
--
-- Table ownership: Machinery owns `equipment`; Product Transformation owns
-- `pt_equipment`. See docs/PT-SCHEMA-OWNERSHIP.md.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Equipment (renamed from `equipment` to avoid the Machinery collision)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pt_equipment (
    equipment_id character varying(50) NOT NULL,
    capacity_kg_per_hour numeric(10,2) NOT NULL,
    created_at timestamp(6) without time zone,
    energy_consumption_kwh_per_kg numeric(10,4) NOT NULL,
    equipment_name character varying(100) NOT NULL,
    equipment_type character varying(50) NOT NULL,
    last_maintenance_date date,
    location character varying(100),
    maintenance_status character varying(20) NOT NULL,
    updated_at timestamp(6) without time zone,
    CONSTRAINT pt_equipment_equipment_type_check CHECK (((equipment_type)::text = ANY ((ARRAY['WASHING'::character varying, 'DRYING'::character varying, 'PACKAGING'::character varying, 'TESTA_DRYER'::character varying, 'SOLAR_DRYER'::character varying, 'TUNNEL_DRYER'::character varying])::text[]))),
    CONSTRAINT pt_equipment_maintenance_status_check CHECK (((maintenance_status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'MAINTENANCE'::character varying, 'RETIRED'::character varying])::text[])))
);

-- ---------------------------------------------------------------------------
-- Operators
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS operator (
    operator_id character varying(50) NOT NULL,
    active_status character varying(20) NOT NULL,
    certifications text,
    created_at timestamp(6) without time zone,
    hire_date date NOT NULL,
    operator_name character varying(100) NOT NULL,
    role character varying(50) NOT NULL,
    updated_at timestamp(6) without time zone,
    CONSTRAINT operator_active_status_check CHECK (((active_status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[]))),
    CONSTRAINT operator_role_check CHECK (((role)::text = ANY ((ARRAY['WASHER'::character varying, 'DRYER'::character varying, 'PACKAGER'::character varying, 'QC_INSPECTOR'::character varying, 'SUPERVISOR'::character varying, 'AUDITOR'::character varying])::text[])))
);

-- ---------------------------------------------------------------------------
-- Batches
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS batch (
    batch_id character varying(50) NOT NULL,
    block_id character varying(50) NOT NULL,
    created_at timestamp(6) without time zone,
    current_status character varying(30) NOT NULL,
    farm_id character varying(50) NOT NULL,
    harvest_date date NOT NULL,
    harvest_quantity_kg numeric(10,2) NOT NULL,
    mango_variety character varying(20) NOT NULL,
    updated_at timestamp(6) without time zone,
    CONSTRAINT batch_current_status_check CHECK (((current_status)::text = ANY ((ARRAY['CREATED'::character varying, 'INTAKE'::character varying, 'WASHING'::character varying, 'DRYING'::character varying, 'PACKAGING'::character varying, 'COMPLETED'::character varying, 'SHIPPED'::character varying, 'REJECTED'::character varying])::text[]))),
    CONSTRAINT batch_mango_variety_check CHECK (((mango_variety)::text = ANY ((ARRAY['KEITT'::character varying, 'KENT'::character varying, 'TOMMY'::character varying, 'AMÉLIE'::character varying, 'OTHER'::character varying])::text[])))
);

-- ---------------------------------------------------------------------------
-- Harvest events (Plants mirror)
-- plants_harvest_id is declared here: it was added to the entity after the
-- first databases were created, so existing installs get it via ALTER below.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS harvest_event (
    harvest_id character varying(50) NOT NULL,
    batch_id character varying(50) NOT NULL,
    block_id character varying(50) NOT NULL,
    created_at timestamp(6) without time zone,
    farm_id character varying(50) NOT NULL,
    harvest_date date NOT NULL,
    harvest_quantity_kg numeric(10,2) NOT NULL,
    harvest_supervisor character varying(100),
    harvest_team_id character varying(50),
    harvest_time time(0) without time zone,
    mango_variety character varying(20) NOT NULL,
    plants_harvest_id bigint,
    quality_grade character(1) NOT NULL,
    quality_grade_description character varying(200),
    storage_location character varying(50),
    updated_at timestamp(6) without time zone,
    weather_conditions character varying(200),
    CONSTRAINT harvest_event_mango_variety_check CHECK (((mango_variety)::text = ANY ((ARRAY['KEITT'::character varying, 'KENT'::character varying, 'TOMMY'::character varying, 'AMÉLIE'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT harvest_event_quality_grade_check CHECK ((quality_grade = ANY (ARRAY['A'::bpchar, 'B'::bpchar, 'C'::bpchar, 'D'::bpchar])))
);

ALTER TABLE harvest_event ADD COLUMN IF NOT EXISTS plants_harvest_id bigint;

-- ---------------------------------------------------------------------------
-- Historical harvests (12-month aggregation)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS historical_harvest (
    mango_variety character varying(20) NOT NULL,
    month integer NOT NULL,
    week integer NOT NULL,
    year integer NOT NULL,
    created_at timestamp(6) without time zone,
    harvest_quantity_kg numeric(10,2) NOT NULL,
    quality_grade_a_pct numeric(5,2),
    quality_grade_b_pct numeric(5,2),
    quality_grade_c_pct numeric(5,2),
    rainfall_mm numeric(10,2),
    temperature_avg_c numeric(5,2),
    updated_at timestamp(6) without time zone,
    weather_condition character varying(50),
    CONSTRAINT historical_harvest_mango_variety_check CHECK (((mango_variety)::text = ANY ((ARRAY['KEITT'::character varying, 'KENT'::character varying, 'TOMMY'::character varying, 'AMÉLIE'::character varying, 'OTHER'::character varying])::text[])))
);

-- ---------------------------------------------------------------------------
-- Raw intake
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS raw_intake (
    intake_id character varying(50) NOT NULL,
    created_at timestamp(6) without time zone,
    intake_date date NOT NULL,
    intake_operator character varying(100),
    received_grade character(1) NOT NULL,
    received_quantity_kg numeric(10,2) NOT NULL,
    received_variety character varying(20) NOT NULL,
    source_block character varying(50) NOT NULL,
    source_farm character varying(100) NOT NULL,
    updated_at timestamp(6) without time zone,
    batch_id character varying(50) NOT NULL,
    CONSTRAINT raw_intake_received_grade_check CHECK ((received_grade = ANY (ARRAY['A'::bpchar, 'B'::bpchar, 'C'::bpchar, 'D'::bpchar]))),
    CONSTRAINT raw_intake_received_variety_check CHECK (((received_variety)::text = ANY ((ARRAY['KEITT'::character varying, 'KENT'::character varying, 'TOMMY'::character varying, 'AMÉLIE'::character varying, 'OTHER'::character varying])::text[])))
);

-- ---------------------------------------------------------------------------
-- Wash / sort records
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wash_sort_record (
    record_id character varying(50) NOT NULL,
    created_at timestamp(6) without time zone,
    end_time timestamp(6) without time zone NOT NULL,
    input_quantity_kg numeric(10,2) NOT NULL,
    output_quantity_kg numeric(10,2) NOT NULL,
    start_time timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    waste_quantity_kg numeric(10,2) NOT NULL,
    water_usage_liters numeric(10,2) NOT NULL,
    batch_id character varying(50) NOT NULL,
    equipment_id character varying(50),
    operator_id character varying(50)
);

-- ---------------------------------------------------------------------------
-- Drying runs
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS drying_run (
    run_id character varying(50) NOT NULL,
    actual_temperature_c numeric(5,2) NOT NULL,
    created_at timestamp(6) without time zone,
    duration_hours numeric(5,2) NOT NULL,
    end_moisture_pct numeric(5,2) NOT NULL,
    end_time timestamp(6) without time zone NOT NULL,
    energy_usage_kwh numeric(10,2) NOT NULL,
    start_moisture_pct numeric(5,2) NOT NULL,
    start_time timestamp(6) without time zone NOT NULL,
    target_temperature_c numeric(5,2) NOT NULL,
    updated_at timestamp(6) without time zone,
    batch_id character varying(50) NOT NULL,
    equipment_id character varying(50),
    operator_id character varying(50)
);

-- ---------------------------------------------------------------------------
-- Packaging records
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS packaging_record (
    record_id character varying(50) NOT NULL,
    created_at timestamp(6) without time zone,
    export_ready boolean NOT NULL,
    lot_code character varying(50) NOT NULL,
    package_quantity_kg numeric(10,2) NOT NULL,
    package_type character varying(20) NOT NULL,
    packaging_date date NOT NULL,
    updated_at timestamp(6) without time zone,
    batch_id character varying(50) NOT NULL,
    equipment_id character varying(50),
    operator_id character varying(50),
    CONSTRAINT packaging_record_package_type_check CHECK (((package_type)::text = ANY ((ARRAY['2KG_BAG'::character varying, '1KG_BAG'::character varying, 'BULK'::character varying])::text[])))
);

-- ---------------------------------------------------------------------------
-- QC checkpoints
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS qc_checkpoint (
    checkpoint_id character varying(50) NOT NULL,
    checkpoint_time timestamp(6) without time zone NOT NULL,
    created_at timestamp(6) without time zone,
    defects text,
    defects_count integer NOT NULL,
    notes text,
    result character varying(10) NOT NULL,
    stage character varying(30) NOT NULL,
    updated_at timestamp(6) without time zone,
    batch_id character varying(50) NOT NULL,
    inspector_id character varying(50),
    CONSTRAINT qc_checkpoint_result_check CHECK (((result)::text = ANY ((ARRAY['PENDING'::character varying, 'PASS'::character varying, 'FAIL'::character varying, 'REWORK'::character varying])::text[]))),
    CONSTRAINT qc_checkpoint_stage_check CHECK (((stage)::text = ANY ((ARRAY['INTAKE'::character varying, 'WASHING'::character varying, 'DRYING'::character varying, 'COOLING'::character varying, 'PACKAGING'::character varying, 'FINAL'::character varying])::text[])))
);

-- ---------------------------------------------------------------------------
-- Compliance records
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS compliance_record (
    record_id character varying(50) NOT NULL,
    audit_date date NOT NULL,
    compliance_type character varying(30) NOT NULL,
    created_at timestamp(6) without time zone,
    evidence text,
    next_audit_date date,
    requirement text NOT NULL,
    result character varying(10) NOT NULL,
    updated_at timestamp(6) without time zone,
    auditor_id character varying(50),
    batch_id character varying(50) NOT NULL,
    CONSTRAINT compliance_record_compliance_type_check CHECK (((compliance_type)::text = ANY ((ARRAY['HACCP'::character varying, 'FOOD_SAFETY'::character varying, 'EU_EXPORT'::character varying, 'HYGIENE'::character varying, 'TRACEABILITY'::character varying])::text[]))),
    CONSTRAINT compliance_record_result_check CHECK (((result)::text = ANY ((ARRAY['COMPLIANT'::character varying, 'NON_COMPLIANT'::character varying, 'PENDING'::character varying])::text[])))
);

-- ---------------------------------------------------------------------------
-- Primary keys (guarded: existing databases already have them)
-- ---------------------------------------------------------------------------
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'pt_equipment'::regclass AND conname = 'pt_equipment_pkey') THEN
        ALTER TABLE pt_equipment ADD CONSTRAINT pt_equipment_pkey PRIMARY KEY (equipment_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'operator'::regclass AND conname = 'operator_pkey') THEN
        ALTER TABLE operator ADD CONSTRAINT operator_pkey PRIMARY KEY (operator_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'batch'::regclass AND conname = 'batch_pkey') THEN
        ALTER TABLE batch ADD CONSTRAINT batch_pkey PRIMARY KEY (batch_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'harvest_event'::regclass AND conname = 'harvest_event_pkey') THEN
        ALTER TABLE harvest_event ADD CONSTRAINT harvest_event_pkey PRIMARY KEY (harvest_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'historical_harvest'::regclass AND conname = 'historical_harvest_pkey') THEN
        ALTER TABLE historical_harvest ADD CONSTRAINT historical_harvest_pkey PRIMARY KEY (mango_variety, month, week, year);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'raw_intake'::regclass AND conname = 'raw_intake_pkey') THEN
        ALTER TABLE raw_intake ADD CONSTRAINT raw_intake_pkey PRIMARY KEY (intake_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'wash_sort_record'::regclass AND conname = 'wash_sort_record_pkey') THEN
        ALTER TABLE wash_sort_record ADD CONSTRAINT wash_sort_record_pkey PRIMARY KEY (record_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'drying_run'::regclass AND conname = 'drying_run_pkey') THEN
        ALTER TABLE drying_run ADD CONSTRAINT drying_run_pkey PRIMARY KEY (run_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'packaging_record'::regclass AND conname = 'packaging_record_pkey') THEN
        ALTER TABLE packaging_record ADD CONSTRAINT packaging_record_pkey PRIMARY KEY (record_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'qc_checkpoint'::regclass AND conname = 'qc_checkpoint_pkey') THEN
        ALTER TABLE qc_checkpoint ADD CONSTRAINT qc_checkpoint_pkey PRIMARY KEY (checkpoint_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'compliance_record'::regclass AND conname = 'compliance_record_pkey') THEN
        ALTER TABLE compliance_record ADD CONSTRAINT compliance_record_pkey PRIMARY KEY (record_id);
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- Uniqueness rules declared by the entities
-- ---------------------------------------------------------------------------
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'harvest_event'::regclass AND conname = 'uk_harvest_event_batch_id') THEN
        ALTER TABLE harvest_event ADD CONSTRAINT uk_harvest_event_batch_id UNIQUE (batch_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'raw_intake'::regclass AND conname = 'uk_raw_intake_batch_id') THEN
        ALTER TABLE raw_intake ADD CONSTRAINT uk_raw_intake_batch_id UNIQUE (batch_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'packaging_record'::regclass AND conname = 'uk_packaging_record_lot_code') THEN
        ALTER TABLE packaging_record ADD CONSTRAINT uk_packaging_record_lot_code UNIQUE (lot_code);
    END IF;
END $$;

-- On an environment built before this migration, the old PT entity created the
-- table `equipment` with PT columns. Renaming it is a deliberate manual step
-- (docs/PT-SCHEMA-OWNERSHIP.md), because `equipment` is Machinery's table name.
-- Until that rename happens, the legacy foreign keys still point at `equipment`:
-- the three equipment FKs below are therefore only created when `equipment` is
-- the Machinery table (no `equipment_id` column).

-- ---------------------------------------------------------------------------
-- Foreign keys
-- ---------------------------------------------------------------------------
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'drying_run'::regclass AND conname = 'fk_drying_run_batch') THEN
        ALTER TABLE drying_run ADD CONSTRAINT fk_drying_run_batch FOREIGN KEY (batch_id) REFERENCES batch(batch_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'drying_run'::regclass AND conname = 'fk_drying_run_equipment')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = 'public' AND table_name = 'equipment' AND column_name = 'equipment_id') THEN
        ALTER TABLE drying_run ADD CONSTRAINT fk_drying_run_equipment FOREIGN KEY (equipment_id) REFERENCES pt_equipment(equipment_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'drying_run'::regclass AND conname = 'fk_drying_run_operator') THEN
        ALTER TABLE drying_run ADD CONSTRAINT fk_drying_run_operator FOREIGN KEY (operator_id) REFERENCES operator(operator_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'wash_sort_record'::regclass AND conname = 'fk_wash_sort_record_batch') THEN
        ALTER TABLE wash_sort_record ADD CONSTRAINT fk_wash_sort_record_batch FOREIGN KEY (batch_id) REFERENCES batch(batch_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'wash_sort_record'::regclass AND conname = 'fk_wash_sort_record_equipment')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = 'public' AND table_name = 'equipment' AND column_name = 'equipment_id') THEN
        ALTER TABLE wash_sort_record ADD CONSTRAINT fk_wash_sort_record_equipment FOREIGN KEY (equipment_id) REFERENCES pt_equipment(equipment_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'wash_sort_record'::regclass AND conname = 'fk_wash_sort_record_operator') THEN
        ALTER TABLE wash_sort_record ADD CONSTRAINT fk_wash_sort_record_operator FOREIGN KEY (operator_id) REFERENCES operator(operator_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'packaging_record'::regclass AND conname = 'fk_packaging_record_batch') THEN
        ALTER TABLE packaging_record ADD CONSTRAINT fk_packaging_record_batch FOREIGN KEY (batch_id) REFERENCES batch(batch_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'packaging_record'::regclass AND conname = 'fk_packaging_record_equipment')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = 'public' AND table_name = 'equipment' AND column_name = 'equipment_id') THEN
        ALTER TABLE packaging_record ADD CONSTRAINT fk_packaging_record_equipment FOREIGN KEY (equipment_id) REFERENCES pt_equipment(equipment_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'packaging_record'::regclass AND conname = 'fk_packaging_record_operator') THEN
        ALTER TABLE packaging_record ADD CONSTRAINT fk_packaging_record_operator FOREIGN KEY (operator_id) REFERENCES operator(operator_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'qc_checkpoint'::regclass AND conname = 'fk_qc_checkpoint_batch') THEN
        ALTER TABLE qc_checkpoint ADD CONSTRAINT fk_qc_checkpoint_batch FOREIGN KEY (batch_id) REFERENCES batch(batch_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'qc_checkpoint'::regclass AND conname = 'fk_qc_checkpoint_inspector') THEN
        ALTER TABLE qc_checkpoint ADD CONSTRAINT fk_qc_checkpoint_inspector FOREIGN KEY (inspector_id) REFERENCES operator(operator_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'raw_intake'::regclass AND conname = 'fk_raw_intake_batch') THEN
        ALTER TABLE raw_intake ADD CONSTRAINT fk_raw_intake_batch FOREIGN KEY (batch_id) REFERENCES batch(batch_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'compliance_record'::regclass AND conname = 'fk_compliance_record_batch') THEN
        ALTER TABLE compliance_record ADD CONSTRAINT fk_compliance_record_batch FOREIGN KEY (batch_id) REFERENCES batch(batch_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'compliance_record'::regclass AND conname = 'fk_compliance_record_auditor') THEN
        ALTER TABLE compliance_record ADD CONSTRAINT fk_compliance_record_auditor FOREIGN KEY (auditor_id) REFERENCES operator(operator_id);
    END IF;
END $$;
