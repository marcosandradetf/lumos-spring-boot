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