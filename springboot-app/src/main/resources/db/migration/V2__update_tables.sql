-- 1. Add new columns to tb_pre_measurements_streets && b_pre_measurements
ALTER TABLE tb_pre_measurements_streets
    ADD COLUMN created_by_user_id uuid NULL,
    ADD COLUMN assigned_by_user_id uuid NULL,
    ADD COLUMN finished_by_user_id uuid NULL,
    ADD COLUMN created_at timestamptz(6) NULL,
    ADD COLUMN assigned_at timestamptz(6) NULL,
    ADD COLUMN finished_at timestamptz(6) NULL,
    ADD COLUMN step int4 NULL;


ALTER TABLE tb_pre_measurements
    ADD COLUMN steps int4 NULL;

-- 2. Copy data from tb_pre_measurements to tb_pre_measurements_streets
UPDATE tb_pre_measurements_streets s
SET created_by_user_id = p.created_by_user_id,
    assigned_by_user_id = p.assigned_by_user_id,
    finished_by_user_id = p.finished_by_user_id,
    created_at = p.created_at,
    assigned_at = p.assigned_at,
    finished_at = p.finished_at,
    step = 1
    FROM tb_pre_measurements p
WHERE s.pre_measurement_id = p.pre_measurement_id;

UPDATE tb_pre_measurements SET
    steps = 1;

-- 3. Drop old columns from tb_pre_measurements
ALTER TABLE tb_pre_measurements
DROP COLUMN created_by_user_id,
    DROP COLUMN assigned_by_user_id,
    DROP COLUMN finished_by_user_id,
    DROP COLUMN created_at,
    DROP COLUMN assigned_at,
    DROP COLUMN finished_at;

-- 4. Add foreign key constraints on tb_pre_measurements_streets
ALTER TABLE tb_pre_measurements_streets
    ADD CONSTRAINT fk_street_created_by
        FOREIGN KEY (created_by_user_id)
            REFERENCES tb_users(id_user)
            ON DELETE RESTRICT
            ON UPDATE RESTRICT;

ALTER TABLE tb_pre_measurements_streets
    ADD CONSTRAINT fk_street_assigned_by
        FOREIGN KEY (assigned_by_user_id)
            REFERENCES tb_users(id_user)
            ON DELETE RESTRICT
            ON UPDATE RESTRICT;

ALTER TABLE tb_pre_measurements_streets
    ADD CONSTRAINT fk_street_finished_by
        FOREIGN KEY (finished_by_user_id)
            REFERENCES tb_users(id_user)
            ON DELETE RESTRICT
            ON UPDATE RESTRICT;