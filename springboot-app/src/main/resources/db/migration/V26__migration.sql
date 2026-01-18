DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1
                       FROM pg_constraint
                       WHERE conname = 'order_material_item_material_id_material_fk') THEN
            ALTER TABLE order_material_item
                ADD CONSTRAINT order_material_item_material_id_material_fk
                    FOREIGN KEY (material_id)
                        REFERENCES material (id_material);

            update material set inactive = false where inactive is null;
        END IF;

        if not exists(select 1
                      from information_schema.columns
                      where column_name = 'truck_stock_control'
                        and table_name = 'contract_reference_item') then
            alter table contract_reference_item
                add column truck_stock_control boolean not null default true;

            UPDATE public.contract_reference_item
            SET truck_stock_control = false
            WHERE contract_reference_item_id = 42;

            UPDATE public.contract_reference_item
            SET truck_stock_control = false
            WHERE contract_reference_item_id = 7;

            UPDATE public.contract_reference_item
            SET truck_stock_control = false
            WHERE contract_reference_item_id = 40;

            UPDATE public.contract_reference_item
            SET truck_stock_control = false
            WHERE contract_reference_item_id = 50;

            UPDATE public.contract_reference_item
            SET truck_stock_control = false
            WHERE contract_reference_item_id = 1;

            UPDATE public.contract_reference_item
            SET truck_stock_control = false
            WHERE contract_reference_item_id = 52;

            UPDATE public.contract_reference_item
            SET truck_stock_control = false
            WHERE contract_reference_item_id = 38;

            UPDATE public.contract_reference_item
            SET truck_stock_control = false
            WHERE contract_reference_item_id = 55;

            UPDATE public.contract_reference_item
            SET truck_stock_control = false
            WHERE contract_reference_item_id = 12;

            UPDATE public.contract_reference_item
            SET truck_stock_control = false
            WHERE contract_reference_item_id = 14;

            UPDATE public.contract_reference_item
            SET truck_stock_control = false
            WHERE contract_reference_item_id = 41;

            UPDATE public.contract_reference_item
            SET truck_stock_control = false
            WHERE contract_reference_item_id = 51;

            UPDATE public.contract_reference_item
            SET truck_stock_control = false
            WHERE contract_reference_item_id = 39;

            UPDATE public.contract_reference_item
            SET truck_stock_control = false
            WHERE contract_reference_item_id = 6;

            UPDATE public.contract_reference_item
            SET truck_stock_control = false
            WHERE contract_reference_item_id = 2;

            UPDATE public.contract_reference_item
            SET truck_stock_control = false
            WHERE contract_reference_item_id = 13;

            UPDATE public.contract_reference_item
            SET truck_stock_control = false
            WHERE contract_reference_item_id = 49;

            UPDATE public.contract_reference_item
            SET truck_stock_control = false
            WHERE contract_reference_item_id = 37;

            UPDATE public.contract_reference_item
            SET truck_stock_control = false
            WHERE contract_reference_item_id = 36;

            UPDATE public.contract_reference_item
            SET truck_stock_control = false
            WHERE contract_reference_item_id = 5;

            UPDATE public.contract_reference_item
            SET truck_stock_control = false
            WHERE contract_reference_item_id = 44;

            UPDATE public.contract_reference_item
            SET truck_stock_control = false
            WHERE contract_reference_item_id = 48;
        end if;

        IF NOT EXISTS (SELECT 1
                       FROM information_schema.columns
                       WHERE table_name = 'stock_movement'
                         and column_name = 'tenant_id') THEN
            ALTER TABLE stock_movement
                ADD COLUMN tenant_id UUID,
                ADD CONSTRAINT app_user_tenant_id_f_key
                    FOREIGN KEY (tenant_id)
                        REFERENCES tenant (tenant_id);

            UPDATE stock_movement
            SET tenant_id = (SELECT tenant_id from tenant limit 1);

            ALTER TABLE stock_movement
                ALTER COLUMN tenant_id SET NOT NULL;
        END IF;

        -- DEMO
        if not exists(select 1 from tenant where tenant_id = '5b435381-e1ce-4e75-86da-fc237ef22b4c') then
            INSERT INTO public.tenant (tenant_id, tenant_number, description, bucket)
            VALUES ('5b435381-e1ce-4e75-86da-fc237ef22b4c', 2, 'DEMO', 'demo');

            UPDATE public.app_user
            SET username = 'supportScl'
            WHERE user_id = '5c95b7a6-1b6e-46e2-859c-bbfb96d73501';

            INSERT INTO public.company (social_reason, fantasy_name, company_cnpj, company_contact, company_phone,
                                        company_email,
                                        company_address, company_logo, tenant_id)
            VALUES ('EMPRESA DEMONSTRAÇÃO', 'EMPRESA DEMONSTRAÇÃO', '00.000.000/0001-01', 'MARCOS ANDRADE',
                    '31999990000',
                    'demo@lumos.com',
                    'Av. Afonso Pena, 1212 - Centro, Belo Horizonte - MG', 'photos/logo/demo.png',
                    '5b435381-e1ce-4e75-86da-fc237ef22b4c');

            INSERT INTO contract_reference_item (description,
                                                 item_dependency,
                                                 linking,
                                                 type,
                                                 name_for_import,
                                                 factor,
                                                 tenant_id,
                                                 truck_stock_control)
            select c.description,
                   c.item_dependency,
                   c.linking,
                   c.type,
                   c.name_for_import,
                   c.factor,
                   '5b435381-e1ce-4e75-86da-fc237ef22b4c',
                   c.truck_stock_control
            from contract_reference_item c
            WHERE c.tenant_id = 'f0dc9ab8-cb2c-4f21-a75f-05b122614862';

            ALTER TABLE deposit
                DROP CONSTRAINT uk1c1hvsr7tdyhxwiglcrwvf7aa;

            create unique index uk1c1hvsr7tdyhxwiglcrwvf7aa
                on deposit (deposit_name, tenant_id);

            INSERT INTO public.deposit (deposit_address, deposit_city, deposit_district, deposit_name, deposit_phone,
                                        deposit_state,
                                        company_id, region_id, is_truck, tenant_id)
            VALUES ('Vila Real 500', 'Belo Horizonte', 'São Francisco', 'GALPÃO BH', '', 'MG', (select id_company from company where tenant_id = '5b435381-e1ce-4e75-86da-fc237ef22b4c'), 1, false,
                    '5b435381-e1ce-4e75-86da-fc237ef22b4c');

            INSERT INTO public.deposit (deposit_address, deposit_city, deposit_district, deposit_name, deposit_phone,
                                        deposit_state,
                                        company_id, region_id, is_truck, tenant_id)
            VALUES (null, 'Itumirim', null, 'SL 03 - ABC5XO1', null, 'MG', (select id_company from company where tenant_id = '5b435381-e1ce-4e75-86da-fc237ef22b4c'), 3, true,
                    '5b435381-e1ce-4e75-86da-fc237ef22b4c');

            INSERT INTO public.team (ufname,
                                     city_name,
                                     team_name,
                                     region_region_id,
                                     plate_vehicle,
                                     deposit_id_deposit,
                                     team_phone,
                                     notification_code,
                                     tenant_id)
            VALUES ('MG',
                    'Itumirim',
                    'SL 03 - ABC5XO1',
                    3,
                    'ABC5XO1',
                    (select id_deposit
                     from deposit
                     where tenant_id = '5b435381-e1ce-4e75-86da-fc237ef22b4c'
                       and is_truck = true),
                    null,
                    '5daf18f6-b859-4b65-99ee-c5d644b72db7',
                    '5b435381-e1ce-4e75-86da-fc237ef22b4c');

            INSERT INTO public.app_user (user_id, code_reset_password, date_of_birth, email, last_name, name, password,
                                         status,
                                         username, cpf, phone_number, team_id, tenant_id, support)
            VALUES ('3c0030b9-e7fa-4cd9-b2fd-741ce34f4ad3', null, '2000-01-15', 'demo@lumos.com', 'Suporte', 'Usuário',
                    '$2a$10$/uJhIyYrm39frUWt1RTCguwTA0PDQJa3.NIUgEd1G9EshOEL5LJrG', true, 'supportDemo', '12345678900',
                    null,
                    (select id_team from team where team.tenant_id = '5b435381-e1ce-4e75-86da-fc237ef22b4c'),
                    '5b435381-e1ce-4e75-86da-fc237ef22b4c', true);

            INSERT INTO public.user_role (id_user, id_role)
            VALUES ('3c0030b9-e7fa-4cd9-b2fd-741ce34f4ad3', 1);

            INSERT INTO public.user_role (id_user, id_role)
            VALUES ('3c0030b9-e7fa-4cd9-b2fd-741ce34f4ad3', 2);

            INSERT INTO public.user_role (id_user, id_role)
            VALUES ('3c0030b9-e7fa-4cd9-b2fd-741ce34f4ad3', 3);

            INSERT INTO public.user_role (id_user, id_role)
            VALUES ('3c0030b9-e7fa-4cd9-b2fd-741ce34f4ad3', 4);

            INSERT INTO public.user_role (id_user, id_role)
            VALUES ('3c0030b9-e7fa-4cd9-b2fd-741ce34f4ad3', 5);

            INSERT INTO public.user_role (id_user, id_role)
            VALUES ('3c0030b9-e7fa-4cd9-b2fd-741ce34f4ad3', 6);

            INSERT INTO public.user_role (id_user, id_role)
            VALUES ('3c0030b9-e7fa-4cd9-b2fd-741ce34f4ad3', 7);

            alter table material_contract_reference_item
                add column material_name text;

            alter table material_contract_reference_item
                add column description text;

            update material_contract_reference_item mcri
            set material_name = m.material_name
            from material m
            where m.id_material = mcri.material_id;

            update material_contract_reference_item mcri
            set description = cri.description
            from contract_reference_item cri
            where mcri.contract_reference_item_id = cri.contract_reference_item_id;

            alter table material
                drop constraint uk15knrd692wikv56ii1ony1utn;

            create unique index uk15knrd692wikv56ii1ony1utn
                on material (name_for_import, tenant_id);

            INSERT INTO public.material (material_amps, material_brand, material_length, material_name, material_power,
                                         stock_available, stock_quantity, id_material_type, inactive, name_for_import,
                                         material_name_unaccent, unit_base, is_generic, parent_material_id,
                                         default_quantity,
                                         material_width, material_gauge, material_model, barcode, is_fractionable,
                                         material_function, subtype_id, material_weight, tenant_id, buy_unit,
                                         request_unit,
                                         truck_stock_control)
            select m.material_amps,
                   m.material_brand,
                   m.material_length,
                   m.material_name,
                   m.material_power,
                   m.stock_available,
                   m.stock_quantity,
                   m.id_material_type,
                   m.inactive,
                   m.name_for_import,
                   m.material_name_unaccent,
                   m.unit_base,
                   m.is_generic,
                   m.parent_material_id,
                   m.default_quantity,
                   m.material_width,
                   m.material_gauge,
                   m.material_model,
                   m.barcode,
                   m.is_fractionable,
                   m.material_function,
                   m.subtype_id,
                   m.material_weight,
                   '5b435381-e1ce-4e75-86da-fc237ef22b4c',
                   m.buy_unit,
                   m.request_unit,
                   m.truck_stock_control
            from material m;

            insert into material_contract_reference_item
            (material_id, contract_reference_item_id)
            select distinct m.id_material, cri.contract_reference_item_id
            from material_contract_reference_item mcri
                     join material m on m.material_name = mcri.material_name
                     join contract_reference_item cri on cri.description = mcri.description
            where m.tenant_id = '5b435381-e1ce-4e75-86da-fc237ef22b4c'
                and cri.tenant_id = '5b435381-e1ce-4e75-86da-fc237ef22b4c';

            insert into contract_item_dependency(contract_item_reference_id,
                                                 contract_item_reference_id_dependency,
                                                 factor)
            select distinct new_item.contract_reference_item_id       as contract_item_reference_id,
                            new_dependency.contract_reference_item_id as contract_item_reference_id_dependency,
                            coalesce(new_dependency.factor, 1)        as factor
            from contract_item_dependency cid
                     join contract_reference_item item
                          on item.contract_reference_item_id = cid.contract_item_reference_id
                     join contract_reference_item dependency
                          on dependency.contract_reference_item_id = cid.contract_item_reference_id_dependency
                     join contract_reference_item new_item
                          on new_item.description = item.description
                     join contract_reference_item new_dependency
                          on new_dependency.description = dependency.description
            where new_item.tenant_id = '5b435381-e1ce-4e75-86da-fc237ef22b4c'
              and new_dependency.tenant_id = '5b435381-e1ce-4e75-86da-fc237ef22b4c';

            WITH deposits AS (SELECT id_deposit
                              FROM deposit
                              WHERE tenant_id = '5b435381-e1ce-4e75-86da-fc237ef22b4c')
            INSERT
            INTO material_stock (buy_unit,
                                 cost_per_item,
                                 cost_price,
                                 inactive,
                                 request_unit,
                                 stock_available,
                                 stock_quantity,
                                 deposit_id,
                                 material_id,
                                 tenant_id)
            SELECT m.buy_unit,
                   NULL AS cost_per_item,
                   NULL AS cost_price,
                   m.inactive,
                   m.request_unit,
                   0    AS stock_available,
                   0    AS stock_quantity,
                   d.id_deposit,
                   m.id_material,
                   m.tenant_id
            FROM material m
                     CROSS JOIN deposits d
            WHERE m.is_generic = false
              AND m.tenant_id = '5b435381-e1ce-4e75-86da-fc237ef22b4c';

            INSERT INTO public.contract (address, cnpj, contract_file, contract_number, contract_value, contractor,
                                         creation_date,
                                         notice_file, phone, status, unify_services, created_by_id_user, company_id,
                                         tenant_id)
            VALUES ('Praça Padre Altamiro de Faria, nº 178 - Centro – CEP. 35.506-000-SÃO SEBASTIÃO DO OESTE',
                    '18.308.734/0001-06',
                    null, '009/2023', 133534374.50, 'PREFEITURA MUNICIPAL DE SÃO SEBASTIÃO DO OESTE',
                    '2025-12-15 12:06:24.000457 +00:00', null, null, 'ACTIVE', false,
                    (select user_id from app_user where tenant_id = '5b435381-e1ce-4e75-86da-fc237ef22b4c'), 5,
                    '5b435381-e1ce-4e75-86da-fc237ef22b4c');

            INSERT INTO contract_item (contracted_quantity, total_price, unit_price, contract_contract_id,
                                       contract_item_reference_id, quantity_executed)
            select ci.contracted_quantity,
                   ci.total_price,
                   ci.unit_price,
                   (select contract_id from contract where tenant_id = '5b435381-e1ce-4e75-86da-fc237ef22b4c'),
                   new_cri.contract_reference_item_id,
                   0
            from contract_item ci
                     join contract_reference_item cri
                          on cri.contract_reference_item_id = ci.contract_item_reference_id
                     join contract_reference_item new_cri on new_cri.description = cri.description
            where new_cri.tenant_id = '5b435381-e1ce-4e75-86da-fc237ef22b4c'
              and ci.contract_contract_id = 95;
        end if;

    end;
$$;
