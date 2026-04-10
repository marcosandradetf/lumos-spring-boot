ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS must_change_password       BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS activation_code_hash       TEXT,
    ADD COLUMN IF NOT EXISTS activation_code_expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS activation_attempt_count   INTEGER NOT NULL DEFAULT 0;


-- 1. Cria uma nova coluna temporária com o tipo desejado
ALTER TABLE app_user
    ADD COLUMN status_backup TEXT;

-- 2. Alimenta a nova coluna com base na antiga, tratando o valor como texto puro para evitar erro de tipo
UPDATE app_user
SET status_backup = CASE
                        WHEN status::text IN ('true', 't', '1', 'y', 'yes') THEN 'ACTIVE'
                        ELSE 'BLOCKED'
    END;

-- 3. Remove a coluna antiga
ALTER TABLE app_user
    DROP COLUMN status;

-- 4. Renomeia a nova coluna para o nome original
ALTER TABLE app_user
    RENAME COLUMN status_backup TO status;

ALTER TABLE app_user
    ALTER COLUMN status SET DEFAULT 'ACTIVE',
    ALTER COLUMN status SET NOT NULL;

DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1
                       FROM pg_constraint
                       WHERE conname = 'chk_app_user_status') THEN
            ALTER TABLE app_user
                ADD CONSTRAINT chk_app_user_status
                    CHECK (status IN ('PENDING_ACTIVATION', 'ACTIVE', 'BLOCKED'));
        END IF;
    END
$$;

DROP INDEX IF EXISTS idx_app_user_tenant_billing;

CREATE INDEX IF NOT EXISTS idx_app_user_tenant_billing
    ON app_user (tenant_id)
    WHERE support IS FALSE
        AND status = 'ACTIVE'
        AND deactivated_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_app_user_activation_status
    ON app_user (tenant_id, status);
