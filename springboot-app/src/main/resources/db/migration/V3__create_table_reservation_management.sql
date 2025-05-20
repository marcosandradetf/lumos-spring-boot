-- 1. new table tb_reservation_managements
CREATE TABLE tb_reservation_managements
(
    reservation_management_id BIGSERIAL PRIMARY KEY,
    description               VARCHAR(100),
    stockist_id               UUID   NOT NULL,
    CONSTRAINT FK_TB_RESERVATION_MANAGEMENTS_ON_STOCKIST FOREIGN KEY (stockist_id) REFERENCES tb_users (id_user)
);

ALTER TABLE tb_pre_measurements_streets
    ADD COLUMN reservation_management_id BIGINT NULL
        CONSTRAINT fk_reservation_management
            REFERENCES tb_reservation_managements(reservation_management_id);