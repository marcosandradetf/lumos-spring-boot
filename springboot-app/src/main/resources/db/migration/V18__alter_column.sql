UPDATE maintenance_street_item
SET quantity_executed = CAST(quantity_executed AS NUMERIC);

ALTER TABLE maintenance_street_item
    ALTER COLUMN quantity_executed TYPE NUMERIC;

CREATE TABLE IF NOT EXISTS material_history
(
    material_history_id   UUID PRIMARY KEY,
    material_stock_id     BIGINT                   NOT NULL,
    maintenance_street_id UUID,
    execution_street_id   BIGINT,
    used_quantity         NUMERIC                  NOT NULL,
    used_date             TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_material_stock FOREIGN KEY (material_stock_id) REFERENCES material_stock(material_id_stock),
    CONSTRAINT fk_maintenance_street FOREIGN KEY (maintenance_street_id) REFERENCES maintenance_street(maintenance_street_id),
    CONSTRAINT fk_execution_street FOREIGN KEY (execution_street_id) REFERENCES direct_execution_street(direct_execution_street_id)
);


ALTER TABLE material
    ADD COLUMN if not exists contract_reference_item_id INTEGER;
DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'fk_contract_item'
        ) THEN
            ALTER TABLE material
                ADD CONSTRAINT fk_contract_item
                    FOREIGN KEY (contract_reference_item_id)
                        REFERENCES contract_reference_item(contract_reference_item_id);
        END IF;
    END;
$$;


WITH to_update AS (
    SELECT linking, contract_reference_item_id, type
    FROM contract_reference_item
)
UPDATE material m
SET contract_reference_item_id = tu.contract_reference_item_id
FROM to_update tu,
     material_type mt
WHERE mt.id_type = m.id_material_type
  AND LOWER(mt.type_name) = LOWER(tu.type)
  AND (
    LOWER(tu.linking) = LOWER(m.material_power)
        OR LOWER(tu.linking) = LOWER(m.material_length)
    );


WITH to_update AS (
    SELECT linking, contract_reference_item_id, type
    FROM contract_reference_item
)
UPDATE material m
SET contract_reference_item_id = tu.contract_reference_item_id
FROM to_update tu,
     material_type mt
WHERE mt.id_type = m.id_material_type
  AND lower(mt.type_name) = lower(tu.type)
  AND tu.linking is null;

update material
set material_power = null,
    material_length = '150mm'
where id_material = 330;

update material
set material_power = null,
    material_length = '200mm'
where id_material = 331;

update material
set material_power = null,
    material_length = '260mm'
where id_material = 332;

update material
set material_power = null,
    material_length = '300mm'
where id_material = 333;