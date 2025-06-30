alter table direct_execution_item
    add column if not exists item_status TEXT not null default 'PENDING';

ALTER TABLE pre_measurement_street_item
    ALTER COLUMN item_status SET NOT NULL,
    ALTER COLUMN item_status SET DEFAULT 'PENDING';

ALTER TABLE direct_execution
    add column if not exists description TEXT NULL;

alter table direct_execution_street
    drop column if exists contract_id;

-- 1. Adiciona a coluna (se ainda não existir)
DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'direct_execution_street'
              AND column_name = 'direct_execution_id'
        ) THEN
            ALTER TABLE direct_execution_street
                ADD COLUMN direct_execution_id BIGINT NOT NULL;
        END IF;
    END$$;

-- 2. Adiciona a constraint de chave estrangeira (se ainda não existir)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
                 JOIN information_schema.key_column_usage kcu
                      ON tc.constraint_name = kcu.constraint_name
        WHERE tc.constraint_type = 'FOREIGN KEY'
          AND tc.table_name = 'direct_execution_street'
          AND kcu.column_name = 'direct_execution_id'
    ) THEN
        ALTER TABLE direct_execution_street
            ADD CONSTRAINT fk_direct_execution
                FOREIGN KEY (direct_execution_id)
                    REFERENCES direct_execution(direct_execution_id);
    END IF;
END$$;

