-- 1. new table tb_reservation_managements
ALTER TABLE tb_pre_measurements_streets
    ADD COLUMN prioritized BOOLEAN NULL,
    ADD COLUMN comment VARCHAR(150) NULL;