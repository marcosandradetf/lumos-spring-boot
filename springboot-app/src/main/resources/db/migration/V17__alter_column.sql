ALTER TABLE maintenance
    ALTER COLUMN date_of_visit
        TYPE timestamptz
        USING date_of_visit AT TIME ZONE 'UTC';

UPDATE stock_movement
    SET quantity_package = CAST(quantity_package AS numeric(15, 2)),
        input_quantity = CAST(input_quantity AS numeric(15, 2)),
        total_quantity= CAST(total_quantity AS numeric(15, 2));

UPDATE contract_item
    SET quantity_executed = CAST(quantity_executed AS numeric(15, 2)),
        contracted_quantity = CAST(contracted_quantity AS numeric(15, 2));

UPDATE material_stock
    SET stock_quantity = CAST(stock_quantity AS numeric(15, 2)),
        stock_available = CAST(stock_available AS numeric(15, 2));

UPDATE material_reservation
    SET quantity_completed = CAST(quantity_completed AS numeric(15, 2)),
        reserved_quantity = CAST(reserved_quantity AS numeric(15, 2));

ALTER TABLE stock_movement
    ALTER COLUMN quantity_package TYPE numeric(15, 2),
    ALTER COLUMN input_quantity TYPE numeric(15, 2),
    ALTER COLUMN total_quantity TYPE numeric(15, 2);

ALTER TABLE contract_item
    ALTER COLUMN quantity_executed TYPE numeric(15, 2),
    ALTER COLUMN contracted_quantity TYPE numeric(15, 2);

ALTER TABLE material_stock
    ALTER COLUMN stock_quantity TYPE numeric(15, 2),
    ALTER COLUMN stock_available TYPE numeric(15, 2);

ALTER TABLE material_reservation
    ALTER COLUMN quantity_completed TYPE numeric(15, 2),
    ALTER COLUMN reserved_quantity TYPE numeric(15, 2);