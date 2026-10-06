-- Persistance du motif de report sur les plannings d'irrigation.
-- Jusqu'à présent le motif était uniquement tracé dans l'alerte, pas sur l'entité.
ALTER TABLE irrigation_schedules ADD COLUMN IF NOT EXISTS postpone_reason VARCHAR(120);
