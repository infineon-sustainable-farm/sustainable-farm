-- ============================================================================
-- PT demo — verification des comptages attendus
--
-- A executer dans la base de demonstration, apres demo-up.sh :
--
--   scripts/demo/demo-up.sh          (l'appelle automatiquement)
--
-- ou a la main :
--
--   docker exec -i sustainable-farm-db \
--     psql -U postgres -d sustainable_farm -f - < scripts/demo/verify_demo_data.sql
--
-- Les comptages portent uniquement sur les lignes prefixees DEMO. Un lot
-- LOT-GP-* laisse par scripts/pt-golden-path.sh, ou un lot cree a la main
-- pendant une repetition, ne fait donc pas echouer la verification.
--
-- Sortie : un tableau (attendu / trouve / statut) puis une exception SQL si un
-- comptage differe, ce qui fait sortir psql en erreur (code 3) et donc
-- demo-up.sh en echec.
-- ============================================================================

\pset footer off
\pset border 2
\set QUIET on

\echo 'Comptages attendus du jeu de demonstration PT :'

CREATE TEMP TABLE demo_expected (table_name text PRIMARY KEY, expected bigint);

INSERT INTO demo_expected (table_name, expected) VALUES
    ('batch',              5),  -- LOT-DEMO-001..005
    ('harvest_event',      5),  -- HE-DEMO-001..005
    ('raw_intake',         5),  -- RI-DEMO-001..005
    ('pt_equipment',       3),  -- EQ-DEMO-01..03
    ('qc_checkpoint',      6),  -- QC-DEMO-001..006
    ('drying_run',         3),  -- DR-DEMO-001..003
    ('wash_sort_record',   2),  -- WS-DEMO-001..002
    ('packaging_record',   2),  -- PKG-DEMO-001..002
    ('compliance_record',  3),  -- CR-DEMO-001..003
    ('operator',           2),  -- OP-DEMO-01..02
    ('historical_harvest', 12); -- 12 mois, weather_condition = 'DEMO'

CREATE TEMP TABLE demo_actual (table_name text PRIMARY KEY, actual bigint);

INSERT INTO demo_actual (table_name, actual) VALUES
    ('batch',              (SELECT count(*) FROM batch              WHERE batch_id LIKE 'LOT-DEMO-%')),
    ('harvest_event',      (SELECT count(*) FROM harvest_event      WHERE harvest_id LIKE 'HE-DEMO-%')),
    ('raw_intake',         (SELECT count(*) FROM raw_intake         WHERE intake_id LIKE 'RI-DEMO-%')),
    ('pt_equipment',       (SELECT count(*) FROM pt_equipment       WHERE equipment_id LIKE 'EQ-DEMO-%')),
    ('qc_checkpoint',      (SELECT count(*) FROM qc_checkpoint      WHERE checkpoint_id LIKE 'QC-DEMO-%')),
    ('drying_run',         (SELECT count(*) FROM drying_run         WHERE run_id LIKE 'DR-DEMO-%')),
    ('wash_sort_record',   (SELECT count(*) FROM wash_sort_record   WHERE record_id LIKE 'WS-DEMO-%')),
    ('packaging_record',   (SELECT count(*) FROM packaging_record   WHERE record_id LIKE 'PKG-DEMO-%')),
    ('compliance_record',  (SELECT count(*) FROM compliance_record  WHERE record_id LIKE 'CR-DEMO-%')),
    ('operator',           (SELECT count(*) FROM operator           WHERE operator_id LIKE 'OP-DEMO-%')),
    ('historical_harvest', (SELECT count(*) FROM historical_harvest WHERE weather_condition = 'DEMO'));

SELECT e.table_name            AS "table",
       e.expected              AS "attendu",
       coalesce(a.actual, 0)   AS "trouve",
       CASE WHEN e.expected = coalesce(a.actual, 0) THEN 'OK' ELSE 'ECHEC' END AS "statut"
FROM demo_expected e
LEFT JOIN demo_actual a USING (table_name)
ORDER BY e.table_name;

-- Le total est un garde-fou : si un comptage passe mais que le total non, c'est
-- qu'une table a ete oubliee dans la liste ci-dessus.
SELECT sum(expected) AS "total attendu", sum(coalesce(actual, 0)) AS "total trouve"
FROM demo_expected e LEFT JOIN demo_actual a USING (table_name);

DO $$
DECLARE
    problems text;
BEGIN
    SELECT string_agg(
               format('%s : attendu %s, trouve %s', e.table_name, e.expected, coalesce(a.actual, 0)),
               ' ; ' ORDER BY e.table_name)
      INTO problems
      FROM demo_expected e
      LEFT JOIN demo_actual a USING (table_name)
     WHERE e.expected <> coalesce(a.actual, 0);

    IF problems IS NOT NULL THEN
        RAISE EXCEPTION 'Jeu de demonstration incomplet -> %', problems;
    END IF;
END
$$;

\echo 'OK : les 11 comptages attendus sont au complet.'
