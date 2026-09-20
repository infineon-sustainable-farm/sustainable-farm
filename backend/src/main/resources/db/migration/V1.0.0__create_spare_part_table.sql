-- Create spare_part table
CREATE TABLE IF NOT EXISTS spare_part (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    quantity INTEGER NOT NULL DEFAULT 0,
    reorder_threshold INTEGER NOT NULL DEFAULT 0,
    unit_cost DECIMAL(10, 2) NOT NULL,
    equipment_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add foreign key constraint for equipment_id
ALTER TABLE spare_part
ADD CONSTRAINT fk_spare_part_equipment
FOREIGN KEY (equipment_id) REFERENCES equipment(id) ON DELETE SET NULL;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_spare_part_equipment ON spare_part(equipment_id);
CREATE INDEX IF NOT EXISTS idx_spare_part_name ON spare_part(name);

-- Add comment to table
COMMENT ON TABLE spare_part IS 'Stores spare parts inventory with an optional compatible equipment association';

-- Add comment to columns
COMMENT ON COLUMN spare_part.id IS 'Unique identifier for the spare part';
COMMENT ON COLUMN spare_part.name IS 'Name of the spare part';
COMMENT ON COLUMN spare_part.quantity IS 'Current available quantity in stock';
COMMENT ON COLUMN spare_part.reorder_threshold IS 'Threshold to trigger low stock alert';
COMMENT ON COLUMN spare_part.unit_cost IS 'Cost per unit of the spare part';
COMMENT ON COLUMN spare_part.equipment_id IS 'Optional compatible equipment this spare part is associated with';
COMMENT ON COLUMN spare_part.created_at IS 'Timestamp when the record was created';
COMMENT ON COLUMN spare_part.updated_at IS 'Timestamp when the record was last updated';
