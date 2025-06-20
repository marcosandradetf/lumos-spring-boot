
CREATE TABLE IF NOT EXISTS direct_execution
(
    direct_execution_id       BIGSERIAL PRIMARY KEY,
    instructions              TEXT,
    contract_id               BIGINT      NOT NULL,
    direct_execution_status   VARCHAR(50) NOT NULL,
    team_id                   BIGINT      NOT NULL,
    assigned_user_id          UUID        NOT NULL,
    reservation_management_id BIGINT      NOT NULL
);

ALTER TABLE direct_execution
    ADD CONSTRAINT FK_DIRECT_EXECUTION_ON_CONTRACT FOREIGN KEY (contract_id) REFERENCES contract (contract_id);

ALTER TABLE direct_execution
    ADD CONSTRAINT FK_DIRECT_EXECUTION_ON_RESERVATION_MANAGEMENT FOREIGN KEY (reservation_management_id) REFERENCES reservation_management (reservation_management_id);

ALTER TABLE direct_execution
    ADD CONSTRAINT FK_DIRECT_EXECUTION_ON_TEAM FOREIGN KEY (team_id) REFERENCES team (id_team);

ALTER TABLE direct_execution
    ADD CONSTRAINT FK_DIRECT_EXECUTION_ON_USER FOREIGN KEY (assigned_user_id) REFERENCES app_user (user_id);

CREATE TABLE direct_execution_item
(
    direct_execution_item_id BIGSERIAL PRIMARY KEY,
    measured_item_quantity   DOUBLE PRECISION NOT NULL,
    contract_item_id         BIGINT           NOT NULL,
    direct_execution_id      BIGINT           NOT NULL
);

ALTER TABLE direct_execution_item
    ADD CONSTRAINT FK_DIRECT_EXECUTION_ITEM_ON_CONTRACT_ITEM FOREIGN KEY (contract_item_id) REFERENCES contract_item (contract_item_id);

ALTER TABLE direct_execution_item
    ADD CONSTRAINT FK_DIRECT_EXECUTION_ITEM_ON_DIRECT_EXECUTION FOREIGN KEY (direct_execution_id) REFERENCES direct_execution (direct_execution_id);


CREATE TABLE IF NOT EXISTS material_reservation
(
    material_id_reservation   BIGSERIAL PRIMARY KEY,
    description               VARCHAR(100),
    central_material_stock_id BIGINT,
    truck_material_stock_id   BIGINT           NOT NULL,
    pre_measurement_street_id BIGINT,
    direct_execution_id       BIGINT,
    contract_item_id          BIGINT           NOT NULL,
    reserved_quantity         DOUBLE PRECISION NOT NULL,
    quantity_completed        DOUBLE PRECISION NOT NULL,
    status                    VARCHAR(50),
    team_id                   BIGINT           NOT NULL
);

ALTER TABLE material_reservation
    ADD CONSTRAINT FK_MATERIAL_RESERVATION_ON_DIRECT_EXECUTION FOREIGN KEY (direct_execution_id) REFERENCES direct_execution (direct_execution_id),
    ADD CONSTRAINT FK_MATERIAL_RESERVATION_ON_MATERIAL_STOCK FOREIGN KEY (central_material_stock_id) REFERENCES material_stock (material_id_stock),
    ADD CONSTRAINT FK_MATERIAL_RESERVATION_ON_MATERIAL_STOCK_TRUCK FOREIGN KEY (truck_material_stock_id) REFERENCES material_stock (material_id_stock),
    ADD CONSTRAINT FK_MATERIAL_RESERVATION_ON_PRE_MEASUREMENT_STREET FOREIGN KEY (pre_measurement_street_id) REFERENCES pre_measurement_street (pre_measurement_street_id),
    ADD CONSTRAINT FK_MATERIAL_RESERVATION_ON_CONTRACT_ITEM FOREIGN KEY (contract_item_id) REFERENCES contract_item (contract_item_id),
    ADD CONSTRAINT FK_MATERIAL_RESERVATION_ON_TEAM FOREIGN KEY (team_id) REFERENCES team (id_team);
