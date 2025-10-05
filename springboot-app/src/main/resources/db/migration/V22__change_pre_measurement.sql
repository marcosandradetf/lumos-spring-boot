alter table pre_measurement
    add column if not exists device_pre_measurement_id uuid;

alter table pre_measurement_street
    drop column if exists deviceId;

alter table pre_measurement_street
    drop column if exists deviceStreetId;

alter table pre_measurement_street
    drop column if exists number;

alter table pre_measurement_street
    drop column if exists neighborhood;

alter table pre_measurement_street
    drop column if exists state;

alter table pre_measurement_street
    drop column if exists city;

alter table pre_measurement_street
    add column if not exists device_pre_measurement_street_id uuid;

DO
$$
    BEGIN
        -- Verifica se a coluna 'steps' existe na tabela 'pre_measurement'
        IF EXISTS (SELECT 1
                   FROM information_schema.columns
                   WHERE table_name = 'pre_measurement'
                     AND column_name = 'street') THEN
            -- Se existir, renomeia a coluna 'steps' para 'step'
            ALTER TABLE pre_measurement
                RENAME COLUMN street TO address;
        END IF;

        IF EXISTS (SELECT 1
                   FROM information_schema.columns
                   WHERE table_name = 'pre_measurement'
                     AND column_name = 'steps') THEN
            -- Se existir, renomeia a coluna 'steps' para 'step'
            ALTER TABLE pre_measurement
                RENAME COLUMN steps TO step;
        END IF;

        if not exists (select 1 from information_schema.columns where column_name = 'reservation_management_id' and table_name = 'pre_measurement') then
            alter table pre_measurement
                add column reservation_management_id bigint,
                ADD CONSTRAINT pre_measurement_reservation_management_id_fkey
                    FOREIGN KEY (reservation_management_id)
                        REFERENCES reservation_management (reservation_management_id);
        end if;

        if not exists (select 1 from information_schema.columns where column_name = 'team_id' and table_name = 'pre_measurement') then
            alter table pre_measurement
                add column team_id bigint,
                ADD CONSTRAINT pre_measurement_team_id_fkey
                    FOREIGN KEY (team_id)
                        REFERENCES team (id_team);
        end if;

        if not exists (select 1 from information_schema.columns where column_name = 'assign_by_user_id' and table_name = 'pre_measurement') then
            alter table pre_measurement
                add column assign_by_user_id uuid,
                ADD CONSTRAINT pre_measurement_assign_by_user_id_fkey
                    FOREIGN KEY (assign_by_user_id)
                        REFERENCES app_user (user_id);
        end if;

        if not exists (select 1 from information_schema.columns where column_name = 'created_by_user_id' and table_name = 'pre_measurement') then
            alter table pre_measurement
                add column created_by_user_id uuid,
                ADD CONSTRAINT pre_measurement_created_by_user_id_fkey
                    FOREIGN KEY (created_by_user_id)
                        REFERENCES app_user (user_id);
        end if;

        if not exists (select 1 from information_schema.columns where column_name = 'pre_measurement_id' and table_name = 'material_reservation') then
            alter table material_reservation
                add column pre_measurement_id bigint,
                ADD CONSTRAINT material_reservation_pre_measurement_id_fkey
                    FOREIGN KEY (pre_measurement_id)
                        REFERENCES pre_measurement (pre_measurement_id);
        end if;
    END
$$;


ALTER TABLE pre_measurement_street_item
    ALTER COLUMN measured_item_quantity TYPE NUMERIC;

alter table pre_measurement
    add column if not exists created_at timestamptz not null default now(),
    add column if not exists comment text;

alter table pre_measurement_street
    drop column if exists comment,
    drop column if exists team_id;

alter table material_reservation
    drop column if exists pre_measurement_street_id;

-- add index for device_pre_measurement_street_id
CREATE INDEX IF NOT EXISTS idx_device_pre_measurement_street_id ON pre_measurement_street (device_pre_measurement_street_id);

-- add unique for device_pre_measurement_street_id
CREATE UNIQUE INDEX IF NOT EXISTS idx_device_pre_measurement_street_id_unique
    ON pre_measurement_street (device_pre_measurement_street_id);

-- add unique for device_pre_measurement_id
CREATE UNIQUE INDEX IF NOT EXISTS idx_device_pre_measurement_id_unique
    ON pre_measurement (device_pre_measurement_id);