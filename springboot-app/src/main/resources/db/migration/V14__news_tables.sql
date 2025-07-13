drop table if exists maintenance;

CREATE TABLE IF NOT EXISTS maintenance
(
    maintenance_id          uuid primary key,
    contract_id             bigint,
    created_at              timestamp not null,
    pending_points          boolean   not null,
    quantity_pending_points int,
    date_of_visit           timestamp not null,
    type                    text      not null, -- rural ou urbana
    status                  text      not null,
    FOREIGN KEY (contract_id) references contract (contract_id)
);

CREATE TABLE maintenance_street
(
    maintenance_street_id uuid primary key,
    maintenance_id        uuid not null,
    address               text not null,

    latitude              double precision,
    longitude             double precision,

    comment               text,
    lastPower             text,
    lastSupply            text,
    currentSupply         text,
    reason                text,

    FOREIGN KEY (maintenance_id) REFERENCES maintenance (maintenance_id)
);

CREATE TABLE maintenance_street_item
(
    maintenance_id        uuid             not null,
    maintenance_street_id uuid             not null,
    material_stock_id     bigint           not null,
    quantity_executed     double precision not null,

    PRIMARY KEY (maintenance_id, maintenance_street_id, material_stock_id),

    FOREIGN KEY (maintenance_id) REFERENCES maintenance (maintenance_id),
    FOREIGN KEY (maintenance_street_id) REFERENCES maintenance_street (maintenance_street_id),
    FOREIGN KEY (material_stock_id) REFERENCES material_stock (material_id_stock)
);
