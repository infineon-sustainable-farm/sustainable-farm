-- SWMS - Schema initial (V1)
-- Cree les tables principales de la base.

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    status BOOLEAN
);

CREATE TABLE IF NOT EXISTS farms (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    address VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    area_hectares DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS fields (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    farm_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    area_hectares DOUBLE PRECISION NOT NULL,
    crop_type VARCHAR(255),
    soil_type VARCHAR(255),
    coordinates VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS zones (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    field_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    area_hectares DOUBLE PRECISION NOT NULL,
    irrigation_method VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS water_sources (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    farm_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    capacity_liters DOUBLE PRECISION NOT NULL,
    current_level_liters DOUBLE PRECISION NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS water_consumptions (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    farm_id UUID NOT NULL,
    source_id UUID NOT NULL,
    consumption_liters DOUBLE PRECISION NOT NULL,
    consumption_date TIMESTAMP,
    irrigation_id UUID
);

CREATE TABLE IF NOT EXISTS water_quality_tests (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    source_id UUID NOT NULL,
    ph DOUBLE PRECISION,
    temperature_celsius DOUBLE PRECISION,
    turbidity_ntu DOUBLE PRECISION,
    conductivity_us_cm DOUBLE PRECISION,
    salinity_ppt DOUBLE PRECISION,
    test_date TIMESTAMP
);

CREATE TABLE IF NOT EXISTS irrigation_schedules (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    zone_id UUID NOT NULL,
    start_time TIMESTAMP NOT NULL,
    duration_minutes INTEGER NOT NULL,
    water_quantity_liters DOUBLE PRECISION NOT NULL,
    status VARCHAR(20),
    created_by UUID NOT NULL
);

CREATE TABLE IF NOT EXISTS irrigation_logs (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    schedule_id UUID NOT NULL,
    actual_start_time TIMESTAMP,
    actual_end_time TIMESTAMP,
    water_used_liters DOUBLE PRECISION,
    status VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_read BOOLEAN NOT NULL,
    action_url VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS rainwater_harvests (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    source_id UUID NOT NULL,
    catchment_area_m2 DOUBLE PRECISION NOT NULL,
    rainfall_mm DOUBLE PRECISION NOT NULL,
    runoff_coefficient DOUBLE PRECISION NOT NULL,
    harvested_liters DOUBLE PRECISION,
    capture_date TIMESTAMP
);

CREATE TABLE IF NOT EXISTS drip_maintenance_logs (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    zone_id UUID NOT NULL,
    task_type VARCHAR(255) NOT NULL,
    issue_found VARCHAR(255),
    action_taken VARCHAR(255),
    technician VARCHAR(100),
    log_date TIMESTAMP,
    next_check_date TIMESTAMP
);
