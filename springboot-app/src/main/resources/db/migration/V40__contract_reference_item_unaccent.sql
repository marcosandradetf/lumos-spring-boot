ALTER TABLE contract_reference_item
    ADD COLUMN IF NOT EXISTS description_unaccent text;

CREATE OR REPLACE FUNCTION contract_reference_item_unaccent_trigger()
    RETURNS trigger AS $$
BEGIN
    NEW.description_unaccent := unaccent(lower(NEW.description));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_contract_reference_item_unaccent ON contract_reference_item;

CREATE TRIGGER trg_contract_reference_item_unaccent
    BEFORE INSERT OR UPDATE ON contract_reference_item
    FOR EACH ROW EXECUTE FUNCTION contract_reference_item_unaccent_trigger();

CREATE INDEX IF NOT EXISTS idx_contract_reference_item_tenant_type
    ON contract_reference_item(tenant_id, type);

CREATE INDEX IF NOT EXISTS idx_material_is_generic_tenant
    ON material(tenant_id, is_generic);

UPDATE contract_reference_item
SET description_unaccent = unaccent(lower(description))
WHERE description_unaccent IS NULL OR description_unaccent = '';
