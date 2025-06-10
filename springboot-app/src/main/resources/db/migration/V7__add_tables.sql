ALTER TABLE tb_contracts_items
    ADD COLUMN IF NOT EXISTS quantity_executed float8 NOT NULL DEFAULT(0.0);

ALTER TABLE tb_pre_measurements_streets
    ADD COLUMN IF NOT EXISTS execution_photo_uri varchar(255) NULL,
    ADD COLUMN IF NOT EXISTS pre_measurement_photo_uri varchar(255) NULL,
    ADD COLUMN IF NOT EXISTS device_street_id bigint NULL,
    ADD COLUMN IF NOT EXISTS device_id VARCHAR(36) null;

ALTER TABLE tb_pre_measurements_streets
    ALTER COLUMN latitude DROP NOT NULL,
    ALTER COLUMN longitude DROP NOT NULL;

UPDATE tb_pre_measurements_streets set neighborhood = null, number = null, latitude = null, longitude = null, last_power = null, comment = null;

UPDATE tb_Contracts SET status = 'ACTIVE';

INSERT INTO tb_roles (role_name)
SELECT 'RESPONSAVEL_TECNICO'
WHERE NOT EXISTS (
    SELECT 1 FROM tb_roles WHERE role_name = 'RESPONSAVEL_TECNICO'
);

drop table if exists tb_stockists;

CREATE TABLE if not exists tb_stockists
(
    stockist_id BIGSERIAL PRIMARY KEY,
    deposit_id_deposit         BIGINT not null,
    user_id_user               UUID   NOT NULL,
    CONSTRAINT FK_TB_DEPOSITS_STOCKISTS FOREIGN KEY (deposit_id_deposit) REFERENCES tb_deposits (id_deposit),
    CONSTRAINT FK_TB_USERS_STOCKISTS FOREIGN KEY (user_id_user) REFERENCES tb_users (id_user)
);

insert into tb_stockists
(deposit_id_deposit, user_id_user)
values
    (1, 'bc2ef2a1-74b7-41d9-a9c0-7b77def9932f'), -- GALPÃO ITAPECERICA - eduardo
    (2, '4abb2d6a-2b1f-4d8d-91cb-015aa0addb5f'), --GALPÃO BH - elton
    (4, 'bc2ef2a1-74b7-41d9-a9c0-7b77def9932f'), --GALPÃO ITUMIRIM - eduardo
    (5, 'bc2ef2a1-74b7-41d9-a9c0-7b77def9932f'); --GALPÃO LADAINHA - eduardo