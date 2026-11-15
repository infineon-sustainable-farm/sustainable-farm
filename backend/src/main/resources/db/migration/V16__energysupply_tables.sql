CREATE TABLE IF NOT EXISTS energy_component (
    component_id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    capacity_value NUMERIC(10, 2),
    purchase_date DATE,
    unit_cost_eur NUMERIC(10, 2),
    status VARCHAR(255),
    location VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS consumption_log (
    log_id VARCHAR(36) PRIMARY KEY,
    date DATE NOT NULL,
    load_category VARCHAR(255) NOT NULL,
    kwh_consumed NUMERIC(10, 2),
    source VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS carbon_metric (
    metric_id VARCHAR(36) PRIMARY KEY,
    period DATE NOT NULL,
    co2_avoided_kg NUMERIC(10, 2),
    emission_factor_used NUMERIC(6, 3),
    scenario VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS generator_event (
    event_id VARCHAR(36) PRIMARY KEY,
    date DATE NOT NULL,
    trigger_reason VARCHAR(500),
    duration_hours NUMERIC(6, 2),
    fuel_liters NUMERIC(8, 2),
    cost_eur NUMERIC(10, 2)
);

CREATE TABLE IF NOT EXISTS solar_generation_log (
    log_id VARCHAR(36) PRIMARY KEY,
    component_id VARCHAR(20) NOT NULL REFERENCES energy_component(component_id),
    date DATE NOT NULL,
    kwh_produced NUMERIC(10, 2),
    efficiency_pct NUMERIC(5, 2)
);

CREATE TABLE IF NOT EXISTS alert (
    alert_id VARCHAR(36) PRIMARY KEY,
    component_id VARCHAR(20) REFERENCES energy_component(component_id),
    severity VARCHAR(255) NOT NULL,
    message VARCHAR(500),
    timestamp TIMESTAMP,
    status VARCHAR(255)
);
