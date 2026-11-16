-- V13: answers of the Plants and Visitor Management teams to the integration contracts
-- (docs/integrations/). V11 and V12 are released: their changes go here.

-- Plants: a zone may cover several blocks, but a block belongs to only one zone of its farm.
-- Links created before this rule are reduced to the oldest one per (farm, block), otherwise the
-- unique index below could not be created and the application would not start.
DELETE FROM zone_block_mapping duplicate
    USING zone_block_mapping kept
WHERE duplicate.farm_id = kept.farm_id
  AND duplicate.block_code = kept.block_code
  AND (duplicate.created_at > kept.created_at
       OR (duplicate.created_at = kept.created_at AND duplicate.id > kept.id));

-- The plain index of V11 on (farm_id, block_code) becomes the unique one.
DROP INDEX IF EXISTS idx_zone_block_mapping_farm_block;
CREATE UNIQUE INDEX IF NOT EXISTS uk_zone_block_mapping_farm_block ON zone_block_mapping(farm_id, block_code);

-- Visitor Management: its tour stops only have a name and a position. Water Supply maps the stops
-- that pass by water facilities to a zone and/or a source; the other stops are ignored.
CREATE TABLE IF NOT EXISTS water_visitor_stops (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    stop_name VARCHAR(255) NOT NULL,
    zone_id UUID,
    source_id UUID,
    CONSTRAINT uk_water_visitor_stops_name UNIQUE (stop_name),
    CONSTRAINT chk_water_visitor_stops_target CHECK (zone_id IS NOT NULL OR source_id IS NOT NULL),
    CONSTRAINT fk_water_visitor_stops_zone FOREIGN KEY (zone_id) REFERENCES zones(id) ON DELETE CASCADE,
    CONSTRAINT fk_water_visitor_stops_source FOREIGN KEY (source_id) REFERENCES water_sources(id) ON DELETE CASCADE
);

-- Visit announcements now carry the stop names sent by Visitor Management.
ALTER TABLE water_visitor_events
    ADD COLUMN IF NOT EXISTS stops TEXT;
