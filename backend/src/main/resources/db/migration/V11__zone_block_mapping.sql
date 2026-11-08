-- V11: link between Water Supply zones (zone_id, UUID) and Plants blocks (block_code).
--
-- The migration number lives in a folder shared between modules: V11 and V12 are reserved by
-- watersupply (announced in docs/integrations/README.md).
--
-- The cardinality is not settled with Plants: a zone may carry several block codes, only the
-- (zone, block) pair is unique. Block codes are stored upper case (normalized by the API).
-- A mapping has no meaning without its zone: it is removed with it.
CREATE TABLE IF NOT EXISTS zone_block_mapping (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    zone_id UUID NOT NULL,
    block_code VARCHAR(50) NOT NULL,
    farm_id UUID NOT NULL,
    CONSTRAINT uk_zone_block_mapping_zone_block UNIQUE (zone_id, block_code),
    CONSTRAINT fk_zone_block_mapping_zone FOREIGN KEY (zone_id) REFERENCES zones(id) ON DELETE CASCADE,
    CONSTRAINT fk_zone_block_mapping_farm FOREIGN KEY (farm_id) REFERENCES farms(id)
);

CREATE INDEX IF NOT EXISTS idx_zone_block_mapping_zone_id ON zone_block_mapping(zone_id);
CREATE INDEX IF NOT EXISTS idx_zone_block_mapping_farm_block ON zone_block_mapping(farm_id, block_code);
