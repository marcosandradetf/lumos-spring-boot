CREATE TABLE tb_direct_executions
(
    direct_execution_id       BIGSERIAL PRIMARY KEY,
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
    contract_item_id         BIGINT,
    direct_execution_id      BIGINT
);

ALTER TABLE tb_direct_executions_items
    ADD CONSTRAINT FK_TB_DIRECT_EXECUTIONS_ITEMS_ON_CONTRACT_ITEM FOREIGN KEY (contract_item_id) REFERENCES tb_contracts_items (contract_item_id);

ALTER TABLE tb_direct_executions_items
    ADD CONSTRAINT FK_TB_DIRECT_EXECUTIONS_ITEMS_ON_DIRECT_EXECUTION FOREIGN KEY (direct_execution_id) REFERENCES tb_direct_executions (direct_execution_id);