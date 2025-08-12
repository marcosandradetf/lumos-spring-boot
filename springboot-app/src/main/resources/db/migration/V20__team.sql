alter table team
    drop column if exists electrician_id,
    drop column if exists driver_id;

DROP TABLE IF EXISTS team_complementary_member;
DROP TABLE IF EXISTS team_complementary_members;

DO
$$
    BEGIN
        IF EXISTS (SELECT 1
                   FROM information_schema.tables
                   WHERE table_schema = 'public'
                     AND table_name = 'maintenance_executors') THEN
            EXECUTE 'ALTER TABLE maintenance_executors RENAME TO maintenance_executor';
        END IF;
    END;
$$;


CREATE TABLE IF NOT EXISTS maintenance_executor
(
    maintenance_id UUID NOT NULL,
    user_id        UUID NOT NULL,
    role           VARCHAR(50),

    CONSTRAINT maintenance_executor_pkey PRIMARY KEY (maintenance_id, user_id),

    CONSTRAINT maintenance_executors_maintenance_id_fkey
        FOREIGN KEY (maintenance_id)
            REFERENCES maintenance (maintenance_id)
            ON DELETE CASCADE,

    CONSTRAINT maintenance_executors_user_id_fkey
        FOREIGN KEY (user_id)
            REFERENCES app_user (user_id)
            ON DELETE CASCADE
);


CREATE TABLE IF NOT EXISTS installation_executor
(
    maintenance_id UUID NOT NULL,
    user_id        UUID NOT NULL,
    role           VARCHAR(50),

    CONSTRAINT maintenance_executor_pkey PRIMARY KEY (maintenance_id, user_id),

    CONSTRAINT maintenance_executors_maintenance_id_fkey
        FOREIGN KEY (maintenance_id)
            REFERENCES maintenance (maintenance_id)
            ON DELETE CASCADE,

    CONSTRAINT maintenance_executors_user_id_fkey
        FOREIGN KEY (user_id)
            REFERENCES app_user (user_id)
            ON DELETE CASCADE
);


CREATE TABLE IF NOT EXISTS installation_executor
(
    direct_execution_id BIGINT,
    user_id             UUID NOT NULL,
    role                VARCHAR(50),

    CONSTRAINT installation_executor_pkey PRIMARY KEY (direct_execution_id, user_id),

    CONSTRAINT installation_executor_maintenance_id_fkey
        FOREIGN KEY (direct_execution_id)
            REFERENCES direct_execution (direct_execution_id)
            ON DELETE CASCADE,

    CONSTRAINT installation_executor_user_id_fkey
        FOREIGN KEY (user_id)
            REFERENCES app_user (user_id)
            ON DELETE CASCADE
);

ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS team_id bigint;

DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1
                       FROM information_schema.table_constraints
                       WHERE table_name = 'app_user'
                         AND constraint_type = 'FOREIGN KEY'
                         AND constraint_name = 'app_user_team_id_fkey') THEN
            ALTER TABLE app_user
                ADD CONSTRAINT app_user_team_id_fkey
                    FOREIGN KEY (team_id)
                        REFERENCES team (id_team)
                        ON DELETE SET NULL;
        END IF;
    END
$$;


ALTER TABLE deposit
    ADD COLUMN IF NOT EXISTS is_truck boolean;
