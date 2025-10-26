DO
$$
    BEGIN
        -- Verifica se a coluna 'steps' existe na tabela 'pre_measurement'
        IF NOT EXISTS (SELECT 1
                       FROM information_schema.columns
                       WHERE table_name = 'tenant') THEN
            CREATE TABLE tenant
            (
                tenant_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                description text NOT NULL
            );

            INSERT INTO tenant(description)
            VALUES ('SCLCONST');
        END IF;

        IF NOT EXISTS (SELECT 1
                       FROM information_schema.columns
                       WHERE table_name = 'app_user' and column_name = 'tenant_id') THEN
            ALTER TABLE app_user
                ADD COLUMN tenant_id UUID,
                ADD CONSTRAINT app_user_tenant_id_f_key
                    FOREIGN KEY (tenant_id)
                        REFERENCES tenant (tenant_id);

            UPDATE app_user
            SET tenant_id = (SELECT tenant_id from tenant limit 1);
        END IF;


    END
$$;