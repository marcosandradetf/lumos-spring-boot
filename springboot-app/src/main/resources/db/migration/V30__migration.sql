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

        -- INSTALLATION
        CREATE OR REPLACE VIEW installation_view AS
        SELECT de.direct_execution_id AS installation_id,
               'DIRECT_EXECUTION'     AS installation_type,
               de.contract_id,
               de.team_id,
               de.assigned_user_id,
               de.description,
               de.step,
               de.signature_uri,
               de.responsible,
               de.sign_date,
               de.report_view_at,
               de.available_at,
               de.finished_at,
               de.tenant_id,
               de.started_at,
               de.direct_execution_status as status,
               de.reservation_management_id
        FROM direct_execution de
        UNION ALL
        SELECT pm.pre_measurement_id   AS installation_id,
               'PRE_MEASUREMENT'       AS installation_type,
               pm.contract_contract_id AS contract_id,
               pm.team_id,
               pm.assign_by_user_id    AS assigned_user_id,
               pm.comment              AS description,
               pm.step,
               pm.signature_uri,
               pm.responsible,
               pm.sign_date,
               pm.report_view_at,
               pm.available_at,
               pm.finished_at,
               pm.tenant_id,
               pm.started_at,
               pm.status,
               pm.reservation_management_id
        FROM pre_measurement pm;

    END;
$$;