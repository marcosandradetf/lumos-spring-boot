DROP TABLE IF EXISTS tb_material_reservation;
DROP TABLE IF EXISTS tb_direct_executions_items;
DROP TABLE IF EXISTS tb_direct_executions;


CREATE TABLE IF NOT EXISTS tb_direct_executions
(
    direct_execution_id       BIGSERIAL PRIMARY KEY,
    instructions              TEXT,
    contract_id               BIGINT      NOT NULL,
    direct_execution_status   VARCHAR(50) NOT NULL,
    team_id                   BIGINT      NOT NULL,
    assigned_user_id          UUID        NOT NULL,
    reservation_management_id BIGINT      NOT NULL
);

ALTER TABLE tb_direct_executions
    ADD CONSTRAINT FK_TB_DIRECT_EXECUTIONS_ON_CONTRACT FOREIGN KEY (contract_id) REFERENCES tb_contracts (contract_id);

ALTER TABLE tb_direct_executions
    ADD CONSTRAINT FK_TB_DIRECT_EXECUTIONS_ON_RESERVATION_MANAGEMENT FOREIGN KEY (reservation_management_id) REFERENCES tb_reservation_managements (reservation_management_id);

ALTER TABLE tb_direct_executions
    ADD CONSTRAINT FK_TB_DIRECT_EXECUTIONS_ON_TEAM FOREIGN KEY (team_id) REFERENCES tb_teams (id_team);

ALTER TABLE tb_direct_executions
    ADD CONSTRAINT FK_TB_DIRECT_EXECUTIONS_ON_USER FOREIGN KEY (assigned_user_id) REFERENCES tb_users (id_user);

CREATE TABLE tb_direct_executions_items
(
    direct_execution_item_id BIGSERIAL PRIMARY KEY,
    measured_item_quantity   DOUBLE PRECISION NOT NULL,
    contract_item_id         BIGINT           NOT NULL,
    direct_execution_id      BIGINT           NOT NULL
);

ALTER TABLE tb_direct_executions_items
    ADD CONSTRAINT FK_TB_DIRECT_EXECUTIONS_ITEMS_ON_CONTRACT_ITEM FOREIGN KEY (contract_item_id) REFERENCES tb_contracts_items (contract_item_id);

ALTER TABLE tb_direct_executions_items
    ADD CONSTRAINT FK_TB_DIRECT_EXECUTIONS_ITEMS_ON_DIRECT_EXECUTION FOREIGN KEY (direct_execution_id) REFERENCES tb_direct_executions (direct_execution_id);


CREATE TABLE IF NOT EXISTS tb_materials_reservations
(
    material_id_reservation   BIGSERIAL PRIMARY KEY,
    description               VARCHAR(100),
    material_stock_id         BIGINT,
    pre_measurement_street_id BIGINT,
    direct_execution_id       BIGINT,
    contract_item_id          BIGINT           NOT NULL,
    reserved_quantity         DOUBLE PRECISION NOT NULL,
    quantity_completed        DOUBLE PRECISION NOT NULL,
    status                    VARCHAR(50),
    team_id                   BIGINT           NOT NULL
);

ALTER TABLE tb_materials_reservations
    ADD CONSTRAINT FK_TB_MATERIALS_RESERVATIONS_ON_DIRECT_EXECUTION FOREIGN KEY (direct_execution_id) REFERENCES tb_direct_executions (direct_execution_id);

ALTER TABLE tb_materials_reservations
    ADD CONSTRAINT FK_TB_MATERIALS_RESERVATIONS_ON_MATERIAL_STOCK FOREIGN KEY (material_stock_id) REFERENCES tb_material_stock (material_id_stock);

ALTER TABLE tb_materials_reservations
    ADD CONSTRAINT FK_TB_MATERIALS_RESERVATIONS_ON_PRE_MEASUREMENT_STREET FOREIGN KEY (pre_measurement_street_id) REFERENCES tb_pre_measurements_streets (pre_measurement_street_id);

ALTER TABLE tb_materials_reservations
    ADD CONSTRAINT FK_TB_MATERIALS_RESERVATIONS_ON_TEAM FOREIGN KEY (team_id) REFERENCES tb_teams (id_team);
