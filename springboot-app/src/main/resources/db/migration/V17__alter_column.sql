ALTER TABLE maintenance
    ALTER COLUMN date_of_visit
        TYPE timestamptz
        USING date_of_visit AT TIME ZONE 'UTC';

UPDATE stock_movement
    SET quantity_package = CAST(quantity_package AS NUMERIC),
        input_quantity = CAST(input_quantity AS NUMERIC),
        total_quantity= CAST(total_quantity AS NUMERIC);

UPDATE contract_item
    SET quantity_executed = CAST(quantity_executed AS NUMERIC),
        contracted_quantity = CAST(contracted_quantity AS NUMERIC);

UPDATE material_stock
    SET stock_quantity = CAST(stock_quantity AS NUMERIC),
        stock_available = CAST(stock_available AS NUMERIC);

UPDATE material_reservation
    SET quantity_completed = CAST(quantity_completed AS NUMERIC),
        reserved_quantity = CAST(reserved_quantity AS NUMERIC);

UPDATE direct_execution_item
SET measured_item_quantity = CAST(measured_item_quantity AS NUMERIC);

UPDATE direct_execution_street_item
SET executed_quantity = CAST(executed_quantity AS NUMERIC);

ALTER TABLE stock_movement
    ALTER COLUMN quantity_package TYPE NUMERIC,
    ALTER COLUMN input_quantity TYPE NUMERIC,
    ALTER COLUMN total_quantity TYPE NUMERIC;

ALTER TABLE contract_item
    ALTER COLUMN quantity_executed TYPE NUMERIC,
    ALTER COLUMN contracted_quantity TYPE NUMERIC;

ALTER TABLE material_stock
    ALTER COLUMN stock_quantity TYPE NUMERIC,
    ALTER COLUMN stock_available TYPE NUMERIC;

ALTER TABLE material_reservation
    ALTER COLUMN quantity_completed TYPE NUMERIC,
    ALTER COLUMN reserved_quantity TYPE NUMERIC;

ALTER TABLE direct_execution_item
    ALTER COLUMN measured_item_quantity TYPE NUMERIC;

ALTER TABLE direct_execution_street_item
    ALTER COLUMN executed_quantity TYPE NUMERIC;

UPDATE direct_execution_street
SET finished_at = finished_at AT TIME ZONE 'America/Sao_Paulo' AT TIME ZONE 'UTC';

ALTER TABLE direct_execution_street
    ADD COLUMN IF NOT EXISTS current_supply text;