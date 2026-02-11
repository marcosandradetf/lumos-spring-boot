DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1
                       FROM information_schema.columns
                       WHERE table_name = 'log'
                         AND column_name = 'tenant_id') THEN
            DELETE FROM log;
            ALTER TABLE log
                ADD COLUMN tenant_id UUID NOT NULL,
                ADD CONSTRAINT log_tenant_id_f_key
                    FOREIGN KEY (tenant_id)
                        REFERENCES tenant (tenant_id);
        END IF;

        IF NOT EXISTS (SELECT 1
                       FROM information_schema.columns
                       WHERE table_name = 'contract'
                         AND column_name = 'last_updated_by') THEN
            ALTER TABLE contract
                ADD COLUMN last_updated_by UUID,
                ADD CONSTRAINT contract_last_updated_user_id_f_key
                    FOREIGN KEY (last_updated_by)
                        REFERENCES app_user (user_id);
        end if;

        drop trigger
            if exists trg_update_contract_value_after_insert on contract_item;

        drop function
            if exists update_contract_value_on_insert;

        alter table contract
            drop column if exists contract_value;


        -- disable user
        update app_user
        set status = false
        where user_id = 'd8511a21-b71d-4bdc-bf42-1f9ebb1098d3';

        delete from stockist
        where user_id_user = 'd8511a21-b71d-4bdc-bf42-1f9ebb1098d3';

        delete from refresh_token
        where id_user = 'd8511a21-b71d-4bdc-bf42-1f9ebb1098d3';

        -- add stockists
        if not exists(select 1 from stockist where user_id_user = '63119b69-cdc8-4900-9f69-f88d47d2f4c2') then
            INSERT INTO stockist (stockist_id, deposit_id_deposit, user_id_user, notification_code, tenant_id)
            VALUES (DEFAULT, 2, '8dcacaa7-d357-4df1-b762-e508f9f85515', '6223a4e7-a85c-402c-b62b-e9fd0957bbda',
                    'f0dc9ab8-cb2c-4f21-a75f-05b122614862');

            INSERT INTO stockist (stockist_id, deposit_id_deposit, user_id_user, notification_code, tenant_id)
            VALUES (DEFAULT, 2, '63119b69-cdc8-4900-9f69-f88d47d2f4c2', '6223a4e7-a85c-402c-b62b-e9fd0957bbda',
                    'f0dc9ab8-cb2c-4f21-a75f-05b122614862');
        end if;
    END;
$$;