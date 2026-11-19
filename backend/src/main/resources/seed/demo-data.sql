-- ============================================================================
-- demo-data.sql: the one demo data set of Sustainable Farm (profile dev only).
--
-- Fictitious but realistic: a small dried mango farm near Banfora (Burkina Faso) that grows
-- Keitt, Kent and Amelie mangoes, washes, dries and packs them, and sells them to importers in
-- Germany and the EU. Every person, company, e-mail address (domain .example) and phone number
-- is invented. A few rows per table, enough for every screen and chart to show something; the
-- modules reference the same rows (one farm, the same people, machines, lots).
--
-- Loaded by core.config.DemoDataSeeder at startup in the dev profile, after the reference data
-- every profile loads (variety catalogue KEITT, KENT, AMELIE, TOMMY, OTHER; the IoT system
-- person), in one transaction, and only when the farm table is empty: a restart adds nothing.
-- PostgreSQL only. The dates are relative to the day it runs (CURRENT_DATE, now()), so the
-- dashboards always show the last days and weeks. Rows are linked by their business keys
-- (e-mail, code, name), never by a hard-coded id. No semicolon inside a literal (the script is
-- split on them).
--
-- Chain of a lot: plants harvest LOT-<year>-A-001 -> product transformation harvest event and
-- batch of the same code -> raw intake, washing (water consumption of that batch), drying on a
-- machinery dryer, quality checks -> packaged lot PKG-<year>-A-001 -> sales product batch.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- staff: the farm people directory (person), every one listed on the Staff screen (staff)
-- ----------------------------------------------------------------------------

INSERT INTO person (created_at, updated_at, full_name, email, phone, active) VALUES
    (now(), now(), 'Moumouni Ouattara',  'moumouni.ouattara@ferme-banfora.example',  '+226 70 11 24 37', true),
    (now(), now(), 'Mariam Sanou',       'mariam.sanou@ferme-banfora.example',       '+226 76 42 18 90', true),
    (now(), now(), 'Souleymane Barro',   'souleymane.barro@ferme-banfora.example',   '+226 71 63 05 28', true),
    (now(), now(), 'Aminata Konate',     'aminata.konate@ferme-banfora.example',     '+226 78 20 47 61', true),
    (now(), now(), 'Issouf Coulibaly',   'issouf.coulibaly@ferme-banfora.example',   '+226 74 35 82 16', true),
    (now(), now(), 'Fatoumata Sawadogo', 'fatoumata.sawadogo@ferme-banfora.example', '+226 70 58 93 04', true),
    (now(), now(), 'Seydou Hien',        'seydou.hien@ferme-banfora.example',        '+226 75 07 61 49', true),
    (now(), now(), 'Rasmata Zongo',      'rasmata.zongo@ferme-banfora.example',      '+226 72 84 30 75', true),
    (now(), now(), 'Lassina Dao',        'lassina.dao@ferme-banfora.example',        '+226 77 19 56 82', true);

-- Visitor roles: manager, reception, two guides; the farm workers have no visitor role.
INSERT INTO staff (created_at, updated_at, person_id, role, is_active)
SELECT now(), now(), p.id, s.role, true
FROM (VALUES
    ('moumouni.ouattara@ferme-banfora.example',  'MANAGER'),
    ('mariam.sanou@ferme-banfora.example',       'RECEPTION'),
    ('souleymane.barro@ferme-banfora.example',   'GUIDE'),
    ('aminata.konate@ferme-banfora.example',     'GUIDE'),
    ('issouf.coulibaly@ferme-banfora.example',   NULL),
    ('fatoumata.sawadogo@ferme-banfora.example', NULL),
    ('seydou.hien@ferme-banfora.example',        NULL),
    ('rasmata.zongo@ferme-banfora.example',      NULL),
    ('lassina.dao@ferme-banfora.example',        NULL)
) AS s (email, role)
JOIN person p ON p.email = s.email;

-- ----------------------------------------------------------------------------
-- farm: one farm, three fields (blocks A, B, C), two irrigation zones per field
-- ----------------------------------------------------------------------------

INSERT INTO farm (created_at, updated_at, name, description, address, latitude, longitude, area_hectares)
VALUES (now(), now(), 'Ferme de Banfora',
        'Mango orchard with a small drying unit, solar energy and drip irrigation.',
        'Route de Beregadougou, Banfora, Burkina Faso', 10.63, -4.77, 18.5);

INSERT INTO block (created_at, updated_at, farm_id, code, name, area_hectares, crop_type, soil_type, coordinates)
SELECT now(), now(), f.id, b.code, b.name, b.area, b.crop, b.soil, b.coordinates
FROM (VALUES
    ('A', 'North orchard', 6.8, 'KEITT',  'sandy loam', '10.6352, -4.7718'),
    ('B', 'River orchard', 5.2, 'KENT',   'clay loam',  '10.6311, -4.7664'),
    ('C', 'East orchard',  4.1, 'AMELIE', 'sandy',      '10.6287, -4.7623')
) AS b (code, name, area, crop, soil, coordinates)
CROSS JOIN farm f;

INSERT INTO zone (created_at, updated_at, block_id, name, area_hectares, irrigation_method, crop_coefficient,
                  emitter_count, emitter_nominal_flow_lh)
SELECT now(), now(), b.id, z.name, z.area, z.method, z.kc, z.emitters, z.flow
FROM (VALUES
    ('A', 'A1 north drip',  3.4, 'drip',      0.80, 1060, 4.0),
    ('A', 'A2 south drip',  3.4, 'drip',      0.80, 1040, 4.0),
    ('B', 'B1 river drip',  2.8, 'drip',      0.75,  880, 4.0),
    ('B', 'B2 sprinkler',   2.4, 'sprinkler', 0.75,  120, 90.0),
    ('C', 'C1 young trees', 2.0, 'drip',      0.65,  420, 2.0),
    ('C', 'C2 east drip',   2.1, 'drip',      0.70,  440, 2.0)
) AS z (block_code, name, area, method, kc, emitters, flow)
JOIN block b ON b.code = z.block_code;

-- ----------------------------------------------------------------------------
-- machinery: the equipment register (field machines and the processing line)
-- ----------------------------------------------------------------------------

INSERT INTO equipment (created_at, updated_at, code, name, category, stage, status) VALUES
    (now(), now(), 'MCH-TRC-01', 'Tractor 75 hp',                'AGRICULTURAL_MACHINERY', 'CULTIVATION', 'OPERATIONAL'),
    (now(), now(), 'MCH-SPR-01', 'Trailed sprayer 600 L',        'AGRICULTURAL_EQUIPMENT', 'CULTIVATION', 'OPERATIONAL'),
    (now(), now(), 'MCH-MOW-01', 'Rotary mower 1.8 m',           'AGRICULTURAL_EQUIPMENT', 'CULTIVATION', 'UNDER_MAINTENANCE'),
    (now(), now(), 'PT-WSH-01',  'Washing and sorting line',     'PROCESSING_MACHINERY',   'PROCESSING',  'OPERATIONAL'),
    (now(), now(), 'PT-SDR-01',  'Solar dryer 1',                'PROCESSING_MACHINERY',   'DRYING',      'OPERATIONAL'),
    (now(), now(), 'PT-TDR-01',  'Gas tunnel dryer',             'PROCESSING_MACHINERY',   'DRYING',      'OPERATIONAL'),
    (now(), now(), 'PT-PKG-01',  'Vacuum packaging line',        'PROCESSING_MACHINERY',   'PROCESSING',  'OPERATIONAL');

INSERT INTO maintenance_schedule (created_at, updated_at, version, equipment_id, type, frequency, last_completed, next_due,
                                  operator_person_id)
SELECT now(), now(), 0, e.id, m.type, m.frequency, CURRENT_DATE - m.last_days, CURRENT_DATE + m.next_days, p.id
FROM (VALUES
    ('MCH-TRC-01', 'PREVENTIVE', 'Every 250 engine hours',  95,  -5, 'lassina.dao@ferme-banfora.example'),
    ('MCH-SPR-01', 'PREVENTIVE', 'Before each spray campaign', 40,   6, 'issouf.coulibaly@ferme-banfora.example'),
    ('PT-SDR-01',  'PREVENTIVE', 'Monthly',                 24,   6, 'lassina.dao@ferme-banfora.example'),
    ('MCH-MOW-01', 'CORRECTIVE', 'Blade replacement',        2,  12, 'lassina.dao@ferme-banfora.example')
) AS m (code, type, frequency, last_days, next_days, email)
JOIN equipment e ON e.code = m.code
JOIN person p ON p.email = m.email;

INSERT INTO operator_assignment (created_at, updated_at, version, equipment_id, person_id, job_title, start_date, end_date)
SELECT now(), now(), 0, e.id, p.id, a.job_title, CURRENT_DATE - a.since_days, NULL
FROM (VALUES
    ('MCH-TRC-01', 'issouf.coulibaly@ferme-banfora.example',   'Tractor driver',        400),
    ('MCH-SPR-01', 'issouf.coulibaly@ferme-banfora.example',   'Sprayer operator',      400),
    ('PT-SDR-01',  'fatoumata.sawadogo@ferme-banfora.example', 'Dryer operator',        300),
    ('PT-WSH-01',  'seydou.hien@ferme-banfora.example',        'Washing line operator', 250),
    ('PT-PKG-01',  'rasmata.zongo@ferme-banfora.example',      'Packaging operator',    250)
) AS a (code, email, job_title, since_days)
JOIN equipment e ON e.code = a.code
JOIN person p ON p.email = a.email;

INSERT INTO usage_log (created_at, updated_at, version, equipment_id, operator_person_id, log_date, hours_used, notes)
SELECT now(), now(), 0, e.id, p.id, CURRENT_DATE - u.days_ago, u.hours, u.notes
FROM (VALUES
    ('MCH-TRC-01', 'issouf.coulibaly@ferme-banfora.example',   27, 5.5, 'Hauling harvest crates, block A'),
    ('MCH-TRC-01', 'issouf.coulibaly@ferme-banfora.example',   20, 3.0, 'Grass slashing between rows, block B'),
    ('MCH-TRC-01', 'issouf.coulibaly@ferme-banfora.example',   10, 4.5, 'Hauling harvest crates, block A'),
    ('MCH-TRC-01', 'issouf.coulibaly@ferme-banfora.example',    1, 4.0, 'Hauling harvest crates, block B'),
    ('MCH-SPR-01', 'issouf.coulibaly@ferme-banfora.example',   15, 3.5, 'Fruit fly bait spray, block A'),
    ('PT-SDR-01',  'fatoumata.sawadogo@ferme-banfora.example',  5, 9.0, 'Drying lot C-001'),
    ('PT-TDR-01',  'fatoumata.sawadogo@ferme-banfora.example', 23, 14.0, 'Drying lot B-001'),
    ('PT-WSH-01',  'seydou.hien@ferme-banfora.example',         1, 6.0, 'Washing lot A-002')
) AS u (code, email, days_ago, hours, notes)
JOIN equipment e ON e.code = u.code
JOIN person p ON p.email = u.email;

INSERT INTO fuel_log (created_at, updated_at, version, equipment_id, log_date, liters, cost_amount, cost_currency)
SELECT now(), now(), 0, e.id, CURRENT_DATE - f.days_ago, f.liters, f.liters * 675, 'XOF'
FROM (VALUES
    ('MCH-TRC-01', 28, 60.0),
    ('MCH-TRC-01', 14, 45.0),
    ('MCH-TRC-01',  2, 50.0),
    ('MCH-SPR-01', 15, 12.0),
    ('MCH-MOW-01', 21, 18.0)
) AS f (code, days_ago, liters)
JOIN equipment e ON e.code = f.code;

INSERT INTO repair_log (created_at, updated_at, version, equipment_id, technician_person_id, log_date, issue, downtime,
                        cost_amount, cost_currency)
SELECT now(), now(), 0, e.id, p.id, CURRENT_DATE - r.days_ago, r.issue, r.downtime, r.cost, 'XOF'
FROM (VALUES
    ('MCH-MOW-01', 3,  'Blade carrier cracked, mower stopped until the new blades arrive', 48.0, 85000),
    ('PT-WSH-01',  33, 'Leaking water pump seal on the washing tank, replaced',             6.0,  42500)
) AS r (code, days_ago, issue, downtime, cost)
JOIN equipment e ON e.code = r.code
JOIN person p ON p.email = 'lassina.dao@ferme-banfora.example';

INSERT INTO spare_part (created_at, updated_at, equipment_id, name, quantity, reorder_threshold, unit_cost_amount,
                        unit_cost_currency)
SELECT now(), now(), e.id, s.name, s.quantity, s.threshold, s.cost, 'XOF'
FROM (VALUES
    ('MCH-TRC-01', 'Tractor engine oil filter',     6, 2, 9500),
    ('MCH-TRC-01', 'Tractor fuel filter',           4, 2, 7500),
    ('MCH-MOW-01', 'Mower blade set',               1, 3, 38000),
    ('MCH-SPR-01', 'Sprayer nozzle (pack of 10)',   5, 2, 12000),
    ('PT-PKG-01',  'Vacuum sealing bar',            2, 1, 64000)
) AS s (code, name, quantity, threshold, cost)
JOIN equipment e ON e.code = s.code;

-- ----------------------------------------------------------------------------
-- plants: plantings, growth calendar, harvests with their lots, maturity, health, inputs
-- ----------------------------------------------------------------------------

INSERT INTO currency_rate (created_at, updated_at, base_currency, quote_currency, rate, source, last_updated)
VALUES (now(), now(), 'EUR', 'XOF', 655.957, 'BCEAO_fixed_parity_1999', now());

-- The mango pests and diseases documented in Burkina Faso, and Other for anything else.
INSERT INTO health_issue_reference (created_at, updated_at, code, name, kind, scientific_name, eppo_code, source,
                                    last_updated) VALUES
    (now(), now(), 'FRUIT_FLY', 'Fruit flies', 'PEST', 'Tephritidae, mainly Bactrocera dorsalis and Ceratitis cosyra', NULL, 'Zida_2023', now()),
    (now(), now(), 'MANGO_MEALYBUG', 'Mango mealybug', 'PEST', 'Rastrococcus invadens', 'RASTIN', 'Vayssieres_2012', now()),
    (now(), now(), 'TERMITES', 'Termites', 'PEST', NULL, NULL, 'Vayssieres_2012', now()),
    (now(), now(), 'SCALE_INSECTS', 'Scale insects', 'PEST', NULL, NULL, 'Vayssieres_2012', now()),
    (now(), now(), 'ANTHRACNOSE', 'Anthracnose', 'DISEASE', 'Colletotrichum gloeosporioides', 'COLLGL', 'Dianda_2025', now()),
    (now(), now(), 'BACTERIAL_BLACK_SPOT', 'Bacterial black spot', 'DISEASE', 'Xanthomonas citri pv. mangiferaeindicae', 'XANTMI', 'Dianda_2025', now()),
    (now(), now(), 'MANGO_DECLINE', 'Mango decline', 'DISEASE', NULL, NULL, 'Dianda_2025', now()),
    (now(), now(), 'OTHER', 'Other', 'OTHER', NULL, NULL, 'by_definition', now());

-- variety_key and block_key are the natural keys the entities write: "<block id>|<name lower case>"
-- and "<block id>".
INSERT INTO block_variety (created_at, updated_at, block_id, name, tree_count, row_spacing_m, tree_spacing_m,
                           tree_density_per_ha, vigor, plant_origin, source, variety_key, last_updated)
SELECT now(), now(), b.id, v.name, v.trees, v.row_spacing, v.tree_spacing, v.density, v.vigor, 'grafted nursery plants',
       'demo_data', b.id || '|' || lower(v.name), now()
FROM (VALUES
    ('A', 'Keitt',  1060, 8.0,  8.0, 156.25, 'GOOD'),
    ('B', 'Kent',    810, 8.0,  8.0, 156.25, 'GOOD'),
    ('C', 'Amélie',  410, 10.0, 10.0, 100.00, 'AVERAGE')
) AS v (block_code, name, trees, row_spacing, tree_spacing, density, vigor)
JOIN block b ON b.code = v.block_code;

INSERT INTO population_event (created_at, updated_at, block_variety_id, event_type, event_date, tree_count, source,
                              last_updated)
SELECT now(), now(), bv.id, e.type, (CURRENT_DATE - e.age)::date, e.trees, 'demo_data', now()
FROM (VALUES
    ('A', 'PLANTING',    INTERVAL '9 years 4 months', 1080),
    ('A', 'MORTALITY',   INTERVAL '1 year 2 months',    20),
    ('B', 'PLANTING',    INTERVAL '7 years 4 months',  810),
    ('C', 'PLANTING',    INTERVAL '4 years 3 months',  410)
) AS e (block_code, type, age, trees)
JOIN block b ON b.code = e.block_code
JOIN block_variety bv ON bv.block_id = b.id;

INSERT INTO growth_calendar (created_at, updated_at, block_id, block_key, planting_date, date_precision, local_rainfall_mm,
                             source, last_updated)
SELECT now(), now(), b.id, b.id::text, (CURRENT_DATE - g.age)::date, 'day known', 1050.0, 'demo_data', now()
FROM (VALUES
    ('A', INTERVAL '9 years 4 months'),
    ('B', INTERVAL '7 years 4 months'),
    ('C', INTERVAL '4 years 3 months')
) AS g (block_code, age)
JOIN block b ON b.code = g.block_code;

-- Five pickings over the last six weeks, each with the lot number plants gives:
-- LOT-<year of the harvest>-<block>-<number of the block in that year>.
INSERT INTO harvest_record (created_at, updated_at, block_variety_id, harvest_date, quantity_kg, lot_number, source,
                            last_updated)
SELECT now(), now(), bv.id, CURRENT_DATE - h.days_ago, h.kg,
       'LOT-' || to_char(CURRENT_DATE - h.days_ago, 'YYYY') || '-' || h.block_code || '-' || h.seq, 'demo_data', now()
FROM (VALUES
    ('A', '001', 40, 3800.0),
    ('B', '001', 24, 2950.0),
    ('C', '001',  6, 1450.0),
    ('A', '002',  1, 3200.0),
    ('B', '002',  0, 2100.0)
) AS h (block_code, seq, days_ago, kg)
JOIN block b ON b.code = h.block_code
JOIN block_variety bv ON bv.block_id = b.id;

INSERT INTO maturity_observation (created_at, updated_at, block_variety_id, observed_on, method, measured_value,
                                  maturity_status, observer_person_id, source, last_updated)
SELECT now(), now(), bv.id, CURRENT_DATE - m.days_ago, m.method, m.value, m.status, p.id, 'demo_data', now()
FROM (VALUES
    ('A', 45, 'DRY_MATTER', 12.5, 'RIPE'),
    ('A',  4, 'DRY_MATTER', 14.2, 'READY_FOR_HARVEST'),
    ('B',  3, 'BRIX',       13.0, 'READY_FOR_HARVEST'),
    ('C',  9, 'FLESH_COLOR', NULL, 'RIPE')
) AS m (block_code, days_ago, method, value, status)
JOIN block b ON b.code = m.block_code
JOIN block_variety bv ON bv.block_id = b.id
JOIN person p ON p.email = 'aminata.konate@ferme-banfora.example';

-- One inspection of block A, by fruit fly traps; the fruit flies are treated with the sprayer of machinery.
INSERT INTO health_inspection (created_at, updated_at, block_id, inspected_on, health_score_pct, observer_person_id,
                               method, source, last_updated)
SELECT now(), now(), b.id, CURRENT_DATE - 16, 82, p.id, 'TRAP', 'demo_data', now()
FROM block b JOIN person p ON p.email = 'aminata.konate@ferme-banfora.example'
WHERE b.code = 'A';

-- Two findings: the fruit flies, treated and resolved, and an anthracnose spot still open.
INSERT INTO health_finding (created_at, updated_at, inspection_id, issue_id, tree_label, resolved_on, resolution_note,
                            source, last_updated)
SELECT now(), now(), i.id, r.id, f.tree_label, CURRENT_DATE - f.resolved_days, f.note, 'demo_data', now()
FROM (VALUES
    ('FRUIT_FLY',   'A-R12-T07', 8,    'Trap counts back under the threshold after the bait spray'),
    ('ANTHRACNOSE', 'A-R03-T11', NULL, NULL)
) AS f (issue_code, tree_label, resolved_days, note)
JOIN health_issue_reference r ON r.code = f.issue_code
CROSS JOIN health_inspection i;

INSERT INTO health_treatment (created_at, updated_at, block_id, treated_on, target_issue_id, finding_id, product_name,
                              active_ingredient, quantity, unit, pre_harvest_interval_days, equipment_id, equipment,
                              applicator_person_id, source, last_updated)
SELECT now(), now(), f_block.id, CURRENT_DATE - 15, r.id, f.id, 'Spinosad fruit fly bait', 'spinosad', 2.5, 'L', 7,
       e.id, e.name, p.id, 'demo_data', now()
FROM health_finding f
JOIN health_inspection i ON i.id = f.inspection_id
JOIN block f_block ON f_block.id = i.block_id
JOIN health_issue_reference r ON r.id = f.issue_id AND r.code = 'FRUIT_FLY'
JOIN equipment e ON e.code = 'MCH-SPR-01'
JOIN person p ON p.email = 'issouf.coulibaly@ferme-banfora.example';

INSERT INTO fertilizer_product (created_at, updated_at, name, fertilizer_type, composition, unit, reorder_threshold,
                                source, last_updated) VALUES
    (now(), now(), 'NPK 15-15-15',          'MINERAL', 'N 15 %, P2O5 15 %, K2O 15 %', 'KG', 200, 'demo_data', now()),
    (now(), now(), 'Composted cattle manure', 'ORGANIC', NULL,                           'KG', 1000, 'demo_data', now());

INSERT INTO fertilizer_movement (created_at, updated_at, product_id, movement_type, movement_date, quantity, block_id,
                                 applicator_person_id, method, supplier, total_cost, currency, reason, source,
                                 last_updated)
SELECT now(), now(), fp.id, m.type, CURRENT_DATE - m.days_ago, m.quantity, b.id, p.id, m.method, m.supplier, m.cost,
       CASE WHEN m.cost IS NULL THEN NULL ELSE 'XOF' END, NULL, 'demo_data', now()
FROM (VALUES
    ('NPK 15-15-15',            'PURCHASE',    60, 500.0,  NULL, NULL,                  NULL,            'Agro-intrants Bobo SARL', 212500),
    ('NPK 15-15-15',            'APPLICATION', 50, 350.0,  'A',  'issouf.coulibaly',    'Ring around the trunk', NULL, NULL),
    ('Composted cattle manure', 'PURCHASE',    75, 6000.0, NULL, NULL,                  NULL,            'Cooperative des eleveurs de Sideradougou', 180000),
    ('Composted cattle manure', 'APPLICATION', 70, 3000.0, 'C',  'issouf.coulibaly',    'Spread under the canopy', NULL, NULL)
) AS m (product, type, days_ago, quantity, block_code, applicator, method, supplier, cost)
JOIN fertilizer_product fp ON fp.name = m.product
LEFT JOIN block b ON b.code = m.block_code
LEFT JOIN person p ON p.email = m.applicator || '@ferme-banfora.example';

INSERT INTO nursery_batch (created_at, updated_at, batch_code, farm_id, variety_name, origin, initial_count, started_on,
                           planned_transplant_on, planned_block_id, supplier, supplier_lot_number, source, last_updated)
SELECT now(), now(), 'NUR-' || to_char(CURRENT_DATE - 150, 'YYYY') || '-01', f.id, 'Kent', 'IN_HOUSE', 120,
       CURRENT_DATE - 150, CURRENT_DATE + 210, b.id, NULL, NULL, 'demo_data', now()
FROM farm f JOIN block b ON b.code = 'B';

INSERT INTO nursery_event (created_at, updated_at, batch_id, event_type, event_date, stage, quantity, reason, block_id,
                           source, last_updated)
SELECT now(), now(), n.id, e.type, CURRENT_DATE - e.days_ago, e.stage, e.quantity, e.reason, NULL, 'demo_data', now()
FROM (VALUES
    ('STAGE_CHANGE', 150, 'GERMINATION',      NULL, NULL),
    ('STAGE_CHANGE', 120, 'ROOTSTOCK_GROWTH', NULL, NULL),
    ('LOSS',          90, NULL,               8,    'Damping-off after heavy rain'),
    ('STAGE_CHANGE',  30, 'GRAFTED',          NULL, NULL)
) AS e (type, days_ago, stage, quantity, reason)
CROSS JOIN nursery_batch n;

-- The business plan of the orchard, in FCFA, for the scenario analysis.
INSERT INTO plant_scenario (created_at, updated_at, name, variety_name, tree_count, tree_count_source, yield_per_tree_kg,
                            yield_source, price_per_kg, price_source, currency, annual_fixed_cost,
                            annual_fixed_cost_source, variable_cost_per_kg, variable_cost_source, loss_rate,
                            loss_rate_source, initial_investment, investment_source, cost_per_additional_tree,
                            cost_per_additional_tree_source, last_updated)
VALUES (now(), now(), 'Ferme de Banfora business plan', 'Keitt', 2280, 'demo_data', 150.0, 'demo_data', 125.0000,
        'demo_data', 'XOF', 4500000.00, 'demo_data', 35.0000, 'demo_data', 0.120, 'demo_data', 38000000.00,
        'demo_data', 2500.00, 'demo_data', now());

-- ----------------------------------------------------------------------------
-- producttransformation: operators, machine specs, the five lots of the harvests above
-- ----------------------------------------------------------------------------

INSERT INTO operator (created_at, updated_at, code, person_id, role, certifications, hire_date, active_status)
SELECT now(), now(), o.code, p.id, o.role, o.certifications, CURRENT_DATE - o.hired_days, 'ACTIVE'
FROM (VALUES
    ('OP-001', 'aminata.konate@ferme-banfora.example',     'QC_INSPECTOR', 'HACCP level 2',          900),
    ('OP-002', 'fatoumata.sawadogo@ferme-banfora.example', 'DRYER',        'Solar drying training',  700),
    ('OP-003', 'seydou.hien@ferme-banfora.example',        'WASHER',       NULL,                     500),
    ('OP-004', 'rasmata.zongo@ferme-banfora.example',      'PACKAGER',     'Food hygiene',           500),
    ('OP-005', 'moumouni.ouattara@ferme-banfora.example',  'SUPERVISOR',   'HACCP level 3, EU organic', 1500)
) AS o (code, email, role, certifications, hired_days)
JOIN person p ON p.email = o.email;

INSERT INTO pt_equipment_spec (created_at, updated_at, equipment_id, equipment_type, capacity_kg_per_hour,
                               energy_consumption_kwh_per_kg, location, last_maintenance_date)
SELECT now(), now(), e.id, s.type, s.capacity, s.energy, s.location, CURRENT_DATE - s.maintained_days
FROM (VALUES
    ('PT-WSH-01', 'WASHING',      600.00, 0.0200, 'Processing hall',  33),
    ('PT-SDR-01', 'SOLAR_DRYER',   40.00, 0.0500, 'Drying yard',      24),
    ('PT-TDR-01', 'TUNNEL_DRYER',  90.00, 1.2000, 'Processing hall',  60),
    ('PT-PKG-01', 'PACKAGING',    120.00, 0.0300, 'Packing room',     45)
) AS s (code, type, capacity, energy, location, maintained_days)
JOIN equipment e ON e.code = s.code;

-- A batch per plants harvest, with the lot number as its code: A-001 shipped, B-001 being packed,
-- C-001 drying, A-002 washing, B-002 just received.
INSERT INTO batch (created_at, updated_at, code, block_id, variety_key, harvest_date, harvest_quantity_kg, current_status)
SELECT now(), now(), h.lot_number, b.id, l.variety_key, h.harvest_date, h.quantity_kg, l.status
FROM (VALUES
    ('A', '001', 'KEITT',  'SHIPPED'),
    ('B', '001', 'KENT',   'PACKAGING'),
    ('C', '001', 'AMELIE', 'DRYING'),
    ('A', '002', 'KEITT',  'WASHING'),
    ('B', '002', 'KENT',   'INTAKE')
) AS l (block_code, seq, variety_key, status)
JOIN block b ON b.code = l.block_code
JOIN block_variety bv ON bv.block_id = b.id
JOIN harvest_record h ON h.block_variety_id = bv.id AND h.lot_number LIKE '%-' || l.block_code || '-' || l.seq;

INSERT INTO harvest_event (created_at, updated_at, code, batch_code, plants_harvest_id, block_id, variety_key,
                           harvest_date, harvest_time, harvest_quantity_kg, quality_grade, quality_grade_description,
                           harvest_team_id, harvest_supervisor_id, weather_conditions, storage_location)
SELECT now(), now(), 'HE-' || substr(b.code, 5), b.code, h.id, b.block_id, b.variety_key, b.harvest_date, time '07:00',
       b.harvest_quantity_kg, CASE WHEN b.code LIKE '%-C-%' THEN 'B' ELSE 'A' END,
       CASE WHEN b.code LIKE '%-C-%' THEN 'Some sunburn marks' ELSE 'Export grade' END,
       'TEAM-1', p.id, 'Dry, sunny, 33 C', 'Shaded reception area'
FROM batch b
JOIN harvest_record h ON h.lot_number = b.code
JOIN person p ON p.email = 'moumouni.ouattara@ferme-banfora.example';

INSERT INTO raw_intake (created_at, updated_at, code, batch_id, intake_date, intake_operator_id, source_block_id,
                        received_variety_key, received_quantity_kg, received_grade)
SELECT now(), now(), 'RI-' || substr(b.code, 5), b.id, b.harvest_date, p.id, b.block_id, b.variety_key,
       b.harvest_quantity_kg, CASE WHEN b.code LIKE '%-C-%' THEN 'B' ELSE 'A' END
FROM batch b
JOIN person p ON p.email = 'seydou.hien@ferme-banfora.example';

-- Washing: about 7 % sorted out, 1.6 L of water per kg (the water consumption below).
INSERT INTO wash_sort_record (created_at, updated_at, code, batch_id, equipment_id, operator_id, start_time, end_time,
                              input_quantity_kg, output_quantity_kg, waste_quantity_kg, water_usage_liters)
SELECT now(), now(), 'WS-' || substr(b.code, 5), b.id, e.id, o.id, b.harvest_date + time '09:00',
       b.harvest_date + time '09:00' + (b.harvest_quantity_kg / 600.0) * INTERVAL '1 hour',
       b.harvest_quantity_kg, round(b.harvest_quantity_kg * 0.93, 2), round(b.harvest_quantity_kg * 0.07, 2),
       round(b.harvest_quantity_kg * 1.6, 2)
FROM batch b
JOIN equipment e ON e.code = 'PT-WSH-01'
JOIN operator o ON o.code = 'OP-003'
WHERE b.current_status <> 'INTAKE';

-- Drying: from about 82 % to 14 % moisture, on the solar dryer or the gas tunnel dryer.
INSERT INTO drying_run (created_at, updated_at, code, batch_id, equipment_id, operator_id, start_time, end_time,
                        duration_hours, target_temperature_c, actual_temperature_c, start_moisture_pct, end_moisture_pct,
                        energy_usage_kwh)
SELECT now(), now(), 'DR-' || substr(b.code, 5), b.id, e.id, o.id, b.harvest_date + 1 + time '08:00',
       b.harvest_date + 1 + time '08:00' + d.hours * INTERVAL '1 hour', d.hours, d.target, d.actual, 82.00, d.end_moisture,
       d.kwh
FROM (VALUES
    ('A-001', 'PT-SDR-01', 30.0, 60.00, 58.50, 13.50, 18.40),
    ('B-001', 'PT-TDR-01', 14.0, 65.00, 64.20, 14.00, 312.00),
    ('C-001', 'PT-SDR-01', 32.0, 60.00, 57.80, 16.00, 9.60)
) AS d (lot, equipment_code, hours, target, actual, end_moisture, kwh)
JOIN batch b ON b.code LIKE '%-' || d.lot
JOIN equipment e ON e.code = d.equipment_code
JOIN operator o ON o.code = 'OP-002';

INSERT INTO qc_checkpoint (created_at, updated_at, code, batch_id, inspector_id, stage, checkpoint_time, result,
                           defects_count, defects, notes)
SELECT now(), now(), 'QC-' || substr(b.code, 5) || '-' || q.n, b.id, o.id, q.stage,
       b.harvest_date + q.day_offset + time '10:00', q.result, q.defects_count, q.defects, q.notes
FROM (VALUES
    ('A-001', 1, 'INTAKE',    0, 'PASS',   0, NULL,                       'Export grade fruit'),
    ('A-001', 2, 'DRYING',    2, 'PASS',   0, NULL,                       'Moisture 13.5 %'),
    ('A-001', 3, 'FINAL',     4, 'PASS',   1, 'One torn bag',             'Released for export'),
    ('B-001', 1, 'INTAKE',    0, 'PASS',   2, 'Bruised fruit',            NULL),
    ('B-001', 2, 'DRYING',    1, 'REWORK', 4, 'Moisture 17 % in the core', 'Back in the dryer for 3 hours'),
    ('B-001', 3, 'DRYING',    2, 'PASS',   0, NULL,                       'Moisture 14 %'),
    ('C-001', 1, 'INTAKE',    0, 'PASS',   3, 'Sunburn marks',            'Grade B'),
    ('C-001', 2, 'WASHING',   0, 'PASS',   0, NULL,                       NULL),
    ('A-002', 1, 'INTAKE',    0, 'PASS',   0, NULL,                       NULL),
    ('B-002', 1, 'INTAKE',    0, 'PENDING', 0, NULL,                      'Sample taken')
) AS q (lot, n, stage, day_offset, result, defects_count, defects, notes)
JOIN batch b ON b.code LIKE '%-' || q.lot
JOIN operator o ON o.code = 'OP-001';

-- Two packaged lots: A-001 ready for export (and shipped), B-001 waiting for its traceability check.
INSERT INTO packaging_record (created_at, updated_at, code, batch_id, equipment_id, operator_id, lot_code, package_type,
                              package_quantity_kg, packaging_date, export_ready)
SELECT now(), now(), 'PR-' || substr(b.code, 5), b.id, e.id, o.id, 'PKG-' || substr(b.code, 5), p.package_type,
       p.kg, CURRENT_DATE - p.days_ago, p.export_ready
FROM (VALUES
    ('A-001', 'BULK',    345.00, 35, true),
    ('B-001', '1KG_BAG', 260.00,  2, false)
) AS p (lot, package_type, kg, days_ago, export_ready)
JOIN batch b ON b.code LIKE '%-' || p.lot
JOIN equipment e ON e.code = 'PT-PKG-01'
JOIN operator o ON o.code = 'OP-004';

INSERT INTO compliance_record (created_at, updated_at, code, batch_id, auditor_id, compliance_type, requirement,
                               audit_date, result, evidence, next_audit_date)
SELECT now(), now(), 'CR-' || substr(b.code, 5) || '-' || c.n, b.id, o.id, c.type, c.requirement,
       CURRENT_DATE - c.days_ago, c.result, c.evidence, CURRENT_DATE + c.next_days
FROM (VALUES
    ('A-001', 1, 'HACCP',        'Critical control points of washing and drying under control', 36, 'COMPLIANT',
     'HACCP plan and drying records', 150),
    ('A-001', 2, 'EU_EXPORT',    'Phytosanitary certificate and EU maximum residue limits',     35, 'COMPLIANT',
     'Phytosanitary certificate and residue analysis report', 180),
    ('B-001', 1, 'TRACEABILITY', 'Harvest lot to packaged lot traceability',                     2, 'PENDING',
     NULL, 5)
) AS c (lot, n, type, requirement, days_ago, result, evidence, next_days)
JOIN batch b ON b.code LIKE '%-' || c.lot
JOIN operator o ON o.code = 'OP-005';

-- Harvests of the last season, month by month (series of the yearly chart).
INSERT INTO historical_harvest (created_at, updated_at, variety_key, year, month, week, harvest_quantity_kg,
                                quality_grade_a_pct, quality_grade_b_pct, quality_grade_c_pct, rainfall_mm,
                                temperature_avg_c, weather_condition)
SELECT now(), now(), v.variety_key, EXTRACT(YEAR FROM m)::int, EXTRACT(MONTH FROM m)::int, EXTRACT(WEEK FROM m)::int,
       round((v.peak * CASE EXTRACT(MONTH FROM m)::int WHEN 3 THEN 0.15 WHEN 4 THEN 0.55 WHEN 5 THEN 1.0
                                                      WHEN 6 THEN 0.8 ELSE 0.3 END)::numeric, 2),
       72.00, 21.00, 7.00,
       CASE EXTRACT(MONTH FROM m)::int WHEN 3 THEN 5 WHEN 4 THEN 35 WHEN 5 THEN 80 WHEN 6 THEN 125 ELSE 190 END,
       CASE EXTRACT(MONTH FROM m)::int WHEN 3 THEN 31.5 WHEN 4 THEN 32.0 WHEN 5 THEN 30.5 WHEN 6 THEN 28.0 ELSE 26.5 END,
       CASE WHEN EXTRACT(MONTH FROM m)::int >= 6 THEN 'Rainy' ELSE 'Dry' END
FROM generate_series(date_trunc('month', CURRENT_DATE) - INTERVAL '11 months', date_trunc('month', CURRENT_DATE),
                     INTERVAL '1 month') AS m
CROSS JOIN (VALUES ('KEITT', 42000.0), ('KENT', 31000.0), ('AMELIE', 12000.0)) AS v (variety_key, peak)
WHERE EXTRACT(MONTH FROM m)::int BETWEEN 3 AND 7;

-- ----------------------------------------------------------------------------
-- energysupply: the solar installation, three weeks of production and consumption
-- ----------------------------------------------------------------------------

INSERT INTO energy_component (created_at, updated_at, code, name, category, capacity_value, purchase_date,
                              unit_cost_amount, unit_cost_currency, status, location) VALUES
    (now(), now(), 'ENG-001', 'Solar PV array 12 kWp',        'PV_ARRAY',   12.00, CURRENT_DATE - 540, 7200000, 'XOF', 'OPERATIONAL', 'Roof of the processing hall'),
    (now(), now(), 'ENG-002', 'Lithium battery bank 20 kWh',  'BATTERY',    20.00, CURRENT_DATE - 540, 5900000, 'XOF', 'OPERATIONAL', 'Technical room'),
    (now(), now(), 'ENG-003', 'Hybrid inverter 10 kW',        'INVERTER',   10.00, CURRENT_DATE - 540, 1450000, 'XOF', 'OPERATIONAL', 'Technical room'),
    (now(), now(), 'ENG-004', 'Backup diesel generator 9 kVA', 'GENERATOR',   9.00, CURRENT_DATE - 900, 2600000, 'XOF', 'OPERATIONAL', 'Generator shed'),
    (now(), now(), 'ENG-005', 'Energy monitoring gateway',    'MONITORING',  0.00, CURRENT_DATE - 540,  230000, 'XOF', 'OPERATIONAL', 'Technical room');

-- 21 days of PV production, lower on the hazy days.
INSERT INTO solar_generation_log (created_at, updated_at, component_id, date, kwh_produced, efficiency_pct)
SELECT now(), now(), c.id, CURRENT_DATE - d,
       round((54 + 6 * sin(d) - CASE WHEN d % 7 = 3 THEN 14 ELSE 0 END)::numeric, 2),
       round((84 + 5 * sin(d) - CASE WHEN d % 7 = 3 THEN 20 ELSE 0 END)::numeric, 2)
FROM generate_series(0, 20) AS d
JOIN energy_component c ON c.code = 'ENG-001';

-- 14 days of consumption per load; processing is higher on the washing and drying days.
INSERT INTO consumption_log (created_at, updated_at, date, load_category, kwh_consumed, source)
SELECT now(), now(), CURRENT_DATE - d, l.category,
       round((l.kwh + CASE WHEN l.category = 'PROCESSING' AND d IN (0, 1, 5, 6) THEN 12 ELSE 0 END
              + (d % 3) * l.step)::numeric, 2), l.source
FROM generate_series(0, 13) AS d
CROSS JOIN (VALUES
    ('IRRIGATION',      14.0, 1.0, 'SOLAR'),
    ('PROCESSING',      10.0, 1.5, 'SOLAR'),
    ('STORAGE_COOLING',  9.0, 0.5, 'SOLAR'),
    ('LIGHTING',         2.5, 0.2, 'BATTERY'),
    ('SECURITY',         1.2, 0.0, 'BATTERY'),
    ('MONITORING',       0.6, 0.0, 'BATTERY')
) AS l (category, kwh, step, source);

INSERT INTO generator_event (created_at, updated_at, date, trigger_reason, duration_hours, fuel_liters, cost_amount,
                             cost_currency) VALUES
    (now(), now(), CURRENT_DATE - 18, 'Battery below 20 % during the night drying run', 2.50, 5.50, 3712.50, 'XOF'),
    (now(), now(), CURRENT_DATE - 4,  'Hazy day, PV output too low for the washing line', 1.50, 3.20, 2160.00, 'XOF');

INSERT INTO carbon_metric (created_at, updated_at, period, co2_avoided_kg, emission_factor_used, scenario)
SELECT now(), now(), (date_trunc('month', CURRENT_DATE) - m * INTERVAL '1 month')::date, c.kg, 0.650, 'Base'
FROM (VALUES (0, 640.00), (1, 1050.00), (2, 980.00)) AS c (m, kg);

INSERT INTO alert (created_at, updated_at, source_module, related_entity_type, related_entity_id, severity, status,
                   message, is_read, raised_at, resolved_at)
SELECT now(), now(), 'ENERGYSUPPLY', 'energy_component', c.id::text, a.severity, a.status, a.message, false,
       now() - a.age, CASE WHEN a.status = 'RESOLVED' THEN now() - a.age + INTERVAL '3 hours' END
FROM (VALUES
    ('ENG-002', 'WARNING', 'OPEN',     'Battery state of charge below 30 % at sunset, check the night loads',
     INTERVAL '5 hours'),
    ('ENG-004', 'INFO',    'RESOLVED', 'Generator started automatically, battery reserve restored',
     INTERVAL '4 days')
) AS a (code, severity, status, message, age)
JOIN energy_component c ON c.code = a.code;

-- ----------------------------------------------------------------------------
-- watersupply: sources, irrigation of the zones, water of the processing lots, quality, quotas
-- ----------------------------------------------------------------------------

INSERT INTO water_sources (created_at, updated_at, farm_id, name, type, capacity_liters, current_level_liters, latitude,
                           longitude)
SELECT now(), now(), f.id, s.name, s.type, s.capacity, s.level, s.lat, s.lon
FROM (VALUES
    ('Borehole F1 and storage tank', 'borehole', 60000, 41500, 10.6334, -4.7697),
    ('Rainwater tank',               'rain',     30000,  9200, 10.6326, -4.7689)
) AS s (name, type, capacity, level, lat, lon)
CROSS JOIN farm f;

INSERT INTO water_level_history (created_at, updated_at, source_id, measured_at, level_liters)
SELECT now(), now(), s.id, (CURRENT_DATE - d) + time '06:00',
       CASE WHEN s.type = 'rain' THEN 9200 + d * 450 ELSE 41500 + 900 * sin(d) END
FROM generate_series(0, 13) AS d
CROSS JOIN water_sources s;

INSERT INTO water_quality_tests (created_at, updated_at, source_id, test_date, ph, temperature_celsius, turbidity_ntu,
                                 conductivity_us_cm, salinity_ppt)
SELECT now(), now(), s.id, (CURRENT_DATE - t.days_ago) + time '08:30', t.ph, t.temp, t.turbidity, t.conductivity, t.salinity
FROM (VALUES
    ('borehole', 30, 7.2, 28.5, 0.8, 410, 0.2),
    ('borehole',  2, 7.1, 29.0, 0.9, 420, 0.2),
    ('rain',      9, 6.6, 27.0, 3.5,  60, 0.0)
) AS t (type, days_ago, ph, temp, turbidity, conductivity, salinity)
JOIN water_sources s ON s.type = t.type;

-- Irrigation every other day on three drip zones over two weeks, this morning included (done),
-- the next ones planned, one postponed after a shower.
INSERT INTO irrigation_schedules (created_at, updated_at, zone_id, start_time, duration_minutes, water_quantity_liters,
                                  status, trigger_source, postpone_reason, created_by)
SELECT now(), now(), z.id, (CURRENT_DATE - d) + time '06:00', 90, round((z.emitter_count * z.emitter_nominal_flow_lh * 1.5)::numeric, 0),
       'completed', CASE WHEN d % 4 = 0 THEN 'auto' ELSE 'manual' END, NULL, p.id
FROM generate_series(0, 14, 2) AS d
CROSS JOIN zone z
JOIN person p ON p.email = 'moumouni.ouattara@ferme-banfora.example'
WHERE z.irrigation_method = 'drip' AND z.name IN ('A1 north drip', 'B1 river drip', 'C2 east drip');

INSERT INTO irrigation_schedules (created_at, updated_at, zone_id, start_time, duration_minutes, water_quantity_liters,
                                  status, trigger_source, postpone_reason, created_by)
SELECT now(), now(), z.id, s.start_at, s.minutes, s.liters, s.status, 'manual', s.reason, p.id
FROM (VALUES
    ('A2 south drip', CURRENT_DATE + 1 + time '06:00', 90, 6240, 'scheduled', NULL),
    ('C1 young trees', CURRENT_DATE + 1 + time '06:30', 60, 840, 'scheduled', NULL),
    ('B2 sprinkler',  CURRENT_DATE - 1 + time '17:00', 45, 8100, 'postponed', '11 mm of rain in the afternoon')
) AS s (zone_name, start_at, minutes, liters, status, reason)
JOIN zone z ON z.name = s.zone_name
JOIN person p ON p.email = 'moumouni.ouattara@ferme-banfora.example';

INSERT INTO irrigation_logs (created_at, updated_at, schedule_id, actual_start_time, actual_end_time, water_used_liters,
                             status)
SELECT now(), now(), s.id, s.start_time, s.start_time + s.duration_minutes * INTERVAL '1 minute',
       round((s.water_quantity_liters * 0.97)::numeric, 0), 'completed'
FROM irrigation_schedules s
WHERE s.status = 'completed';

-- Water used: the irrigations above, and the washing, cutting and cleaning water of the lots.
INSERT INTO water_consumption (created_at, updated_at, farm_id, source_id, zone_id, irrigation_id, consumption_date,
                               consumption_liters, activity, lot_id, processing_batch_id)
SELECT now(), now(), f.id, w.id, s.zone_id, s.id, s.start_time, round((s.water_quantity_liters * 0.97)::numeric, 0),
       'IRRIGATION', NULL, NULL
FROM irrigation_schedules s
CROSS JOIN farm f
JOIN water_sources w ON w.type = 'borehole'
WHERE s.status = 'completed';

INSERT INTO water_consumption (created_at, updated_at, farm_id, source_id, zone_id, irrigation_id, consumption_date,
                               consumption_liters, activity, lot_id, processing_batch_id)
SELECT now(), now(), f.id, w.id, NULL, NULL, ws.start_time, ws.water_usage_liters, 'WASHING', b.code, b.id
FROM wash_sort_record ws
JOIN batch b ON b.id = ws.batch_id
CROSS JOIN farm f
JOIN water_sources w ON w.type = 'borehole';

INSERT INTO water_consumption (created_at, updated_at, farm_id, source_id, zone_id, irrigation_id, consumption_date,
                               consumption_liters, activity, lot_id, processing_batch_id)
SELECT now(), now(), f.id, w.id, NULL, NULL, dr.end_time, c.liters, c.activity, b.code, b.id
FROM drying_run dr
JOIN batch b ON b.id = dr.batch_id
CROSS JOIN farm f
JOIN water_sources w ON w.type = 'rain'
CROSS JOIN (VALUES ('CUTTING', 350.0), ('DRYER_CLEANING', 600.0)) AS c (activity, liters)
WHERE dr.end_time > CURRENT_DATE - 30;

INSERT INTO water_quotas (created_at, updated_at, target_type, target_id, quota_month, quota_liters, label)
SELECT now(), now(), 'farm', f.id, date_trunc('month', CURRENT_DATE)::date, 450000, 'Borehole permit, monthly volume'
FROM farm f;

INSERT INTO water_quotas (created_at, updated_at, target_type, target_id, quota_month, quota_liters, label)
SELECT now(), now(), 'zone', z.id, date_trunc('month', CURRENT_DATE)::date, 60000, 'Keitt drip zone, dry season'
FROM zone z WHERE z.name = 'A1 north drip';

INSERT INTO rainwater_harvests (created_at, updated_at, source_id, capture_date, rainfall_mm, catchment_area_m2,
                                runoff_coefficient, harvested_liters)
SELECT now(), now(), s.id, (CURRENT_DATE - r.days_ago) + time '18:00', r.mm, 420, 0.85, round(r.mm * 420 * 0.85)
FROM (VALUES (48, 32.0), (37, 18.5), (25, 9.0), (1, 11.0)) AS r (days_ago, mm)
JOIN water_sources s ON s.type = 'rain';

INSERT INTO soil_moisture_readings (created_at, updated_at, zone_id, measured_at, moisture_percent, depth_cm)
SELECT now(), now(), z.id, now() - h * INTERVAL '8 hours', 30 + 3 * sin(h + z.id), 30
FROM generate_series(0, 8) AS h
CROSS JOIN zone z;

INSERT INTO drip_maintenance_logs (created_at, updated_at, zone_id, maintenance_date, maintenance_type, filter_cleaned,
                                   clogging_detected, clogging_severity, emitter_replaced_count, notes,
                                   performed_by_person_id)
SELECT now(), now(), z.id, (CURRENT_DATE - m.days_ago) + time '07:30', m.type, m.filter, m.clogging, m.severity,
       m.replaced, m.notes, p.id
FROM (VALUES
    ('A1 north drip', 21, 'cleaning',      true,  true,  'medium', 14, 'Lime deposits on the emitters of rows 10 to 14, acid flush'),
    ('C2 east drip',   6, 'inspection',    true,  false, NULL,      0, 'No anomaly'),
    ('B1 river drip',  2, 'filter_change', true,  true,  'low',     3, 'Disc filter replaced')
) AS m (zone_name, days_ago, type, filter, clogging, severity, replaced, notes)
JOIN zone z ON z.name = m.zone_name
JOIN person p ON p.email = 'lassina.dao@ferme-banfora.example';

INSERT INTO water_unavailability (created_at, updated_at, scope, scope_id, start_at, end_at, reason, status)
SELECT now(), now(), 'zone', z.id, CURRENT_DATE + 3 + time '07:00', CURRENT_DATE + 3 + time '13:00',
       'Main pipe repair on zone B1', 'planned'
FROM zone z WHERE z.name = 'B1 river drip';

INSERT INTO iot_devices (created_at, updated_at, device_id, type, last_seen, battery_percent, rssi) VALUES
    (now(), now(), 'SOIL-A1-01',  'soil',  now() - INTERVAL '12 minutes', 78, -71),
    (now(), now(), 'LEVEL-F1-01', 'level', now() - INTERVAL '5 minutes',  92, -64),
    (now(), now(), 'FLOW-PT-01',  'flow',  now() - INTERVAL '3 hours',    41, -83);

INSERT INTO water_safety_briefing (created_at, updated_at, version, briefing_text, allowed_zones, forbidden_zones, risks,
                                   guide_required, max_capacity)
VALUES (now(), now(), '2026-1',
        'Stay with your guide near the water points. Do not touch the valves or the drip lines. '
        || 'The pump room is closed to visitors.',
        'rainwater tank surroundings|demonstration drip line', 'pump room|borehole head',
        'slippery ground near the tank|electrical hazard at the pump', true, 25);

INSERT INTO alert (created_at, updated_at, source_module, type, severity, status, title, message, recipient_person_id,
                   is_read, action_url, raised_at)
SELECT now(), now(), 'WATERSUPPLY', a.type, a.severity, 'OPEN', a.title, a.message, p.id, a.is_read, a.url, now() - a.age
FROM (VALUES
    ('warning', 'WARNING', 'Rainwater tank below 35 %', 'The rainwater tank holds 9 200 L of 30 000 L.', false,
     '/watersupply/sources', INTERVAL '6 hours'),
    ('info', 'INFO', 'Irrigation postponed on zone B2', 'Rain in the afternoon: the sprinkler run is postponed.', true,
     '/watersupply/irrigation', INTERVAL '1 day'),
    ('warning', 'WARNING', 'Planned outage on zone B1', 'Main pipe repair in 3 days, 07:00 to 13:00.', false,
     '/watersupply/unavailability', INTERVAL '2 hours')
) AS a (type, severity, title, message, is_read, url, age)
JOIN person p ON p.email = 'moumouni.ouattara@ferme-banfora.example';

-- ----------------------------------------------------------------------------
-- salesmarketing: products of the packaged lots, EU importers, orders and shipments
-- ----------------------------------------------------------------------------

INSERT INTO products (created_at, updated_at, name, unit, variety_key) VALUES
    (now(), now(), 'Dried mango slices Keitt, bulk carton 10 kg', 'kg', 'KEITT'),
    (now(), now(), 'Dried mango slices Kent, 1 kg bag',          'kg', 'KENT'),
    (now(), now(), 'Dried mango slices Amelie organic, bulk',     'kg', 'AMELIE'),
    (now(), now(), 'Dried mango slices Kent, 100 g snack pack',  'kg', 'KENT');

-- The packaged lots of product transformation, as sales stock.
INSERT INTO product_batches (created_at, updated_at, batch_code, packaging_record_id, product_id, status, stock_t,
                             synced_at)
SELECT now(), now(), pr.lot_code, pr.id, pd.id, CASE WHEN pr.export_ready THEN 'shipped' ELSE 'in_stock' END,
       CASE WHEN pr.export_ready THEN 0.00 ELSE round(pr.package_quantity_kg / 1000.0, 2) END, now()
FROM packaging_record pr
JOIN batch b ON b.id = pr.batch_id
JOIN products pd ON pd.variety_key = b.variety_key AND pd.name NOT LIKE '%snack%';

INSERT INTO certifications (created_at, updated_at, name) VALUES
    (now(), now(), 'EU Organic'),
    (now(), now(), 'Fairtrade'),
    (now(), now(), 'GLOBALG.A.P.'),
    (now(), now(), 'IFS Food');

INSERT INTO customers (created_at, updated_at, customer_code, company_name, customer_type, country, city, region,
                       payment_terms, status) VALUES
    (now(), now(), 'CUS-001', 'Hanse Trockenfrucht Import GmbH', 'direct_export', 'Germany', 'Hamburg',  'Northern Europe', '30 days net', 'active'),
    (now(), now(), 'CUS-002', 'Rheinland Naturkost GmbH',        'distributor',   'Germany', 'Cologne',  'Western Europe',  '45 days net', 'active'),
    (now(), now(), 'CUS-003', 'BioMarkt Isar eG',                'retailer',      'Germany', 'Munich',   'Southern Germany', '30 days net', 'active'),
    (now(), now(), 'CUS-004', 'Saveurs du Sahel SARL',           'distributor',   'France',  'Lyon',     'Western Europe',  'Prepayment',  'prospect');

INSERT INTO customer_contacts (created_at, updated_at, customer_id, full_name, role, email, phone, is_primary)
SELECT now(), now(), c.id, k.name, k.role, k.email, k.phone, k.is_primary
FROM (VALUES
    ('CUS-001', 'Jana Petersen',   'Head of purchasing',  'j.petersen@hanse-trockenfrucht.example', '+49 40 5550 1234', true),
    ('CUS-001', 'Lukas Brandt',    'Quality manager',     'l.brandt@hanse-trockenfrucht.example',   '+49 40 5550 1240', false),
    ('CUS-002', 'Miriam Schulte',  'Category manager',    'm.schulte@rheinland-naturkost.example',  '+49 221 5550 877', true),
    ('CUS-003', 'Tobias Huber',    'Buyer, dried fruit',  't.huber@biomarkt-isar.example',          '+49 89 5550 3321', true),
    ('CUS-004', 'Claire Fontaine', 'Managing director',   'c.fontaine@saveurs-sahel.example',       '+33 4 55 50 12 34', true)
) AS k (code, name, role, email, phone, is_primary)
JOIN customers c ON c.customer_code = k.code;

INSERT INTO customer_certifications (created_at, updated_at, customer_id, certification_id)
SELECT now(), now(), c.id, ce.id
FROM (VALUES ('CUS-001', 'EU Organic'), ('CUS-001', 'IFS Food'), ('CUS-002', 'EU Organic'), ('CUS-002', 'Fairtrade'),
             ('CUS-003', 'EU Organic'), ('CUS-004', 'Fairtrade')) AS x (code, certification)
JOIN customers c ON c.customer_code = x.code
JOIN certifications ce ON ce.name = x.certification;

INSERT INTO sales_channels (created_at, updated_at, name) VALUES
    (now(), now(), 'Direct export'),
    (now(), now(), 'Distributors'),
    (now(), now(), 'Retail chains');

INSERT INTO sales_channel_targets (created_at, updated_at, channel_id, period_start, period_end, revenue_target_amount,
                                   revenue_target_currency)
SELECT now(), now(), ch.id, date_trunc('quarter', CURRENT_DATE)::date,
       (date_trunc('quarter', CURRENT_DATE) + INTERVAL '3 months' - INTERVAL '1 day')::date, t.target, 'EUR'
FROM (VALUES ('Direct export', 18000), ('Distributors', 9000), ('Retail chains', 4000)) AS t (channel, target)
JOIN sales_channels ch ON ch.name = t.channel;

-- Four orders; the totals are the sum of their items, in euros.
INSERT INTO orders (created_at, updated_at, customer_id, sales_channel_id, order_date, status, total_volume_kg,
                    total_value_amount, total_value_currency)
SELECT now() - o.days_ago * INTERVAL '1 day', now(), c.id, ch.id, CURRENT_DATE - o.days_ago, o.status, o.kg, o.eur, 'EUR'
FROM (VALUES
    ('CUS-001', 'Direct export', 75, 'delivered', 400.0, 3920.00),
    ('CUS-001', 'Direct export', 33, 'shipped',   345.0, 3450.00),
    ('CUS-002', 'Distributors',  12, 'confirmed', 200.0, 2160.00),
    ('CUS-003', 'Retail chains',  3, 'pending',    60.0,  930.00)
) AS o (code, channel, days_ago, status, kg, eur)
JOIN customers c ON c.customer_code = o.code
JOIN sales_channels ch ON ch.name = o.channel;

INSERT INTO order_items (created_at, updated_at, order_id, product_id, quantity_kg, unit_price_amount, unit_price_currency)
SELECT now(), now(), o.id, p.id, i.kg, i.price, 'EUR'
FROM (VALUES
    (75, 'Dried mango slices Keitt, bulk carton 10 kg', 400.0,  9.80),
    (33, 'Dried mango slices Keitt, bulk carton 10 kg', 345.0, 10.00),
    (12, 'Dried mango slices Kent, 1 kg bag',           200.0, 10.80),
    (3,  'Dried mango slices Kent, 100 g snack pack',    60.0, 15.50)
) AS i (days_ago, product, kg, price)
JOIN orders o ON o.order_date = CURRENT_DATE - i.days_ago
JOIN products p ON p.name = i.product;

-- Banfora -> Abidjan port by truck -> Hamburg by sea.
INSERT INTO shipments (created_at, updated_at, shipment_code, order_id, origin, destination, status, eta, vessel_name,
                       voyage_number, current_location_label, current_lat, current_lon, speed_knots,
                       distance_remaining_nm, commodity_description, container_count, container_type, cargo_weight_kg,
                       current_temp_c, temp_status, delay_reason, manifest_url)
SELECT now(), now(), s.code, o.id, 'Banfora, Burkina Faso', 'Hamburg, Germany', s.status, CURRENT_DATE + s.eta_days,
       s.vessel, s.voyage, s.location, s.lat, s.lon, s.speed, s.remaining, s.commodity, 1, '20ft dry', s.kg, s.temp,
       'normal', NULL, NULL
FROM (VALUES
    (75, 'SHP-001', 'delivered', -38, 'MV Atlantic Kora', 'AK-0412W', 'Hamburg, Burchardkai', 53.5065, 9.9310, 0.0, 0.0,
     'Dried mango slices Keitt, 40 cartons', 420.0, 18.0),
    (33, 'SHP-002', 'in_transit', 9, 'MV Sahel Star', 'SS-1123N', 'Off the Canary Islands', 28.1000, -16.2000, 15.5, 1680.0,
     'Dried mango slices Keitt, lot PKG A-001, 35 cartons', 360.0, 21.5)
) AS s (order_days_ago, code, status, eta_days, vessel, voyage, location, lat, lon, speed, remaining, commodity, kg, temp)
JOIN orders o ON o.order_date = CURRENT_DATE - s.order_days_ago;

INSERT INTO shipment_events (created_at, updated_at, shipment_id, step, note, event_time)
SELECT now(), now(), sh.id, e.step, e.note, (CURRENT_DATE - e.days_ago) + time '10:00'
FROM (VALUES
    ('SHP-001', 'Packed',            'Loaded at the farm',               72),
    ('SHP-001', 'Truck departed',    'Banfora to Abidjan by road',       70),
    ('SHP-001', 'Port of Abidjan',   'Container gated in',               66),
    ('SHP-001', 'At sea',            'Vessel sailed',                    63),
    ('SHP-001', 'Hamburg port',      'Discharged and cleared',           42),
    ('SHP-001', 'Delivered',         'Received by the customer',         40),
    ('SHP-002', 'Packed',            'Loaded at the farm',               31),
    ('SHP-002', 'Truck departed',    'Banfora to Abidjan by road',       30),
    ('SHP-002', 'Port of Abidjan',   'Container gated in',               26),
    ('SHP-002', 'At sea',            'Vessel sailed',                    22)
) AS e (code, step, note, days_ago)
JOIN shipments sh ON sh.shipment_code = e.code;

INSERT INTO campaigns (created_at, updated_at, name, status, start_date, end_date, budget_amount, budget_currency,
                       budget_spent_amount, budget_spent_currency, currency, estimated_reach) VALUES
    (now(), now(), 'Organic Kent launch in Germany', 'active',    CURRENT_DATE - 20, CURRENT_DATE + 40, 4000, 'EUR', 1650, 'EUR', 'EUR', 25000),
    (now(), now(), 'Trade fair follow-up mailing',   'scheduled', CURRENT_DATE + 30, CURRENT_DATE + 60, 1500, 'EUR',    0, 'EUR', 'EUR', 6000);

INSERT INTO campaign_metrics (created_at, updated_at, campaign_id, metric_date, reach, clicks, signups)
SELECT now(), now(), c.id, CURRENT_DATE - d, 900 + d * 35, 40 + d * 2, d % 4
FROM generate_series(0, 13) AS d
JOIN campaigns c ON c.name = 'Organic Kent launch in Germany';

-- Monthly demand in tonnes: the past months as actuals, the next ones as forecasts.
INSERT INTO demand_forecasts (created_at, updated_at, product_id, forecast_month, scenario, forecasted_volume_t,
                              lower_bound_t, upper_bound_t, capacity_risk, is_actual, model_version)
SELECT now(), now(), p.id, (date_trunc('month', CURRENT_DATE) + m * INTERVAL '1 month')::date, 'base',
       round((f.base + m * f.step)::numeric, 2), round((f.base + m * f.step - 0.1 - greatest(m, 0) * 0.05)::numeric, 2),
       round((f.base + m * f.step + 0.1 + greatest(m, 0) * 0.05)::numeric, 2),
       CASE WHEN f.base + m * f.step > 0.9 THEN 'high' WHEN f.base + m * f.step > 0.6 THEN 'medium' ELSE 'low' END,
       m < 0, 'v1'
FROM generate_series(-5, 3) AS m
CROSS JOIN (VALUES ('Dried mango slices Keitt, bulk carton 10 kg', 0.70, 0.04),
                   ('Dried mango slices Kent, 1 kg bag', 0.25, 0.02)) AS f (product, base, step)
JOIN products p ON p.name = f.product;

INSERT INTO forecast_model_runs (created_at, updated_at, crop_variety, region, model_status, confidence_interval_pct,
                                 use_weather_data, use_commodity_prices, use_geopolitical_index)
VALUES (now(), now(), 'Keitt', 'Germany', 'trained', 85.00, true, true, false);

INSERT INTO pricing_history (created_at, updated_at, product_id, price_date, price_per_kg_amount, price_per_kg_currency,
                             season_label, harvest_season_factor, demand_factor, demand_index_label, stock_level_tons,
                             stock_target_tons, is_applied)
SELECT now(), now(), p.id, (date_trunc('month', CURRENT_DATE) - m * INTERVAL '1 month')::date,
       round((h.price + CASE WHEN m BETWEEN 4 AND 6 THEN -0.4 ELSE 0.2 END)::numeric, 2), 'EUR',
       CASE WHEN m BETWEEN 4 AND 7 THEN 'Harvest season' ELSE 'Off season' END,
       CASE WHEN m BETWEEN 4 AND 7 THEN 0.90 ELSE 1.05 END, 1.00 + m * 0.01, 'normal', 0.40, 0.60, m = 0
FROM generate_series(0, 5) AS m
CROSS JOIN (VALUES ('Dried mango slices Keitt, bulk carton 10 kg', 9.80),
                   ('Dried mango slices Kent, 1 kg bag', 10.80)) AS h (product, price)
JOIN products p ON p.name = h.product;

INSERT INTO competitor_prices (created_at, updated_at, product_id, competitor_name, price_date, price_amount,
                               price_currency, is_target)
SELECT now(), now(), p.id, c.name, CURRENT_DATE - c.days_ago, c.price, 'EUR', c.is_target
FROM (VALUES ('Ghana organic mango', 10, 10.40, false), ('Peru dried mango', 6, 8.90, false),
             ('Our target price', 1, 10.20, true)) AS c (name, days_ago, price, is_target)
JOIN products p ON p.name = 'Dried mango slices Keitt, bulk carton 10 kg';

INSERT INTO pricing_recommendations (created_at, updated_at, product_id, message, suggested_price_amount,
                                     suggested_price_currency)
SELECT now(), now(), p.id, 'Stock below target and demand up before Christmas: raise the bulk price by 2 %', 10.20, 'EUR'
FROM products p WHERE p.name = 'Dried mango slices Keitt, bulk carton 10 kg';

INSERT INTO demand_reports (created_at, updated_at, report_type, period_start, period_end, generated_at,
                            generated_by_person_id, status, summary, total_forecasted_volume_t, total_actual_volume_t,
                            variance_pct, is_scheduled, schedule_frequency, next_run_at)
SELECT now(), now(), 'monthly', (date_trunc('month', CURRENT_DATE) - INTERVAL '1 month')::date,
       (date_trunc('month', CURRENT_DATE) - INTERVAL '1 day')::date, now() - INTERVAL '3 days', p.id, 'ready',
       'Demand on target for Keitt, Kent slightly under the forecast.', 1.08, 1.02, -5.56, true, 'monthly',
       date_trunc('month', CURRENT_DATE) + INTERVAL '1 month 1 day'
FROM person p WHERE p.email = 'moumouni.ouattara@ferme-banfora.example';

INSERT INTO report_files (created_at, updated_at, report_id, format, file_url)
SELECT now(), now(), r.id, 'pdf', NULL FROM demand_reports r;

INSERT INTO alert (created_at, updated_at, source_module, code, type, severity, status, message, region, origin, is_read,
                   raised_at) VALUES
    (now(), now(), 'SALESMARKETING', 'ALT-001', 'High Demand', 'CRITICAL', 'OPEN',
     'Christmas orders in Germany above the forecast: 1.2 t asked for 0.9 t in stock', 'Germany', 'Burkina Faso', false,
     now() - INTERVAL '1 day'),
    (now(), now(), 'SALESMARKETING', 'ALT-002', 'Data Deviation', 'INFO', 'ACKNOWLEDGED',
     'Kent sales 6 % under the forecast last month', 'Germany', 'Burkina Faso', false, now() - INTERVAL '5 days');

-- ----------------------------------------------------------------------------
-- visitormanagement: farm visits, guided by the staff above, paid in FCFA
-- ----------------------------------------------------------------------------

INSERT INTO agri_activity (created_at, updated_at, name, description, price_amount, price_currency, capacity,
                           duration_minutes, active) VALUES
    (now(), now(), 'Farm tour', 'Guided tour of the orchard, the drip irrigation, the solar plant and the drying unit',
     5000, 'XOF', 20, 120, true),
    (now(), now(), 'Mango drying workshop', 'Wash, cut and dry mango slices with the processing team', 7500, 'XOF', 12, 90, true),
    (now(), now(), 'School discovery morning', 'Half-day program for school classes', 1500, 'XOF', 40, 180, true);

-- Two slots a day from Monday of this week to Saturday of next week; the past ones are completed.
INSERT INTO time_slot (created_at, updated_at, slot_date, start_time, end_time, max_capacity, status, guide_id)
SELECT now(), now(), d::date, s.start_at, s.end_at, 20, CASE WHEN d::date < CURRENT_DATE THEN 'COMPLETED' ELSE 'AVAILABLE' END,
       st.id
FROM generate_series(date_trunc('week', CURRENT_DATE), date_trunc('week', CURRENT_DATE) + INTERVAL '12 days',
                     INTERVAL '1 day') AS d
CROSS JOIN (VALUES (time '09:00', time '11:00', 'souleymane.barro@ferme-banfora.example'),
                   (time '15:00', time '17:00', 'aminata.konate@ferme-banfora.example')) AS s (start_at, end_at, guide)
JOIN person p ON p.email = s.guide
JOIN staff st ON st.person_id = p.id
WHERE EXTRACT(ISODOW FROM d) <> 7;

INSERT INTO visitor (created_at, updated_at, full_name, visitor_type, group_size, email, phone, language, special_needs) VALUES
    (now(), now(), 'Ecole primaire de Tengrela',   'SCHOOL',     32, 'direction.tengrela@ecoles.example', '+226 20 91 00 11', 'French', NULL),
    (now(), now(), 'Julia Becker',                 'INDIVIDUAL',  2, 'julia.becker@mail.example',          '+49 171 5550 2468', 'German', NULL),
    (now(), now(), 'Hanse Trockenfrucht delegation', 'PARTNER',   3, 'j.petersen@hanse-trockenfrucht.example', '+49 40 5550 1234', 'English', NULL),
    (now(), now(), 'Women group of Beregadougou',  'GROUP',      14, 'femmes.beregadougou@assoc.example',  '+226 70 33 44 55', 'Dioula', 'Two elderly members, short walking distance'),
    (now(), now(), 'Lycee provincial de Banfora',  'SCHOOL',     28, 'lycee.banfora@ecoles.example',       '+226 20 91 02 22', 'French', NULL);

-- Bookings of the activities: a completed one, two confirmed, one pending, one cancelled.
INSERT INTO booking (created_at, updated_at, visitor_id, agri_activity_id, time_slot_id, people_count, total_amount,
                     total_currency, status, payment_status, payment_method, confirmation_sent_at)
SELECT now(), now(), v.id, a.id, ts.id, b.people, a.price_amount * b.people, 'XOF', b.status, b.payment, b.method,
       CASE WHEN b.status IN ('CONFIRMED', 'COMPLETED') THEN now() - INTERVAL '2 days' END
FROM (VALUES
    ('Julia Becker',                 'Farm tour',                 -2, time '09:00',  2, 'COMPLETED', 'PAID',     'ORANGE_MONEY'),
    ('Women group of Beregadougou',  'Mango drying workshop',      1, time '15:00', 12, 'CONFIRMED', 'PAID',     'CASH'),
    ('Ecole primaire de Tengrela',   'School discovery morning',   2, time '09:00', 32, 'CONFIRMED', 'UNPAID',   NULL),
    ('Julia Becker',                 'Mango drying workshop',      4, time '15:00',  2, 'PENDING',   'UNPAID',   NULL),
    ('Lycee provincial de Banfora',  'Farm tour',                  6, time '09:00', 20, 'CANCELLED', 'REFUNDED', 'MOOV_MONEY')
) AS b (visitor, activity, day_offset, start_at, people, status, payment, method)
JOIN visitor v ON v.full_name = b.visitor
JOIN agri_activity a ON a.name = b.activity
JOIN time_slot ts ON ts.start_time = b.start_at
 AND ts.slot_date = (SELECT min(t2.slot_date) FROM time_slot t2
                     WHERE t2.slot_date >= CURRENT_DATE + b.day_offset AND EXTRACT(ISODOW FROM t2.slot_date) <> 7);

-- Visit registrations: a checked-in partner visit, a confirmed and a pending one, a visitor at the open day.
INSERT INTO event (created_at, updated_at, title, event_type, start_date_time, end_date_time, max_capacity, location,
                   description, status)
SELECT now(), now(), 'Mango open day', 'OPEN_DAY', d + time '09:00', d + time '16:00', 120, 'Ferme de Banfora',
       'Guided tours, drying demonstrations and tasting of the new season lots', 'PUBLISHED'
FROM (SELECT (date_trunc('week', CURRENT_DATE) + INTERVAL '12 days')::date AS d) AS saturday;

INSERT INTO registration (created_at, updated_at, visitor_id, time_slot_id, event_id, visit_purpose, is_prospect, status)
SELECT now(), now(), v.id, ts.id, NULL, r.purpose, r.prospect, r.status
FROM (VALUES
    ('Hanse Trockenfrucht delegation', -1, time '09:00', 'PARTNERSHIP', false, 'CHECKED_IN'),
    ('Ecole primaire de Tengrela',      2, time '09:00', 'EDUCATION',   false, 'CONFIRMED'),
    ('Women group of Beregadougou',     5, time '15:00', 'PURCHASE',    true,  'PENDING')
) AS r (visitor, day_offset, start_at, purpose, prospect, status)
JOIN visitor v ON v.full_name = r.visitor
JOIN time_slot ts ON ts.start_time = r.start_at
 AND ts.slot_date = (SELECT min(t2.slot_date) FROM time_slot t2
                     WHERE t2.slot_date >= CURRENT_DATE + r.day_offset AND EXTRACT(ISODOW FROM t2.slot_date) <> 7);

INSERT INTO registration (created_at, updated_at, visitor_id, time_slot_id, event_id, visit_purpose, is_prospect, status)
SELECT now(), now(), v.id, NULL, e.id, 'TOURISM', false, 'CONFIRMED'
FROM visitor v CROSS JOIN event e
WHERE v.full_name = 'Julia Becker';

INSERT INTO briefing (created_at, updated_at, registration_id, staff_member_person_id, status, delivered_at, signature)
SELECT now(), now(), r.id, p.id, 'DONE', ts.slot_date + time '09:05', 'J. Petersen'
FROM registration r
JOIN time_slot ts ON ts.id = r.time_slot_id
JOIN person p ON p.email = 'souleymane.barro@ferme-banfora.example'
WHERE r.status = 'CHECKED_IN';

INSERT INTO tour_stop (created_at, updated_at, stop_position, name, description, duration_minutes, max_capacity, location,
                       demo, safety_notes, active) VALUES
    (now(), now(), 1, 'Welcome and safety briefing', 'The farm, the visit and the safety rules', 10, 40, 'Reception hut', NULL, 'Water and sun hat for everyone', true),
    (now(), now(), 2, 'Keitt orchard', 'Tree care, fruit fly traps and harvest of block A', 25, 25, 'Block A', 'Fruit fly trap', 'Stay on the paths', true),
    (now(), now(), 3, 'Drip irrigation', 'How the drip lines and the soil sensors save water', 15, 25, 'Zone A1', 'Soil moisture sensor', 'Do not step on the drip lines', true),
    (now(), now(), 4, 'Solar plant', 'The PV array and the battery bank that run the farm', 15, 25, 'Processing hall roof', 'Energy dashboard', 'Keep away from the inverter room', true),
    (now(), now(), 5, 'Drying unit', 'Washing, cutting and drying of the mango slices', 25, 15, 'Processing hall', 'Solar dryer', 'Hair net, no jewellery', true),
    (now(), now(), 6, 'Tasting and questions', 'Dried mango tasting and exit survey', 15, 40, 'Reception hut', NULL, NULL, true);

INSERT INTO workshop (created_at, updated_at, name, description, duration_minutes, target_group, facilitator_person_id,
                      status)
SELECT now(), now(), w.name, w.description, w.minutes, w.target, p.id, w.status
FROM (VALUES
    ('From mango to dried slice', 'Hands-on drying workshop in the processing hall', 90, 'General public',
     'fatoumata.sawadogo@ferme-banfora.example', 'ACTIVE'),
    ('Water and the orchard', 'Drip irrigation and water saving for school classes', 60, 'Schools',
     'souleymane.barro@ferme-banfora.example', 'ACTIVE'),
    ('Solar energy on the farm', 'How the farm runs on the sun', 45, 'Professionals',
     'lassina.dao@ferme-banfora.example', 'DRAFT')
) AS w (name, description, minutes, target, email, status)
JOIN person p ON p.email = w.email;

INSERT INTO survey_send (created_at, updated_at, visitor_id, channel, message_template, sent_at, status)
SELECT now(), now(), v.id, s.channel, 'Thank you for your visit to the Ferme de Banfora. How was it?',
       now() - s.age, 'RECEIVED'
FROM (VALUES ('Julia Becker', 'EMAIL', INTERVAL '1 day'),
             ('Hanse Trockenfrucht delegation', 'WHATSAPP', INTERVAL '20 hours')) AS s (visitor, channel, age)
JOIN visitor v ON v.full_name = s.visitor;

INSERT INTO feedback (created_at, updated_at, visitor_id, visitor_name, survey_send_id, origin, rating, comment,
                      briefing_clear, educational_value, recommend, routed_to, submitted_at)
SELECT now(), now(), v.id, v.full_name, ss.id, ss.channel, f.rating, f.comment, 'yes', f.educational, 'yes', NULL,
       ss.sent_at + INTERVAL '3 hours'
FROM (VALUES
    ('Julia Becker', 5, 'Wonderful tour, we learnt a lot about drying mangoes.', 'high'),
    ('Hanse Trockenfrucht delegation', 4, 'Clean processing hall. We would like to see the traceability records next time.', 'medium')
) AS f (visitor, rating, comment, educational)
JOIN visitor v ON v.full_name = f.visitor
JOIN survey_send ss ON ss.visitor_id = v.id;

INSERT INTO feedback (created_at, updated_at, visitor_id, visitor_name, survey_send_id, origin, rating, comment,
                      briefing_clear, educational_value, recommend, routed_to, submitted_at)
VALUES (now(), now(), NULL, 'Anonymous visitor', NULL, 'ON_SITE', 3, 'Too hot in the afternoon, more shade at the dryer please.',
        'yes', 'medium', 'maybe', NULL, now() - INTERVAL '6 days');

-- The confirmed bookings, announced to the water team (water supply visitor events, visit id
-- booking-<id> as Visitor Management sends it).
INSERT INTO water_visitor_events (created_at, updated_at, visit_id, group_name, expected_at, zones, conflict)
SELECT now(), now(), 'booking-' || b.id, v.full_name, ts.slot_date + ts.start_time, '', false
FROM booking b
JOIN visitor v ON v.id = b.visitor_id
JOIN time_slot ts ON ts.id = b.time_slot_id
WHERE b.status = 'CONFIRMED';
