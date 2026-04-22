ALTER TABLE material
ADD COLUMN IF NOT EXISTS relationship_status text;

CREATE INDEX IF NOT EXISTS idx_status on contract_reference_item(status);

CREATE INDEX IF NOT EXISTS idx_relationship_status
    on material(relationship_status);
