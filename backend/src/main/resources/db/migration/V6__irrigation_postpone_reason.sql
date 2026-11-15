-- Persistence of the postpone reason on irrigation schedules.
-- Until now the reason was only traced in the alert, not on the entity.
ALTER TABLE irrigation_schedules ADD COLUMN IF NOT EXISTS postpone_reason VARCHAR(120);
