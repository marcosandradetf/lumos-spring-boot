DO
$$
    BEGIN
        create index if not exists material_tenant_index
            on material (tenant_id);

        create index if not exists material_bar_code_index
            on material (barcode);

        create index if not exists contract_reference_item_tenant_index
            on contract_reference_item (tenant_id);

        create index if not exists app_user_tenant_index
            on app_user (tenant_id);

        create index if not exists contract_tenant_index
            on contract (tenant_id);

        create index if not exists company_tenant_index
            on company (tenant_id);

        create index if not exists maintenance_tenant_index
            on maintenance (tenant_id);

        create index if not exists material_stock_tenant_index
            on material_stock (tenant_id);

        create index if not exists team_tenant_index
            on team (tenant_id);

        create index if not exists deposit_tenant_index
            on deposit (tenant_id);

        create index if not exists direct_execution_tenant_index
            on direct_execution (tenant_id);

        create index if not exists pre_measurement_tenant_index
            on pre_measurement (tenant_id);

        create index if not exists reservation_management_tenant_index
            on reservation_management (tenant_id);

        create index if not exists material_reservation_tenant_index
            on material_reservation (tenant_id);

        create index if not exists material_history_tenant_index
            on material_history (tenant_id);

        create table if not exists contract_item_dependency
        (
            contract_item_reference_id            bigint
                constraint contract_item_id_contract_reference_item_id_fk
                    references contract_reference_item,
            contract_item_reference_id_dependency bigint
                constraint contract_item_dependency_id_contract_reference_item_id_fk
                    references contract_reference_item,
            factor                                numeric default 1 not null,
            constraint contract_item_dependency_pk
                primary key (contract_item_reference_id, contract_item_reference_id_dependency)
        );

        if not exists(select 1
                      from information_schema.columns
                      where table_name = 'material_type'
                        and column_name = 'default_unit') then
            alter table material_type
                add column default_unit text not null default 'UN';

            UPDATE public.material_type
            SET default_unit = 'CX'
            WHERE id_type = 5;

            UPDATE public.material_type
            SET default_unit = 'PCT'
            WHERE id_type = 14;

            UPDATE public.material_type
            SET default_unit = 'PCT'
            WHERE id_type = 35;

            UPDATE public.material_type
            SET default_unit = 'PAR'
            WHERE id_type = 19;

            UPDATE public.material_type
            SET default_unit = 'PÇ'
            WHERE id_type = 36;

            UPDATE public.material_type
            SET default_unit = 'SACO'
            WHERE id_type = 41;

            UPDATE public.material_type
            SET default_unit = 'CX'
            WHERE id_type = 30;

            UPDATE public.material_type
            SET default_unit = 'ROLO'
            WHERE id_type = 3;

            UPDATE public.material_type
            SET default_unit = 'PCT'
            WHERE id_type = 39;

            UPDATE material m
            set request_unit = t.default_unit,
                buy_unit     = t.default_unit
            FROM material_type t
            where t.id_type = m.id_material_type;

            UPDATE public.material
            SET request_unit = 'UN',
                buy_unit     = 'UN'
            WHERE id_material = 138;

            UPDATE public.material
            SET request_unit = 'UN',
                buy_unit     = 'UN'
            WHERE id_material = 725;

            UPDATE public.material
            SET request_unit = 'UN',
                buy_unit     = 'UN'
            WHERE id_material = 342;

            UPDATE material_stock ms
            set buy_unit     = m.buy_unit,
                request_unit = m.request_unit
            FROM material m
            WHERE m.id_material = ms.material_id;
        END IF;

        -- vínculo de itens e serviços
        IF NOT EXISTS(SELECT 1 FROM contract_item_dependency) THEN
            --BRAÇO -> SERVIÇO
            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (10, 2, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (9, 2, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (11, 2, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (43, 2, DEFAULT);

            --LUMINÁRIA LED -> SERVIÇO/PROJETO
            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (16, 12, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (17, 12, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (18, 12, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (19, 12, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (15, 12, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (21, 12, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (22, 12, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (23, 12, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (20, 12, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (16, 1, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (17, 1, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (18, 1, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (19, 1, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (15, 1, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (21, 1, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (22, 1, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (23, 1, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (20, 1, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (16, 42, 0.17);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (17, 42, 0.17);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (18, 42, 0.17);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (19, 42, 0.17);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (15, 42, 0.17);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (21, 42, 0.17);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (22, 42, 0.17);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (23, 42, 0.17);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (20, 42, 0.17);

            -- REFLETOR LED -> SERVIÇO
            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (24, 1, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (25, 1, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (26, 1, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (27, 1, DEFAULT);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (28, 1, DEFAULT);

            -- BRAÇO -> CABO
            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (9, 36, 6.5);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (10, 36, 9.5);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (11, 36, 12.5);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (43, 36, 6.5);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (9, 37, 6.5);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (10, 37, 9.5);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (11, 37, 12.5);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (43, 37, 6.5);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (9, 6, 6.5);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (10, 6, 9.5);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (11, 6, 12.5);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (43, 6, 6.5);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (9, 41, 6.5);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (10, 41, 9.5);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (11, 41, 12.5);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (43, 41, 6.5);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (9, 5, 6.5);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (10, 5, 9.5);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (11, 5, 12.5);

            INSERT INTO public.contract_item_dependency (contract_item_reference_id,
                                                         contract_item_reference_id_dependency, factor)
            VALUES (43, 5, 6.5);

            UPDATE public.contract_reference_item
            SET factor = 6.5
            WHERE contract_reference_item_id = 43;

            UPDATE public.contract_reference_item
            SET factor = 12.5
            WHERE contract_reference_item_id = 11;

            UPDATE public.contract_reference_item
            SET factor = 9.5
            WHERE contract_reference_item_id = 10;

            UPDATE public.contract_reference_item
            SET factor = 6.5
            WHERE contract_reference_item_id = 9;
        end if;

        IF NOT EXISTS (SELECT 1 FROM material_contract_reference_item) THEN
            update material_reservation mr
            set truck_material_stock_id =
                    (select ms2.material_id_stock
                     from material_stock ms2
                              join material m2 on m2.id_material = ms2.material_id
                     where m2.material_name = m.material_name
                       and ms2.material_id_stock <> ms.material_id_stock
                       and ms2.deposit_id = ms.deposit_id)
            from material_stock ms,
                 material m
            where mr.truck_material_stock_id = ms.material_id_stock
              and m.id_material = ms.material_id
              and ms.material_id_stock in (1144, 1060);

            delete from material_stock where material_id in (340, 341);
            delete from material where id_material in (340, 341);

            INSERT INTO material_contract_reference_item
                (material_id, contract_reference_item_id)
            VALUES
                -- BRAÇOS
                (122, 9),
                (123, 10),
                (124, 11),
                (338, 43),

                -- CABOS
                (125, 5),
                (326, 6),

                -- CIMENTO
                (339, 44),

                -- CINTAS
                (133, 45),
                (134, 46),

                -- CONECTORES
                (138, 4),
                (342, 47),
                (137, 48),
                (343, 48),

                -- FITAS
                (140, 50),
                (345, 50),
                (344, 49),

                -- LUMINÁRIAS
                (85, 19),
                (97, 20),
                (86, 21),
                (83, 22),
                (88, 23),
                (89, 15),
                (90, 16),
                (84, 17),
                (92, 18),

                -- PARAFUSOS / PORCA
                (350, 55),
                (346, 51),
                (347, 52),

                -- POSTES
                (348, 53),
                (349, 54),
                (320, 30),
                (321, 31),
                (319, 29),
                (324, 34),
                (325, 35),
                (322, 32),
                (323, 33),

                -- REFLETORES
                (112, 24),
                (111, 25),
                (114, 26),
                (113, 27),
                (115, 28),

                -- RELÉ
                (116, 3),

                -- DEMAIS ITENS
                (326, 41),
                (330, 8),
                (127, 8),
                (128, 8),
                (129, 8),
                (331, 8),
                (130, 8),
                (131, 8),
                (132, 8),
                (133, 8),
                (134, 8),
                (332, 8),
                (333, 8),
                (100, 7),
                (101, 7),
                (102, 7),
                (103, 7),
                (104, 7),
                (105, 7),
                (350, 7),
                (346, 7);
        end if;

        CREATE TABLE if not exists unit
        (
            unit_id             SERIAL PRIMARY KEY,
            code                VARCHAR(5)  NOT NULL UNIQUE,
            description         VARCHAR(50) NOT NULL,
            truck_stock_control boolean     NOT NULL
        );

        CREATE TABLE if not exists material_type_buy_unit
        (
            material_type_id INT NOT NULL,
            unit_id          INT NOT NULL,

            PRIMARY KEY (material_type_id, unit_id),

            FOREIGN KEY (material_type_id)
                REFERENCES material_type (id_type),

            FOREIGN KEY (unit_id)
                REFERENCES unit (unit_id)
        );

        CREATE TABLE if not exists material_type_request_unit
        (
            material_type_id INT NOT NULL,
            unit_id          INT NOT NULL,

            PRIMARY KEY (material_type_id, unit_id),

            FOREIGN KEY (material_type_id)
                REFERENCES material_type (id_type),

            FOREIGN KEY (unit_id)
                REFERENCES unit (unit_id)
        );

        if not exists(select 1 from unit) then
            INSERT INTO public.unit (unit_id, code, description, truck_stock_control)
            VALUES (DEFAULT, 'CM', 'Centímetro', false);

            INSERT INTO public.unit (unit_id, code, description, truck_stock_control)
            VALUES (DEFAULT, 'CX', 'Caixa', false);

            INSERT INTO public.unit (unit_id, code, description, truck_stock_control)
            VALUES (DEFAULT, 'KG', 'Quilograma', false);

            INSERT INTO public.unit (unit_id, code, description, truck_stock_control)
            VALUES (DEFAULT, 'KIT', 'Kit', true);

            INSERT INTO public.unit (unit_id, code, description, truck_stock_control)
            VALUES (DEFAULT, 'L', 'Litro', false);

            INSERT INTO public.unit (unit_id, code, description, truck_stock_control)
            VALUES (DEFAULT, 'M', 'Metro', false);

            INSERT INTO public.unit (unit_id, code, description, truck_stock_control)
            VALUES (DEFAULT, 'ML', 'Mililitro', false);

            INSERT INTO public.unit (unit_id, code, description, truck_stock_control)
            VALUES (DEFAULT, 'PAR', 'Par', true);

            INSERT INTO public.unit (unit_id, code, description, truck_stock_control)
            VALUES (DEFAULT, 'PCT', 'Pacote', false);

            INSERT INTO public.unit (unit_id, code, description, truck_stock_control)
            VALUES (DEFAULT, 'PÇ', 'Peça', true);

            INSERT INTO public.unit (unit_id, code, description, truck_stock_control)
            VALUES (DEFAULT, 'ROLO', 'Rolo de cabo ou fita', false);

            INSERT INTO public.unit (unit_id, code, description, truck_stock_control)
            VALUES (DEFAULT, 'SACO', 'Saco', false);

            INSERT INTO public.unit (unit_id, code, description, truck_stock_control)
            VALUES (DEFAULT, 'T', 'Tonelada', false);

            INSERT INTO public.unit (unit_id, code, description, truck_stock_control)
            VALUES (DEFAULT, 'UN', 'Unidade', true);
        end if;

        if not exists(select 1 from material_type_request_unit) then
            INSERT INTO public.material_type_request_unit (material_type_id, unit_id)
            VALUES (1, 14);

            INSERT INTO public.material_type_request_unit (material_type_id, unit_id)
            VALUES (2, 14);

            INSERT INTO public.material_type_request_unit (material_type_id, unit_id)
            VALUES (3, 11);

            INSERT INTO public.material_type_request_unit (material_type_id, unit_id)
            VALUES (4, 14);

            INSERT INTO public.material_type_request_unit (material_type_id, unit_id)
            VALUES (5, 14);

            INSERT INTO public.material_type_request_unit (material_type_id, unit_id)
            VALUES (6, 14);

            INSERT INTO public.material_type_request_unit (material_type_id, unit_id)
            VALUES (12, 14);

            INSERT INTO public.material_type_request_unit (material_type_id, unit_id)
            VALUES (14, 2);

            INSERT INTO public.material_type_request_unit (material_type_id, unit_id)
            VALUES (18, 14);

            INSERT INTO public.material_type_request_unit (material_type_id, unit_id)
            VALUES (19, 8);

            INSERT INTO public.material_type_request_unit (material_type_id, unit_id)
            VALUES (20, 14);

            INSERT INTO public.material_type_request_unit (material_type_id, unit_id)
            VALUES (26, 14);

            INSERT INTO public.material_type_request_unit (material_type_id, unit_id)
            VALUES (28, 14);

            INSERT INTO public.material_type_request_unit (material_type_id, unit_id)
            VALUES (29, 14);

            INSERT INTO public.material_type_request_unit (material_type_id, unit_id)
            VALUES (30, 11);

            INSERT INTO public.material_type_request_unit (material_type_id, unit_id)
            VALUES (31, 14);

            INSERT INTO public.material_type_request_unit (material_type_id, unit_id)
            VALUES (32, 14);

            INSERT INTO public.material_type_request_unit (material_type_id, unit_id)
            VALUES (35, 14);

            INSERT INTO public.material_type_request_unit (material_type_id, unit_id)
            VALUES (35, 2);

            INSERT INTO public.material_type_request_unit (material_type_id, unit_id)
            VALUES (36, 14);

            INSERT INTO public.material_type_request_unit (material_type_id, unit_id)
            VALUES (39, 2);

            INSERT INTO public.material_type_request_unit (material_type_id, unit_id)
            VALUES (41, 12);
        end if;

        if not exists(select 1 from material_type_buy_unit) then
            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (1, 2);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (1, 14);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (2, 14);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (3, 11);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (4, 2);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (4, 14);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (5, 2);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (5, 14);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (6, 2);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (6, 14);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (12, 2);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (12, 14);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (14, 2);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (14, 9);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (18, 2);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (18, 14);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (19, 8);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (20, 14);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (26, 2);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (26, 14);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (28, 2);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (28, 14);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (29, 2);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (29, 14);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (30, 11);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (31, 2);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (31, 14);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (32, 14);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (35, 2);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (35, 14);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (36, 14);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (39, 2);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (39, 9);

            INSERT INTO public.material_type_buy_unit (material_type_id, unit_id)
            VALUES (41, 12);
        end if;

        if not exists(select 1
                      from information_schema.columns
                      where column_name = 'truck_stock_control' and table_name = 'material') then
            alter table material
                add column if not exists truck_stock_control boolean not null default false;

            update material m
            set truck_stock_control = u.truck_stock_control
            from material_type_request_unit ru,
                 unit u
            where m.id_material_type = ru.material_type_id
              and u.unit_id = ru.unit_id
              AND (SELECT count(*)
                   FROM material_type_request_unit
                   WHERE material_type_id = m.id_material_type) = 1;
            update material set truck_stock_control = true where id_material in (138, 342, 725);
        end if;

    end;
$$;
