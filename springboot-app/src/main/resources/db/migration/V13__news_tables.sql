CREATE TABLE IF NOT EXISTS order_material
(
    order_id   uuid primary key,
    order_code text      not null,
    created_at timestamp not null,
    deposit_id bigint    not null,
    status     text      not null,
    team_id    bigint    not null
);

CREATE TABLE order_material_item
(
    order_id    UUID   NOT NULL,
    material_id BIGINT NOT NULL,
    PRIMARY KEY (order_id, material_id),
    FOREIGN KEY (order_id) REFERENCES order_material (order_id)
);

alter table order_material
    add constraint fk_deposit_order_material foreign key (deposit_id) references deposit (id_deposit),
    add constraint fk_team_order_material foreign key (team_id) references team (id_team);

