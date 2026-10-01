-- ============================================================================
-- Demonstration de la logique d'affichage de la vue Irrigation
--   * Plannings d'irrigation : uniquement les plannings ACTIFS (en cours / prevu)
--   * Journal des irrigations : uniquement les cycles TERMINES, les 10 plus recents
--     (les plus anciens restent conserves en base de donnees)
-- ============================================================================

TRUNCATE irrigation_logs, irrigation_schedules;

-- 1) 12 cycles TERMINES -> seuls les 10 plus recents seront affiches dans le journal
INSERT INTO irrigation_schedules (id, created_at, created_by, duration_minutes, start_time, status, water_quantity_liters, zone_id)
SELECT gen_random_uuid(),
       now(),
       '00000000-0000-0000-0000-000000000000',
       45,
       ((now() - (i || ' days')::interval)::date + time '06:15'),
       'completed',
       1500,
       '0e8fd9c1-8dcc-4dcc-bb04-ffc71e33f42f'
FROM generate_series(1, 12) i;

INSERT INTO irrigation_logs (id, created_at, actual_end_time, actual_start_time, schedule_id, status, water_used_liters)
SELECT gen_random_uuid(),
       now(),
       s.start_time + interval '58 minutes',
       s.start_time + interval '6 minutes',
       s.id,
       'completed',
       1400 + s.rn * 7
FROM (
  SELECT id, start_time, row_number() OVER (ORDER BY start_time) AS rn
  FROM irrigation_schedules
  WHERE status = 'completed'
) s;

-- 2) Planning EN COURS + log ouvert (l'irrigation tourne : visible dans les plannings, PAS dans le journal)
INSERT INTO irrigation_schedules (id, created_at, created_by, duration_minutes, start_time, status, water_quantity_liters, zone_id)
VALUES (gen_random_uuid(), now(), '00000000-0000-0000-0000-000000000000', 60,
        now() - interval '6 minutes', 'running', 2200, '0e8fd9c1-8dcc-4dcc-bb04-ffc71e33f42f');

INSERT INTO irrigation_logs (id, created_at, actual_end_time, actual_start_time, schedule_id, status, water_used_liters)
SELECT gen_random_uuid(), now(), NULL, now() - interval '6 minutes', id, 'in_progress', 0
FROM irrigation_schedules
WHERE status = 'running';

-- 3) Planning PLANIFIE pour demain
INSERT INTO irrigation_schedules (id, created_at, created_by, duration_minutes, start_time, status, water_quantity_liters, zone_id)
VALUES (gen_random_uuid(), now(), '00000000-0000-0000-0000-000000000000', 30,
        ((CURRENT_DATE + 1) + time '06:00'), 'scheduled', 900, '0e8fd9c1-8dcc-4dcc-bb04-ffc71e33f42f');

-- 4) Planning REPORTE (toujours actif : necessite une action, donc reste visible)
INSERT INTO irrigation_schedules (id, created_at, created_by, duration_minutes, start_time, status, water_quantity_liters, zone_id)
VALUES (gen_random_uuid(), now(), '00000000-0000-0000-0000-000000000000', 25,
        ((CURRENT_DATE + 2) + time '18:30'), 'postponed', 700, '0e8fd9c1-8dcc-4dcc-bb04-ffc71e33f42f');

-- 5) Verification
SELECT status, count(*) AS plannings FROM irrigation_schedules GROUP BY status ORDER BY status;
SELECT status, count(*) AS logs FROM irrigation_logs GROUP BY status ORDER BY status;