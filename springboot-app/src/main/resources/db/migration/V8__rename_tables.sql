DROP TABLE IF EXISTS tb_material_reservation;
DROP TABLE IF EXISTS tb_direct_executions_items;
DROP TABLE IF EXISTS tb_direct_executions;
DROP TABLE IF EXISTS tb_material_requisitions;
DROP TABLE IF EXISTS tb_version_control;

DO
$$
    DECLARE
        rec RECORD;
        r_fk RECORD;
    BEGIN
        -- Cria tabela temporária para armazenar os dados das FKs
        CREATE TEMP TABLE temp_foreign_keys (
                                                constraint_name TEXT,
                                                referencing_table TEXT,
                                                referencing_column TEXT,
                                                referenced_table TEXT,
                                                referenced_column TEXT
        );

        FOR rec IN
            SELECT * FROM (VALUES
                               ('tb_material_stock', 'material_stock'),
                               ('tb_deposits', 'deposit'),
                               ('tb_companies', 'company'),
                               ('tb_contract_reference_items', 'contract_reference_item'),
                               ('tb_contracts', 'contract'),
                               ('tb_contracts_items', 'contract_item'),
                               ('tb_email_config', 'email_config'),
                               ('tb_groups', 'material_group'),
                               ('tb_logs', 'log'),
                               ('tb_maintenance', 'maintenance'),
                               ('tb_materials', 'material'),
                               ('tb_pre_measurements', 'pre_measurement'),
                               ('tb_pre_measurements_streets', 'pre_measurement_street'),
                               ('tb_pre_measurements_streets_items', 'pre_measurement_street_item'),
                               ('tb_refresh_token', 'refresh_token'),
                               ('tb_regions', 'region'),
                               ('tb_related_materials', 'related_material'),
                               ('tb_reservation_managements', 'reservation_management'),
                               ('tb_roles', 'role'),
                               ('tb_stock_movement', 'stock_movement'),
                               ('tb_stockists', 'stockist'),
                               ('tb_supplier', 'supplier'),
                               ('tb_team_complementary_members', 'team_complementary_member'),
                               ('tb_teams', 'team'),
                               ('tb_types', 'material_type'),
                               ('tb_users', 'app_user'),
                               ('tb_users_roles', 'user_role')
                          ) AS t(old_name, new_name)
            LOOP
                -- Armazena as FKs antes de dropar
                INSERT INTO temp_foreign_keys (constraint_name, referencing_table, referencing_column, referenced_table, referenced_column)
                SELECT
                    con.conname,
                    cl.relname,
                    att.attname,
                    rec.old_name,
                    confatt.attname
                FROM
                    pg_constraint con
                        JOIN pg_class cl ON cl.oid = con.conrelid
                        JOIN pg_attribute att ON att.attrelid = cl.oid AND att.attnum = con.conkey[1]
                        JOIN pg_attribute confatt ON confatt.attrelid = con.confrelid AND confatt.attnum = con.confkey[1]
                WHERE
                    con.contype = 'f'
                  AND con.confrelid = rec.old_name::regclass;

                -- Dropa as FKs que apontam para a tabela antiga
                FOR r_fk IN
                    SELECT constraint_name, referencing_table
                    FROM temp_foreign_keys
                    WHERE referenced_table = rec.old_name
                    LOOP
                        EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I;', r_fk.referencing_table, r_fk.constraint_name);
                    END LOOP;

                -- Renomeia a tabela
                EXECUTE format('ALTER TABLE %I RENAME TO %I;', rec.old_name, rec.new_name);

                -- Recria as FKs com o novo nome da tabela
                FOR r_fk IN
                    SELECT *
                    FROM temp_foreign_keys
                    WHERE referenced_table = rec.old_name
                    LOOP
                        EXECUTE format(
                                'ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES %I(%I);',
                                r_fk.referencing_table,
                                r_fk.constraint_name,
                                r_fk.referencing_column,
                                rec.new_name,
                                r_fk.referenced_column
                                );
                    END LOOP;
            END LOOP;
    END
$$;


DO
$$
    DECLARE
        r RECORD;
    BEGIN
        -- Cria tabela temporária para armazenar os metadados das FKs
        CREATE TEMP TABLE temp_user_fks (
                                            constraint_name TEXT,
                                            referencing_table TEXT,
                                            referencing_column TEXT
        );

        -- 1. Armazena todas as FKs que apontam para app_user(id_user)
        INSERT INTO temp_user_fks (constraint_name, referencing_table, referencing_column)
        SELECT
            con.conname,
            cl.relname,
            att.attname
        FROM
            pg_constraint con
                JOIN pg_class cl ON cl.oid = con.conrelid
                JOIN pg_attribute att ON att.attrelid = cl.oid AND att.attnum = con.conkey[1]
        WHERE
            con.contype = 'f'
          AND con.confrelid = 'app_user'::regclass
          AND con.confkey[1] = (
            SELECT attnum
            FROM pg_attribute
            WHERE attrelid = 'app_user'::regclass AND attname = 'id_user'
            LIMIT 1
        );

        -- 2. Dropar todas essas FKs
        FOR r IN SELECT * FROM temp_user_fks LOOP
                EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I;', r.referencing_table, r.constraint_name);
            END LOOP;

        -- 3. Renomear a coluna da PK
        EXECUTE 'ALTER TABLE app_user RENAME COLUMN id_user TO user_id;';

        -- 4. Recriar as FKs com a nova coluna
        FOR r IN SELECT * FROM temp_user_fks LOOP
                EXECUTE format(
                        'ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES app_user(user_id);',
                        r.referencing_table,
                        r.constraint_name,
                        r.referencing_column
                        );
            END LOOP;
    END
$$;
