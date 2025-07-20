-- Usado para joins com maintenance_street_item
CREATE INDEX IF NOT EXISTS idx_msi_maintenance_street_id ON maintenance_street_item (maintenance_street_id);

-- Usado para joins com material_stock
CREATE INDEX IF NOT EXISTS idx_material_stock_id_stock ON material_stock (material_id_stock);
CREATE INDEX IF NOT EXISTS idx_material_stock_material_id ON material_stock (material_id);

-- Usado para joins com material
CREATE INDEX IF NOT EXISTS idx_material_id ON material (id_material);

-- Usado para WHERE maintenance_street.maintenance_id = ...
CREATE INDEX IF NOT EXISTS idx_maintenance_street_maintenance_id ON maintenance_street (maintenance_id);

-- Para a cláusula WHERE IN (...) de maintenance_street_id
CREATE INDEX IF NOT EXISTS idx_maintenance_street_id ON maintenance_street (maintenance_street_id);

-- Usado para a cláusula WHERE m.maintenance_id = ...
CREATE INDEX IF NOT EXISTS idx_maintenance_id ON maintenance (maintenance_id);

-- Create index on unaccented, lowercased column
ALTER TABLE material ADD COLUMN IF NOT EXISTS material_name_unaccent text;

CREATE OR REPLACE FUNCTION material_unaccent_trigger()
    RETURNS trigger AS $$
BEGIN
    NEW.material_name_unaccent := unaccent(lower(NEW.material_name));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_material_unaccent
    BEFORE INSERT OR UPDATE ON material
    FOR EACH ROW EXECUTE FUNCTION material_unaccent_trigger();

CREATE INDEX if not exists idx_material_name_unaccent ON material(material_name_unaccent);

UPDATE material
SET material_name_unaccent = unaccent(lower(material_name))
WHERE material_name_unaccent IS NULL OR material_name_unaccent = '';

alter table maintenance
    alter column sign_date type timestamptz;
