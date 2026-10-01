-- V10 : pilotage automatique de l'irrigation (F1), detection du colmatage par le debit (F3),
-- suivi du niveau des sources de pluie (F2) et niveau temps reel (F5).
--
-- Le numero de migration vit dans un dossier partage entre les modules : a annoncer a
-- l'equipe avant la pull request, pour eviter deux V10 concurrents.

-- F1 : l'irrigation peut desormais etre creee par la regle d'humidite du sol, pas seulement
-- a la main. La colonne distingue les deux origines (valeurs : manual, auto).
ALTER TABLE irrigation_schedules
    ADD COLUMN IF NOT EXISTS trigger_source VARCHAR(20) NOT NULL DEFAULT 'manual';

-- F3 : le debit theorique d'une zone se calcule a partir de son reseau goutte-a-goutte :
-- debit_theorique (L/h) = nombre de goutteurs x debit nominal d'un goutteur (L/h).
ALTER TABLE zones
    ADD COLUMN IF NOT EXISTS emitter_count INTEGER,
    ADD COLUMN IF NOT EXISTS emitter_nominal_flow_lh DOUBLE PRECISION;

-- F3 : rattacher une mesure de debit a une zone permet de comparer le volume reellement
-- mesure (compteur) au volume theorique du reseau sur la meme duree d'arrosage.
ALTER TABLE water_consumption
    ADD COLUMN IF NOT EXISTS zone_id UUID;

ALTER TABLE water_consumption
    ADD CONSTRAINT fk_water_consumption_zone
    FOREIGN KEY (zone_id) REFERENCES zones(id);

CREATE INDEX IF NOT EXISTS idx_water_consumption_zone_date
    ON water_consumption(zone_id, consumption_date);
