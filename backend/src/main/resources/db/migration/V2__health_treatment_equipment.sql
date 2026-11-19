-- V2: the machine of a plant protection treatment is picked in the machinery register.
--
-- Machinery owns the equipment (resource ownership, 2026-11-19): plants stores the id of the
-- machine (health_treatment.equipment_id, foreign key to equipment.id) and keeps its name in
-- health_treatment.equipment, as machinery had it when the treatment was recorded. The
-- treatments recorded before keep their typed name, without id. Deleting the machine in
-- machinery empties the id and leaves the name (PlantsEquipmentForeignKeys).

ALTER TABLE health_treatment ADD COLUMN equipment_id bigint;

CREATE INDEX idx_health_treatment_equipment_id ON health_treatment (equipment_id);

ALTER TABLE health_treatment
    ADD CONSTRAINT fk_health_treatment_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(id) ON DELETE SET NULL;
