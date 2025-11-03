DO
$$
    BEGIN
        -- tenant table
        IF NOT EXISTS (SELECT 1
                       FROM information_schema.columns
                       WHERE table_name = 'tenant') THEN
            CREATE TABLE tenant
            (
                tenant_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                tenant_number BIGSERIAL NOT NULL UNIQUE,
                description   TEXT      NOT NULL,
                bucket        TEXT      NOT NULL
            );

            INSERT INTO tenant(tenant_id, description, bucket)
            VALUES ('f0dc9ab8-cb2c-4f21-a75f-05b122614862','SCLCONST', 'scl-construtora');
        END IF;

        -- set tenant on app_user table
        IF NOT EXISTS (SELECT 1
                       FROM information_schema.columns
                       WHERE table_name = 'app_user'
                         and column_name = 'tenant_id') THEN
            ALTER TABLE app_user
                ADD COLUMN tenant_id UUID,
                ADD CONSTRAINT app_user_tenant_id_f_key
                    FOREIGN KEY (tenant_id)
                        REFERENCES tenant (tenant_id);

            UPDATE app_user
            SET tenant_id = (SELECT tenant_id from tenant limit 1);

            ALTER TABLE app_user
                ALTER COLUMN tenant_id SET NOT NULL;
        END IF;

        IF NOT EXISTS (SELECT 1
                       FROM information_schema.columns
                       WHERE table_name = 'contract'
                         AND column_name = 'company_id') THEN
            ALTER TABLE contract
                ADD COLUMN company_id bigint,
                ADD CONSTRAINT contract_company_f_key
                    FOREIGN KEY (company_id)
                        REFERENCES company (id_company);

            UPDATE contract
            SET company_id = (SELECT id_company from company limit 1);
        END IF;

        IF NOT EXISTS (SELECT 1
                       FROM information_schema.columns
                       WHERE table_name = 'contract'
                         AND column_name = 'tenant_id') THEN
            ALTER TABLE contract
                ADD COLUMN tenant_id UUID,
                ADD CONSTRAINT contract_tenant_f_key
                    FOREIGN KEY (tenant_id)
                        REFERENCES tenant (tenant_id);

            UPDATE contract
            SET tenant_id = (SELECT tenant_id from tenant limit 1);

            ALTER TABLE contract
                ALTER COLUMN tenant_id SET NOT NULL;
        END IF;

        IF NOT EXISTS (SELECT 1
                       FROM information_schema.columns
                       WHERE table_name = 'company'
                         AND column_name = 'tenant_id') THEN
            ALTER TABLE company
                ADD COLUMN tenant_id UUID,
                ADD CONSTRAINT company_tenant_f_key
                    FOREIGN KEY (tenant_id)
                        REFERENCES tenant (tenant_id);

            UPDATE company
            SET tenant_id = (SELECT tenant_id from tenant limit 1);

            ALTER TABLE company
                ALTER COLUMN tenant_id SET NOT NULL;
        END IF;

        IF NOT EXISTS (SELECT 1
                       FROM information_schema.columns
                       WHERE table_name = 'direct_execution'
                         AND column_name = 'tenant_id') THEN
            ALTER TABLE direct_execution
                ADD COLUMN tenant_id UUID,
                ADD CONSTRAINT direct_execution_tenant_f_key
                    FOREIGN KEY (tenant_id)
                        REFERENCES tenant (tenant_id);

            UPDATE direct_execution
            SET tenant_id = (SELECT tenant_id from tenant limit 1);

            ALTER TABLE direct_execution
                ALTER COLUMN tenant_id SET NOT NULL;
        END IF;

        IF NOT EXISTS (SELECT 1
                       FROM information_schema.columns
                       WHERE table_name = 'maintenance'
                         AND column_name = 'tenant_id') THEN
            ALTER TABLE maintenance
                ADD COLUMN tenant_id UUID,
                ADD CONSTRAINT maintenance_tenant_f_key
                    FOREIGN KEY (tenant_id)
                        REFERENCES tenant (tenant_id);

            UPDATE maintenance
            SET tenant_id = (SELECT tenant_id from tenant limit 1);

            ALTER TABLE maintenance
                ALTER COLUMN tenant_id SET NOT NULL;
        END IF;

        IF NOT EXISTS (SELECT 1
                       FROM information_schema.columns
                       WHERE table_name = 'pre_measurement'
                         AND column_name = 'tenant_id') THEN
            ALTER TABLE pre_measurement
                ADD COLUMN tenant_id UUID,
                ADD CONSTRAINT pre_measurement_tenant_f_key
                    FOREIGN KEY (tenant_id)
                        REFERENCES tenant (tenant_id);

            UPDATE pre_measurement
            SET tenant_id = (SELECT tenant_id from tenant limit 1);

            ALTER TABLE pre_measurement
                ALTER COLUMN tenant_id SET NOT NULL;
        END IF;

        IF NOT EXISTS (SELECT 1
                       FROM information_schema.columns
                       WHERE table_name = 'material_stock'
                         AND column_name = 'tenant_id') THEN
            ALTER TABLE material_stock
                ADD COLUMN tenant_id UUID,
                ADD CONSTRAINT material_stock_tenant_f_key
                    FOREIGN KEY (tenant_id)
                        REFERENCES tenant (tenant_id);

            UPDATE material_stock
            SET tenant_id = (SELECT tenant_id from tenant limit 1);

            ALTER TABLE material_stock
                ALTER COLUMN tenant_id SET NOT NULL;
        END IF;

        IF NOT EXISTS (SELECT 1
                       FROM information_schema.columns
                       WHERE table_name = 'team'
                         AND column_name = 'tenant_id') THEN
            ALTER TABLE team
                ADD COLUMN tenant_id UUID,
                ADD CONSTRAINT team_tenant_f_key
                    FOREIGN KEY (tenant_id)
                        REFERENCES tenant (tenant_id);

            UPDATE team
            SET tenant_id = (SELECT tenant_id from tenant limit 1);

            ALTER TABLE team
                ALTER COLUMN tenant_id SET NOT NULL;
        END IF;

        IF NOT EXISTS (SELECT 1
                       FROM information_schema.columns
                       WHERE table_name = 'deposit'
                         AND column_name = 'tenant_id') THEN
            ALTER TABLE deposit
                ADD COLUMN tenant_id UUID,
                ADD CONSTRAINT deposit_tenant_f_key
                    FOREIGN KEY (tenant_id)
                        REFERENCES tenant (tenant_id);

            UPDATE deposit
            SET tenant_id = (SELECT tenant_id from tenant limit 1);

            ALTER TABLE deposit
                ALTER COLUMN tenant_id SET NOT NULL;
        END IF;

    END
$$;

ALTER TABLE company
DROP COLUMN IF EXISTS bucket_file_name;

ALTER TABLE material_stock
    DROP COLUMN IF EXISTS company_id;