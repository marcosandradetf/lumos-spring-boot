DO
$$
    BEGIN
        alter table direct_execution
            ADD COLUMN IF NOT EXISTS signature_uri TEXT,
            ADD COLUMN IF NOT EXISTS responsible   TEXT,
            ADD COLUMN IF NOT EXISTS sign_date     timestamp;

        alter table pre_measurement_street
            add column if not exists current_supply         text,
            add column if not exists installation_latitude  DOUBLE PRECISION,
            add column if not exists installation_longitude DOUBLE PRECISION;

        alter table pre_measurement_street_item
            add column if not exists quantity_executed numeric not null default 0;

        CREATE TABLE IF NOT EXISTS material_subtype
        (
            subtype_id   SERIAL PRIMARY KEY,
            type_id      BIGINT NOT NULL,
            subtype_name TEXT   NOT NULL,
            FOREIGN KEY (type_id) REFERENCES material_type (id_type)
        );

        create unique index IF NOT EXISTS material_subtype_type_id_subtype_name_uindex
            on material_subtype (type_id, subtype_name);

        UPDATE material_type set type_name = 'POSTE' where id_type = 36;
        UPDATE material_type set type_name = 'LUMINÁRIA' where id_type = 6;
        UPDATE MATERIAL SET id_material_type = 36 WHERE id_material_type = 37;

        IF NOT EXISTS (SELECT 1 FROM material_subtype) THEN
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 3, 'CONDUTOR');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 3, 'PP');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 3, 'FLEXÍVEL');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 20, 'GALVANIZADO');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 20, 'SUPORTE');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 30, 'ADESIVO');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 30, 'AUTOFUSÃO');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 35, 'TORÇAO');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 35, 'PERFURANTE');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 36, 'AÇO');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 36, 'CIMENTO');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 36, 'ORNAMENTAL GALVANIZADO');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 19, 'AÇO');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 35, 'CUNHA');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 6, 'LED');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 18, 'MERCÚRIO');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 18, 'SÓDIO');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 18, 'VAPOR DE SÓDIO');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 18, 'VAPOR METÁLICO');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 12, 'EXTERNO');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 12, 'MERCÚRIO EXTERNO');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 12, 'SÓDIO INTERNO');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 28, 'LED');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 4, 'FOTOCONTROLADOR');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 26, 'PORCELANA');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 20, 'PROJEÇÃO HORIZONTAL');
            INSERT INTO public.material_subtype (subtype_id, type_id, subtype_name) VALUES (DEFAULT, 18, 'VAPOR DE MERCÚRIO');
        END IF;

        alter table material
            add if not exists subtype_id bigint
                constraint material_material_subtype_subtype_id_fk
                    references material_subtype;

        alter table material
            add if not exists material_weight text;

        UPDATE public.material
        SET subtype_id = 26
        WHERE id_material = 124;

        UPDATE public.material
        SET subtype_id = 22
        WHERE id_material = 110;

        UPDATE public.material
        SET subtype_id = 9
        WHERE id_material = 138;

        UPDATE public.material
        SET subtype_id = 11
        WHERE id_material = 319;

        UPDATE public.material
        SET subtype_id = 7
        WHERE id_material = 344;

        UPDATE public.material
        SET subtype_id = 13
        WHERE id_material = 332;

        UPDATE public.material
        SET subtype_id = 16
        WHERE id_material = 144;

        UPDATE public.material
        SET subtype_id = 16
        WHERE id_material = 145;

        UPDATE public.material
        SET subtype_id = 25
        WHERE id_material = 120;

        UPDATE public.material
        SET subtype_id = 15
        WHERE id_material = 86;

        UPDATE public.material
        SET subtype_id = 21
        WHERE id_material = 107;

        UPDATE public.material
        SET subtype_id = 16
        WHERE id_material = 142;

        UPDATE public.material
        SET subtype_id = 13
        WHERE id_material = 130;

        UPDATE public.material
        SET subtype_id = 17
        WHERE id_material = 146;

        UPDATE public.material
        SET subtype_id = 15
        WHERE id_material = 92;

        UPDATE public.material
        SET subtype_id = 15
        WHERE id_material = 89;

        UPDATE public.material
        SET subtype_id = 17
        WHERE id_material = 148;

        UPDATE public.material
        SET subtype_id = 13
        WHERE id_material = 127;

        UPDATE public.material
        SET subtype_id = 17
        WHERE id_material = 149;

        UPDATE public.material
        SET subtype_id = 26
        WHERE id_material = 122;

        UPDATE public.material
        SET subtype_id = 18
        WHERE id_material = 141;

        UPDATE public.material
        SET subtype_id = 8
        WHERE id_material = 137;

        UPDATE public.material
        SET subtype_id = 16
        WHERE id_material = 143;

        UPDATE public.material
        SET subtype_id = 14
        WHERE id_material = 136;

        UPDATE public.material
        SET subtype_id = 14
        WHERE id_material = 135;

        UPDATE public.material
        SET subtype_id = 8
        WHERE id_material = 343;

        UPDATE public.material
        SET subtype_id = 2
        WHERE id_material = 326;

        UPDATE public.material
        SET subtype_id = 13
        WHERE id_material = 131;

        UPDATE public.material
        SET subtype_id = 13
        WHERE id_material = 128;

        UPDATE public.material
        SET subtype_id = 15
        WHERE id_material = 83;

        UPDATE public.material
        SET subtype_id = 15
        WHERE id_material = 97;

        UPDATE public.material
        SET subtype_id = 18
        WHERE id_material = 156;

        UPDATE public.material
        SET subtype_id = 23
        WHERE id_material = 114;

        UPDATE public.material
        SET subtype_id = 23
        WHERE id_material = 112;

        UPDATE public.material
        SET subtype_id = 23
        WHERE id_material = 115;

        UPDATE public.material
        SET subtype_id = 6
        WHERE id_material = 345;

        UPDATE public.material
        SET subtype_id = 18
        WHERE id_material = 158;

        UPDATE public.material
        SET subtype_id = 12
        WHERE id_material = 325;

        UPDATE public.material
        SET subtype_id = 23
        WHERE id_material = 113;

        UPDATE public.material
        SET subtype_id = 11
        WHERE id_material = 321;

        UPDATE public.material
        SET subtype_id = 6
        WHERE id_material = 140;

        UPDATE public.material
        SET subtype_id = 18
        WHERE id_material = 154;

        UPDATE public.material
        SET subtype_id = 19
        WHERE id_material = 152;

        UPDATE public.material
        SET subtype_id = 15
        WHERE id_material = 90;

        UPDATE public.material
        SET subtype_id = 23
        WHERE id_material = 111;

        UPDATE public.material
        SET subtype_id = 11
        WHERE id_material = 320;

        UPDATE public.material
        SET subtype_id = 13
        WHERE id_material = 134;

        UPDATE public.material
        SET subtype_id = 13
        WHERE id_material = 129;

        UPDATE public.material
        SET subtype_id = 24
        WHERE id_material = 116;

        UPDATE public.material
        SET subtype_id = 9
        WHERE id_material = 342;

        UPDATE public.material
        SET subtype_id = 25
        WHERE id_material = 119;

        UPDATE public.material
        SET subtype_id = 22
        WHERE id_material = 109;

        UPDATE public.material
        SET subtype_id = 18
        WHERE id_material = 155;

        UPDATE public.material
        SET subtype_id = 13
        WHERE id_material = 331;

        UPDATE public.material
        SET subtype_id = 20
        WHERE id_material = 106;

        UPDATE public.material
        SET subtype_id = 13
        WHERE id_material = 330;

        UPDATE public.material
        SET subtype_id = 17
        WHERE id_material = 150;

        UPDATE public.material
        SET subtype_id = 26
        WHERE id_material = 123;

        UPDATE public.material
        SET subtype_id = 17
        WHERE id_material = 147;

        UPDATE public.material
        SET subtype_id = 25
        WHERE id_material = 118;

        UPDATE public.material
        SET subtype_id = 13
        WHERE id_material = 132;

        UPDATE public.material
        SET subtype_id = 12
        WHERE id_material = 323;

        UPDATE public.material
        SET subtype_id = 13
        WHERE id_material = 341;

        UPDATE public.material
        SET subtype_id = 21
        WHERE id_material = 108;

        UPDATE public.material
        SET subtype_id = 10
        WHERE id_material = 349;

        UPDATE public.material
        SET subtype_id = 15
        WHERE id_material = 85;

        UPDATE public.material
        SET subtype_id = 13
        WHERE id_material = 333;

        UPDATE public.material
        SET subtype_id = 10
        WHERE id_material = 348;

        UPDATE public.material
        SET subtype_id = 15
        WHERE id_material = 88;

        UPDATE public.material
        SET subtype_id = 15
        WHERE id_material = 328;

        UPDATE public.material
        SET subtype_id = 15
        WHERE id_material = 84;

        UPDATE public.material
        SET subtype_id = 19
        WHERE id_material = 159;

        UPDATE public.material
        SET subtype_id = 12
        WHERE id_material = 324;

        UPDATE public.material
        SET subtype_id = 13
        WHERE id_material = 133;

        UPDATE public.material
        SET subtype_id = 5
        WHERE id_material = 338;

        UPDATE public.material
        SET subtype_id = 13
        WHERE id_material = 340;

        UPDATE public.material
        SET subtype_id = 3
        WHERE id_material = 125;

        UPDATE public.material
        SET subtype_id = 12
        WHERE id_material = 322;

        UPDATE public.material
        SET subtype_id = 27
        WHERE id_material = 153;

        DELETE
        FROM public.material_type
        WHERE id_type = 8;

        DELETE
        FROM public.material_type
        WHERE id_type = 23;

        DELETE
        FROM public.material_type
        WHERE id_type = 17;

        DELETE
        FROM public.material_type
        WHERE id_type = 34;

        DELETE
        FROM public.material_type
        WHERE id_type = 7;

        DELETE
        FROM public.material_type
        WHERE id_type = 10;

        DELETE
        FROM public.material_type
        WHERE id_type = 11;

        DELETE
        FROM public.material_type
        WHERE id_type = 9;

        DELETE
        FROM public.material_type
        WHERE id_type = 24;

        DELETE
        FROM public.material_type
        WHERE id_type = 33;

        DELETE
        FROM public.material_type
        WHERE id_type = 37;

        DELETE
        FROM public.material_type
        WHERE id_type = 25;

        UPDATE public.material_type
        SET type_name = 'FUSÍVEL'
        WHERE id_type = 5;

        UPDATE public.material_type
        SET type_name = 'MOTOR'
        WHERE id_type = 2;

        UPDATE public.material_type
        SET type_name = 'FITA ISOLANTE'
        WHERE id_type = 30;

        UPDATE public.material_type
        SET type_name = 'DISJUNTOR'
        WHERE id_type = 1;

        UPDATE MATERIAL SET id_material_type = 36 WHERE id_material_type = 40;
        UPDATE MATERIAL SET id_material_type = 30 WHERE id_material_type = 38;

        DELETE
        FROM public.material_type
        WHERE id_type = 40;

        DELETE
        FROM public.material_type
        WHERE id_type = 38;

        UPDATE material set parent_material_id = null;
        delete from material where is_generic = true;

        UPDATE public.material
        SET material_model = 'CP-II-E32'
        WHERE id_material = 339;

        UPDATE public.material
        SET material_model = 'ITEM 4 VM'
        WHERE id_material = 136;

        UPDATE public.material
        SET material_model = 'E-27'
        WHERE id_material = 118;

        UPDATE public.material
        SET material_model = 'G12'
        WHERE id_material = 120;

        UPDATE public.material
        SET material_model = 'ITEM 3 VM'
        WHERE id_material = 135;

        UPDATE public.material
        SET material_model = 'TIPO C'
        WHERE id_material = 338;

        UPDATE public.material
        SET material_model = 'E-40'
        WHERE id_material = 119;

        UPDATE public.material
        SET material_model = 'G12'
        WHERE id_material = 152;

        if not exists(select 1 from information_schema.columns where column_name = 'tenant_id' and table_name = 'material') then
            ALTER TABLE material
                ADD COLUMN tenant_id UUID,
                ADD CONSTRAINT material_sku_tenant_f_key
                    FOREIGN KEY (tenant_id)
                        REFERENCES tenant (tenant_id);

            UPDATE material
            SET tenant_id = (SELECT tenant_id from tenant limit 1);

            ALTER TABLE material
                ALTER COLUMN tenant_id SET NOT NULL;
        end if;

        if not exists(select 1 from information_schema.columns where column_name = 'tenant_id' and table_name = 'contract_reference_item') then
            ALTER TABLE contract_reference_item
                ADD COLUMN tenant_id UUID,
                ADD CONSTRAINT contract_reference_item_tenant_f_key
                    FOREIGN KEY (tenant_id)
                        REFERENCES tenant (tenant_id);

            UPDATE contract_reference_item
            SET tenant_id = (SELECT tenant_id from tenant limit 1);

            ALTER TABLE contract_reference_item
                ALTER COLUMN tenant_id SET NOT NULL;
        end if;

        -- generic
        IF NOT EXISTS(select 1 from material where is_generic = true) THEN
            alter table material
                alter column id_material_type drop not null;

            INSERT INTO material(
                material_name,
                is_generic,
                id_material_type,
                subtype_id,
                tenant_id
            )
            SELECT
                distinct base_name AS material_name,
                         true,
                         m.id_material_type,
                         m.subtype_id,
                         m.tenant_id
            FROM (
                     SELECT
                         m.*,
                         UPPER(
                                 REGEXP_REPLACE(
                                         TRIM(
                                                 CONCAT_WS(
                                                         ' ',
                                                         t.type_name,
                                                         s.subtype_name
                                                 )
                                         ),
                                         '\s+',
                                         ' ',
                                         'g'
                                 )
                         ) AS base_name
                     FROM material m
                              JOIN material_type t on t.id_type = m.id_material_type
                              left join material_subtype s on s.subtype_id = m.subtype_id
                 ) m;

            UPDATE material m
            SET parent_material_id = m2.id_material
            FROM material m2
            WHERE m.id_material_type = m2.id_material_type
              AND m2.subtype_id IS NOT DISTINCT FROM m.subtype_id
              AND m2.is_generic = true;
        END IF;

        WITH valid_material AS (
            SELECT m.id_material, t.type_name, s.subtype_name,
                   m.material_function, m.material_model, m.material_brand,
                   m.material_amps, m.material_length, m.material_width,
                   m.material_power, m.material_gauge, m.material_weight
            FROM material m
                     JOIN material_type t ON m.id_material_type = t.id_type
                     LEFT JOIN material_subtype s ON m.subtype_id = s.subtype_id
            WHERE m.is_generic = false
        )
        UPDATE material m
        SET material_name = UPPER(
                REGEXP_REPLACE(
                        TRIM(
                                CONCAT_WS(
                                        ' ',
                                        vm.type_name,
                                        vm.subtype_name,
                                        vm.material_function,
                                        vm.material_model,
                                        vm.material_brand,
                                        vm.material_amps,
                                        vm.material_length,
                                        vm.material_width,
                                        vm.material_power,
                                        vm.material_gauge,
                                        vm.material_weight
                                )
                        ),
                        '\s+',
                        ' ',
                        'g'
                )
                            )
        FROM valid_material vm
        WHERE m.id_material = vm.id_material;

        create index if not exists contract_reference_item_description_index
            on contract_reference_item (description);

        create unique index if not exists contract_reference_item_description_uindex
            on contract_reference_item (description, tenant_id);

        create table if not exists item_rule_distribution
        (
            item_rule_distribution_id serial
                constraint item_rule_distribution_pk
                    primary key,
            description                      text not null,
            tenant_id                 uuid
                constraint item_rule_distribution_tenant_tenant_id_fk
                    references tenant
        );

        create index if not exists item_rule_distribution_tenant_id_index
            on item_rule_distribution (tenant_id);

        if not exists(select 1 from item_rule_distribution) then
            INSERT INTO public.item_rule_distribution (item_rule_distribution_id, description, tenant_id)
            VALUES (DEFAULT, 'CABO FIO PP 2,5MM', 'f0dc9ab8-cb2c-4f21-a75f-05b122614862');

            INSERT INTO public.item_rule_distribution (item_rule_distribution_id, description, tenant_id)
            VALUES (DEFAULT, 'CABO FLEXÍVEL 1,5MM', 'f0dc9ab8-cb2c-4f21-a75f-05b122614862');

            INSERT INTO public.item_rule_distribution (item_rule_distribution_id, description, tenant_id)
            VALUES (DEFAULT, 'CABO 16MM²', 'f0dc9ab8-cb2c-4f21-a75f-05b122614862');

            INSERT INTO public.item_rule_distribution (item_rule_distribution_id, description, tenant_id)
            VALUES (DEFAULT, 'CABO 25MM²', 'f0dc9ab8-cb2c-4f21-a75f-05b122614862');

            INSERT INTO public.item_rule_distribution (item_rule_distribution_id, description, tenant_id)
            VALUES (DEFAULT, 'CABO FIO PP 3 X 2,5 MM COR DA COBERTURA PRETO', 'f0dc9ab8-cb2c-4f21-a75f-05b122614862');
        end if;

        alter table material
            drop column if exists cost_price;

        alter table material
            drop column if exists cost_per_item;

        alter table material
            add if not exists buy_unit text not null default 'UN';

        alter table material
            add if not exists request_unit text not null default 'UN';

    END;
$$;