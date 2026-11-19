-- Plants integration (option B): keep the Plants identity on the harvest event.
--
-- A harvest lot and a transformation lot are not always the same thing: one
-- harvest can be split into several drying lots, and several harvests can be
-- merged into one transformation lot. Product Transformation therefore keeps
-- its own batch_id and stores the Plants identity in two dedicated fields:
--
--   lot_number        Plants lot number (for example LOT-2026-A-001)
--   plants_harvest_id Plants harvest id (column already created by V14)
--
-- The unique index on plants_harvest_id makes the import idempotent: importing
-- the same harvest twice cannot create a second event. PostgreSQL allows many
-- NULL values in a unique index, so events created manually (without a Plants
-- harvest) are unaffected.
--
-- farm_id
--   Plants runs a single farm and its screens never ask for a farm identifier,
--   so a harvest coming from Plants has none (confirmed by the Plants team on
--   2026-11-12). The common cross-module farm identifier is still to be agreed.
--   Requiring a value here would force the import to invent one, so the column
--   becomes nullable: NULL means "not assigned yet". The create request still
--   validates farmId as required, so only the import path can leave it empty.
--
-- Guarded so the migration can be re-run on a database where Hibernate
-- (ddl-auto=update) already added the column.

ALTER TABLE harvest_event ADD COLUMN IF NOT EXISTS lot_number character varying(50);

ALTER TABLE harvest_event ALTER COLUMN farm_id DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_harvest_event_plants_harvest_id
    ON harvest_event (plants_harvest_id);
