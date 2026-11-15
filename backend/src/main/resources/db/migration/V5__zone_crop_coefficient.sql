-- Coefficient cultural (Kc) par zone, utilisé pour estimer le besoin hydrique théorique :
-- besoin (L) = surface (m2) x ET0 (mm) x Kc / efficacité du système d'irrigation.
-- Null = valeur par défaut 1.0 appliquée par le backend.
ALTER TABLE zones ADD COLUMN IF NOT EXISTS crop_coefficient DOUBLE PRECISION;
