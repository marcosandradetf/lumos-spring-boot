-- 1. new table tb_reservation_managements
ALTER TABLE tb_pre_measurements_streets
    ADD COLUMN IF NOT EXISTS prioritized BOOLEAN      NULL,
    ADD COLUMN IF NOT EXISTS comment     VARCHAR(150) NULL;

ALTER TABLE tb_reservation_managements
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NULL;

ALTER TABLE tb_material_reservation
    DROP COLUMN IF EXISTS material_truck_stock_id,
    DROP COLUMN IF EXISTS material_first_deposit_stock_id,
    DROP COLUMN IF EXISTS material_second_deposit_stock_id,
    ADD COLUMN IF NOT EXISTS material_stock_id BIGINT NULL
        CONSTRAINT fk_material_stock
            REFERENCES tb_material_stock (material_id_stock);

