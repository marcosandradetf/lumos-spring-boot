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

    end;
$$;