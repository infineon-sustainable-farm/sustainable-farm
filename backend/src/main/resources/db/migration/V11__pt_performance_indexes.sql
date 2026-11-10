-- Product Transformation performance fix (B3): indexes on the columns the
-- dashboard and the repositories actually filter on.
--
-- Before this migration the PT tables had only PRIMARY KEY and UNIQUE indexes,
-- so every date-range / status filter made a full table scan.
--
-- NOTE on column choices: the Day 12 audit listed `qc_checkpoint(run_id)`;
-- this module's qc_checkpoint table has NO run_id column (it is keyed on
-- checkpoint_time and linked to batch_id). Indexes are created on the real
-- filter columns instead: checkpoint_time and batch_id.
--
-- The drying run -> batch relation uses join column batch_id (verified against
-- the entity @JoinColumn names).
--
-- CREATE INDEX IF NOT EXISTS makes the migration idempotent and safe to run in
-- development and CI where the PT schema may be partially initialised.

CREATE INDEX IF NOT EXISTS idx_harvest_event_harvest_date ON harvest_event (harvest_date);
CREATE INDEX IF NOT EXISTS idx_harvest_event_farm_id    ON harvest_event (farm_id);

CREATE INDEX IF NOT EXISTS idx_batch_current_status     ON batch (current_status);
CREATE INDEX IF NOT EXISTS idx_batch_harvest_date       ON batch (harvest_date);

CREATE INDEX IF NOT EXISTS idx_drying_run_batch_id      ON drying_run (batch_id);
CREATE INDEX IF NOT EXISTS idx_drying_run_start_time    ON drying_run (start_time);

CREATE INDEX IF NOT EXISTS idx_qc_checkpoint_checkpoint_time ON qc_checkpoint (checkpoint_time);
CREATE INDEX IF NOT EXISTS idx_qc_checkpoint_batch_id   ON qc_checkpoint (batch_id);

CREATE INDEX IF NOT EXISTS idx_wash_sort_record_start_time ON wash_sort_record (start_time);
