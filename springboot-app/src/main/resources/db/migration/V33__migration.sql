ALTER TABLE pre_measurement_street_item
ADD COLUMN IF NOT EXISTS material_stock_id BIGINT
references material_stock;

CREATE OR REPLACE VIEW installation_street_item_view AS
SELECT desi.direct_execution_street_item_id AS installation_street_item_id,
       desi.direct_execution_street_id      AS installation_street_id,
       'DIRECT_EXECUTION'                   AS installation_type,
       desi.contract_item_id,
       desi.executed_quantity               AS executed_quantity,
       desi.material_stock_id
FROM direct_execution_street_item desi

UNION ALL

SELECT pmsi.pre_measurement_street_item_id AS installation_street_item_id,
       pmsi.pre_measurement_street_id      AS installation_street_id,
       'PRE_MEASUREMENT'                   AS installation_type,
       pmsi.contract_item_id,
       pmsi.quantity_executed              AS executed_quantity,
       pmsi.material_stock_id
FROM pre_measurement_street_item pmsi;