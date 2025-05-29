ALTER TABLE tb_material_reservation
    DROP COLUMN IF EXISTS pre_measurement_id,
    ADD COLUMN IF NOT EXISTS contract_item_id BIGINT NOT NULL
        CONSTRAINT fk_contract_items
            REFERENCES tb_contracts_items (contract_item_id);

ALTER TABLE tb_teams
    ADD COLUMN IF NOT EXISTS team_phone VARCHAR(15) NULL;

ALTER TABLE tb_users
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(15) NULL;