-- Step 1: Add the column if it doesn't exist (nullable for now)
ALTER TABLE maintenance
    ADD COLUMN IF NOT EXISTS team_id BIGINT;

-- Step 2: Add the foreign key constraint if it doesn't exist
DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1
                       FROM information_schema.table_constraints
                       WHERE constraint_name = 'fk_team_maintenance'
                         AND table_name = 'maintenance') THEN
            ALTER TABLE maintenance
                ADD CONSTRAINT fk_team_maintenance
                    FOREIGN KEY (team_id)
                        REFERENCES team (id_team);
        END IF;
    END
$$;
