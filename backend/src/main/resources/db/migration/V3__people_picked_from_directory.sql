-- V3: people are picked in the people directory (resource ownership, 2026-11-19).
--
-- The visitor management Staff screen owns the people directory (table person). The other
-- modules store the id of a person picked in GET /api/staff/persons, never a typed name:
-- plants (inspection and maturity observer, treatment and fertilizer applicator) and sales
-- (author of a demand report) replace their free-text column by a person id. The visitor role
-- of a directory entry becomes optional (farm workers without a visitor role). The database is
-- throwaway (docs/adr/0002): the typed names are dropped, not converted.

ALTER TABLE staff ALTER COLUMN role DROP NOT NULL;

ALTER TABLE health_inspection DROP COLUMN observer;
ALTER TABLE health_inspection ADD COLUMN observer_person_id bigint;
ALTER TABLE health_inspection
    ADD CONSTRAINT fk_health_inspection_observer_person FOREIGN KEY (observer_person_id) REFERENCES person(id);

ALTER TABLE maturity_observation DROP COLUMN observer;
ALTER TABLE maturity_observation ADD COLUMN observer_person_id bigint;
ALTER TABLE maturity_observation
    ADD CONSTRAINT fk_maturity_observation_observer_person FOREIGN KEY (observer_person_id) REFERENCES person(id);

ALTER TABLE health_treatment DROP COLUMN applicator;
ALTER TABLE health_treatment ADD COLUMN applicator_person_id bigint NOT NULL;
ALTER TABLE health_treatment
    ADD CONSTRAINT fk_health_treatment_applicator_person FOREIGN KEY (applicator_person_id) REFERENCES person(id);

ALTER TABLE fertilizer_movement DROP COLUMN applicator;
ALTER TABLE fertilizer_movement ADD COLUMN applicator_person_id bigint;
ALTER TABLE fertilizer_movement
    ADD CONSTRAINT fk_fertilizer_movement_applicator_person FOREIGN KEY (applicator_person_id) REFERENCES person(id);

ALTER TABLE demand_reports DROP COLUMN generated_by;
ALTER TABLE demand_reports ADD COLUMN generated_by_person_id bigint;
ALTER TABLE demand_reports
    ADD CONSTRAINT fk_demand_reports_generated_by_person FOREIGN KEY (generated_by_person_id) REFERENCES person(id);
