-- 1. new table tb_reservation_managements
ALTER TABLE tb_pre_measurements_streets
    ADD COLUMN IF NOT EXISTS prioritized BOOLEAN NULL,
    ADD COLUMN IF NOT EXISTS comment VARCHAR(150) NULL;

ALTER TABLE tb_reservation_managements
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NULL;

